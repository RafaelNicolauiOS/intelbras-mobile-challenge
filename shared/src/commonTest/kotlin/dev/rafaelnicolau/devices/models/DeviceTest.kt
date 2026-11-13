package dev.rafaelnicolau.devices.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeviceTest {
    private val alarm = Device.Alarm(id = "a1", name = "Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    private val video = Device.Video(id = "v1", name = "Garagem", serial = "SN-42", username = "admin", password = "x")

    @Test
    fun eachKindReportsItsType() {
        assertEquals(DeviceType.ALARM, alarm.type)
        assertEquals(DeviceType.VIDEO, video.type)
    }

    @Test
    fun devicesAreNotFavoritesByDefault() {
        assertFalse(alarm.isFavorite)
        assertFalse(video.isFavorite)
    }

    @Test
    fun alarmIsIdentifiedByItsFormattedMac() {
        assertEquals("1A:2B:3C:4D:5E:6F", alarm.identifier)
    }

    @Test
    fun alarmWithAnUnexpectedMacShowsItAsStored() {
        assertEquals("abc", alarm.copy(macAddress = "abc").identifier)
    }

    @Test
    fun videoIsIdentifiedByItsSerial() {
        assertEquals("SN-42", video.identifier)
    }

    @Test
    fun devicesWithTheSameDataAreEqual() {
        assertEquals(alarm, alarm.copy())
        assertNotEquals(alarm, alarm.copy(isFavorite = true))
    }

    @Test
    fun formErrorsReportWhetherAnyFieldFailed() {
        assertFalse(DeviceFormErrors().hasErrors)
        assertTrue(DeviceFormErrors(name = FieldError.REQUIRED).hasErrors)
        assertTrue(DeviceFormErrors(macAddress = FieldError.INVALID_MAC).hasErrors)
        assertTrue(DeviceFormErrors(serial = FieldError.DUPLICATE).hasErrors)
        assertTrue(DeviceFormErrors(username = FieldError.REQUIRED).hasErrors)
        assertTrue(DeviceFormErrors(password = FieldError.INVALID_ALARM_PASSWORD).hasErrors)
    }

    @Test
    fun outcomesAndMessagesCompareByValue() {
        assertEquals(Outcome.Success(Unit), Outcome.Success(Unit))
        assertEquals(Outcome.Failure(DeviceError.NETWORK), Outcome.Failure(DeviceError.NETWORK))
        assertNotEquals<Outcome<Unit>>(Outcome.Failure(DeviceError.NETWORK), Outcome.Failure(DeviceError.TIMEOUT))
        assertEquals(UserMessage.Success(SuccessKind.COPIED), UserMessage.Success(SuccessKind.COPIED))
        assertEquals(UserMessage.Failure(DeviceError.CONFLICT), UserMessage.Failure(DeviceError.CONFLICT))
    }
}
