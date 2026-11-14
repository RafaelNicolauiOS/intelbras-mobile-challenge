package dev.rafaelnicolau.devices.telemetry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * The OTLP/HTTP JSON encoding (https://opentelemetry.io/docs/specs/otlp/#json-protobuf-encoding),
 * limited to what the app sends. Details that matter to collectors:
 * - field names are lowerCamelCase;
 * - trace and span ids are lowercase hex strings;
 * - 64-bit integers (timestamps, intValue) are JSON strings.
 */

internal val otlpJson = Json {
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
internal data class OtlpTracesRequest(val resourceSpans: List<ResourceSpans>)

@Serializable
internal data class ResourceSpans(val resource: OtlpResource, val scopeSpans: List<ScopeSpans>)

@Serializable
internal data class ScopeSpans(val scope: OtlpScope, val spans: List<OtlpSpan>)

@Serializable
internal data class OtlpSpan(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val name: String,
    val kind: Int,
    val startTimeUnixNano: String,
    val endTimeUnixNano: String,
    val attributes: List<KeyValue>,
    val status: OtlpStatus,
)

@Serializable
internal data class OtlpStatus(val code: Int, val message: String? = null)

@Serializable
internal data class OtlpLogsRequest(val resourceLogs: List<ResourceLogs>)

@Serializable
internal data class ResourceLogs(val resource: OtlpResource, val scopeLogs: List<ScopeLogs>)

@Serializable
internal data class ScopeLogs(val scope: OtlpScope, val logRecords: List<OtlpLogRecord>)

@Serializable
internal data class OtlpLogRecord(
    val timeUnixNano: String,
    val severityNumber: Int,
    val severityText: String,
    val body: AnyValue,
    val attributes: List<KeyValue>,
)

@Serializable
internal data class OtlpResource(val attributes: List<KeyValue>)

@Serializable
internal data class OtlpScope(val name: String, val version: String)

@Serializable
internal data class KeyValue(val key: String, val value: AnyValue)

/** Exactly one field is set, as in the protobuf `oneof`. */
@Serializable
internal data class AnyValue(
    val stringValue: String? = null,
    val boolValue: Boolean? = null,
    val intValue: String? = null,
    val doubleValue: Double? = null,
)

internal const val INSTRUMENTATION_SCOPE = "dev.rafaelnicolau.devices"

internal fun Map<String, Any>.toKeyValues(): List<KeyValue> = map { (key, value) -> KeyValue(key, value.toAnyValue()) }

internal fun Any.toAnyValue(): AnyValue = when (this) {
    is Boolean -> AnyValue(boolValue = this)
    is Int -> AnyValue(intValue = toString())
    is Long -> AnyValue(intValue = toString())
    is Double -> AnyValue(doubleValue = this)
    else -> AnyValue(stringValue = toString())
}

/** SPAN_KIND_INTERNAL = 1, SPAN_KIND_CLIENT = 3. */
internal val SpanKind.otlp: Int
    get() = when (this) {
        SpanKind.INTERNAL -> 1
        SpanKind.CLIENT -> 3
    }

/** STATUS_CODE_UNSET = 0, STATUS_CODE_ERROR = 2. */
internal val SpanStatus.otlp: Int
    get() = when (this) {
        SpanStatus.UNSET -> 0
        SpanStatus.ERROR -> 2
    }

/** The first number of each OpenTelemetry severity range. */
internal val Severity.otlp: Int
    get() = when (this) {
        Severity.DEBUG -> 5
        Severity.INFO -> 9
        Severity.WARN -> 13
        Severity.ERROR -> 17
    }

internal fun tracesRequest(resource: OtlpResource, scope: OtlpScope, spans: List<SpanData>) = OtlpTracesRequest(
    listOf(
        ResourceSpans(
            resource = resource,
            scopeSpans = listOf(
                ScopeSpans(
                    scope = scope,
                    spans = spans.map { span ->
                        OtlpSpan(
                            traceId = span.traceId,
                            spanId = span.spanId,
                            parentSpanId = span.parentSpanId,
                            name = span.name,
                            kind = span.kind.otlp,
                            startTimeUnixNano = span.startEpochNanos.toString(),
                            endTimeUnixNano = span.endEpochNanos.toString(),
                            attributes = span.attributes.toKeyValues(),
                            status = OtlpStatus(span.status.otlp, span.statusMessage),
                        )
                    },
                ),
            ),
        ),
    ),
)

internal fun logsRequest(resource: OtlpResource, scope: OtlpScope, logs: List<LogData>) = OtlpLogsRequest(
    listOf(
        ResourceLogs(
            resource = resource,
            scopeLogs = listOf(
                ScopeLogs(
                    scope = scope,
                    logRecords = logs.map { log ->
                        OtlpLogRecord(
                            timeUnixNano = log.epochNanos.toString(),
                            severityNumber = log.severity.otlp,
                            severityText = log.severity.name,
                            body = AnyValue(stringValue = log.name),
                            attributes = log.attributes.toKeyValues(),
                        )
                    },
                ),
            ),
        ),
    ),
)
