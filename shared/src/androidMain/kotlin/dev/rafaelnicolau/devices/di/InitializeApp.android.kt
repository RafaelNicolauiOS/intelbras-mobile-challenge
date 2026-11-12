package dev.rafaelnicolau.devices.di

import android.content.Context
import androidx.room.Room
import dev.rafaelnicolau.devices.repository.local.AppDatabase
import dev.rafaelnicolau.devices.repository.local.DATABASE_FILE_NAME
import kotlinx.coroutines.Dispatchers

/** Call once from `Application.onCreate`. */
fun initializeApp(context: Context, config: ApiConfig) {
    val appContext = context.applicationContext
    val builder = Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
    ).setQueryCoroutineContext(Dispatchers.IO)

    AppContainer.initialize(config, builder)
}
