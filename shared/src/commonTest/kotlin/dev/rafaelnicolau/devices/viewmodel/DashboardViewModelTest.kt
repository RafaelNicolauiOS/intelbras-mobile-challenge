package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val alarm = Device.Alarm(id = "a1", name = "Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    private val video = Device.Video(id = "v1", name = "Garagem", serial = "S1", username = "admin", password = "x")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsInLoadingState() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm)))

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun exposesDevicesFromRepository() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)))

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf(alarm, video), state.devices)
    }

    @Test
    fun toggleFavoriteMarksTheDevice() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)))

        viewModel.toggleFavorite(alarm)

        val state = viewModel.uiState.first { state -> state.devices.any { it.isFavorite } }
        assertEquals(listOf("a1"), state.devices.filter { it.isFavorite }.map { it.id })
    }
}
