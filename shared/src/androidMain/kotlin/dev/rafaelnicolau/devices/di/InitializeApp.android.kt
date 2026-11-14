package dev.rafaelnicolau.devices.di

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.room.Room
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.DATABASE_FILE_NAME
import dev.rafaelnicolau.devices.telemetry.TelemetryConfig
import dev.rafaelnicolau.devices.telemetry.TelemetryDevice
import kotlinx.coroutines.Dispatchers

/** Call once from `Application.onCreate`. */
fun initializeApp(context: Context, config: ApiConfig, telemetry: TelemetryConfig = TelemetryConfig()) {
    val appContext = context.applicationContext
    val builder = Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
    ).setQueryCoroutineContext(Dispatchers.IO)

    AppContainer.initialize(config, telemetry, device = telemetryDevice(appContext), databaseBuilder = builder)
}

/**
 * ANDROID_ID is unique per device, user and app signing key, survives reinstalls and changes
 * only on a factory reset; it identifies the device in telemetry, never for ads.
 */
@SuppressLint("HardwareIds")
private fun telemetryDevice(context: Context) = TelemetryDevice(
    platform = "android",
    id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
    model = Build.MODEL.orEmpty(),
    manufacturer = Build.MANUFACTURER.orEmpty(),
)
