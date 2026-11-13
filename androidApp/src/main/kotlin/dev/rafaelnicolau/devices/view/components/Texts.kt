package dev.rafaelnicolau.devices.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage

fun UserMessage.text(): String = when (this) {
    is UserMessage.Success -> when (kind) {
        SuccessKind.DEVICE_CREATED -> "Dispositivo adicionado"
        SuccessKind.DEVICE_UPDATED -> "Dispositivo atualizado"
        SuccessKind.DEVICE_DELETED -> "Dispositivo removido"
        SuccessKind.COPIED -> "Copiado"
    }
    is UserMessage.Failure -> error.message()
}

fun DeviceError.message(): String = when (this) {
    DeviceError.NETWORK -> "Sem conexão. Verifique sua internet."
    DeviceError.TIMEOUT -> "O servidor demorou para responder."
    DeviceError.UNAUTHORIZED -> "Token de acesso inválido ou expirado."
    DeviceError.NOT_FOUND -> "Dispositivo não encontrado no servidor."
    DeviceError.CONFLICT -> "Já existe um dispositivo com esses dados."
    DeviceError.UNKNOWN -> "Algo deu errado. Tente novamente."
}

fun FieldError.text(): String = when (this) {
    FieldError.REQUIRED -> "Campo obrigatório."
    FieldError.INVALID_MAC -> "Informe os 12 caracteres do MAC (0-9, A-F)."
    FieldError.INVALID_ALARM_PASSWORD -> "A senha deve ter 4 ou 6 dígitos."
    FieldError.DUPLICATE -> "Já está em uso por outro dispositivo."
}

/** How the type is named in the form selector and in the information screen. */
fun DeviceType.label(): String = when (this) {
    DeviceType.ALARM -> "Alarme"
    DeviceType.VIDEO -> "Vídeo"
}

/** How the type is named as the title of the actions sheet. */
fun DeviceType.sheetTitle(): String = when (this) {
    DeviceType.ALARM -> "Alarme"
    DeviceType.VIDEO -> "Câmera"
}

fun DeviceType.icon(): ImageVector = when (this) {
    DeviceType.ALARM -> Icons.Outlined.NotificationsActive
    DeviceType.VIDEO -> Icons.Outlined.Videocam
}

fun DeviceFilter.label(): String = when (this) {
    DeviceFilter.ALL -> "Todos"
    DeviceFilter.FAVORITES -> "Favoritos"
    DeviceFilter.CAMERAS -> "Câmeras"
    DeviceFilter.ALARMS -> "Alarmes"
}
