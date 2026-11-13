package dev.rafaelnicolau.devices.view.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.view.components.AppCard
import dev.rafaelnicolau.devices.view.components.DeviceTypeIcon
import dev.rafaelnicolau.devices.view.components.StatusMessage
import dev.rafaelnicolau.devices.view.components.label
import dev.rafaelnicolau.devices.view.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    device: Device?,
    onCopy: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Informações", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                    titleContentColor = AppColors.Text,
                    navigationIconContentColor = AppColors.Text,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (device == null) {
                StatusMessage("Dispositivo não encontrado.", Modifier.fillMaxWidth())
                return@Column
            }
            AppCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeviceTypeIcon(device.type)
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Text,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            AppCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("Tipo", device.type.label())
                    when (device) {
                        is Device.Alarm -> InfoRow("MAC Address", device.identifier, onCopy)
                        is Device.Video -> {
                            InfoRow("Número de série", device.serial, onCopy)
                            InfoRow("Usuário", device.username)
                        }
                    }
                }
            }
        }
    }
}

/** A label and its value; with [onCopy] the value gets a copy button. */
@Composable
private fun InfoRow(label: String, value: String, onCopy: ((String) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = AppColors.Text, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AppColors.Text)
        if (onCopy != null) {
            IconButton(onClick = { onCopy(value) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copiar $label",
                    tint = AppColors.Text,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
