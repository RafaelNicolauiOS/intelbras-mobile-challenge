package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.telemetry.Severity
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryKeys
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Carries user feedback from any ViewModel to the single place in each app that shows it.
 * A channel (not state) so each message is delivered exactly once and survives the screen
 * that produced it being closed, e.g. "device created" after the form is dismissed.
 */
class MessageCenter {
    private val channel = Channel<UserMessage>(Channel.BUFFERED)

    /** Every message is also recorded as a `user.message` event: what users actually saw. */
    internal var telemetry: Telemetry = Telemetry.disabled

    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun post(message: UserMessage) {
        channel.trySend(message)
        val severity = if (message is UserMessage.Success) Severity.INFO else Severity.WARN
        telemetry.log(severity, "user.message", mapOf(TelemetryKeys.MESSAGE_KIND to message.kind))
    }
}

/** e.g. `device_created`, `failure.network`, `invalid.invalid_serial`. */
private val UserMessage.kind: String
    get() = when (this) {
        is UserMessage.Success -> kind.name.lowercase()
        is UserMessage.Failure -> "failure." + error.name.lowercase()
        is UserMessage.Invalid -> "invalid." + error.name.lowercase()
    }
