package dev.rafaelnicolau.devices.repository.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.rafaelnicolau.devices.models.DeviceType

/** Mirror of what the server knows about a device. Rewritten on every sync. */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val type: DeviceType,
    val name: String,
    @ColumnInfo(name = "mac_address") val macAddress: String? = null,
    val serial: String? = null,
    val username: String? = null,
    val password: String,
)

/**
 * Favorites exist only on this device. They live in their own table, which the sync
 * never writes, so refreshing server data cannot erase them.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "device_id") val deviceId: String,
)

data class DeviceWithFavorite(
    @Embedded val device: DeviceEntity,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
)
