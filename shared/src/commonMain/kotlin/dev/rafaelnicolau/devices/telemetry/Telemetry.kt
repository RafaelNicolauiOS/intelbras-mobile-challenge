package dev.rafaelnicolau.devices.telemetry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * The app's single entry point for observability: it times work as spans and records
 * events as logs, then hands them to every [TelemetryExporter]. Which backend receives
 * them (console, an OpenTelemetry collector, a vendor) is only a matter of exporters.
 *
 * Only attributes whose key is in [allowedKeys] are kept, so passwords, tokens, request
 * bodies or anything else not explicitly listed can never leave the device by mistake.
 */
class Telemetry(
    private val exporters: List<TelemetryExporter>,
    private val allowedKeys: Set<String> = TelemetryKeys.allowed,
    private val clock: TelemetryClock = SystemTelemetryClock,
    private val ids: TelemetryIds = RandomTelemetryIds,
    private val flushTimeout: Duration = 20.seconds,
) {
    val isEnabled: Boolean get() = exporters.isNotEmpty()

    /**
     * Runs [block] as a span named [name] and exports it when the block ends. A span started
     * inside another one (in the same coroutine) becomes its child, sharing the trace id.
     * An exception marks the span as an error and is rethrown unchanged.
     */
    suspend fun <T> span(
        name: String,
        kind: SpanKind = SpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap(),
        block: suspend SpanScope.() -> T,
    ): T {
        if (!isEnabled) return NoOpSpanScope.block()

        val parent = currentCoroutineContext()[ActiveSpan]
        val traceId = parent?.traceId ?: ids.traceId()
        val spanId = ids.spanId()
        val scope = RecordingSpanScope(allowed(attributes).toMutableMap())
        val start = clock.nowEpochNanos()
        try {
            return withContext(ActiveSpan(traceId, spanId)) { scope.block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            scope.setAttribute(TelemetryKeys.ERROR_TYPE, e::class.simpleName ?: "Throwable")
            scope.setError(e.message ?: "")
            throw e
        } finally {
            val span = SpanData(
                traceId = traceId,
                spanId = spanId,
                parentSpanId = parent?.spanId,
                name = name,
                kind = kind,
                startEpochNanos = start,
                endEpochNanos = clock.nowEpochNanos(),
                attributes = allowed(scope.attributes),
                status = scope.status,
                statusMessage = scope.statusMessage,
            )
            exporters.forEach { it.export(span) }
        }
    }

    /** Records an event, e.g. a message the user saw. */
    fun log(severity: Severity, name: String, attributes: Map<String, Any> = emptyMap()) {
        if (!isEnabled) return
        val log = LogData(
            epochNanos = clock.nowEpochNanos(),
            severity = severity,
            name = name,
            attributes = allowed(attributes),
        )
        exporters.forEach { it.export(log) }
    }

    /** Asks every exporter to send what it has buffered, e.g. when the app goes to the background. */
    fun flush() {
        exporters.forEach { it.flush() }
    }

    /**
     * Sends what every exporter has buffered and waits for it, for at most [flushTimeout].
     * iOS calls it inside a background task, which must end before the system's limit (~30 s);
     * whatever is not sent in time stays buffered for the next flush.
     */
    suspend fun flushAndWait() {
        withTimeoutOrNull(flushTimeout) {
            coroutineScope { exporters.forEach { launch { it.flushAndWait() } } }
        }
    }

    private fun allowed(attributes: Map<String, Any>): Map<String, Any> =
        attributes.filter { (key, value) -> key in allowedKeys && value.isSupportedAttribute() }

    companion object {
        /** Records nothing; spans still run their block. The default until the app configures telemetry. */
        val disabled = Telemetry(emptyList())
    }
}

private fun Any.isSupportedAttribute() = this is String || this is Boolean || this is Int || this is Long || this is Double

enum class SpanKind { INTERNAL, CLIENT }

enum class SpanStatus { UNSET, ERROR }

enum class Severity { DEBUG, INFO, WARN, ERROR }

/** What code inside a span can add to it while it runs. */
interface SpanScope {
    fun setAttribute(key: String, value: Any)

    /** Marks the span as failed, e.g. an HTTP 404 or a sync that did not complete. */
    fun setError(description: String)
}

private object NoOpSpanScope : SpanScope {
    override fun setAttribute(key: String, value: Any) = Unit

    override fun setError(description: String) = Unit
}

private class RecordingSpanScope(val attributes: MutableMap<String, Any>) : SpanScope {
    var status = SpanStatus.UNSET
        private set
    var statusMessage: String? = null
        private set

    override fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }

    override fun setError(description: String) {
        status = SpanStatus.ERROR
        statusMessage = description.ifEmpty { null }
    }
}

/** The span running in the current coroutine, so spans started inside it become its children. */
private class ActiveSpan(val traceId: String, val spanId: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ActiveSpan>
}

data class SpanData(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    val name: String,
    val kind: SpanKind,
    val startEpochNanos: Long,
    val endEpochNanos: Long,
    val attributes: Map<String, Any>,
    val status: SpanStatus,
    val statusMessage: String?,
) {
    val durationMillis: Double get() = (endEpochNanos - startEpochNanos) / 1_000_000.0
}

data class LogData(
    val epochNanos: Long,
    val severity: Severity,
    val name: String,
    val attributes: Map<String, Any>,
)

/** Receives finished spans and logs. Implementations must return quickly and never throw. */
interface TelemetryExporter {
    fun export(span: SpanData)

    fun export(log: LogData)

    /** Sends whatever is buffered, without waiting for it. */
    fun flush() = Unit

    /** Sends whatever is buffered and returns once it is done; by default the same as [flush]. */
    suspend fun flushAndWait() = flush()
}

fun interface TelemetryClock {
    fun nowEpochNanos(): Long
}

@OptIn(ExperimentalTime::class)
private object SystemTelemetryClock : TelemetryClock {
    override fun nowEpochNanos(): Long {
        val now = Clock.System.now()
        return now.epochSeconds * 1_000_000_000 + now.nanosecondsOfSecond
    }
}

/** Trace ids are 16 random bytes and span ids 8, both as lowercase hex, as OpenTelemetry expects. */
interface TelemetryIds {
    fun traceId(): String

    fun spanId(): String
}

private object RandomTelemetryIds : TelemetryIds {
    override fun traceId() = Random.nextBytes(16).toHex()

    override fun spanId() = Random.nextBytes(8).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
