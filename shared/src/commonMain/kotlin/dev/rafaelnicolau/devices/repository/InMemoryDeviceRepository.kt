package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Keeps devices in memory, with no network or database. Used by ViewModel tests and previews. */
class InMemoryDeviceRepository(initial: List<Device> = emptyList()) : DeviceRepository {
    private val devices = MutableStateFlow(initial)
    private var nextId = 1

    /** What the next server-backed operations answer; lets tests simulate failures. */
    var nextOutcome: Outcome<Unit> = Outcome.Success(Unit)

    override fun observeDevices(): Flow<List<Device>> = devices

    override fun observeDevice(id: String): Flow<Device?> =
        devices.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(): Outcome<Unit> = nextOutcome

    override suspend fun create(draft: DeviceDraft): Outcome<Unit> {
        if (nextOutcome is Outcome.Success) {
            devices.update { it + draft.toDevice(id = "local-${nextId++}", isFavorite = false) }
        }
        return nextOutcome
    }

    override suspend fun update(id: String, draft: DeviceDraft): Outcome<Unit> {
        if (nextOutcome is Outcome.Success) {
            devices.update { list ->
                list.map { if (it.id == id) draft.toDevice(id, it.isFavorite) else it }
            }
        }
        return nextOutcome
    }

    override suspend fun delete(device: Device): Outcome<Unit> {
        if (nextOutcome is Outcome.Success) {
            devices.update { list -> list.filterNot { it.id == device.id } }
        }
        return nextOutcome
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        devices.update { list ->
            list.map { device ->
                when {
                    device.id != id -> device
                    device is Device.Alarm -> device.copy(isFavorite = favorite)
                    device is Device.Video -> device.copy(isFavorite = favorite)
                    else -> device
                }
            }
        }
    }

    private fun DeviceDraft.toDevice(id: String, isFavorite: Boolean): Device = when (this) {
        is DeviceDraft.Alarm -> Device.Alarm(id, name, isFavorite, macAddress, password)
        is DeviceDraft.Video -> Device.Video(id, name, isFavorite, serial, username, password)
    }
}
