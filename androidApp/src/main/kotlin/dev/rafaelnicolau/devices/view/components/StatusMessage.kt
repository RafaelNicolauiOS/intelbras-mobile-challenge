package dev.rafaelnicolau.devices.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rafaelnicolau.devices.models.DeviceError

/** Full-screen message for empty and error states, with an optional retry action. */
@Composable
fun StatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

fun DeviceError.message(): String = when (this) {
    DeviceError.NETWORK -> "Sem conexão. Verifique sua internet."
    DeviceError.TIMEOUT -> "O servidor demorou para responder."
    DeviceError.UNAUTHORIZED -> "Token de acesso inválido ou expirado."
    DeviceError.NOT_FOUND -> "Dispositivo não encontrado no servidor."
    DeviceError.CONFLICT -> "Já existe um dispositivo com esses dados."
    DeviceError.UNKNOWN -> "Algo deu errado. Tente novamente."
}
