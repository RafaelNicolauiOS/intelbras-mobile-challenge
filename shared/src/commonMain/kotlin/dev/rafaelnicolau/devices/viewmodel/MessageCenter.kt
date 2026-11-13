package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.UserMessage
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

    val messages: Flow<UserMessage> = channel.receiveAsFlow()

    fun post(message: UserMessage) {
        channel.trySend(message)
    }
}
