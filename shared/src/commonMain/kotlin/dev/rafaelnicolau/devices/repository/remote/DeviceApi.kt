package dev.rafaelnicolau.devices.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Thin mapping of the REST endpoints. Throws on failure; callers wrap calls in [apiCall]. */
class DeviceApi(private val client: HttpClient) {
    suspend fun getAlarmCentrals(): List<AlarmCentralDto> =
        client.get("alarm-centrals").body<ListResponse<AlarmCentralDto>>().data

    suspend fun createAlarmCentral(body: AlarmCentralBody): AlarmCentralDto =
        client.post("alarm-centrals") { setBody(body) }.body()

    suspend fun updateAlarmCentral(id: String, body: AlarmCentralBody): AlarmCentralDto =
        client.patch("alarm-centrals/$id") { setBody(body) }.body()

    suspend fun deleteAlarmCentral(id: String) {
        client.delete("alarm-centrals/$id")
    }

    suspend fun getVideoDevices(): List<VideoDeviceDto> =
        client.get("video-devices").body<ListResponse<VideoDeviceDto>>().data

    suspend fun createVideoDevice(body: VideoDeviceBody): VideoDeviceDto =
        client.post("video-devices") { setBody(body) }.body()

    suspend fun updateVideoDevice(id: String, body: VideoDeviceBody): VideoDeviceDto =
        client.patch("video-devices/$id") { setBody(body) }.body()

    suspend fun deleteVideoDevice(id: String) {
        client.delete("video-devices/$id")
    }
}
