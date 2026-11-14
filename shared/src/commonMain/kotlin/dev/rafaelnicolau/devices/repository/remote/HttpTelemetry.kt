package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.telemetry.SpanKind
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryKeys
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin

internal class HttpTelemetryConfig {
    var telemetry: Telemetry = Telemetry.disabled
}

/**
 * One CLIENT span per HTTP request, named like `GET /alarm-centrals/{id}`, with the method,
 * the path template, the host and the status. Network failures and 4xx/5xx answers mark the
 * span as an error. Inside another span (e.g. a sync) the request becomes its child.
 */
internal val HttpTelemetry = createClientPlugin("HttpTelemetry", ::HttpTelemetryConfig) {
    val telemetry = pluginConfig.telemetry

    on(Send) { request ->
        val url = request.url.build()
        val template = urlTemplate(url.encodedPath)
        val method = request.method.value
        val attributes = mapOf<String, Any>(
            TelemetryKeys.HTTP_METHOD to method,
            TelemetryKeys.URL_TEMPLATE to template,
            TelemetryKeys.SERVER_ADDRESS to url.host,
        )
        telemetry.span("$method $template", SpanKind.CLIENT, attributes) {
            val call = proceed(request)
            val status = call.response.status
            setAttribute(TelemetryKeys.HTTP_STATUS, status.value)
            if (status.value >= 400) {
                setAttribute(TelemetryKeys.ERROR_TYPE, status.value.toString())
                setError(status.description)
            }
            call
        }
    }
}

/**
 * The path with every segment after the resource name replaced by `{id}`, e.g.
 * `/alarm-centrals/3f2c…` → `/alarm-centrals/{id}`. Keeps one series per endpoint instead
 * of one per device, and keeps device ids out of telemetry.
 */
internal fun urlTemplate(path: String): String {
    val segments = path.split('/').filter { it.isNotEmpty() }
    return segments.mapIndexed { index, segment -> if (index == 0) segment else "{id}" }
        .joinToString("/", prefix = "/")
}
