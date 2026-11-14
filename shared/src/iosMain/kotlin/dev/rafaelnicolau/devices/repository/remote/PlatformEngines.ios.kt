package dev.rafaelnicolau.devices.repository.remote

import io.ktor.client.engine.HttpClientEngine

/** Darwin (URLSession), the only engine on iOS, with the system TLS. */
internal actual fun defaultApiEngine(): HttpClientEngine? = null

internal actual fun defaultTelemetryEngine(): HttpClientEngine? = null
