package dev.rafaelnicolau.devices.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.rafaelnicolau.devices.models.DeviceType
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query(
        """
        SELECT d.*, (f.device_id IS NOT NULL) AS is_favorite
        FROM devices d LEFT JOIN favorites f ON f.device_id = d.id
        ORDER BY d.name COLLATE NOCASE
        """,
    )
    fun observeAll(): Flow<List<DeviceWithFavorite>>

    @Query(
        """
        SELECT d.*, (f.device_id IS NOT NULL) AS is_favorite
        FROM devices d LEFT JOIN favorites f ON f.device_id = d.id
        WHERE d.id = :id
        """,
    )
    fun observeById(id: String): Flow<DeviceWithFavorite?>

    @Upsert
    suspend fun upsert(devices: List<DeviceEntity>)

    @Query("DELETE FROM devices WHERE type = :type AND id NOT IN (:ids)")
    suspend fun deleteOfTypeNotIn(type: DeviceType, ids: List<String>)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE device_id = :deviceId")
    suspend fun removeFavorite(deviceId: String)

    @Query("DELETE FROM favorites WHERE device_id NOT IN (SELECT id FROM devices)")
    suspend fun deleteOrphanFavorites()

    /**
     * Makes the local devices of one [type] match the server list: upserts what came,
     * removes what the server no longer has. Favorites are untouched, except those whose
     * device is gone.
     */
    @Transaction
    suspend fun replaceAllOfType(type: DeviceType, devices: List<DeviceEntity>) {
        upsert(devices)
        deleteOfTypeNotIn(type, devices.map { it.id })
        deleteOrphanFavorites()
    }

    @Transaction
    suspend fun deleteDevice(id: String) {
        deleteById(id)
        removeFavorite(id)
    }
}
