package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailsViewModelTest {
    private val video = Device.Video(id = "v1", name = "Garagem", serial = "S1", username = "admin", password = "x")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsEmptyUntilTheDeviceIsRead() = runTest {
        val viewModel = DeviceDetailsViewModel("v1", InMemoryDeviceRepository(listOf(video)), MessageCenter())

        assertNull(viewModel.device.value)
    }

    @Test
    fun exposesTheStoredDevice() = runTest {
        val viewModel = DeviceDetailsViewModel("v1", InMemoryDeviceRepository(listOf(video)), MessageCenter())

        assertEquals(video, viewModel.device.first { it != null })
    }

    @Test
    fun unknownDeviceStaysNull() = runTest {
        val viewModel = DeviceDetailsViewModel("missing", InMemoryDeviceRepository(listOf(video)), MessageCenter())
        val job = launch { viewModel.device.collect {} }

        advanceUntilIdle()

        assertNull(viewModel.device.value)
        job.cancel()
    }

    @Test
    fun followsChangesToTheDevice() = runTest {
        val repository = InMemoryDeviceRepository(listOf(video))
        val viewModel = DeviceDetailsViewModel("v1", repository, MessageCenter())
        viewModel.device.first { it != null }

        repository.update("v1", DeviceDraft.Video("Garagem nova", "S1", "admin", "x"))

        assertEquals("Garagem nova", viewModel.device.first { it?.name == "Garagem nova" }?.name)
    }

    @Test
    fun deviceRemovedElsewhereBecomesNull() = runTest {
        val repository = InMemoryDeviceRepository(listOf(video))
        val viewModel = DeviceDetailsViewModel("v1", repository, MessageCenter())
        viewModel.device.first { it != null }

        repository.delete(video)

        assertNull(viewModel.device.first { it == null })
    }

    @Test
    fun copyingTellsTheUserItWorked() = runTest {
        val messages = MessageCenter()
        val viewModel = DeviceDetailsViewModel("v1", InMemoryDeviceRepository(listOf(video)), messages)

        viewModel.onCopied()

        assertEquals(UserMessage.Success(SuccessKind.COPIED), messages.messages.first())
    }

    @Test
    fun clearStopsTheViewModelWork() = runTest {
        val viewModel = DeviceDetailsViewModel("v1", InMemoryDeviceRepository(listOf(video)), MessageCenter())

        viewModel.clear()

        assertFalse(viewModel.viewModelScope.isActive)
    }
}
