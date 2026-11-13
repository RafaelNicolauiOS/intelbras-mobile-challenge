package dev.rafaelnicolau.devices.models

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceFilterTest {
    private val alarm = Device.Alarm(id = "a1", name = "Central da Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    private val camera = Device.Video(id = "v1", name = "Câmera Portão", isFavorite = true, serial = "S1", username = "admin", password = "x")
    private val garage = Device.Video(id = "v2", name = "Garagem", serial = "S2", username = "admin", password = "x")
    private val all = listOf(alarm, camera, garage)

    private fun ids(query: String = "", filter: DeviceFilter = DeviceFilter.ALL) =
        all.filteredBy(query, filter).map { it.id }

    @Test
    fun chipsSelectByTypeAndFavorite() {
        assertEquals(listOf("a1", "v1", "v2"), ids(filter = DeviceFilter.ALL))
        assertEquals(listOf("v1"), ids(filter = DeviceFilter.FAVORITES))
        assertEquals(listOf("v1", "v2"), ids(filter = DeviceFilter.CAMERAS))
        assertEquals(listOf("a1"), ids(filter = DeviceFilter.ALARMS))
    }

    @Test
    fun searchMatchesAnyPartOfTheNameIgnoringCase() {
        assertEquals(listOf("a1"), ids("CASA"))
        assertEquals(listOf("v2"), ids("rag"))
        assertEquals(emptyList(), ids("inexistente"))
    }

    @Test
    fun searchIgnoresAccentsAndSurroundingSpaces() {
        assertEquals(listOf("v1"), ids("  camera portao "))
        assertEquals(listOf("v1"), ids("PORTÃO"))
    }

    @Test
    fun blankSearchKeepsEverything() {
        assertEquals(listOf("a1", "v1", "v2"), ids("   "))
    }

    @Test
    fun searchAndChipCombine() {
        assertEquals(listOf("v2"), ids("ga", DeviceFilter.CAMERAS))
        assertEquals(emptyList(), ids("ga", DeviceFilter.FAVORITES))
    }
}
