package dev.rafaelnicolau.devices.telemetry

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class OtlpExporterTest {
    /** One recorded request: path, headers and the body parsed as JSON. */
    private class Sent(val path: String, val request: HttpRequestData, val json: JsonObject)

    private val sent = mutableListOf<Sent>()

    /** What the fake collector answers, request by request; OK once the list runs out. */
    private val answers = ArrayDeque<HttpStatusCode>()

    /** Signals each request as it arrives; the mock engine answers on a real thread. */
    private val arrivals = Channel<Sent>(Channel.UNLIMITED)

    private val engine = MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        val recorded = Sent(request.url.encodedPath, request, Json.parseToJsonElement(body).jsonObject)
        sent += recorded
        arrivals.trySend(recorded)
        respond("{}", answers.removeFirstOrNull() ?: HttpStatusCode.OK)
    }

    /** Waits, in real time, for the next request the exporter sends on its own. */
    private suspend fun nextRequest(): Sent =
        withContext(Dispatchers.Default) { withTimeout(5.seconds) { arrivals.receive() } }

    private fun TestScope.exporter(batchSize: Int = 100, maxBuffered: Int = 500) = OtlpExporter(
        endpoint = "http://collector.test:4318/",
        headers = mapOf(HttpHeaders.Authorization to "Basic key"),
        resource = TelemetryConfig(serviceVersion = "1.0", environment = "debug").resourceAttributes("ios"),
        scope = backgroundScope,
        engine = engine,
        batchSize = batchSize,
        maxBuffered = maxBuffered,
        flushInterval = 30.seconds,
        retryDelay = 1.seconds,
    )

    private fun span(name: String = "devices.refresh", parent: String? = null) = SpanData(
        traceId = "0af7651916cd43dd8448eb211c80319c",
        spanId = "b7ad6b7169203331",
        parentSpanId = parent,
        name = name,
        kind = SpanKind.CLIENT,
        startEpochNanos = 1_700_000_000_000_000_000,
        endEpochNanos = 1_700_000_000_012_000_000,
        attributes = mapOf(TelemetryKeys.HTTP_STATUS to 404, TelemetryKeys.URL_TEMPLATE to "/alarm-centrals/{id}"),
        status = SpanStatus.ERROR,
        statusMessage = "Not Found",
    )

    private fun log() = LogData(
        epochNanos = 1_700_000_000_000_000_000,
        severity = Severity.WARN,
        name = "user.message",
        attributes = mapOf(TelemetryKeys.MESSAGE_KIND to "failure"),
    )

    private val JsonObject.firstSpan
        get() = this["resourceSpans"]!!.jsonArray[0].jsonObject["scopeSpans"]!!.jsonArray[0].jsonObject["spans"]!!.jsonArray

    @Test
    fun spansArePostedToTheTracesPathAsOtlpJson() = runTest {
        val exporter = exporter()
        exporter.export(span(parent = "00f067aa0ba902b7"))

        exporter.flushAndWait()

        val request = sent.single()
        assertEquals("/v1/traces", request.path)
        assertEquals("Basic key", request.request.headers[HttpHeaders.Authorization])
        assertTrue(request.request.body.contentType.toString().startsWith("application/json"))

        val spanJson = request.json.firstSpan[0].jsonObject
        assertEquals("0af7651916cd43dd8448eb211c80319c", spanJson["traceId"]!!.jsonPrimitive.content)
        assertEquals("00f067aa0ba902b7", spanJson["parentSpanId"]!!.jsonPrimitive.content)
        assertEquals(3, spanJson["kind"]!!.jsonPrimitive.content.toInt(), "SPAN_KIND_CLIENT")
        assertTrue(spanJson["startTimeUnixNano"]!!.jsonPrimitive.isString, "64-bit numbers travel as strings")
        assertEquals("1700000000012000000", spanJson["endTimeUnixNano"]!!.jsonPrimitive.content)
        assertEquals("2", spanJson["status"]!!.jsonObject["code"]!!.jsonPrimitive.content)

        val attributes = spanJson["attributes"]!!.jsonArray.associate {
            it.jsonObject["key"]!!.jsonPrimitive.content to it.jsonObject["value"]!!.jsonObject
        }
        assertTrue(attributes.getValue(TelemetryKeys.HTTP_STATUS)["intValue"]!!.jsonPrimitive.isString)
        assertEquals("/alarm-centrals/{id}", attributes.getValue(TelemetryKeys.URL_TEMPLATE)["stringValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun rootSpanHasNoParentField() = runTest {
        val exporter = exporter()
        exporter.export(span(parent = null))

        exporter.flushAndWait()

        assertTrue("parentSpanId" !in sent.single().json.firstSpan[0].jsonObject)
    }

    @Test
    fun resourceDescribesTheApp() = runTest {
        val exporter = exporter()
        exporter.export(span())

        exporter.flushAndWait()

        val resource = sent.single().json["resourceSpans"]!!.jsonArray[0].jsonObject["resource"]!!.jsonObject
        val keys = resource["attributes"]!!.jsonArray.map { it.jsonObject["key"]!!.jsonPrimitive.content }
        assertTrue(keys.containsAll(listOf("service.name", "service.version", "deployment.environment.name", "os.type")))
    }

    @Test
    fun logsArePostedToTheLogsPath() = runTest {
        val exporter = exporter()
        exporter.export(log())

        exporter.flushAndWait()

        val request = sent.single()
        assertEquals("/v1/logs", request.path)
        val record = request.json["resourceLogs"]!!.jsonArray[0].jsonObject["scopeLogs"]!!.jsonArray[0]
            .jsonObject["logRecords"]!!.jsonArray[0].jsonObject
        assertEquals("13", record["severityNumber"]!!.jsonPrimitive.content)
        assertEquals("user.message", record["body"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun nothingIsSentWhenThereIsNothingToSend() = runTest {
        exporter().flushAndWait()

        assertTrue(sent.isEmpty())
    }

    @Test
    fun aFullBatchIsSentWithoutWaitingForTheTimer() = runTest {
        val exporter = exporter(batchSize = 2)

        exporter.export(span("a"))
        exporter.export(span("b"))
        runCurrent()

        assertEquals(2, nextRequest().json.firstSpan.size, "sent without any flush or timer")
    }

    @Test
    fun theTimerSendsWhatIsWaiting() = runTest {
        val exporter = exporter()
        exporter.export(span())
        runCurrent()
        assertTrue(sent.isEmpty(), "nothing is sent before the timer")

        advanceTimeBy(31.seconds)

        assertEquals("/v1/traces", nextRequest().path)
    }

    @Test
    fun bufferKeepsOnlyTheNewestItemsWhenFull() = runTest {
        val exporter = exporter(maxBuffered = 2)

        listOf("old", "middle", "new").forEach { exporter.export(span(it)) }
        exporter.flushAndWait()

        val names = sent.single().json.firstSpan.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(listOf("middle", "new"), names)
    }

    @Test
    fun serverErrorsAreRetriedUntilTheyPass() = runTest {
        answers += listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.TooManyRequests)
        val exporter = exporter()
        exporter.export(span())

        exporter.flushAndWait()

        assertEquals(3, sent.size, "two failures, then success")
    }

    @Test
    fun aRejectedBatchIsNotRetried() = runTest {
        answers += HttpStatusCode.BadRequest
        val exporter = exporter()
        exporter.export(span())

        exporter.flushAndWait()

        assertEquals(1, sent.size)
    }

    @Test
    fun aBatchThatKeepsFailingIsKeptForTheNextFlush() = runTest {
        answers += List(3) { HttpStatusCode.InternalServerError }
        val exporter = exporter()
        exporter.export(span())
        exporter.flushAndWait()
        assertEquals(3, sent.size, "three attempts, then kept")

        exporter.flushAndWait()

        assertEquals(4, sent.size, "sent again on the next flush, and accepted")
        exporter.flushAndWait()
        assertEquals(4, sent.size, "nothing left once it went through")
    }

    @Test
    fun offlineDataIsSentOnceTheNetworkIsBack() = runTest {
        var online = false
        val received = mutableListOf<String>()
        val exporter = OtlpExporter(
            endpoint = "http://collector.test:4318",
            headers = emptyMap(),
            resource = emptyMap(),
            scope = backgroundScope,
            engine = MockEngine { request ->
                if (!online) throw IOException("network access blocked")
                received += request.url.encodedPath
                respond("{}", HttpStatusCode.OK)
            },
            retryDelay = 1.seconds,
        )
        exporter.export(span())
        exporter.export(log())
        exporter.flushAndWait()
        assertTrue(received.isEmpty())

        online = true
        exporter.flushAndWait()

        assertEquals(listOf("/v1/traces", "/v1/logs"), received)
    }

    @Test
    fun keptDataStillRespectsTheBufferLimit() = runTest {
        answers += List(3) { HttpStatusCode.ServiceUnavailable }
        val exporter = exporter(maxBuffered = 2)
        exporter.export(span("first"))
        exporter.flushAndWait()

        listOf("second", "third").forEach { exporter.export(span(it)) }
        exporter.flushAndWait()

        val names = sent.last().json.firstSpan.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(listOf("second", "third"), names, "the oldest gave way to newer data")
    }

    @Test
    fun exportsThroughTelemetryEndToEnd() = runTest {
        val exporter = exporter()
        val telemetry = Telemetry(listOf(exporter), clock = SteppingClock())

        telemetry.span("devices.refresh") {
            telemetry.span("GET /alarm-centrals", SpanKind.CLIENT) {}
        }
        exporter.flushAndWait()

        val spans = sent.single().json.firstSpan.map { it.jsonObject }
        assertEquals(listOf("GET /alarm-centrals", "devices.refresh"), spans.map { it["name"]!!.jsonPrimitive.content })
        assertEquals(spans[1]["spanId"], spans[0]["parentSpanId"])
    }

    @Test
    fun eachExportReportsItsResultWithoutTheHeaders() = runTest {
        answers += HttpStatusCode.Forbidden
        val lines = mutableListOf<String>()
        val exporter = OtlpExporter(
            endpoint = "http://collector.test:4318",
            headers = mapOf("api-key" to "secret-key"),
            resource = emptyMap(),
            scope = backgroundScope,
            engine = engine,
            report = { lines += it },
        )

        exporter.export(span())
        exporter.flushAndWait()
        exporter.export(log())
        exporter.flushAndWait()

        assertEquals(
            listOf(
                "[TELEMETRY] export /v1/traces 1 spans: dropped (HTTP 403)",
                "[TELEMETRY] export /v1/logs 1 logs: sent (HTTP 200)",
            ),
            lines,
        )
        assertTrue(lines.none { "secret-key" in it })
    }

    @Test
    fun unreachableBackendIsReportedAsKept() = runTest {
        answers += List(3) { HttpStatusCode.BadGateway }
        val lines = mutableListOf<String>()
        val exporter = OtlpExporter(
            endpoint = "http://collector.test:4318",
            headers = emptyMap(),
            resource = emptyMap(),
            scope = backgroundScope,
            engine = engine,
            retryDelay = 1.seconds,
            report = { lines += it },
        )
        exporter.export(span())

        exporter.flushAndWait()

        assertEquals(listOf("[TELEMETRY] export /v1/traces 1 spans: kept for later (HTTP 502)"), lines)
    }
}
