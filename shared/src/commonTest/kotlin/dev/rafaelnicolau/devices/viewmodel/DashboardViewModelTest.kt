package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.DeviceRepository
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
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
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm)), MessageCenter())

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun exposesDevicesFromRepository() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), MessageCenter())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf(alarm, video), state.devices)
    }

    @Test
    fun toggleFavoriteMarksTheDevice() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), MessageCenter())

        viewModel.toggleFavorite(alarm)

        val state = viewModel.uiState.first { state -> state.devices.any { it.isFavorite } }
        assertEquals(listOf("a1"), state.devices.filter { it.isFavorite }.map { it.id })
    }

    @Test
    fun failedSyncKeepsCachedDevicesAndReportsTheError() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm))
        repository.nextOutcome = Outcome.Failure(DeviceError.NETWORK)
        val viewModel = DashboardViewModel(repository, MessageCenter())

        val state = viewModel.uiState.first { it.error != null }

        assertEquals(DeviceError.NETWORK, state.error)
        assertEquals(listOf(alarm), state.devices)
    }

    @Test
    fun emptyDatabaseStopsLoadingOnceTheFirstSyncFinishes() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(), MessageCenter())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(emptyList(), state.devices)
        assertNull(state.error)
    }

    @Test
    fun retryClearsThePreviousError() = runTest {
        val repository = InMemoryDeviceRepository()
        repository.nextOutcome = Outcome.Failure(DeviceError.TIMEOUT)
        val viewModel = DashboardViewModel(repository, MessageCenter())
        viewModel.uiState.first { it.error != null }

        repository.nextOutcome = Outcome.Success(Unit)
        viewModel.refresh()

        val state = viewModel.uiState.first { it.error == null && !it.isRefreshing }
        assertNull(state.error)
    }

    @Test
    fun deleteRemovesTheDeviceAndReportsSuccess() = runTest {
        val messages = MessageCenter()
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), messages)

        viewModel.delete(alarm)

        assertEquals(UserMessage.Success(SuccessKind.DEVICE_DELETED), messages.messages.first())
        assertEquals(listOf(video), viewModel.uiState.first { !it.isLoading }.devices)
    }

    @Test
    fun failedDeleteKeepsTheDeviceAndReportsTheError() = runTest {
        val messages = MessageCenter()
        val repository = InMemoryDeviceRepository(listOf(alarm))
        val viewModel = DashboardViewModel(repository, messages)
        viewModel.uiState.first { !it.isRefreshing }
        repository.nextOutcome = Outcome.Failure(DeviceError.NOT_FOUND)

        viewModel.delete(alarm)

        assertEquals(UserMessage.Failure(DeviceError.NOT_FOUND), messages.messages.first())
        assertEquals(listOf(alarm), viewModel.uiState.first { !it.isLoading }.devices)
    }

    @Test
    fun searchAndChipNarrowTheListTogether() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), MessageCenter())

        viewModel.onFilterSelected(DeviceFilter.CAMERAS)
        assertEquals(listOf(video), viewModel.uiState.first { it.filter == DeviceFilter.CAMERAS }.devices)

        viewModel.onQueryChange("casa")
        val state = viewModel.uiState.first { it.query == "casa" }
        assertEquals(emptyList(), state.devices)
        assertTrue(state.hasAnyDevice, "an empty search result is not an empty database")
    }

    @Test
    fun favoritesChipFollowsFavoriteChanges() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), MessageCenter())
        viewModel.onFilterSelected(DeviceFilter.FAVORITES)
        assertEquals(emptyList(), viewModel.uiState.first { it.filter == DeviceFilter.FAVORITES }.devices)

        viewModel.toggleFavorite(video)

        assertEquals(listOf("v1"), viewModel.uiState.first { it.devices.isNotEmpty() }.devices.map { it.id })
    }

    @Test
    fun countsEveryStoredDeviceRegardlessOfTheSearch() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm, video)), MessageCenter())

        viewModel.onQueryChange("garagem")

        val state = viewModel.uiState.first { it.query == "garagem" }
        assertEquals(2, state.totalCount)
        assertEquals(listOf("v1"), state.devices.map { it.id })
    }

    @Test
    fun emptyDatabaseHasNoDevices() = runTest {
        val state = DashboardViewModel(InMemoryDeviceRepository(), MessageCenter()).uiState.first { !it.isLoading }

        assertEquals(0, state.totalCount)
        assertTrue(!state.hasAnyDevice)
    }

    @Test
    fun startsWithNoSearchAndTheAllChip() = runTest {
        val state = DashboardViewModel(InMemoryDeviceRepository(), MessageCenter()).uiState.value

        assertEquals("", state.query)
        assertEquals(DeviceFilter.ALL, state.filter)
    }

    /** Holds every sync until the test opens [gate], so the "refreshing" state can be observed. */
    private class SlowRefreshRepository(
        private val inner: InMemoryDeviceRepository,
    ) : DeviceRepository by inner {
        var gate = CompletableDeferred<Unit>()

        override suspend fun refresh(): Outcome<Unit> {
            gate.await()
            return inner.refresh()
        }
    }

    @Test
    fun refreshReportsProgressUntilTheSyncEnds() = runTest {
        val repository = SlowRefreshRepository(InMemoryDeviceRepository(listOf(alarm)))
        repository.gate.complete(Unit)
        val viewModel = DashboardViewModel(repository, MessageCenter())
        viewModel.uiState.first { !it.isRefreshing && !it.isLoading }

        repository.gate = CompletableDeferred()
        viewModel.refresh()

        val during = viewModel.uiState.first { it.isRefreshing }
        assertEquals(listOf(alarm), during.devices, "cached devices stay on screen while syncing")
        repository.gate.complete(Unit)
        assertTrue(!viewModel.uiState.first { !it.isRefreshing }.isRefreshing)
    }

    @Test
    fun togglingAFavoriteTwiceUnmarksIt() = runTest {
        val viewModel = DashboardViewModel(InMemoryDeviceRepository(listOf(alarm)), MessageCenter())
        viewModel.toggleFavorite(alarm)
        val marked = viewModel.uiState.first { it.devices.firstOrNull()?.isFavorite == true }.devices.single()

        viewModel.toggleFavorite(marked)

        val unmarked = viewModel.uiState.first { it.devices.firstOrNull()?.isFavorite == false }
        assertTrue(unmarked.devices.none { it.isFavorite })
    }
}
