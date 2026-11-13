package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.view.theme.AppColors
import dev.rafaelnicolau.devices.view.theme.AppShapes

/** The bordered card used across the app to group related content. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.SurfaceStrong),
        content = content,
    )
}

/** The square with the device-type icon that leads a device row. */
@Composable
fun DeviceTypeIcon(type: DeviceType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(AppColors.Background, AppShapes.Card)
            .border(1.dp, AppColors.SurfaceStrong, AppShapes.Card),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = type.sheetTitle(),
            tint = AppColors.Text,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** All devices inside one card, separated by dividers, as in the prototype. */
@Composable
fun DeviceListCard(
    devices: List<Device>,
    onOpenDevice: (Device) -> Unit,
    onMoreClick: (Device) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Column {
            devices.forEachIndexed { index, device ->
                key(device.id) {
                    if (index > 0) {
                        HorizontalDivider(
                            color = AppColors.SurfaceStrong,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    DeviceRow(
                        device = device,
                        onClick = { onOpenDevice(device) },
                        onMoreClick = { onMoreClick(device) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceTypeIcon(device.type)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = device.identifier,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Mais ações de ${device.name}",
                tint = AppColors.Text,
            )
        }
    }
}
