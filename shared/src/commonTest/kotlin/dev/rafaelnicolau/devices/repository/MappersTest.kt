package dev.rafaelnicolau.devices.repository

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceDraft
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.repository.local.DeviceEntity
import dev.rafaelnicolau.devices.repository.local.DeviceWithFavorite
import dev.rafaelnicolau.devices.repository.remote.AlarmCentralBody
import dev.rafaelnicolau.devices.repository.remote.AlarmCentralDto
import dev.rafaelnicolau.devices.repository.remote.VideoDeviceBody
import dev.rafaelnicolau.devices.repository.remote.VideoDeviceDto
import kotlin.test.Test
import kotlin.test.assertEquals

class MappersTest {
    private val alarmEntity = DeviceEntity(
        id = "a1", type = DeviceType.ALARM, name = "Casa", macAddress = "1A2B3C4D5E6F", password = "1234",
    )
    private val videoEntity = DeviceEntity(
        id = "v1", type = DeviceType.VIDEO, name = "Garagem", serial = "S1", username = "admin", password = "x",
    )

    @Test
    fun alarmDtoBecomesAnAlarmRow() {
        assertEquals(alarmEntity, AlarmCentralDto("a1", "Casa", "1A2B3C4D5E6F", "1234").toEntity())
    }

    @Test
    fun videoDtoBecomesAVideoRow() {
        assertEquals(videoEntity, VideoDeviceDto("v1", "Garagem", "S1", "admin", "x").toEntity())
    }

    @Test
    fun alarmRowBecomesAnAlarmWithItsFavoriteMark() {
        assertEquals(
            Device.Alarm("a1", "Casa", isFavorite = true, macAddress = "1A2B3C4D5E6F", password = "1234"),
            DeviceWithFavorite(alarmEntity, isFavorite = true).toDomain(),
        )
    }

    @Test
    fun videoRowBecomesAVideo() {
        assertEquals(
            Device.Video("v1", "Garagem", isFavorite = false, serial = "S1", username = "admin", password = "x"),
            DeviceWithFavorite(videoEntity, isFavorite = false).toDomain(),
        )
    }

    @Test
    fun missingTypeSpecificColumnsBecomeEmptyText() {
        val alarm = DeviceWithFavorite(alarmEntity.copy(macAddress = null), false).toDomain() as Device.Alarm
        val video = DeviceWithFavorite(videoEntity.copy(serial = null, username = null), false).toDomain() as Device.Video

        assertEquals("", alarm.macAddress)
        assertEquals("", video.serial)
        assertEquals("", video.username)
    }

    @Test
    fun draftsBecomeRequestBodies() {
        assertEquals(
            AlarmCentralBody("Casa", "1A2B3C4D5E6F", "1234"),
            DeviceDraft.Alarm("Casa", "1A2B3C4D5E6F", "1234").toBody(),
        )
        assertEquals(
            VideoDeviceBody("Garagem", "S1", "admin", "x"),
            DeviceDraft.Video("Garagem", "S1", "admin", "x").toBody(),
        )
    }
}
