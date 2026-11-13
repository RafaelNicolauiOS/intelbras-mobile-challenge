package dev.rafaelnicolau.devices.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceValidatorTest {
    private val alarm = DeviceDraft.Alarm(name = "Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    private val video = DeviceDraft.Video(name = "Garagem", serial = "S1", username = "admin", password = "x")

    @Test
    fun validDraftsHaveNoErrors() {
        assertFalse(DeviceValidator.validate(alarm).hasErrors)
        assertFalse(DeviceValidator.validate(alarm.copy(password = "123456")).hasErrors)
        assertFalse(DeviceValidator.validate(video).hasErrors)
    }

    @Test
    fun blankFieldsAreRequired() {
        val alarmErrors = DeviceValidator.validate(DeviceDraft.Alarm(" ", "", ""))
        assertEquals(FieldError.REQUIRED, alarmErrors.name)
        assertEquals(FieldError.REQUIRED, alarmErrors.macAddress)
        assertEquals(FieldError.REQUIRED, alarmErrors.password)

        val videoErrors = DeviceValidator.validate(DeviceDraft.Video("", " ", "", ""))
        assertEquals(FieldError.REQUIRED, videoErrors.name)
        assertEquals(FieldError.REQUIRED, videoErrors.serial)
        assertEquals(FieldError.REQUIRED, videoErrors.username)
        assertEquals(FieldError.REQUIRED, videoErrors.password)
    }

    @Test
    fun macMustBeTwelveHexCharactersWithoutSeparators() {
        for (mac in listOf("1A2B3C4D5E6", "1A2B3C4D5E6F7", "1A2B3C4D5E6G", "1A:2B:3C:4D:5E:6F", "1a2b3c4d5e6f")) {
            assertEquals(FieldError.INVALID_MAC, DeviceValidator.validate(alarm.copy(macAddress = mac)).macAddress, mac)
        }
    }

    @Test
    fun normalizeMacAcceptsCommonWaysOfTypingIt() {
        assertEquals("1A2B3C4D5E6F", DeviceValidator.normalizeMac(" 1a:2b:3c:4d:5e:6f "))
        assertEquals("1A2B3C4D5E6F", DeviceValidator.normalizeMac("1A-2B-3C-4D-5E-6F"))
    }

    @Test
    fun alarmPasswordMustBeFourOrSixDigits() {
        for (password in listOf("123", "12345", "1234567", "12a4", "12 4")) {
            assertEquals(
                FieldError.INVALID_ALARM_PASSWORD,
                DeviceValidator.validate(alarm.copy(password = password)).password,
                password,
            )
        }
    }

    @Test
    fun videoPasswordIsFreeText() {
        assertNull(DeviceValidator.validate(video.copy(password = "p@ss word!")).password)
        assertTrue(DeviceValidator.validate(video.copy(password = "")).hasErrors)
    }
}
