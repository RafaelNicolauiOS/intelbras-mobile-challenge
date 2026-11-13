package dev.rafaelnicolau.devices.view.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.view.components.label
import dev.rafaelnicolau.devices.view.components.text
import dev.rafaelnicolau.devices.view.theme.AppColors
import dev.rafaelnicolau.devices.view.theme.AppShapes
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
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditing) "Editar dispositivo" else "Novo dispositivo",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                    }
                },
                actions = {
                    TextButton(onClick = actions.onSubmit, enabled = !state.isSaving && !state.isLoading) {
                        Text(if (state.isSaving) "Salvando…" else "Salvar", style = MaterialTheme.typography.labelLarge)
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
                Text("Tipo de dispositivo", style = MaterialTheme.typography.labelLarge, color = AppColors.Text)
                DeviceTypeSelector(selected = state.type, onSelected = actions.onTypeChange)
            }

            FormField("Nome", state.name, state.errors.name, actions.onNameChange)

            when (state.type) {
                DeviceType.ALARM -> {
                    FormField(
                        label = "MAC Address",
                        value = state.macAddress,
                        error = state.errors.macAddress,
                        onValueChange = actions.onMacAddressChange,
                        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    )
                    FormField(
                        label = "Senha",
                        value = state.password,
                        error = state.errors.password,
                        onValueChange = actions.onPasswordChange,
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isPassword = true,
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
                        isPassword = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTypeSelector(selected: DeviceType, onSelected: (DeviceType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceStrong, AppShapes.Card)
            .padding(2.dp),
    ) {
        DeviceType.entries.forEach { type ->
            val isSelected = type == selected
            Surface(
                onClick = { onSelected(type) },
                shape = AppShapes.Card,
                color = if (isSelected) AppColors.Background else AppColors.SurfaceStrong,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .semantics {
                        this.selected = isSelected
                        role = Role.Tab
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = type.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) AppColors.Primary else AppColors.Text,
                        textAlign = TextAlign.Center,
                    )
                }
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
    isPassword: Boolean = false,
) {
    var isHidden by rememberSaveable { mutableStateOf(true) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it.text()) } },
        singleLine = true,
        keyboardOptions = keyboard,
        visualTransformation = if (isPassword && isHidden) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (!isPassword) null else {
            {
                IconButton(onClick = { isHidden = !isHidden }) {
                    Icon(
                        imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isHidden) "Mostrar senha" else "Ocultar senha",
                    )
                }
            }
        },
        shape = AppShapes.Field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.TextSecondary,
            focusedLabelColor = AppColors.Primary,
            unfocusedLabelColor = AppColors.TextSecondary,
            cursorColor = AppColors.Primary,
            focusedTrailingIconColor = AppColors.TextSecondary,
            unfocusedTrailingIconColor = AppColors.TextSecondary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
