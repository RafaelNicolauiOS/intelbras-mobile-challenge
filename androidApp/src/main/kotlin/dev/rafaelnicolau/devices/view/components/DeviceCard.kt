package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = device.type.label(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            if (device.isFavorite) {
                Text(text = "★", modifier = Modifier.semantics { contentDescription = "Favorito" })
            }
            TextButton(
                onClick = onMoreClick,
                modifier = Modifier.semantics { contentDescription = "Mais opções de ${device.name}" },
            ) {
                Text("⋮", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
