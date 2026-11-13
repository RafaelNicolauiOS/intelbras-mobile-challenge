package dev.rafaelnicolau.devices.coordinator

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun detailsRouteCarriesTheDeviceId() {
        assertEquals("devices/v1", Route.deviceDetails("v1"))
    }

    @Test
    fun formRouteWithoutIdIsAddMode() {
        assertEquals("device-form", Route.deviceForm(null))
    }

    @Test
    fun formRouteWithIdIsEditMode() {
        assertEquals("device-form?deviceId=a1", Route.deviceForm("a1"))
    }

    @Test
    fun patternsDeclareTheDeviceIdArgument() {
        assertEquals("devices/{deviceId}", Route.DEVICE_DETAILS)
        assertEquals("device-form?deviceId={deviceId}", Route.DEVICE_FORM)
    }
}
