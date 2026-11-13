package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.view.theme.AppColors
import dev.rafaelnicolau.devices.view.theme.AppShapes

/** The options behind a device's "more actions" button. Every choice also dismisses the sheet. */
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Background, dragHandle = null) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
            Text(
                text = device.type.sheetTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
            SheetAction("Informações", Icons.Outlined.Info) { onDismiss(); onInfo() }
            SheetAction("Editar dispositivo", Icons.Outlined.Edit) { onDismiss(); onEdit() }
            SheetAction(
                label = if (device.isFavorite) "Desmarcar favorito" else "Marcar favorito",
                icon = if (device.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
            ) {
                onDismiss()
                onToggleFavorite()
            }
            HorizontalDivider(color = AppColors.BorderMedium, modifier = Modifier.padding(horizontal = 16.dp))
            SheetAction("Remover dispositivo", Icons.Outlined.Delete, tint = AppColors.Error) {
                onDismiss()
                onDelete()
            }
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    icon: ImageVector,
    tint: Color = AppColors.Text,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
fun ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AppShapes.Card, color = AppColors.Surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Remover dispositivo?", style = MaterialTheme.typography.titleLarge, color = AppColors.Text)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Você tem certeza que deseja remover este dispositivo?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.Text,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Confirmar", style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, AppColors.BorderButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Text),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
