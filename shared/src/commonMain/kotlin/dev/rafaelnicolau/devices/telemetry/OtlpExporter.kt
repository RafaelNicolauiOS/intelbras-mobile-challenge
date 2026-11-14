package dev.rafaelnicolau.devices.telemetry

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sends spans and logs to any OpenTelemetry-compatible backend over OTLP/HTTP (JSON),
 * with nothing but Ktor. The backend is configuration: a local collector, Grafana Cloud,
 * Honeycomb, Datadog… only [endpoint] and [headers] change.
 *
 * Built so telemetry can never hurt the app:
 * - [export] only hands the item to a channel and returns; all work happens in [scope],
 *   which the app runs off the main thread;
 * - at most [maxQueued] items wait to be processed and [maxBuffered] wait to be sent;
 *   past that the oldest are dropped, so memory stays bounded while offline;
 * - a batch that fails is retried [maxAttempts] times, then dropped;
 * - it has its own HTTP client, so its requests are not traced themselves.
 */
class OtlpExporter(
    private val endpoint: String,
    private val headers: Map<String, String>,
    resource: Map<String, Any>,
    private val scope: CoroutineScope,
    engine: HttpClientEngine? = null,
    private val batchSize: Int = 100,
    private val maxBuffered: Int = 500,
    maxQueued: Int = 1_000,
    flushInterval: Duration = 30.seconds,
    private val maxAttempts: Int = 3,
    private val retryDelay: Duration = 2.seconds,
) : TelemetryExporter {

    private sealed interface Message {
        data class Span(val data: SpanData) : Message

        data class Log(val data: LogData) : Message

        data class Flush(val done: CompletableDeferred<Unit>?) : Message
    }

    private val inbox = Channel<Message>(capacity = maxQueued, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val spans = ArrayDeque<SpanData>()
    private val logs = ArrayDeque<LogData>()

    private val otlpResource = OtlpResource(resource.toKeyValues())
    private val otlpScope = OtlpScope(INSTRUMENTATION_SCOPE, version = "1")

    private val client: HttpClient = run {
        val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
        }
        if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
    }

    init {
        // A single consumer owns the buffers, so no locking is needed.
        scope.launch {
            for (message in inbox) {
                when (message) {
                    is Message.Span -> spans.addBounded(message.data)
                    is Message.Log -> logs.addBounded(message.data)
                    is Message.Flush -> {
                        sendAll()
                        message.done?.complete(Unit)
                    }
                }
                if (spans.size >= batchSize || logs.size >= batchSize) sendAll()
            }
        }
        scope.launch {
            while (isActive) {
                delay(flushInterval)
                inbox.trySend(Message.Flush(done = null))
            }
        }
    }

    override fun export(span: SpanData) {
        inbox.trySend(Message.Span(span))
    }

    override fun export(log: LogData) {
        inbox.trySend(Message.Log(log))
    }

    override fun flush() {
        inbox.trySend(Message.Flush(done = null))
    }

    /** Sends everything received so far and waits for it; for tests and shutdown. */
    suspend fun flushAndWait() {
        val done = CompletableDeferred<Unit>()
        inbox.send(Message.Flush(done))
        done.await()
    }

    private fun <T> ArrayDeque<T>.addBounded(item: T) {
        if (size >= maxBuffered) removeFirst()
        addLast(item)
    }

    private suspend fun sendAll() {
        while (spans.isNotEmpty()) {
            val batch = spans.take(batchSize)
            repeat(batch.size) { spans.removeFirst() }
            send("v1/traces", otlpJson.encodeToString(OtlpTracesRequest.serializer(), tracesRequest(otlpResource, otlpScope, batch)))
        }
        while (logs.isNotEmpty()) {
            val batch = logs.take(batchSize)
            repeat(batch.size) { logs.removeFirst() }
            send("v1/logs", otlpJson.encodeToString(OtlpLogsRequest.serializer(), logsRequest(otlpResource, otlpScope, batch)))
        }
    }

    /** Retries network failures, 429 and 5xx; any other answer will not change, so the batch is dropped. */
    private suspend fun send(path: String, body: String) {
        val url = endpoint.trimEnd('/') + "/" + path
        for (attempt in 1..maxAttempts) {
            val retry = try {
                val response = client.post(url) {
                    headers { this@OtlpExporter.headers.forEach { (name, value) -> append(name, value) } }
                    setBody(TextContent(body, ContentType.Application.Json))
                }
                val status = response.status
                !status.isSuccess() && (status == HttpStatusCode.TooManyRequests || status.value >= 500)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                true
            }
            if (!retry) return
            if (attempt < maxAttempts) delay(retryDelay * attempt)
        }
    }
}
