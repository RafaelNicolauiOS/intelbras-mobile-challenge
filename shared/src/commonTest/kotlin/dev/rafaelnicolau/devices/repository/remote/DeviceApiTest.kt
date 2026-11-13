package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceApiTest {
    private val config = ApiConfig(baseUrl = "http://api.test:1234", token = "test-token")

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        DeviceApi(createHttpClient(config, MockEngine(handler)))

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun listUnwrapsTheDataEnvelope() = runTest {
        val api = api {
            json("""{"count":1,"data":[{"id":"v1","name":"Garagem","serial":"S1","username":"admin","password":"x"}]}""")
        }

        val devices = api.getVideoDevices()

        assertEquals(listOf(VideoDeviceDto("v1", "Garagem", "S1", "admin", "x")), devices)
    }

    @Test
    fun requestsGoToTheBaseUrlWithTheBearerToken() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json("""{"count":0,"data":[]}""")
        }

        api.getAlarmCentrals()

        assertEquals("http://api.test:1234/alarm-centrals", seen?.url.toString())
        assertEquals("Bearer test-token", seen?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun updateSendsAPatchWithEveryField() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json("""{"id":"a1","name":"Casa","macAddress":"1A2B3C4D5E6F","password":"1234"}""")
        }

        api.updateAlarmCentral("a1", AlarmCentralBody("Casa", "1A2B3C4D5E6F", "1234"))

        assertEquals(HttpMethod.Patch, seen?.method)
        assertEquals("http://api.test:1234/alarm-centrals/a1", seen?.url.toString())
        val body = seen!!.body.toByteArray().decodeToString()
        assertTrue("\"macAddress\":\"1A2B3C4D5E6F\"" in body && "\"password\":\"1234\"" in body, body)
    }

    @Test
    fun invalidTokenBecomesUnauthorized() = runTest {
        val unauthorized = api { json("""{"message":"Unauthorized"}""", HttpStatusCode.Unauthorized) }
        val forbidden = api { json("""{"message":"Forbidden"}""", HttpStatusCode.Forbidden) }

        assertEquals(Outcome.Failure(DeviceError.UNAUTHORIZED), apiCall { unauthorized.getVideoDevices() })
        assertEquals(Outcome.Failure(DeviceError.UNAUTHORIZED), apiCall { forbidden.getVideoDevices() })
    }

    @Test
    fun duplicateAndMissingDevicesAreToldApart() = runTest {
        val conflict = api { json("{}", HttpStatusCode.Conflict) }
        val notFound = api { json("{}", HttpStatusCode.NotFound) }

        assertEquals(
            Outcome.Failure(DeviceError.CONFLICT),
            apiCall { conflict.createVideoDevice(VideoDeviceBody("n", "s", "u", "p")) },
        )
        assertEquals(Outcome.Failure(DeviceError.NOT_FOUND), apiCall { notFound.deleteVideoDevice("gone") })
    }

    @Test
    fun connectionFailureBecomesNetworkError() = runTest {
        val api = api { throw IOException("no route to host") }

        assertEquals(Outcome.Failure(DeviceError.NETWORK), apiCall { api.getVideoDevices() })
    }

    @Test
    fun unresolvedHostWhileOfflineBecomesNetworkError() = runTest {
        val api = api { throw UnresolvedAddressException() }

        assertEquals(Outcome.Failure(DeviceError.NETWORK), apiCall { api.getVideoDevices() })
    }

    @Test
    fun serverErrorBecomesUnknown() = runTest {
        val api = api { json("{}", HttpStatusCode.InternalServerError) }

        assertEquals(Outcome.Failure(DeviceError.UNKNOWN), apiCall { api.getVideoDevices() })
    }

    @Test
    fun requestLoggingPrintsTheJsonBodiesButNeverTheToken() = runTest {
        val lines = mutableListOf<String>()
        val logging = ApiConfig(baseUrl = "http://api.test:1234", token = "secret-token", logRequests = true)
        val engine = MockEngine {
            json("""{"id":"v1","name":"Garagem","serial":"S1","username":"admin","password":"x"}""")
        }
        val api = DeviceApi(createHttpClient(logging, engine, log = { lines += it }))

        api.createVideoDevice(VideoDeviceBody("Garagem", "S1", "admin", "x"))

        val output = lines.joinToString("\n")
        assertTrue("http://api.test:1234/video-devices" in output && "POST" in output, output)
        assertTrue("\"serial\":\"S1\"" in output, "request body is logged")
        assertTrue("\"id\":\"v1\"" in output, "response body is logged")
        assertTrue("secret-token" !in output, "the token must not be logged")
    }

    @Test
    fun alarmListIsReadFromTheAlarmResource() = runTest {
        val api = api { request ->
            assertEquals("/alarm-centrals", request.url.encodedPath)
            json("""{"count":1,"data":[{"id":"a1","name":"Casa","macAddress":"1A2B3C4D5E6F","password":"1234","extra":true}]}""")
        }

        assertEquals(listOf(AlarmCentralDto("a1", "Casa", "1A2B3C4D5E6F", "1234")), api.getAlarmCentrals())
    }

    @Test
    fun listWithoutDataIsEmpty() = runTest {
        assertEquals(emptyList(), api { json("{}") }.getVideoDevices())
    }

    @Test
    fun createSendsAJsonPostToTheRightResource() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        val api = api { request ->
            seen += request
            if (request.url.encodedPath.startsWith("/alarm")) {
                json("""{"id":"a1","name":"Casa","macAddress":"1A2B3C4D5E6F","password":"1234"}""")
            } else {
                json("""{"id":"v1","name":"Garagem","serial":"S1","username":"admin","password":"x"}""")
            }
        }

        val alarm = api.createAlarmCentral(AlarmCentralBody("Casa", "1A2B3C4D5E6F", "1234"))
        val video = api.createVideoDevice(VideoDeviceBody("Garagem", "S1", "admin", "x"))

        assertEquals("a1", alarm.id)
        assertEquals("v1", video.id)
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Post), seen.map { it.method })
        assertEquals(listOf("/alarm-centrals", "/video-devices"), seen.map { it.url.encodedPath })
        assertTrue(seen.all { it.body.contentType?.toString()?.startsWith("application/json") == true })
    }

    @Test
    fun updateVideoSendsAPatchToTheDevice() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json("""{"id":"v1","name":"G","serial":"S1","username":"admin","password":"x"}""")
        }

        api.updateVideoDevice("v1", VideoDeviceBody("G", "S1", "admin", "x"))

        assertEquals(HttpMethod.Patch, seen?.method)
        assertEquals("/video-devices/v1", seen?.url?.encodedPath)
    }

    @Test
    fun deleteTargetsTheDeviceOfEachType() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        val api = api { request ->
            seen += request
            json("")
        }

        api.deleteAlarmCentral("a1")
        api.deleteVideoDevice("v1")

        assertEquals(listOf(HttpMethod.Delete, HttpMethod.Delete), seen.map { it.method })
        assertEquals(listOf("/alarm-centrals/a1", "/video-devices/v1"), seen.map { it.url.encodedPath })
    }

    @Test
    fun baseUrlWithTrailingSlashIsNotDoubled() = runTest {
        var seen: HttpRequestData? = null
        val engine = MockEngine { request ->
            seen = request
            json("""{"count":0,"data":[]}""")
        }

        DeviceApi(createHttpClient(ApiConfig("http://api.test/", "t"), engine)).getVideoDevices()

        assertEquals("http://api.test/video-devices", seen?.url.toString())
    }

    @Test
    fun nothingIsLoggedUnlessEnabled() = runTest {
        val lines = mutableListOf<String>()
        val engine = MockEngine { json("""{"count":0,"data":[]}""") }

        DeviceApi(createHttpClient(config, engine, log = { lines += it })).getVideoDevices()

        assertTrue(lines.isEmpty())
    }
}
