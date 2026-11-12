package dev.rafaelnicolau.devices.di

import androidx.room.RoomDatabase
import dev.rafaelnicolau.devices.repository.DefaultDeviceRepository
import dev.rafaelnicolau.devices.repository.DeviceRepository
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.buildAppDatabase
import dev.rafaelnicolau.devices.repository.remote.DeviceApi
import dev.rafaelnicolau.devices.repository.remote.createHttpClient
import dev.rafaelnicolau.devices.viewmodel.DashboardViewModel
import dev.rafaelnicolau.devices.viewmodel.DeviceDetailsViewModel

/**
 * Hand-written composition root: builds the object graph once and gives both apps the
 * same way to obtain ViewModels. Each app calls its platform `initializeApp` at startup.
 */
object AppContainer {
    lateinit var deviceRepository: DeviceRepository
        private set

    internal fun initialize(config: ApiConfig, databaseBuilder: RoomDatabase.Builder<AppDatabase>) {
        val database = databaseBuilder.buildAppDatabase()
        val api = DeviceApi(createHttpClient(config))
        deviceRepository = DefaultDeviceRepository(api, database.deviceDao())
    }

    fun dashboardViewModel(): DashboardViewModel = DashboardViewModel(deviceRepository)

    fun deviceDetailsViewModel(deviceId: String): DeviceDetailsViewModel =
        DeviceDetailsViewModel(deviceId, deviceRepository)
}
