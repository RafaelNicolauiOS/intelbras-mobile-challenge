package dev.rafaelnicolau.devices.di

import androidx.room.RoomDatabase
import dev.rafaelnicolau.devices.repository.DefaultDeviceRepository
import dev.rafaelnicolau.devices.repository.DeviceRepository
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.buildAppDatabase
import dev.rafaelnicolau.devices.repository.remote.DeviceApi
import dev.rafaelnicolau.devices.repository.remote.createHttpClient
import dev.rafaelnicolau.devices.telemetry.Telemetry
import dev.rafaelnicolau.devices.telemetry.TelemetryConfig
import dev.rafaelnicolau.devices.telemetry.TelemetryDevice
import dev.rafaelnicolau.devices.viewmodel.DashboardViewModel
import dev.rafaelnicolau.devices.viewmodel.DeviceDetailsViewModel
import dev.rafaelnicolau.devices.viewmodel.DeviceFormViewModel
import dev.rafaelnicolau.devices.viewmodel.MessageCenter

/**
 * Hand-written composition root: builds the object graph once and gives both apps the
 * same way to obtain ViewModels. Each app calls its platform `initializeApp` at startup.
 */
object AppContainer {
    lateinit var deviceRepository: DeviceRepository
        private set

    /** The one channel every ViewModel reports user feedback to. */
    val messageCenter = MessageCenter()

    /** Disabled until [initialize]; the apps call [Telemetry.flush] when they go to the background. */
    var telemetry: Telemetry = Telemetry.disabled
        private set

    internal fun initialize(
        config: ApiConfig,
        telemetryConfig: TelemetryConfig,
        device: TelemetryDevice,
        databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    ) {
        telemetry = telemetryConfig.build(device)
        messageCenter.telemetry = telemetry
        val database = databaseBuilder.buildAppDatabase()
        val api = DeviceApi(createHttpClient(config, telemetry = telemetry))
        deviceRepository = DefaultDeviceRepository(api, database.deviceDao(), telemetry)
    }

    fun dashboardViewModel(): DashboardViewModel = DashboardViewModel(deviceRepository, messageCenter)

    fun deviceDetailsViewModel(deviceId: String): DeviceDetailsViewModel =
        DeviceDetailsViewModel(deviceId, deviceRepository, messageCenter)

    /** A null [deviceId] opens the form in "add" mode. */
    fun deviceFormViewModel(deviceId: String?): DeviceFormViewModel =
        DeviceFormViewModel(deviceId, deviceRepository, messageCenter)
}
