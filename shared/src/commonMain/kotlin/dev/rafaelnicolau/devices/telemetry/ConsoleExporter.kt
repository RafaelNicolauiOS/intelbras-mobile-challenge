package dev.rafaelnicolau.devices.telemetry

/**
 * Prints every span and log with the `[TELEMETRY]` prefix: Xcode's debug area on iOS,
 * Logcat on Android. For debug builds, to see telemetry without any backend.
 */
class ConsoleExporter(private val print: (String) -> Unit = ::println) : TelemetryExporter {
    override fun export(span: SpanData) {
        val status = if (span.status == SpanStatus.ERROR) " ERROR ${span.statusMessage.orEmpty()}" else ""
        val millis = (span.durationMillis * 10).toLong() / 10.0
        print("[TELEMETRY] span ${span.name} ${millis}ms$status ${span.attributes.describe()}".trimEnd())
    }

    override fun export(log: LogData) {
        print("[TELEMETRY] ${log.severity} ${log.name} ${log.attributes.describe()}".trimEnd())
    }

    private fun Map<String, Any>.describe() = if (isEmpty()) "" else entries.joinToString(", ", "{", "}")
}
