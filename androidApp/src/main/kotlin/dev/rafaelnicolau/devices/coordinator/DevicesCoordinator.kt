package dev.rafaelnicolau.devices.coordinator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Owns navigation for the devices flow. Screens never touch the NavController: they
 * report user intent through callbacks and the coordinator decides where to go.
 */
class DevicesCoordinator(val navController: NavHostController) {
    fun showDeviceDetails(deviceId: String) = navController.navigate(Route.deviceDetails(deviceId))

    fun showAddDevice() = navController.navigate(Route.deviceForm(deviceId = null))

    fun showEditDevice(deviceId: String) = navController.navigate(Route.deviceForm(deviceId))

    fun back() {
        navController.popBackStack()
    }
}

@Composable
fun rememberDevicesCoordinator(
    navController: NavHostController = rememberNavController(),
): DevicesCoordinator = remember(navController) { DevicesCoordinator(navController) }
