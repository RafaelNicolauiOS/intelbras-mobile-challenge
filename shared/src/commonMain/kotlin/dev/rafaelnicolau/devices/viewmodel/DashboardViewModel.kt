package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
)

class DashboardViewModel(
    private val repository: DeviceRepository,
) : SharedViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.observeDevices()
        .map { devices -> DashboardUiState(isLoading = false, devices = devices) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun toggleFavorite(device: Device) {
        viewModelScope.launch {
            repository.setFavorite(device.id, !device.isFavorite)
        }
    }
}
