package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.models.filteredBy
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.DeviceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    /** First load with nothing cached yet. */
    val isLoading: Boolean = true,
    /** A sync is running; cached devices, if any, stay on screen. */
    val isRefreshing: Boolean = false,
    /** The devices to list: already narrowed by [query] and [filter]. */
    val devices: List<Device> = emptyList(),
    /** False when nothing is stored at all, as opposed to nothing matching the search. */
    val hasAnyDevice: Boolean = false,
    /** How many devices are stored, regardless of the search. */
    val totalCount: Int = 0,
    val query: String = "",
    val filter: DeviceFilter = DeviceFilter.ALL,
    /** Why the last sync failed, or null if it succeeded. */
    val error: DeviceError? = null,
)

/** How long the search waits for the user to stop typing before filtering. */
internal const val SEARCH_DEBOUNCE_MS = 120L

/**
 * [computeDispatcher] runs the filtering, so a keystroke never waits for it: the screen
 * only records the new query and gets the filtered list back when it is ready. Tests pass
 * their own dispatcher to keep virtual time.
 */
class DashboardViewModel(
    private val repository: DeviceRepository,
    private val messages: MessageCenter,
    computeDispatcher: CoroutineDispatcher,
) : SharedViewModel() {

    constructor(repository: DeviceRepository, messages: MessageCenter) :
        this(repository, messages, Dispatchers.Default)

    private data class SyncState(
        val isRefreshing: Boolean = true,
        val finishedOnce: Boolean = false,
        val error: DeviceError? = null,
    )

    private val sync = MutableStateFlow(SyncState())
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(DeviceFilter.ALL)

    /** Devices always come from the local database; the sync only adds status around them. */
    val uiState: StateFlow<DashboardUiState> =
        combine(repository.observeDevices(), sync, debouncedQuery(), filter) { devices, sync, query, filter ->
            DashboardUiState(
                isLoading = devices.isEmpty() && !sync.finishedOnce,
                isRefreshing = sync.isRefreshing,
                devices = devices.filteredBy(query, filter),
                hasAnyDevice = devices.isNotEmpty(),
                totalCount = devices.size,
                query = query,
                filter = filter,
                error = sync.error,
            )
        }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** Waits for a pause in typing; an empty query (initial or cleared search) applies at once. */
    @OptIn(FlowPreview::class)
    private fun debouncedQuery(): Flow<String> =
        query.debounce { value -> if (value.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterSelected(value: DeviceFilter) {
        filter.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            sync.update { it.copy(isRefreshing = true) }
            val error = (repository.refresh() as? Outcome.Failure)?.error
            sync.value = SyncState(isRefreshing = false, finishedOnce = true, error = error)
        }
    }

    fun toggleFavorite(device: Device) {
        viewModelScope.launch {
            repository.setFavorite(device.id, !device.isFavorite)
        }
    }

    /** Removes on the server first; the local copy only goes away once that succeeds. */
    fun delete(device: Device) {
        viewModelScope.launch {
            val message = when (val outcome = repository.delete(device)) {
                is Outcome.Success -> UserMessage.Success(SuccessKind.DEVICE_DELETED)
                is Outcome.Failure -> UserMessage.Failure(outcome.error)
            }
            messages.post(message)
        }
    }
}
