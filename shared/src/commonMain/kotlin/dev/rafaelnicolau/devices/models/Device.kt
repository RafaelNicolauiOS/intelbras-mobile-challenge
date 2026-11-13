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

    /** What tells this device apart from others of its type, as shown under its name. */
    val identifier: String

    data class Alarm(
        override val id: String,
        override val name: String,
        override val isFavorite: Boolean = false,
        val macAddress: String,
        val password: String,
    ) : Device {
        override val type: DeviceType get() = DeviceType.ALARM

        // Computed once per device instead of on every row drawn: formatting uses a regex.
        override val identifier: String = DeviceValidator.formatMac(macAddress)

        /** The name as the search compares it; see [searchKey]. Not part of equals. */
        internal val normalizedName: String = name.searchable()
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
        override val identifier: String get() = serial

        internal val normalizedName: String = name.searchable()
    }
}

/**
 * The device name lowercased and without accents, computed when the device is created.
 * Searching then only compares strings, instead of normalizing every name on every keystroke.
 */
internal val Device.searchKey: String
    get() = when (this) {
        is Device.Alarm -> normalizedName
        is Device.Video -> normalizedName
    }
