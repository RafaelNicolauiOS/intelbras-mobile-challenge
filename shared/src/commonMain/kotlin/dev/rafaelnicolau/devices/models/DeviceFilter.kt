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
        filter.matches(device) && (needle.isEmpty() || device.name.searchable().contains(needle))
    }
}

private val accents = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c', 'ñ' to 'n',
)

/** Lowercased and stripped of the accents used in Portuguese, so "camera" finds "Câmera". */
private fun String.searchable(): String =
    lowercase().map { accents[it] ?: it }.joinToString("")
