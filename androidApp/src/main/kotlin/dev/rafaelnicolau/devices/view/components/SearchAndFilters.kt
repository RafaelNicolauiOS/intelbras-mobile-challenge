package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.view.theme.AppColors
import dev.rafaelnicolau.devices.view.theme.AppShapes

/**
 * Search by device name. The text being typed is kept here and only reported upwards:
 * the screen state arrives asynchronously, and echoing it back into the field would
 * fight with fast typing.
 */
@Composable
fun SearchField(onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    val update = { value: String ->
        text = value
        onQueryChange(value)
    }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = AppShapes.Pill,
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.SurfaceStrong),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColors.Text)
            Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (text.isEmpty()) {
                    Text("Pesquisar", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
                BasicTextField(
                    value = text,
                    onValueChange = update,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Text),
                    cursorBrush = SolidColor(AppColors.Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (text.isNotEmpty()) {
                IconButton(onClick = { update("") }) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Limpar busca",
                        tint = AppColors.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selected: DeviceFilter,
    onSelected: (DeviceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DeviceFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Surface(
                onClick = { onSelected(filter) },
                shape = AppShapes.Pill,
                color = if (isSelected) AppColors.PrimarySoft else AppColors.SurfaceStrong,
                modifier = Modifier.semantics {
                    this.selected = isSelected
                    role = Role.Tab
                },
            ) {
                Text(
                    text = filter.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) AppColors.Primary else AppColors.Text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
