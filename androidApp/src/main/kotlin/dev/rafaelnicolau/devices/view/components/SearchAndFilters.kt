package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.DeviceFilter

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
    OutlinedTextField(
        value = text,
        onValueChange = update,
        placeholder = { Text("Buscar por nome") },
        trailingIcon = if (text.isEmpty()) null else {
            { TextButton(onClick = { update("") }) { Text("Limpar") } }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
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
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

fun DeviceFilter.label(): String = when (this) {
    DeviceFilter.ALL -> "Todos"
    DeviceFilter.FAVORITES -> "Favoritos"
    DeviceFilter.CAMERAS -> "Câmeras"
    DeviceFilter.ALARMS -> "Alarmes"
}
