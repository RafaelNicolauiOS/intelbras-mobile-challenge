package dev.rafaelnicolau.devices.models

enum class FieldError {
    REQUIRED,

    /** MAC must be 12 hexadecimal characters, without separators. */
    INVALID_MAC,

    /** Alarm central password must be exactly 4 or 6 digits. */
    INVALID_ALARM_PASSWORD,

    /** The server refused it because another device already uses this value. */
    DUPLICATE,
}

data class DeviceFormErrors(
    val name: FieldError? = null,
    val macAddress: FieldError? = null,
    val serial: FieldError? = null,
    val username: FieldError? = null,
    val password: FieldError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, macAddress, serial, username, password).any { it != null }
}

/** Client-side rules mirroring what the API accepts, so the user gets feedback before any request. */
object DeviceValidator {
    private val macRegex = Regex("^[0-9A-F]{12}$")
    private val alarmPasswordRegex = Regex("^(\\d{4}|\\d{6})$")

    /** Users often type MACs as `1a:2b:...` or `1A-2B-...`; the API wants `1A2B...`. */
    fun normalizeMac(input: String): String =
        input.trim().replace(":", "").replace("-", "").uppercase()

    /** The API stores `A0B1C2D3E4F5`; people read `A0:B1:C2:D3:E4:F5`. */
    fun formatMac(mac: String): String =
        if (macRegex.matches(mac)) mac.chunked(2).joinToString(":") else mac

    fun validate(draft: DeviceDraft): DeviceFormErrors = when (draft) {
        is DeviceDraft.Alarm -> DeviceFormErrors(
            name = required(draft.name),
            macAddress = required(draft.macAddress)
                ?: FieldError.INVALID_MAC.takeUnless { macRegex.matches(draft.macAddress) },
            password = required(draft.password)
                ?: FieldError.INVALID_ALARM_PASSWORD.takeUnless { alarmPasswordRegex.matches(draft.password) },
        )
        is DeviceDraft.Video -> DeviceFormErrors(
            name = required(draft.name),
            serial = required(draft.serial),
            username = required(draft.username),
            password = required(draft.password),
        )
    }

    private fun required(value: String): FieldError? = FieldError.REQUIRED.takeIf { value.isBlank() }
}
