package dev.rafaelnicolau.devices.models

/** The dashboard chips. */
enum class DeviceFilter {
    ALL,
    FAVORITES,
    CAMERAS,
    ALARMS,
    ;

    fun matches(device: Device): Boolean = when (this) {
        ALL -> true
        FAVORITES -> device.isFavorite
        CAMERAS -> device.type == DeviceType.VIDEO
        ALARMS -> device.type == DeviceType.ALARM
    }
}

/** Devices that pass the chip [filter] and whose name contains [query], ignoring case and accents. */
fun List<Device>.filteredBy(query: String, filter: DeviceFilter): List<Device> {
    val needle = query.trim().searchable()
    return filter { device ->
        filter.matches(device) && (needle.isEmpty() || device.searchKey.contains(needle))
    }
}

/**
 * Lowercased and stripped of the accents used in Portuguese, so "camera" finds "Câmera".
 * Written with a StringBuilder and a `when` so no character is boxed: this runs for every
 * device name, and on Kotlin/Native (iOS) the boxed version was about 4x slower.
 */
internal fun String.searchable(): String {
    val lower = lowercase()
    val result = StringBuilder(lower.length)
    for (char in lower) result.append(char.withoutAccent())
    return result.toString()
}

private fun Char.withoutAccent(): Char = when (this) {
    'á', 'à', 'â', 'ã', 'ä' -> 'a'
    'é', 'è', 'ê', 'ë' -> 'e'
    'í', 'ì', 'î', 'ï' -> 'i'
    'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
    'ú', 'ù', 'û', 'ü' -> 'u'
    'ç' -> 'c'
    'ñ' -> 'n'
    else -> this
}
