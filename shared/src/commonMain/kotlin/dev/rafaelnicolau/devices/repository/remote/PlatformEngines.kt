package dev.rafaelnicolau.devices.repository.remote

import io.ktor.client.engine.HttpClientEngine

/*
 * The HTTP engines are chosen explicitly: with more than one engine on the classpath Ktor's
 * default pick is not guaranteed. Null means "the platform's only engine" (Darwin on iOS).
 */

/** Engine for the device API. */
internal expect fun defaultApiEngine(): HttpClientEngine?

/** Engine for telemetry exports, which go to HTTPS backends such as New Relic. */
internal expect fun defaultTelemetryEngine(): HttpClientEngine?
