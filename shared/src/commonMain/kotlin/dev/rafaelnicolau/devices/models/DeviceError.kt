package dev.rafaelnicolau.devices.models

/** Why an operation against the API failed, in terms the UI can explain to the user. */
enum class DeviceError {
    /** No connection, DNS failure, connection dropped. */
    NETWORK,
    TIMEOUT,

    /** Missing, invalid or expired token (HTTP 401/403). */
    UNAUTHORIZED,

    /** The device no longer exists on the server (HTTP 404). */
    NOT_FOUND,

    /** Another device already uses this MAC address or serial number (HTTP 409). */
    CONFLICT,
    UNKNOWN,
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>

    data class Failure(val error: DeviceError) : Outcome<Nothing>
}
