package dev.rafaelnicolau.devices.telemetry

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Keeps what it receives, for assertions. */
class RecordingExporter : TelemetryExporter {
    val spans = mutableListOf<SpanData>()
    val logs = mutableListOf<LogData>()
    var flushes = 0

    override fun export(span: SpanData) {
        spans += span
    }

    override fun export(log: LogData) {
        logs += log
    }

    override fun flush() {
        flushes++
    }
}

/** Advances 1 ms (in nanoseconds) on every reading, so durations are predictable. */
class SteppingClock(private var now: Long = 1_700_000_000_000_000_000) : TelemetryClock {
    override fun nowEpochNanos(): Long = now.also { now += 1_000_000 }
}

class TelemetryTest {
    private val exporter = RecordingExporter()
    private val telemetry = Telemetry(listOf(exporter), clock = SteppingClock())

    @Test
    fun spanRecordsNameKindDurationAndAttributes() = runTest {
        val result = telemetry.span("devices.refresh", SpanKind.INTERNAL, mapOf(TelemetryKeys.ALARM_COUNT to 2)) {
            setAttribute(TelemetryKeys.VIDEO_COUNT, 3)
            "done"
        }

        assertEquals("done", result)
        val span = exporter.spans.single()
        assertEquals("devices.refresh", span.name)
        assertEquals(SpanKind.INTERNAL, span.kind)
        assertEquals(1.0, span.durationMillis)
        assertEquals(mapOf<String, Any>(TelemetryKeys.ALARM_COUNT to 2, TelemetryKeys.VIDEO_COUNT to 3), span.attributes)
        assertEquals(SpanStatus.UNSET, span.status)
        assertEquals(32, span.traceId.length)
        assertEquals(16, span.spanId.length)
        assertNull(span.parentSpanId)
    }

    @Test
    fun exceptionMarksTheSpanAsErrorAndIsRethrown() = runTest {
        assertFailsWith<IllegalStateException> {
            telemetry.span("work") { error("boom") }
        }

        val span = exporter.spans.single()
        assertEquals(SpanStatus.ERROR, span.status)
        assertEquals("boom", span.statusMessage)
        assertEquals("IllegalStateException", span.attributes[TelemetryKeys.ERROR_TYPE])
    }

    @Test
    fun setErrorMarksAFailureThatDidNotThrow() = runTest {
        telemetry.span("http") { setError("Not Found") }

        assertEquals(SpanStatus.ERROR, exporter.spans.single().status)
    }

    @Test
    fun spanStartedInsideAnotherBecomesItsChild() = runTest {
        telemetry.span("parent") {
            telemetry.span("child") {}
        }

        val (child, parent) = exporter.spans
        assertEquals("child", child.name)
        assertEquals(parent.traceId, child.traceId)
        assertEquals(parent.spanId, child.parentSpanId)
    }

    @Test
    fun separateSpansStartSeparateTraces() = runTest {
        telemetry.span("first") {}
        telemetry.span("second") {}

        assertTrue(exporter.spans[0].traceId != exporter.spans[1].traceId)
    }

    @Test
    fun keysOutsideTheAllowlistNeverLeave() = runTest {
        telemetry.span("save", attributes = mapOf("password" to "1234", TelemetryKeys.DEVICE_ERROR to "CONFLICT")) {
            setAttribute("device.serial", "ABC1234567890")
        }
        telemetry.log(Severity.WARN, "user.message", mapOf("token" to "secret", TelemetryKeys.MESSAGE_KIND to "failure"))

        assertEquals(mapOf<String, Any>(TelemetryKeys.DEVICE_ERROR to "CONFLICT"), exporter.spans.single().attributes)
        assertEquals(mapOf<String, Any>(TelemetryKeys.MESSAGE_KIND to "failure"), exporter.logs.single().attributes)
    }

    @Test
    fun unsupportedValueTypesAreDropped() = runTest {
        telemetry.span("work", attributes = mapOf(TelemetryKeys.DEVICE_ERROR to listOf("a"))) {}

        assertTrue(exporter.spans.single().attributes.isEmpty())
    }

    @Test
    fun logIsSentToEveryExporter() {
        val second = RecordingExporter()
        val telemetry = Telemetry(listOf(exporter, second), clock = SteppingClock())

        telemetry.log(Severity.INFO, "user.message", mapOf(TelemetryKeys.MESSAGE_KIND to "device_created"))

        assertEquals(Severity.INFO, exporter.logs.single().severity)
        assertEquals("user.message", second.logs.single().name)
    }

    @Test
    fun flushReachesEveryExporter() {
        telemetry.flush()

        assertEquals(1, exporter.flushes)
    }

    @Test
    fun flushAndWaitReturnsOnceEveryExporterHasSent() = runTest {
        val sent = mutableListOf<String>()
        fun slow(name: String, seconds: Int) = object : TelemetryExporter {
            override fun export(span: SpanData) = Unit
            override fun export(log: LogData) = Unit
            override suspend fun flushAndWait() {
                delay(seconds.seconds)
                sent += name
            }
        }

        Telemetry(listOf(slow("a", 1), slow("b", 3))).flushAndWait()

        assertEquals(listOf("a", "b"), sent)
        assertEquals(3_000, currentTime, "exporters are flushed together, not one after another")
    }

    @Test
    fun flushAndWaitGivesUpInTimeForTheBackgroundLimit() = runTest {
        val stuck = object : TelemetryExporter {
            override fun export(span: SpanData) = Unit
            override fun export(log: LogData) = Unit
            override suspend fun flushAndWait() = awaitCancellation()
        }

        Telemetry(listOf(stuck), flushTimeout = 20.seconds).flushAndWait()

        assertEquals(20_000, currentTime)
    }

    @Test
    fun disabledTelemetryRunsTheBlockAndRecordsNothing() = runTest {
        val result = Telemetry.disabled.span("work") {
            setAttribute(TelemetryKeys.ALARM_COUNT, 1)
            42
        }
        Telemetry.disabled.log(Severity.ERROR, "nothing")

        assertEquals(42, result)
        assertTrue(!Telemetry.disabled.isEnabled)
    }

    @Test
    fun consoleExporterPrintsSpansAndLogs() = runTest {
        val lines = mutableListOf<String>()
        val telemetry = Telemetry(listOf(ConsoleExporter { lines += it }), clock = SteppingClock())

        telemetry.span("devices.refresh", attributes = mapOf(TelemetryKeys.ALARM_COUNT to 2)) { setError("NETWORK") }
        telemetry.log(Severity.WARN, "user.message")

        assertEquals("[TELEMETRY] span devices.refresh 1.0ms ERROR NETWORK {devices.alarm.count=2}", lines[0])
        assertEquals("[TELEMETRY] WARN user.message", lines[1])
    }
}
