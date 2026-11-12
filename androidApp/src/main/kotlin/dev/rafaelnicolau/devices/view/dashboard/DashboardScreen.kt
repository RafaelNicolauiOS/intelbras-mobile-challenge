package dev.rafaelnicolau.devices.view.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.view.components.DeviceCard
import dev.rafaelnicolau.devices.view.components.StatusMessage
import dev.rafaelnicolau.devices.view.components.message
import dev.rafaelnicolau.devices.viewmodel.DashboardUiState

/** Stateless: renders [state] and reports intent upwards. Navigation belongs to the coordinator. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAddDevice: () -> Unit,
    onOpenDevice: (Device) -> Unit,
    onToggleFavorite: (Device) -> Unit,
    onRefresh: () -> Unit,
) {
    val error = state.error
    Scaffold(
        topBar = { TopAppBar(title = { Text("Dispositivos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddDevice) { Text("Adicionar") }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()

                // Nothing cached and the sync failed: the error takes the whole screen.
                state.devices.isEmpty() && error != null ->
                    StatusMessage(message = error.message(), onRetry = onRefresh)

                state.devices.isEmpty() ->
                    StatusMessage(message = "Nenhum dispositivo cadastrado.", onRetry = onRefresh)

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Cached devices stay visible; the failed sync is only a notice.
                        if (error != null) {
                            item(key = "sync-error") {
                                Text(
                                    text = "${error.message()} Mostrando os últimos dados salvos.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        items(state.devices, key = { it.id }) { device ->
                            DeviceCard(
                                device = device,
                                onClick = { onOpenDevice(device) },
                                onToggleFavorite = { onToggleFavorite(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}
