package dev.rafaelnicolau.devices.view.details

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.rafaelnicolau.devices.Fixtures.alarm
import dev.rafaelnicolau.devices.Fixtures.camera
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.view.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceDetailsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val copied = mutableListOf<String>()
    private var backs = 0

    private fun show(device: Device?) = compose.setContent {
        AppTheme {
            DeviceDetailsScreen(device = device, onCopy = { copied += it }, onBack = { backs++ })
        }
    }

    @Test
    fun alarmShowsItsTypeAndFormattedMac() {
        show(alarm)

        compose.onNodeWithText("Informações").assertExists()
        compose.onNodeWithText("Central da Casa").assertExists()
        compose.onNodeWithText("Alarme").assertExists()
        compose.onNodeWithText("1A:2B:3C:4D:5E:6F").assertExists()
        compose.onNodeWithText("Usuário").assertDoesNotExist()
    }

    @Test
    fun copyingTheMacCopiesTheFormattedValue() {
        show(alarm)

        compose.onNodeWithContentDescription("Copiar MAC Address").performClick()

        assertEquals(listOf("1A:2B:3C:4D:5E:6F"), copied)
    }

    @Test
    fun videoShowsSerialAndUserButNeverThePassword() {
        show(camera)

        compose.onNodeWithText("Vídeo").assertExists()
        compose.onNodeWithText("SN-001").assertExists()
        compose.onNodeWithText("admin").assertExists()
        compose.onNodeWithText("x").assertDoesNotExist()
    }

    @Test
    fun onlyTheSerialCanBeCopied() {
        show(camera)

        compose.onNodeWithContentDescription("Copiar Número de série").performClick()
        compose.onNodeWithContentDescription("Copiar Usuário").assertDoesNotExist()

        assertEquals(listOf("SN-001"), copied)
    }

    @Test
    fun missingDeviceSaysItWasNotFound() {
        show(null)

        compose.onNodeWithText("Dispositivo não encontrado.").assertExists()
    }

    @Test
    fun backButtonReportsBack() {
        show(alarm)

        compose.onNodeWithContentDescription("Voltar").performClick()

        assertEquals(1, backs)
    }
}
