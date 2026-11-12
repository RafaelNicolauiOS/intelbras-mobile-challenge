package dev.rafaelnicolau.devices.di

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.repository.DeviceRepository
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import dev.rafaelnicolau.devices.viewmodel.DashboardViewModel
import dev.rafaelnicolau.devices.viewmodel.DeviceDetailsViewModel

/**
 * Hand-written composition root so both apps build their ViewModels the same way.
 * Temporary: sample data and manual wiring give way to Koin and the real repository.
 */
object AppContainer {
    /** Set once by each app at startup, before anything else is requested. */
    lateinit var apiConfig: ApiConfig
        private set

    fun initialize(config: ApiConfig) {
        apiConfig = config
    }

    val deviceRepository: DeviceRepository by lazy {
        InMemoryDeviceRepository(
            listOf(
                Device.Alarm(id = "sample-alarm", name = "Central da casa", macAddress = "1A2B3C4D5E6F", password = "1234"),
                Device.Video(id = "sample-video", name = "Câmera da garagem", serial = "ABCDEF1234567", username = "admin", password = "1234"),
            ),
        )
    }

    fun dashboardViewModel(): DashboardViewModel = DashboardViewModel(deviceRepository)

    fun deviceDetailsViewModel(deviceId: String): DeviceDetailsViewModel =
        DeviceDetailsViewModel(deviceId, deviceRepository)
}
