package dev.rafaelnicolau.devices.repository

import androidx.room.Room
import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.buildAppDatabase
import dev.rafaelnicolau.devices.repository.remote.DeviceApi
import dev.rafaelnicolau.devices.repository.remote.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Exercises the repository against a real (in-memory) Room database and a fake HTTP server,
 * so the sync rules are verified through actual SQL. Runs on the iOS simulator target.
 */
class DefaultDeviceRepositoryTest {
    /** What the fake server answers; tests change these to simulate server-side changes and failures. */
    private var alarmsJson = """{"count":0,"data":[]}"""
    private var videosJson = """{"count":0,"data":[]}"""
    private var alarmsStatus = HttpStatusCode.OK
    private var writeStatus = HttpStatusCode.OK
    private var writeJson = "{}"

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>().buildAppDatabase()

    private val engine = MockEngine { request ->
        val isAlarms = request.url.encodedPath.startsWith("/alarm-centrals")
        val (body, status) = when {
            request.method != HttpMethod.Get -> writeJson to writeStatus
            isAlarms -> alarmsJson to alarmsStatus
            else -> videosJson to HttpStatusCode.OK
        }
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private val repository = DefaultDeviceRepository(
        api = DeviceApi(createHttpClient(ApiConfig("http://api.test", "token"), engine)),
        dao = database.deviceDao(),
    )

    @AfterTest
    fun tearDown() = database.close()

    private fun alarm(id: String, name: String) =
        """{"id":"$id","name":"$name","macAddress":"1A2B3C4D5E6F","password":"1234"}"""

    private fun video(id: String, name: String) =
        """{"id":"$id","name":"$name","serial":"S-$id","username":"admin","password":"x"}"""

    private fun list(vararg items: String) = """{"count":${items.size},"data":[${items.joinToString(",")}]}"""

    private suspend fun devices() = repository.observeDevices().first()

    @Test
    fun refreshStoresBothDeviceTypesInTheDatabase() = runTest {
        alarmsJson = list(alarm("a1", "Casa"))
        videosJson = list(video("v1", "Garagem"))

        val result = repository.refresh()

        assertIs<Outcome.Success<Unit>>(result)
        assertEquals(listOf("Casa", "Garagem"), devices().map { it.name })
        assertIs<Device.Alarm>(devices().first { it.id == "a1" })
        assertIs<Device.Video>(devices().first { it.id == "v1" })
    }

    @Test
    fun favoritesSurviveARefresh() = runTest {
        videosJson = list(video("v1", "Garagem"), video("v2", "Portão"))
        repository.refresh()
        repository.setFavorite("v1", favorite = true)

        // The server renames the device; the favorite mark is local and must stay.
        videosJson = list(video("v1", "Garagem nova"), video("v2", "Portão"))
        repository.refresh()

        val favorite = devices().single { it.isFavorite }
        assertEquals("v1", favorite.id)
        assertEquals("Garagem nova", favorite.name)
    }

    @Test
    fun devicesRemovedOnTheServerDisappearLocally() = runTest {
        videosJson = list(video("v1", "Garagem"), video("v2", "Portão"))
        repository.refresh()
        repository.setFavorite("v2", favorite = true)

        videosJson = list(video("v1", "Garagem"))
        repository.refresh()

        assertEquals(listOf("v1"), devices().map { it.id })

        // If the id ever comes back it must not be born as a favorite.
        videosJson = list(video("v1", "Garagem"), video("v2", "Portão"))
        repository.refresh()
        assertTrue(devices().none { it.isFavorite })
    }

    @Test
    fun failedRefreshKeepsTheLastSyncedDevices() = runTest {
        alarmsJson = list(alarm("a1", "Casa"))
        videosJson = list(video("v1", "Garagem"))
        repository.refresh()

        // Alarms endpoint fails; videos endpoint now reports a new device.
        alarmsStatus = HttpStatusCode.Unauthorized
        videosJson = list(video("v1", "Garagem"), video("v2", "Portão"))
        val result = repository.refresh()

        assertEquals(Outcome.Failure(DeviceError.UNAUTHORIZED), result)
        assertEquals(setOf("a1", "v1", "v2"), devices().map { it.id }.toSet())
    }

    @Test
    fun createStoresTheDeviceReturnedByTheServer() = runTest {
        writeJson = video("server-id", "Portão")

        val result = repository.create(DeviceDraft.Video("Portão", "S1", "admin", "x"))

        assertIs<Outcome.Success<Unit>>(result)
        assertEquals(listOf("server-id"), devices().map { it.id })
    }

    @Test
    fun failedCreateLeavesTheDatabaseUntouched() = runTest {
        writeStatus = HttpStatusCode.Conflict

        val result = repository.create(DeviceDraft.Video("Portão", "S1", "admin", "x"))

        assertEquals(Outcome.Failure(DeviceError.CONFLICT), result)
        assertTrue(devices().isEmpty())
    }

    @Test
    fun deleteRemovesTheDeviceOnlyAfterTheServerConfirms() = runTest {
        videosJson = list(video("v1", "Garagem"))
        repository.refresh()
        val device = devices().single()

        writeStatus = HttpStatusCode.InternalServerError
        assertIs<Outcome.Failure>(repository.delete(device))
        assertEquals(1, devices().size)

        writeStatus = HttpStatusCode.OK
        assertIs<Outcome.Success<Unit>>(repository.delete(device))
        assertTrue(devices().isEmpty())
    }
}
