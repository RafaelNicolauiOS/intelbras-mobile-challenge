package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.Outcome
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/** Runs an API call and turns every failure into a [DeviceError]. The only place that knows HTTP. */
suspend fun <T> apiCall(block: suspend () -> T): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        Outcome.Failure(e.response.status.toDeviceError())
    } catch (e: HttpRequestTimeoutException) {
        Outcome.Failure(DeviceError.TIMEOUT)
    } catch (e: ConnectTimeoutException) {
        Outcome.Failure(DeviceError.TIMEOUT)
    } catch (e: SocketTimeoutException) {
        Outcome.Failure(DeviceError.TIMEOUT)
    } catch (e: IOException) {
        Outcome.Failure(DeviceError.NETWORK)
    } catch (e: UnresolvedAddressException) {
        // What the Android engine throws when offline (DNS lookup fails); it is not an IOException.
        Outcome.Failure(DeviceError.NETWORK)
    } catch (e: Exception) {
        Outcome.Failure(DeviceError.UNKNOWN)
    }

private fun HttpStatusCode.toDeviceError(): DeviceError = when (this) {
    // The docs list 403, but the server answers 401 when the token is missing or invalid.
    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DeviceError.UNAUTHORIZED
    HttpStatusCode.NotFound -> DeviceError.NOT_FOUND
    HttpStatusCode.Conflict -> DeviceError.CONFLICT
    HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> DeviceError.TIMEOUT
    else -> DeviceError.UNKNOWN
}
