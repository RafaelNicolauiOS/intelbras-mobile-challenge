package dev.rafaelnicolau.devices.repository.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.cio.CIO

/** CIO, as before: the challenge API is plain HTTP, so CIO's own TLS is never involved. */
internal actual fun defaultApiEngine(): HttpClientEngine? = CIO.create()

/**
 * The platform engine (HttpURLConnection). Android requires hostname-aware certificate checks
 * when the network security config has domain rules, which CIO's TLS does not do: with CIO,
 * every HTTPS export failed with "Domain specific configurations require that hostname aware
 * checkServerTrusted(...) is used".
 */
internal actual fun defaultTelemetryEngine(): HttpClientEngine? = Android.create()
