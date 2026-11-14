package dev.rafaelnicolau.devices

import android.app.Application
import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.di.initializeApp
import dev.rafaelnicolau.devices.telemetry.TelemetryConfig
import dev.rafaelnicolau.devices.telemetry.parseOtlpHeaders

class DevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeApp(
            context = this,
            config = ApiConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                token = BuildConfig.API_TOKEN,
                // Requests and responses go to Logcat (filter by "[HTTP]") in debug builds only.
                logRequests = BuildConfig.DEBUG,
            ),
            telemetry = TelemetryConfig(
                serviceVersion = BuildConfig.VERSION_NAME,
                environment = BuildConfig.BUILD_TYPE,
                otlpEndpoint = BuildConfig.OTLP_ENDPOINT,
                otlpHeaders = parseOtlpHeaders(BuildConfig.OTLP_HEADERS),
                // Spans and events go to Logcat (filter by "[TELEMETRY]") in debug builds.
                console = BuildConfig.DEBUG,
            ),
        )
    }
}
