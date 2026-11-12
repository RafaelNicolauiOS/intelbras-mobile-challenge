package dev.rafaelnicolau.devices.view.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    device: Device?,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informações") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
                actions = { TextButton(onClick = onEdit) { Text("Editar") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (device) {
                null -> Text("Dispositivo não encontrado")
                is Device.Alarm -> {
                    Text("Nome: ${device.name}")
                    Text("MAC: ${device.macAddress}")
                }
                is Device.Video -> {
                    Text("Nome: ${device.name}")
                    Text("Serial: ${device.serial}")
                    Text("Usuário: ${device.username}")
                }
            }
        }
    }
}
