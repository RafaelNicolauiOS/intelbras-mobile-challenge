package dev.rafaelnicolau.devices.view.components

import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage

fun UserMessage.text(): String = when (this) {
    is UserMessage.Success -> when (kind) {
        SuccessKind.DEVICE_CREATED -> "Dispositivo adicionado."
        SuccessKind.DEVICE_UPDATED -> "Dispositivo atualizado."
        SuccessKind.DEVICE_DELETED -> "Dispositivo removido."
    }
    is UserMessage.Failure -> error.message()
}

fun FieldError.text(): String = when (this) {
    FieldError.REQUIRED -> "Campo obrigatório."
    FieldError.INVALID_MAC -> "Informe os 12 caracteres do MAC (0-9, A-F)."
    FieldError.INVALID_ALARM_PASSWORD -> "A senha deve ter 4 ou 6 dígitos."
    FieldError.DUPLICATE -> "Já está em uso por outro dispositivo."
}

fun DeviceType.label(): String = when (this) {
    DeviceType.ALARM -> "Alarme"
    DeviceType.VIDEO -> "Câmera"
}
