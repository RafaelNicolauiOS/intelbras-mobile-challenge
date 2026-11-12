package dev.rafaelnicolau.devices.models

/** The editable fields of a device: what the user fills in to create or update one. */
sealed interface DeviceDraft {
    val name: String

    data class Alarm(
        override val name: String,
        val macAddress: String,
        val password: String,
    ) : DeviceDraft

    data class Video(
        override val name: String,
        val serial: String,
        val username: String,
        val password: String,
    ) : DeviceDraft
}
