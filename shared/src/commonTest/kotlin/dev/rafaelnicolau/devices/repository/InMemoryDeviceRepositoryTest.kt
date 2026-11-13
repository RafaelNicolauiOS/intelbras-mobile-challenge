package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryDeviceRepositoryTest {
    private val alarm = Device.Alarm(id = "a1", name = "Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    private val video = Device.Video(id = "v1", name = "Garagem", isFavorite = true, serial = "S1", username = "admin", password = "x")

    @Test
    fun startsWithTheGivenDevices() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm, video))

        assertEquals(listOf(alarm, video), repository.observeDevices().first())
        assertEquals(video, repository.observeDevice("v1").first())
        assertNull(repository.observeDevice("nope").first())
    }

    @Test
    fun createAddsDevicesWithDistinctIds() = runTest {
        val repository = InMemoryDeviceRepository()

        repository.create(DeviceDraft.Alarm("Casa", "1A2B3C4D5E6F", "1234"))
        repository.create(DeviceDraft.Video("Garagem", "S1", "admin", "x"))

        val devices = repository.observeDevices().first()
        assertIs<Device.Alarm>(devices[0])
        assertIs<Device.Video>(devices[1])
        assertNotEquals(devices[0].id, devices[1].id)
    }

    @Test
    fun updateReplacesTheDeviceButKeepsItsFavoriteMark() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm, video))

        repository.update("v1", DeviceDraft.Video("Garagem nova", "S2", "root", "y"))

        assertEquals(
            Device.Video("v1", "Garagem nova", isFavorite = true, serial = "S2", username = "root", password = "y"),
            repository.observeDevice("v1").first(),
        )
        assertEquals(alarm, repository.observeDevice("a1").first(), "other devices are untouched")
    }

    @Test
    fun deleteRemovesOnlyThatDevice() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm, video))

        repository.delete(alarm)

        assertEquals(listOf<Device>(video), repository.observeDevices().first())
    }

    @Test
    fun setFavoriteMarksAndUnmarks() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm, video))

        repository.setFavorite("a1", true)
        repository.setFavorite("v1", false)

        val devices = repository.observeDevices().first().associateBy { it.id }
        assertTrue(devices.getValue("a1").isFavorite)
        assertTrue(!devices.getValue("v1").isFavorite)
    }

    @Test
    fun failureOutcomeChangesNothing() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm))
        val failure = Outcome.Failure(DeviceError.UNKNOWN)
        repository.nextOutcome = failure

        assertEquals(failure, repository.refresh())
        assertEquals(failure, repository.create(DeviceDraft.Video("Garagem", "S1", "admin", "x")))
        assertEquals(failure, repository.update("a1", DeviceDraft.Alarm("Outro", "AAAAAAAAAAAA", "0000")))
        assertEquals(failure, repository.delete(alarm))
        assertEquals(listOf<Device>(alarm), repository.observeDevices().first())
    }
}
