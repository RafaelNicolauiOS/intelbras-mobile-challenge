package dev.rafaelnicolau.devices.di

import androidx.room.Room
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.DATABASE_FILE_NAME
import dev.rafaelnicolau.devices.telemetry.TelemetryConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Call once from the app delegate, before any ViewModel is requested. */
fun initializeApp(config: ApiConfig, telemetry: TelemetryConfig) {
    val builder = Room.databaseBuilder<AppDatabase>(name = databasePath())
        .setQueryCoroutineContext(Dispatchers.IO)

    AppContainer.initialize(config, telemetry, platform = "ios", databaseBuilder = builder)
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
