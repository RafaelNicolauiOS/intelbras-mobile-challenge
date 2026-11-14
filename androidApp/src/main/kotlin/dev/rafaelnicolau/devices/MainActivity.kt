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

    /** The app may be killed in the background: send what telemetry has buffered now. */
    override fun onStop() {
        super.onStop()
        AppContainer.telemetry.flush()
    }
}
