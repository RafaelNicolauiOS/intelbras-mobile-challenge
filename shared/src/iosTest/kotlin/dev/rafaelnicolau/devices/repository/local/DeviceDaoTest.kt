package dev.rafaelnicolau.devices.repository.local

import androidx.room.Room
import dev.rafaelnicolau.devices.models.DeviceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The DAO's SQL against a real in-memory Room database (iOS simulator target). */
class DeviceDaoTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>().buildAppDatabase()
    private val dao = database.deviceDao()

    @AfterTest
    fun tearDown() = database.close()

    private fun alarm(id: String, name: String = id) =
        DeviceEntity(id = id, type = DeviceType.ALARM, name = name, macAddress = "1A2B3C4D5E6F", password = "1234")

    private fun video(id: String, name: String = id) =
        DeviceEntity(id = id, type = DeviceType.VIDEO, name = name, serial = "S-$id", username = "admin", password = "x")

    private suspend fun ids() = dao.observeAll().first().map { it.device.id }

    @Test
    fun upsertInsertsAndThenReplaces() = runTest {
        dao.upsert(listOf(alarm("a1", "Casa")))
        dao.upsert(listOf(alarm("a1", "Casa nova")))

        assertEquals(listOf("Casa nova"), dao.observeAll().first().map { it.device.name })
    }

    @Test
    fun replaceAllOfTypeOnlyTouchesThatType() = runTest {
        dao.upsert(listOf(alarm("a1"), alarm("a2"), video("v1")))

        dao.replaceAllOfType(DeviceType.ALARM, listOf(alarm("a2"), alarm("a3")))

        assertEquals(setOf("a2", "a3", "v1"), ids().toSet())
    }

    @Test
    fun replaceAllOfTypeDropsFavoritesOfRemovedDevicesOnly() = runTest {
        dao.upsert(listOf(alarm("a1"), alarm("a2")))
        dao.addFavorite(FavoriteEntity("a1"))
        dao.addFavorite(FavoriteEntity("a2"))

        dao.replaceAllOfType(DeviceType.ALARM, listOf(alarm("a2")))
        dao.upsert(listOf(alarm("a1")))

        val favorites = dao.observeAll().first().associate { it.device.id to it.isFavorite }
        assertEquals(mapOf("a1" to false, "a2" to true), favorites)
    }

    @Test
    fun observeByIdJoinsTheFavoriteMark() = runTest {
        dao.upsert(listOf(video("v1")))
        dao.addFavorite(FavoriteEntity("v1"))

        assertTrue(dao.observeById("v1").first()!!.isFavorite)
        assertNull(dao.observeById("nope").first())
    }

    @Test
    fun deleteDeviceRemovesRowAndFavorite() = runTest {
        dao.upsert(listOf(video("v1")))
        dao.addFavorite(FavoriteEntity("v1"))

        dao.deleteDevice("v1")
        dao.upsert(listOf(video("v1")))

        assertEquals(false, dao.observeById("v1").first()?.isFavorite)
    }

    @Test
    fun addingTheSameFavoriteTwiceIsIgnored() = runTest {
        dao.upsert(listOf(video("v1")))

        dao.addFavorite(FavoriteEntity("v1"))
        dao.addFavorite(FavoriteEntity("v1"))
        dao.removeFavorite("v1")

        assertEquals(false, dao.observeById("v1").first()?.isFavorite)
    }
}
