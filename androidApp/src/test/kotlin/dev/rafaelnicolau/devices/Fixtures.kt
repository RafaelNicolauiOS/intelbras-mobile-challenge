package dev.rafaelnicolau.devices

import dev.rafaelnicolau.devices.models.Device

/** Devices shared by the Android UI tests. */
object Fixtures {
    val alarm = Device.Alarm(id = "a1", name = "Central da Casa", macAddress = "1A2B3C4D5E6F", password = "1234")
    val camera = Device.Video(id = "v1", name = "Câmera Garagem", isFavorite = true, serial = "SN-001", username = "admin", password = "x")
}
