package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to device data. ViewModels only talk to this interface; the
 * implementation decides where data comes from (local database synced with the API).
 */
interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>

    fun observeDevice(id: String): Flow<Device?>

    suspend fun setFavorite(id: String, favorite: Boolean)
}
