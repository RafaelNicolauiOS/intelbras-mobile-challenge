package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.di.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MS = 15_000L
private const val CONNECT_TIMEOUT_MS = 10_000L

/**
 * Builds the client used for every API call. Without an [engine] Ktor picks the platform
 * one (CIO on Android, Darwin on iOS); tests pass a MockEngine.
 */
fun createHttpClient(config: ApiConfig, engine: HttpClientEngine? = null): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        // Non-2xx responses throw, so they are translated to DeviceError in one place.
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
        defaultRequest {
            url(config.baseUrl.trimEnd('/') + "/")
            bearerAuth(config.token)
            contentType(ContentType.Application.Json)
        }
    }
    return if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
}
