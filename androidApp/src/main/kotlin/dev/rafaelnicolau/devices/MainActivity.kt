package dev.rafaelnicolau.devices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.rafaelnicolau.devices.coordinator.DevicesCoordinatorHost
import dev.rafaelnicolau.devices.di.AppContainer
import dev.rafaelnicolau.devices.view.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                DevicesCoordinatorHost()
            }
        }
    }

    /**
     * Sends what telemetry kept while the app was away (Android blocks network access in the
     * background, so exports attempted then are kept, not lost).
     */
    override fun onStart() {
        super.onStart()
        AppContainer.telemetry.flush()
    }

    /** Best effort: Android may already have blocked network access; if so the data is kept for [onStart]. */
    override fun onStop() {
        super.onStop()
        AppContainer.telemetry.flush()
    }
}
