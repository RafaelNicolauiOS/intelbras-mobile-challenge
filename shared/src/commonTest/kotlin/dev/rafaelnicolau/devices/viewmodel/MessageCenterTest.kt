package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.telemetry.RecordingExporter
import dev.rafaelnicolau.devices.telemetry.Severity
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageCenterTest {
    private val created = UserMessage.Success(SuccessKind.DEVICE_CREATED)
    private val failed = UserMessage.Failure(DeviceError.NETWORK)

    @Test
    fun messagePostedBeforeAnyoneListensIsStillDelivered() = runTest {
        val center = MessageCenter()

        center.post(created)

        assertEquals(created, center.messages.first())
    }

    @Test
    fun messagesArriveInTheOrderTheyWerePosted() = runTest {
        val center = MessageCenter()

        center.post(created)
        center.post(failed)

        assertEquals(listOf(created, failed), center.messages.take(2).toList())
    }

    @Test
    fun eachMessageIsDeliveredOnlyOnce() = runTest {
        val center = MessageCenter()
        center.post(created)
        center.messages.first()

        val again = withTimeoutOrNull(100) { center.messages.first() }

        assertNull(again)
    }

    @Test
    fun aListenerReceivesMessagesPostedWhileItWaits() = runTest {
        val center = MessageCenter()
        val received = mutableListOf<UserMessage>()
        val job = launch { center.messages.take(1).toList(received) }
        testScheduler.advanceUntilIdle()

        center.post(failed)
        job.join()

        assertEquals(listOf<UserMessage>(failed), received)
    }

    @Test
    fun everyMessageIsRecordedAsAUserMessageEvent() {
        val exporter = RecordingExporter()
        val center = MessageCenter().apply { telemetry = Telemetry(listOf(exporter)) }

        center.post(created)
        center.post(failed)
        center.post(UserMessage.Invalid(FieldError.INVALID_SERIAL))

        assertEquals(List(3) { "user.message" }, exporter.logs.map { it.name })
        assertEquals(listOf(Severity.INFO, Severity.WARN, Severity.WARN), exporter.logs.map { it.severity })
        assertEquals(
            listOf("device_created", "failure.network", "invalid.invalid_serial"),
            exporter.logs.map { it.attributes[TelemetryKeys.MESSAGE_KIND] },
        )
    }
}
