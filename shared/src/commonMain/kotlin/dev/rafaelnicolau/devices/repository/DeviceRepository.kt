package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to device data. The local database is the single source of truth:
 * the `observe*` flows read from it, and every other operation ends by writing to it.
 */
interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>

    fun observeDevice(id: String): Flow<Device?>

    /** Fetches both device lists from the API and stores them locally. */
    suspend fun refresh(): Outcome<Unit>

    suspend fun create(draft: DeviceDraft): Outcome<Unit>

    suspend fun update(id: String, draft: DeviceDraft): Outcome<Unit>

    suspend fun delete(device: Device): Outcome<Unit>

    /** Favorites are local only; this never reaches the server. */
    suspend fun setFavorite(id: String, favorite: Boolean)
}
