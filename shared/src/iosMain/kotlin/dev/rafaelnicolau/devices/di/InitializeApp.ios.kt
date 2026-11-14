package dev.rafaelnicolau.devices.di

import androidx.room.Room
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.DATABASE_FILE_NAME
import dev.rafaelnicolau.devices.telemetry.TelemetryConfig
import dev.rafaelnicolau.devices.telemetry.TelemetryDevice
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

/** Call once from the app delegate, before any ViewModel is requested. */
fun initializeApp(config: ApiConfig, telemetry: TelemetryConfig) {
    val builder = Room.databaseBuilder<AppDatabase>(name = databasePath())
        .setQueryCoroutineContext(Dispatchers.IO)

    AppContainer.initialize(config, telemetry, device = telemetryDevice(), databaseBuilder = builder)
}

/**
 * identifierForVendor is the same for all apps of this vendor on the device and changes
 * when the last of them is removed; it needs no tracking permission.
 */
private fun telemetryDevice() = TelemetryDevice(
    platform = "ios",
    id = UIDevice.currentDevice.identifierForVendor?.UUIDString.orEmpty(),
    model = modelIdentifier(),
    manufacturer = "Apple",
)

/** The hardware model, e.g. `iPhone15,2`; on the simulator, the model it simulates. */
@OptIn(ExperimentalForeignApi::class)
private fun modelIdentifier(): String {
    val simulated = NSProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] as? String
    if (!simulated.isNullOrBlank()) return simulated
    return memScoped {
        val info = alloc<utsname>()
        uname(info.ptr)
        info.machine.toKString()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documents?.path) { "Documents directory is unavailable" } + "/$DATABASE_FILE_NAME"
}
