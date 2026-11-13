package dev.rafaelnicolau.devices.coordinator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.rafaelnicolau.devices.di.AppContainer
import dev.rafaelnicolau.devices.view.components.text
import dev.rafaelnicolau.devices.view.dashboard.DashboardScreen
import dev.rafaelnicolau.devices.view.details.DeviceDetailsScreen
import dev.rafaelnicolau.devices.view.form.DeviceFormActions
import dev.rafaelnicolau.devices.view.form.DeviceFormScreen

/** Builds each screen with its ViewModel and wires the screen's callbacks to the coordinator. */
@Composable
fun DevicesCoordinatorHost(coordinator: DevicesCoordinator = rememberDevicesCoordinator()) {
    // One Snackbar host above every screen, so feedback outlives the screen that caused it
    // (e.g. "device added" shows on the dashboard after the form closes).
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        AppContainer.messageCenter.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message.text())
        }
    }

    Box(Modifier.fillMaxSize()) {
        DevicesNavHost(coordinator)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun DevicesNavHost(coordinator: DevicesCoordinator) {
    NavHost(navController = coordinator.navController, startDestination = Route.DASHBOARD) {
        composable(Route.DASHBOARD) {
            val viewModel = viewModel { AppContainer.dashboardViewModel() }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onAddDevice = coordinator::showAddDevice,
                onOpenDevice = { coordinator.showDeviceDetails(it.id) },
                onEditDevice = { coordinator.showEditDevice(it.id) },
                onToggleFavorite = viewModel::toggleFavorite,
                onDeleteDevice = viewModel::delete,
                onRefresh = viewModel::refresh,
                onQueryChange = viewModel::onQueryChange,
                onFilterSelected = viewModel::onFilterSelected,
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
            val deviceId = entry.arguments?.getString(Route.ARG_DEVICE_ID)
            val viewModel = viewModel { AppContainer.deviceFormViewModel(deviceId) }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.saved.collect { coordinator.back() }
            }
            DeviceFormScreen(
                state = state,
                actions = remember(viewModel) {
                    DeviceFormActions(
                        onTypeChange = viewModel::onTypeChange,
                        onNameChange = viewModel::onNameChange,
                        onMacAddressChange = viewModel::onMacAddressChange,
                        onSerialChange = viewModel::onSerialChange,
                        onUsernameChange = viewModel::onUsernameChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onSubmit = viewModel::submit,
                        onBack = coordinator::back,
                    )
                },
            )
        }
    }
}
