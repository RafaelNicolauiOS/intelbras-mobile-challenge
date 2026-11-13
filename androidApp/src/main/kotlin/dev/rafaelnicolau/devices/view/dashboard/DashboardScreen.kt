package dev.rafaelnicolau.devices.view.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.view.components.ConfirmDeleteDialog
import dev.rafaelnicolau.devices.view.components.DeviceActionsSheet
import dev.rafaelnicolau.devices.view.components.DeviceListCard
import dev.rafaelnicolau.devices.view.components.FilterChipsRow
import dev.rafaelnicolau.devices.view.components.SearchField
import dev.rafaelnicolau.devices.view.components.StatusMessage
import dev.rafaelnicolau.devices.view.components.message
import dev.rafaelnicolau.devices.view.theme.AppColors
import dev.rafaelnicolau.devices.view.theme.AppShapes
import dev.rafaelnicolau.devices.viewmodel.DashboardUiState

/** Stateless: renders [state] and reports intent upwards. Navigation belongs to the coordinator. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAddDevice: () -> Unit,
    onOpenDevice: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onToggleFavorite: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (DeviceFilter) -> Unit,
) {
    val error = state.error
    // Ids, not devices: the sheet and dialog always show the current data from the state.
    var sheetDeviceId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteDeviceId by rememberSaveable { mutableStateOf<String?>(null) }

    state.devices.firstOrNull { it.id == sheetDeviceId }?.let { device ->
        DeviceActionsSheet(
            device = device,
            onEdit = { onEditDevice(device) },
            onInfo = { onOpenDevice(device) },
            onToggleFavorite = { onToggleFavorite(device) },
            onDelete = { deleteDeviceId = device.id },
            onDismiss = { sheetDeviceId = null },
        )
    }
    state.devices.firstOrNull { it.id == deleteDeviceId }?.let { device ->
        ConfirmDeleteDialog(
            onConfirm = {
                deleteDeviceId = null
                onDeleteDevice(device)
            },
            onDismiss = { deleteDeviceId = null },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = { AddDeviceButton(onClick = onAddDevice) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text(
                text = "Meus dispositivos",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.Text,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
            )
            Text(
                text = deviceCount(state.totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Text,
            )

            // Search and chips stay on screen whenever there is something to search in,
            // including when the current search matches nothing.
            if (state.hasAnyDevice) {
                SearchField(onQueryChange = onQueryChange, modifier = Modifier.padding(top = 16.dp))
                FilterChipsRow(
                    selected = state.filter,
                    onSelected = onFilterSelected,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator()

                    // Nothing cached and the sync failed: the error takes the whole screen.
                    !state.hasAnyDevice && error != null ->
                        StatusMessage(message = error.message(), onRetry = onRefresh)

                    !state.hasAnyDevice ->
                        StatusMessage(message = "Nenhum dispositivo cadastrado.", onRetry = onRefresh)

                    else -> PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Cached devices stay visible; the failed sync is only a notice.
                            if (error != null) {
                                item(key = "sync-error") {
                                    Text(
                                        text = "${error.message()} Mostrando os últimos dados salvos.",
                                        color = AppColors.Error,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            if (state.devices.isEmpty()) {
                                item(key = "no-results") {
                                    StatusMessage(
                                        message = "Nenhum dispositivo encontrado.",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                item(key = "devices") {
                                    DeviceListCard(
                                        devices = state.devices,
                                        onOpenDevice = onOpenDevice,
                                        onMoreClick = { sheetDeviceId = it.id },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun deviceCount(count: Int): String = when (count) {
    0 -> "Nenhum dispositivo"
    1 -> "1 dispositivo"
    else -> "$count dispositivos"
}

@Composable
private fun AddDeviceButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = AppShapes.Pill,
        color = AppColors.SurfaceStrong,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .heightIn(min = 48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = AppColors.Text)
            Text(
                text = "Adicionar",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.Text,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
