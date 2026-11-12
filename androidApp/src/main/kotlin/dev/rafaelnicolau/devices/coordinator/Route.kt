package dev.rafaelnicolau.devices.coordinator

/** Destinations the devices flow can show. */
object Route {
    const val ARG_DEVICE_ID = "deviceId"

    const val DASHBOARD = "dashboard"
    const val DEVICE_DETAILS = "devices/{$ARG_DEVICE_ID}"
    const val DEVICE_FORM = "device-form?$ARG_DEVICE_ID={$ARG_DEVICE_ID}"

    fun deviceDetails(deviceId: String) = "devices/$deviceId"

    /** A null [deviceId] opens the form in "add" mode. */
    fun deviceForm(deviceId: String?) =
        if (deviceId == null) "device-form" else "device-form?$ARG_DEVICE_ID=$deviceId"
}
