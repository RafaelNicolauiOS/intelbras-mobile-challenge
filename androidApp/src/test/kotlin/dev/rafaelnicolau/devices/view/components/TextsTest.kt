package dev.rafaelnicolau.devices.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Videocam
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Portuguese copy shown to users. Pure functions, so plain JUnit is enough. */
class TextsTest {
    @Test
    fun successMessages() {
        assertEquals("Dispositivo adicionado", UserMessage.Success(SuccessKind.DEVICE_CREATED).text())
        assertEquals("Dispositivo atualizado", UserMessage.Success(SuccessKind.DEVICE_UPDATED).text())
        assertEquals("Dispositivo removido", UserMessage.Success(SuccessKind.DEVICE_DELETED).text())
        assertEquals("Copiado", UserMessage.Success(SuccessKind.COPIED).text())
    }

    @Test
    fun failureMessageIsTheErrorMessage() {
        DeviceError.entries.forEach { error ->
            assertEquals(error.message(), UserMessage.Failure(error).text())
        }
    }

    @Test
    fun errorMessages() {
        assertEquals("Sem conexão. Verifique sua internet.", DeviceError.NETWORK.message())
        assertEquals("O servidor demorou para responder.", DeviceError.TIMEOUT.message())
        assertEquals("Token de acesso inválido ou expirado.", DeviceError.UNAUTHORIZED.message())
        assertEquals("Dispositivo não encontrado no servidor.", DeviceError.NOT_FOUND.message())
        assertEquals("Já existe um dispositivo com esses dados.", DeviceError.CONFLICT.message())
        assertEquals("Algo deu errado. Tente novamente.", DeviceError.UNKNOWN.message())
    }

    @Test
    fun everyErrorHasItsOwnMessage() {
        val messages = DeviceError.entries.map { it.message() }
        assertEquals(messages.size, messages.toSet().size)
        assertTrue(messages.all { it.isNotBlank() })
    }

    @Test
    fun fieldErrors() {
        assertEquals("Campo obrigatório.", FieldError.REQUIRED.text())
        assertEquals("Informe os 12 caracteres do MAC (0-9, A-F).", FieldError.INVALID_MAC.text())
        assertEquals("A senha deve ter 4 ou 6 dígitos.", FieldError.INVALID_ALARM_PASSWORD.text())
        assertEquals("Já está em uso por outro dispositivo.", FieldError.DUPLICATE.text())
        assertEquals("O número de série deve conter 13 caracteres.", FieldError.INVALID_SERIAL.text())
    }

    @Test
    fun invalidFormMessageIsTheFieldErrorText() {
        assertEquals(
            "O número de série deve conter 13 caracteres.",
            UserMessage.Invalid(FieldError.INVALID_SERIAL).text(),
        )
    }

    @Test
    fun typeNames() {
        assertEquals("Alarme", DeviceType.ALARM.label())
        assertEquals("Vídeo", DeviceType.VIDEO.label())
        assertEquals("Alarme", DeviceType.ALARM.sheetTitle())
        assertEquals("Câmera", DeviceType.VIDEO.sheetTitle())
    }

    @Test
    fun typeIcons() {
        assertEquals(Icons.Outlined.NotificationsActive, DeviceType.ALARM.icon())
        assertEquals(Icons.Outlined.Videocam, DeviceType.VIDEO.icon())
    }

    @Test
    fun filterLabels() {
        assertEquals(
            listOf("Todos", "Favoritos", "Câmeras", "Alarmes"),
            DeviceFilter.entries.map { it.label() },
        )
    }
}
