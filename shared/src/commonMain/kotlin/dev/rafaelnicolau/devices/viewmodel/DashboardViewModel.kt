package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    /** First load with nothing cached yet. */
    val isLoading: Boolean = true,
    /** A sync is running; cached devices, if any, stay on screen. */
    val isRefreshing: Boolean = false,
    val devices: List<Device> = emptyList(),
    /** Why the last sync failed, or null if it succeeded. */
    val error: DeviceError? = null,
)

class DashboardViewModel(
    private val repository: DeviceRepository,
) : SharedViewModel() {

    private data class SyncState(
        val isRefreshing: Boolean = true,
        val finishedOnce: Boolean = false,
        val error: DeviceError? = null,
    )

    private val sync = MutableStateFlow(SyncState())

    /** Devices always come from the local database; the sync only adds status around them. */
    val uiState: StateFlow<DashboardUiState> =
        combine(repository.observeDevices(), sync) { devices, sync ->
            DashboardUiState(
                isLoading = devices.isEmpty() && !sync.finishedOnce,
                isRefreshing = sync.isRefreshing,
                devices = devices,
                error = sync.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        refresh()
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
}
