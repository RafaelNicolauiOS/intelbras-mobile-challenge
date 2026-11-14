package dev.rafaelnicolau.devices.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetryConfigTest {
    @Test
    fun readsASingleHeader() {
        assertEquals(mapOf("api-key" to "abc123NRAL"), parseOtlpHeaders("api-key=abc123NRAL"))
    }

    @Test
    fun readsSeveralHeadersAndIgnoresSpaces() {
        assertEquals(
            mapOf("api-key" to "abc", "x-team" to "mobile"),
            parseOtlpHeaders(" api-key = abc , x-team=mobile "),
        )
    }

    @Test
    fun valueMayContainEqualsSigns() {
        assertEquals(mapOf("Authorization" to "Basic dGVzdDp0ZXN0=="), parseOtlpHeaders("Authorization=Basic dGVzdDp0ZXN0=="))
    }

    @Test
    fun emptyOrMalformedEntriesAreSkipped() {
        assertTrue(parseOtlpHeaders("").isEmpty())
        assertEquals(mapOf("a" to "1"), parseOtlpHeaders("a=1,,=nameless,noequals"))
    }

    @Test
    fun resourceNamesTheServiceAndPlatform() {
        val resource = TelemetryConfig(serviceVersion = "1.0", environment = "debug").resourceAttributes("android")

        assertEquals("devices-app", resource["service.name"])
        assertEquals("android", resource["os.type"])
        assertEquals("debug", resource["deployment.environment.name"])
    }
}
