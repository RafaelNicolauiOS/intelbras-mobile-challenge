package dev.rafaelnicolau.devices.repository.remote

import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.telemetry.Telemetry
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MS = 15_000L
private const val CONNECT_TIMEOUT_MS = 10_000L

/**
 * Builds the client used for every API call. Without an [engine] Ktor picks the platform
 * one (CIO on Android, Darwin on iOS); tests pass a MockEngine.
 */
fun createHttpClient(
    config: ApiConfig,
    engine: HttpClientEngine? = null,
    log: (String) -> Unit = ::printHttpLog,
    telemetry: Telemetry = Telemetry.disabled,
): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        if (config.logRequests) installRequestLogging(log)
        if (telemetry.isEnabled) install(HttpTelemetry) { this.telemetry = telemetry }
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
    val chosen = engine ?: defaultApiEngine()
    return if (chosen != null) HttpClient(chosen, configure) else HttpClient(configure)
}

private const val LOG_TAG = "[HTTP]"

/**
 * Logs each request and response with its JSON body. The Authorization header is
 * replaced by a placeholder so the token never reaches the console.
 */
private fun HttpClientConfig<*>.installRequestLogging(log: (String) -> Unit) {
    install(Logging) {
        level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) = log(message)
        }
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}

/** Standard output: shown in Xcode's debug area on iOS and in Logcat (tag System.out) on Android. */
private fun printHttpLog(message: String) {
    message.lineSequence().forEach { line -> println("$LOG_TAG $line") }
}
