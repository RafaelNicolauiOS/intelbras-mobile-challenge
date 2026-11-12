package dev.rafaelnicolau.devices.models

enum class DeviceType { ALARM, VIDEO }

/**
 * A device as the app sees it. The API exposes alarm centrals and video devices as two
 * separate resources; this model unifies them so the UI can list both together.
 */
sealed interface Device {
    val id: String
    val name: String
    val isFavorite: Boolean
    val type: DeviceType

    data class Alarm(
        override val id: String,
        override val name: String,
        override val isFavorite: Boolean = false,
        val macAddress: String,
        val password: String,
    ) : Device {
        override val type: DeviceType get() = DeviceType.ALARM
    }

    data class Video(
        override val id: String,
        override val name: String,
        override val isFavorite: Boolean = false,
        val serial: String,
        val username: String,
        val password: String,
    ) : Device {
        override val type: DeviceType get() = DeviceType.VIDEO
    }
}
