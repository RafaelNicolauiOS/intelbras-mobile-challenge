package dev.rafaelnicolau.devices.coordinator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.rafaelnicolau.devices.di.AppContainer
import dev.rafaelnicolau.devices.view.dashboard.DashboardScreen
import dev.rafaelnicolau.devices.view.details.DeviceDetailsScreen
import dev.rafaelnicolau.devices.view.form.DeviceFormScreen

/** Builds each screen with its ViewModel and wires the screen's callbacks to the coordinator. */
@Composable
fun DevicesCoordinatorHost(coordinator: DevicesCoordinator = rememberDevicesCoordinator()) {
    NavHost(navController = coordinator.navController, startDestination = Route.DASHBOARD) {
        composable(Route.DASHBOARD) {
            val viewModel = viewModel { AppContainer.dashboardViewModel() }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onAddDevice = coordinator::showAddDevice,
                onOpenDevice = { coordinator.showDeviceDetails(it.id) },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }

        composable(
            route = Route.DEVICE_DETAILS,
            arguments = listOf(navArgument(Route.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) { entry ->
            val deviceId = requireNotNull(entry.arguments?.getString(Route.ARG_DEVICE_ID))
            val viewModel = viewModel { AppContainer.deviceDetailsViewModel(deviceId) }
            val device by viewModel.device.collectAsStateWithLifecycle()
            DeviceDetailsScreen(
                device = device,
                onEdit = { coordinator.showEditDevice(deviceId) },
                onBack = coordinator::back,
            )
        }

        composable(
            route = Route.DEVICE_FORM,
            arguments = listOf(
                navArgument(Route.ARG_DEVICE_ID) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) { entry ->
            DeviceFormScreen(
                deviceId = entry.arguments?.getString(Route.ARG_DEVICE_ID),
                onBack = coordinator::back,
            )
        }
    }
}
