package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Keeps devices in memory. Used by tests and previews, and as a stand-in until the database exists. */
class InMemoryDeviceRepository(initial: List<Device> = emptyList()) : DeviceRepository {
    private val devices = MutableStateFlow(initial)

    override fun observeDevices(): Flow<List<Device>> = devices

    override fun observeDevice(id: String): Flow<Device?> =
        devices.map { list -> list.firstOrNull { it.id == id } }

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
}
