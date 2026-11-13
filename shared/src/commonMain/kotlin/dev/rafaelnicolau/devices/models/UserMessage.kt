package dev.rafaelnicolau.devices.models

enum class SuccessKind { DEVICE_CREATED, DEVICE_UPDATED, DEVICE_DELETED }

/** One-off feedback about an operation, shown to the user as a Snackbar/toast. */
sealed interface UserMessage {
    data class Success(val kind: SuccessKind) : UserMessage

    data class Failure(val error: DeviceError) : UserMessage
}
