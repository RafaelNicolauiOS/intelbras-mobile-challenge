package dev.rafaelnicolau.devices.telemetry

import dev.rafaelnicolau.devices.repository.remote.defaultTelemetryEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * How each app wants telemetry. Both apps read these from their untracked build
 * configuration, like the API token, and hand them over at startup.
 */
data class TelemetryConfig(
    val serviceName: String = "devices-app",
    val serviceVersion: String = "",
    /** e.g. `debug` or `release`; lets one backend keep both apart. */
    val environment: String = "",
    /** Base URL of an OTLP/HTTP receiver, e.g. `http://localhost:4318`. Empty: nothing is sent. */
    val otlpEndpoint: String = "",
    /**
     * Sent with every export, e.g. `api-key` for New Relic or `Authorization` for others.
     * Build it from configuration text with [parseOtlpHeaders]. Use a write-only key.
     */
    val otlpHeaders: Map<String, String> = emptyMap(),
    /** Also print spans and logs to the console. */
    val console: Boolean = false,
) {
    internal fun build(platform: String): Telemetry {
        val exporters = buildList {
            if (console) add(ConsoleExporter())
            if (otlpEndpoint.isNotBlank()) {
                add(
                    OtlpExporter(
                        endpoint = otlpEndpoint,
                        headers = otlpHeaders,
                        resource = resourceAttributes(platform),
                        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                        engine = defaultTelemetryEngine(),
                        // With the console on, each export reports its result, e.g. a rejected key.
                        report = if (console) ::println else null,
                    ),
                )
            }
        }
        return Telemetry(exporters)
    }

    /** Describes the app sending the data; resource attributes are not subject to the key allowlist. */
    internal fun resourceAttributes(platform: String): Map<String, Any> = buildMap {
        put("service.name", serviceName)
        if (serviceVersion.isNotBlank()) put("service.version", serviceVersion)
        if (environment.isNotBlank()) put("deployment.environment.name", environment)
        put("os.type", platform)
        put("telemetry.sdk.name", "devices-otlp")
        put("telemetry.sdk.language", "kotlin")
    }
}

/**
 * Reads headers written as `name=value,name2=value2`, the format of the standard
 * `OTEL_EXPORTER_OTLP_HEADERS` variable. Spaces around names and values are ignored and
 * entries without a name are skipped. A value may itself contain `=` (e.g. base64).
 */
fun parseOtlpHeaders(text: String): Map<String, String> =
    text.split(',')
        .mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = entry.substring(0, separator).trim()
            val value = entry.substring(separator + 1).trim()
            if (name.isEmpty()) null else name to value
        }
        .toMap()
