package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device

/** The options behind a device's kebab button. Every choice also dismisses the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceActionsSheet(
    device: Device,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SheetAction("Editar dispositivo") { onDismiss(); onEdit() }
            SheetAction("Informações") { onDismiss(); onInfo() }
            SheetAction(if (device.isFavorite) "Desmarcar favorito" else "Marcar favorito") {
                onDismiss()
                onToggleFavorite()
            }
            SheetAction("Remover dispositivo") { onDismiss(); onDelete() }
        }
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun ConfirmDeleteDialog(
    device: Device,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover dispositivo?") },
        text = { Text("\"${device.name}\" será removido do servidor. Essa ação não pode ser desfeita.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Remover") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
