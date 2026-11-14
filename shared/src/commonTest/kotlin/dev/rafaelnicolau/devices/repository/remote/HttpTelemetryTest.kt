package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.telemetry.RecordingExporter
import dev.rafaelnicolau.devices.telemetry.SpanKind
import dev.rafaelnicolau.devices.telemetry.SpanStatus
import dev.rafaelnicolau.devices.telemetry.SteppingClock
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryKeys
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpTelemetryTest {
    private val exporter = RecordingExporter()
    private val telemetry = Telemetry(listOf(exporter), clock = SteppingClock())

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = """{"count":0,"data":[]}""") = DeviceApi(
        createHttpClient(
            ApiConfig("http://api.test:1234", "secret-token"),
            MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
            telemetry = telemetry,
        ),
    )

    @Test
    fun eachRequestBecomesAClientSpan() = runTest {
        api().getAlarmCentrals()

        val span = exporter.spans.single()
        assertEquals("GET /alarm-centrals", span.name)
        assertEquals(SpanKind.CLIENT, span.kind)
        assertEquals(SpanStatus.UNSET, span.status)
        assertEquals(
            mapOf<String, Any>(
                TelemetryKeys.HTTP_METHOD to "GET",
                TelemetryKeys.URL_TEMPLATE to "/alarm-centrals",
                TelemetryKeys.SERVER_ADDRESS to "api.test",
                TelemetryKeys.HTTP_STATUS to 200,
            ),
            span.attributes,
        )
    }

    @Test
    fun deviceIdsAreReplacedInThePath() = runTest {
        api().deleteVideoDevice("3f2c9a7e-0000-4000-8000-1234567890ab")

        val span = exporter.spans.single()
        assertEquals("DELETE /video-devices/{id}", span.name)
        assertTrue(span.attributes.values.none { "3f2c9a7e" in it.toString() })
    }

    @Test
    fun errorStatusMarksTheSpanAndTheErrorStillReachesTheApp() = runTest {
        val result = apiCall { api(HttpStatusCode.NotFound, "{}").deleteAlarmCentral("a1") }

        assertEquals(Outcome.Failure(DeviceError.NOT_FOUND), result)
        val span = exporter.spans.single()
        assertEquals(SpanStatus.ERROR, span.status)
        assertEquals(404, span.attributes[TelemetryKeys.HTTP_STATUS])
    }

    @Test
    fun networkFailureMarksTheSpan() = runTest {
        val offline = DeviceApi(
            createHttpClient(ApiConfig("http://api.test", "t"), MockEngine { throw IOException("offline") }, telemetry = telemetry),
        )

        val result = apiCall { offline.getVideoDevices() }

        assertEquals(Outcome.Failure(DeviceError.NETWORK), result)
        val span = exporter.spans.single()
        assertEquals(SpanStatus.ERROR, span.status)
        assertEquals("IOException", span.attributes[TelemetryKeys.ERROR_TYPE])
    }

    @Test
    fun requestsInsideASpanBecomeItsChildren() = runTest {
        val api = api()

        telemetry.span("devices.refresh") {
            api.getAlarmCentrals()
            api.getVideoDevices()
        }

        val (alarms, videos, refresh) = exporter.spans
        assertEquals("devices.refresh", refresh.name)
        assertEquals(refresh.spanId, alarms.parentSpanId)
        assertEquals(refresh.spanId, videos.parentSpanId)
        assertEquals(refresh.traceId, videos.traceId)
    }

    @Test
    fun nothingIsInstalledWhenTelemetryIsDisabled() = runTest {
        DeviceApi(
            createHttpClient(ApiConfig("http://api.test", "t"), MockEngine { respond("""{"data":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }),
        ).getVideoDevices()

        assertTrue(exporter.spans.isEmpty())
    }

    @Test
    fun urlTemplateKeepsTheResourceAndHidesIds() {
        assertEquals("/alarm-centrals", urlTemplate("/alarm-centrals"))
        assertEquals("/alarm-centrals/{id}", urlTemplate("/alarm-centrals/abc-123"))
        assertEquals("/", urlTemplate("/"))
    }
}
