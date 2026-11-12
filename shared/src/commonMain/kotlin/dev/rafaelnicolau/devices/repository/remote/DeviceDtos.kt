package dev.rafaelnicolau.devices.repository.remote

import kotlinx.serialization.Serializable

/** List endpoints wrap their payload as `{ "count": n, "data": [...] }`. */
@Serializable
data class ListResponse<T>(
    val count: Int = 0,
    val data: List<T> = emptyList(),
)

@Serializable
data class AlarmCentralDto(
    val id: String,
    val name: String,
    val macAddress: String,
    val password: String,
)

@Serializable
data class AlarmCentralBody(
    val name: String,
    val macAddress: String,
    val password: String,
)

@Serializable
data class VideoDeviceDto(
    val id: String,
    val name: String,
    val serial: String,
    val username: String,
    val password: String,
)

@Serializable
data class VideoDeviceBody(
    val name: String,
    val serial: String,
    val username: String,
    val password: String,
)
