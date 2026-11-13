package dev.rafaelnicolau.devices.view.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.view.components.label
import dev.rafaelnicolau.devices.view.components.text
import dev.rafaelnicolau.devices.viewmodel.DeviceFormUiState

/** What the form reports upwards; grouped so the screen signature stays readable. */
data class DeviceFormActions(
    val onTypeChange: (DeviceType) -> Unit,
    val onNameChange: (String) -> Unit,
    val onMacAddressChange: (String) -> Unit,
    val onSerialChange: (String) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceFormScreen(state: DeviceFormUiState, actions: DeviceFormActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar dispositivo" else "Adicionar dispositivo") },
                navigationIcon = { TextButton(onClick = actions.onBack) { Text("Voltar") } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The type decides which API resource is used, so it is fixed once the device exists.
            if (!state.isEditing) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DeviceType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { actions.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, DeviceType.entries.size),
                        ) { Text(type.label()) }
                    }
                }
            }

            FormField("Nome", state.name, state.errors.name, actions.onNameChange)

            when (state.type) {
                DeviceType.ALARM -> {
                    FormField(
                        label = "Endereço MAC",
                        value = state.macAddress,
                        error = state.errors.macAddress,
                        onValueChange = actions.onMacAddressChange,
                        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    )
                    FormField(
                        label = "Senha (4 ou 6 dígitos)",
                        value = state.password,
                        error = state.errors.password,
                        onValueChange = actions.onPasswordChange,
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                DeviceType.VIDEO -> {
                    FormField("Número de série", state.serial, state.errors.serial, actions.onSerialChange)
                    FormField("Usuário", state.username, state.errors.username, actions.onUsernameChange)
                    FormField(
                        label = "Senha",
                        value = state.password,
                        error = state.errors.password,
                        onValueChange = actions.onPasswordChange,
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            }

            Button(
                onClick = actions.onSubmit,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(if (state.isSaving) "Salvando…" else "Salvar")
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: FieldError?,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it.text()) } },
        singleLine = true,
        keyboardOptions = keyboard,
        modifier = Modifier.fillMaxWidth(),
    )
}
