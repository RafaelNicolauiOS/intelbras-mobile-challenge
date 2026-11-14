package dev.rafaelnicolau.devices.telemetry

/**
 * Every attribute key the app may send, following the OpenTelemetry semantic conventions
 * where one exists. [Telemetry] drops any other key: adding data to telemetry is a deliberate
 * change to this list, never a side effect of passing a map around.
 *
 * Deliberately absent: device names, MAC addresses, serial numbers, usernames, passwords,
 * the API token and request or response bodies.
 */
object TelemetryKeys {
    const val HTTP_METHOD = "http.request.method"
    const val HTTP_STATUS = "http.response.status_code"

    /** The path with ids replaced, e.g. `/alarm-centrals/{id}`, so every device shares one series. */
    const val URL_TEMPLATE = "url.template"
    const val SERVER_ADDRESS = "server.address"
    const val ERROR_TYPE = "error.type"

    const val DEVICE_ERROR = "device.error"
    const val ALARM_COUNT = "devices.alarm.count"
    const val VIDEO_COUNT = "devices.video.count"
    const val MESSAGE_KIND = "message.kind"

    val allowed: Set<String> = setOf(
        HTTP_METHOD,
        HTTP_STATUS,
        URL_TEMPLATE,
        SERVER_ADDRESS,
        ERROR_TYPE,
        DEVICE_ERROR,
        ALARM_COUNT,
        VIDEO_COUNT,
        MESSAGE_KIND,
    )
}
