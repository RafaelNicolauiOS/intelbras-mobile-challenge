package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.repository.local.DeviceDao
import dev.rafaelnicolau.devices.repository.local.DeviceEntity
import dev.rafaelnicolau.devices.repository.local.FavoriteEntity
import dev.rafaelnicolau.devices.repository.remote.DeviceApi
import dev.rafaelnicolau.devices.repository.remote.apiCall
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryKeys
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultDeviceRepository(
    private val api: DeviceApi,
    private val dao: DeviceDao,
    private val telemetry: Telemetry = Telemetry.disabled,
) : DeviceRepository {

    override fun observeDevices(): Flow<List<Device>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeDevice(id: String): Flow<Device?> =
        dao.observeById(id).map { it?.toDomain() }

    /** Traced as `devices.refresh`: both requests become its children, with the counts and any error. */
    override suspend fun refresh(): Outcome<Unit> = telemetry.span("devices.refresh") {
        coroutineScope {
            val alarms = async { apiCall { api.getAlarmCentrals().map { it.toEntity() } } }
            val videos = async { apiCall { api.getVideoDevices().map { it.toEntity() } } }
            val fetchedAlarms = alarms.await()
            val fetchedVideos = videos.await()
            (fetchedAlarms as? Outcome.Success)?.let { setAttribute(TelemetryKeys.ALARM_COUNT, it.value.size) }
            (fetchedVideos as? Outcome.Success)?.let { setAttribute(TelemetryKeys.VIDEO_COUNT, it.value.size) }

            // Each type is reconciled on its own: if one endpoint fails, the other still
            // syncs and the failed type keeps its last known data instead of being wiped.
            val alarmResult = store(DeviceType.ALARM, fetchedAlarms)
            val videoResult = store(DeviceType.VIDEO, fetchedVideos)

            val result = alarmResult as? Outcome.Failure ?: videoResult
            if (result is Outcome.Failure) {
                setAttribute(TelemetryKeys.DEVICE_ERROR, result.error.name)
                setError(result.error.name)
            }
            result
        }
    }

    private suspend fun store(type: DeviceType, fetched: Outcome<List<DeviceEntity>>): Outcome<Unit> =
        when (fetched) {
            is Outcome.Failure -> fetched
            is Outcome.Success -> {
                dao.replaceAllOfType(type, fetched.value)
                Outcome.Success(Unit)
            }
        }

    override suspend fun create(draft: DeviceDraft): Outcome<Unit> = saveLocally(
        apiCall {
            when (draft) {
                is DeviceDraft.Alarm -> api.createAlarmCentral(draft.toBody()).toEntity()
                is DeviceDraft.Video -> api.createVideoDevice(draft.toBody()).toEntity()
            }
        },
    )

    override suspend fun update(id: String, draft: DeviceDraft): Outcome<Unit> = saveLocally(
        apiCall {
            when (draft) {
                is DeviceDraft.Alarm -> api.updateAlarmCentral(id, draft.toBody()).toEntity()
                is DeviceDraft.Video -> api.updateVideoDevice(id, draft.toBody()).toEntity()
            }
        },
    )

    /** The server's answer is what gets stored, so the UI only ever shows confirmed data. */
    private suspend fun saveLocally(saved: Outcome<DeviceEntity>): Outcome<Unit> = when (saved) {
        is Outcome.Failure -> saved
        is Outcome.Success -> {
            dao.upsert(listOf(saved.value))
            Outcome.Success(Unit)
        }
    }

    override suspend fun delete(device: Device): Outcome<Unit> {
        val result = apiCall {
            when (device) {
                is Device.Alarm -> api.deleteAlarmCentral(device.id)
                is Device.Video -> api.deleteVideoDevice(device.id)
            }
        }
        if (result is Outcome.Success) dao.deleteDevice(device.id)
        return result
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(FavoriteEntity(id)) else dao.removeFavorite(id)
    }
}
