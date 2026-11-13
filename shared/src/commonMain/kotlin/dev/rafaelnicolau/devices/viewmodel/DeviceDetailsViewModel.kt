package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DeviceDetailsViewModel(
    deviceId: String,
    repository: DeviceRepository,
    private val messages: MessageCenter,
) : SharedViewModel() {

    val device: StateFlow<Device?> = repository.observeDevice(deviceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The screen copied a value to the clipboard; tell the user it worked. */
    fun onCopied() {
        messages.post(UserMessage.Success(SuccessKind.COPIED))
    }
}
