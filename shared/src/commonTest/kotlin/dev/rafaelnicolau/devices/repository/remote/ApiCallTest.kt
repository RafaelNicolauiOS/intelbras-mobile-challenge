package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiCallTest {
    private suspend fun statusFailure(status: HttpStatusCode): Outcome<Unit> {
        val api = DeviceApi(createHttpClient(ApiConfig("http://api.test", "t"), MockEngine { respond("{}", status) }))
        return apiCall { api.deleteVideoDevice("x") }
    }

    @Test
    fun successWrapsTheValue() = runTest {
        assertEquals(Outcome.Success(42), apiCall { 42 })
    }

    @Test
    fun timeoutsBecomeTimeout() = runTest {
        assertEquals(Outcome.Failure(DeviceError.TIMEOUT), apiCall { throw HttpRequestTimeoutException("http://api.test", 15_000) })
        assertEquals(Outcome.Failure(DeviceError.TIMEOUT), apiCall { throw ConnectTimeoutException("connect", null) })
    }

    @Test
    fun serverTimeoutStatusesBecomeTimeout() = runTest {
        assertEquals(Outcome.Failure(DeviceError.TIMEOUT), statusFailure(HttpStatusCode.RequestTimeout))
        assertEquals(Outcome.Failure(DeviceError.TIMEOUT), statusFailure(HttpStatusCode.GatewayTimeout))
    }

    @Test
    fun otherStatusesBecomeUnknown() = runTest {
        assertEquals(Outcome.Failure(DeviceError.UNKNOWN), statusFailure(HttpStatusCode.BadRequest))
        assertEquals(Outcome.Failure(DeviceError.UNKNOWN), statusFailure(HttpStatusCode.ServiceUnavailable))
    }

    @Test
    fun unexpectedExceptionBecomesUnknown() = runTest {
        assertEquals(Outcome.Failure(DeviceError.UNKNOWN), apiCall { throw IllegalStateException("bad json") })
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        assertFailsWith<CancellationException> { apiCall { throw CancellationException("screen closed") } }
    }
}
