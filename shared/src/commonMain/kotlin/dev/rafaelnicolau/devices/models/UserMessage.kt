package dev.rafaelnicolau.devices.models

enum class SuccessKind { DEVICE_CREATED, DEVICE_UPDATED, DEVICE_DELETED, COPIED }

/** One-off feedback about an operation, shown to the user as a Snackbar/toast. */
sealed interface UserMessage {
    data class Success(val kind: SuccessKind) : UserMessage

    data class Failure(val error: DeviceError) : UserMessage

    /** The form cannot be saved because of [error], e.g. a serial number that is too short. */
    data class Invalid(val error: FieldError) : UserMessage
}
