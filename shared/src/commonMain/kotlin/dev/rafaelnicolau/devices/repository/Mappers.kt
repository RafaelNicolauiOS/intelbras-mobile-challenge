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

internal fun AlarmCentralDto.toEntity() = DeviceEntity(
    id = id,
    type = DeviceType.ALARM,
    name = name,
    macAddress = macAddress,
    password = password,
)

internal fun VideoDeviceDto.toEntity() = DeviceEntity(
    id = id,
    type = DeviceType.VIDEO,
    name = name,
    serial = serial,
    username = username,
    password = password,
)

internal fun DeviceWithFavorite.toDomain(): Device = when (device.type) {
    DeviceType.ALARM -> Device.Alarm(
        id = device.id,
        name = device.name,
        isFavorite = isFavorite,
        macAddress = device.macAddress.orEmpty(),
        password = device.password,
    )
    DeviceType.VIDEO -> Device.Video(
        id = device.id,
        name = device.name,
        isFavorite = isFavorite,
        serial = device.serial.orEmpty(),
        username = device.username.orEmpty(),
        password = device.password,
    )
}

internal fun DeviceDraft.Alarm.toBody() = AlarmCentralBody(name, macAddress, password)

internal fun DeviceDraft.Video.toBody() = VideoDeviceBody(name, serial, username, password)
