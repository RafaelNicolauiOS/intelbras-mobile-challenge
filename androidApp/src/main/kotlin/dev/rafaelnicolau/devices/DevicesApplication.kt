package dev.rafaelnicolau.devices

import android.app.Application
import dev.rafaelnicolau.devices.di.ApiConfig
import dev.rafaelnicolau.devices.di.initializeApp

class DevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeApp(
            context = this,
            config = ApiConfig(baseUrl = BuildConfig.API_BASE_URL, token = BuildConfig.API_TOKEN),
        )
    }
}
