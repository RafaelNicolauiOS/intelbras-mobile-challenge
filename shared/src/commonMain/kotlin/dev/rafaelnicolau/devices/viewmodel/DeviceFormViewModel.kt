package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.viewModelScope
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFormErrors
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.DeviceValidator
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.DeviceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceFormUiState(
    val isEditing: Boolean = false,
    /** Editing: waiting for the stored device to prefill the fields. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val type: DeviceType = DeviceType.VIDEO,
    val name: String = "",
    val macAddress: String = "",
    val serial: String = "",
    val username: String = "",
    val password: String = "",
    val errors: DeviceFormErrors = DeviceFormErrors(),
)

/** Add (null [deviceId]) or edit a device. */
class DeviceFormViewModel(
    private val deviceId: String?,
    private val repository: DeviceRepository,
    private val messages: MessageCenter,
) : SharedViewModel() {

    private val state = MutableStateFlow(
        DeviceFormUiState(isEditing = deviceId != null, isLoading = deviceId != null),
    )
    val uiState: StateFlow<DeviceFormUiState> = state.asStateFlow()

    private val savedChannel = Channel<Unit>(Channel.BUFFERED)

    /** Fires once when the device was saved, so the coordinator can close the form. */
    val saved: Flow<Unit> = savedChannel.receiveAsFlow()

    init {
        if (deviceId != null) {
            viewModelScope.launch { prefill(repository.observeDevice(deviceId).first()) }
        }
    }

    private fun prefill(device: Device?) = state.update { current ->
        when (device) {
            is Device.Alarm -> current.copy(
                isLoading = false,
                type = DeviceType.ALARM,
                name = device.name,
                macAddress = DeviceValidator.formatMac(device.macAddress),
                password = device.password,
            )
            is Device.Video -> current.copy(
                isLoading = false,
                type = DeviceType.VIDEO,
                name = device.name,
                serial = device.serial,
                username = device.username,
                password = device.password,
            )
            null -> current.copy(isLoading = false)
        }
    }

    /** The type picks the API resource, so it can only be chosen when adding. */
    fun onTypeChange(type: DeviceType) {
        if (deviceId == null) {
            state.update { if (type == it.type) it else it.copy(type = type, errors = DeviceFormErrors()) }
        }
    }

    // A field's error is cleared only when its text really changes, so a UI toolkit
    // re-sending the current text cannot wipe an error that was just shown.

    fun onNameChange(value: String) = state.update {
        if (value == it.name) it else it.copy(name = value, errors = it.errors.copy(name = null))
    }

    fun onMacAddressChange(value: String) = state.update {
        if (value == it.macAddress) it else it.copy(macAddress = value, errors = it.errors.copy(macAddress = null))
    }

    fun onSerialChange(value: String) = state.update {
        if (value == it.serial) it else it.copy(serial = value, errors = it.errors.copy(serial = null))
    }

    fun onUsernameChange(value: String) = state.update {
        if (value == it.username) it else it.copy(username = value, errors = it.errors.copy(username = null))
    }

    fun onPasswordChange(value: String) = state.update {
        if (value == it.password) it else it.copy(password = value, errors = it.errors.copy(password = null))
    }

    fun submit() {
        val current = state.value
        if (current.isSaving || current.isLoading) return

        val draft = current.toDraft()
        val errors = DeviceValidator.validate(draft)
        if (errors.hasErrors) {
            state.update { it.copy(errors = errors) }
            return
        }

        state.update { it.copy(isSaving = true, errors = DeviceFormErrors()) }
        viewModelScope.launch {
            val outcome = if (deviceId == null) repository.create(draft) else repository.update(deviceId, draft)
            when (outcome) {
                is Outcome.Success -> {
                    val kind = if (deviceId == null) SuccessKind.DEVICE_CREATED else SuccessKind.DEVICE_UPDATED
                    messages.post(UserMessage.Success(kind))
                    savedChannel.send(Unit)
                }
                is Outcome.Failure -> {
                    state.update { it.copy(isSaving = false, errors = it.conflictErrors(outcome.error)) }
                    messages.post(UserMessage.Failure(outcome.error))
                }
            }
        }
    }

    private fun DeviceFormUiState.toDraft(): DeviceDraft = when (type) {
        DeviceType.ALARM -> DeviceDraft.Alarm(
            name = name.trim(),
            macAddress = DeviceValidator.normalizeMac(macAddress),
            password = password.trim(),
        )
        DeviceType.VIDEO -> DeviceDraft.Video(
            name = name.trim(),
            serial = serial.trim(),
            username = username.trim(),
            password = password,
        )
    }

    /** A 409 means the unique field of this type is taken: point at it. */
    private fun DeviceFormUiState.conflictErrors(error: DeviceError): DeviceFormErrors = when {
        error != DeviceError.CONFLICT -> errors
        type == DeviceType.ALARM -> errors.copy(macAddress = FieldError.DUPLICATE)
        else -> errors.copy(serial = FieldError.DUPLICATE)
    }
}
