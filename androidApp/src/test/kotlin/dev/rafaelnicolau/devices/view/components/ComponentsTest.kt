package dev.rafaelnicolau.devices.view.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.rafaelnicolau.devices.Fixtures.alarm
import dev.rafaelnicolau.devices.Fixtures.camera
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.view.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun statusMessageWithoutRetryHasNoButton() {
        compose.setContent { AppTheme { StatusMessage("Vazio") } }

        compose.onNodeWithText("Vazio").assertExists()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test
    fun statusMessageRetryCallsBack() {
        compose.setContent { AppTheme { StatusMessage("Erro", onRetry = { events += "retry" }) } }

        compose.onNodeWithText("Tentar novamente").performClick()

        assertEquals(listOf("retry"), events)
    }

    @Test
    fun searchFieldClearButtonAppearsOnlyWithTextAndResetsTheQuery() {
        compose.setContent { AppTheme { SearchField(onQueryChange = { events += it }) } }
        compose.onNodeWithContentDescription("Limpar busca").assertDoesNotExist()

        compose.onNode(hasSetTextAction()).performTextInput("gar")
        compose.onNodeWithContentDescription("Limpar busca").performClick()

        assertEquals(listOf("gar", ""), events)
        compose.onNodeWithContentDescription("Limpar busca").assertDoesNotExist()
        compose.onNodeWithText("Pesquisar").assertExists()
    }

    @Test
    fun filterChipsShowEveryFilterAndMarkTheSelectedOne() {
        compose.setContent {
            AppTheme { FilterChipsRow(selected = DeviceFilter.CAMERAS, onSelected = { events += it.name }) }
        }

        compose.onNodeWithText("Câmeras").assertIsSelected()
        compose.onNodeWithText("Todos").assertIsNotSelected()
        compose.onNodeWithText("Alarmes").performClick()

        assertEquals(listOf("ALARMS"), events)
    }

    @Test
    fun deviceListCardReportsOpenAndMore() {
        compose.setContent {
            AppTheme {
                DeviceListCard(
                    devices = listOf(alarm, camera),
                    onOpenDevice = { events += "open:${it.id}" },
                    onMoreClick = { events += "more:${it.id}" },
                )
            }
        }

        compose.onNodeWithText("Central da Casa").performClick()
        compose.onNodeWithContentDescription("Mais ações de Câmera Garagem").performClick()

        assertEquals(listOf("open:a1", "more:v1"), events)
    }

    @Test
    fun deviceTypeIconDescribesTheType() {
        compose.setContent { AppTheme { DeviceTypeIcon(DeviceType.VIDEO) } }

        compose.onNodeWithContentDescription("Câmera").assertExists()
    }

    @Test
    fun appCardShowsItsContent() {
        compose.setContent { AppTheme { AppCard { Text("dentro") } } }

        compose.onNodeWithText("dentro").assertExists()
    }

    @Test
    fun everySheetActionAlsoDismissesTheSheet() {
        compose.setContent {
            AppTheme {
                DeviceActionsSheet(
                    device = alarm,
                    onEdit = { events += "edit" },
                    onInfo = { events += "info" },
                    onToggleFavorite = { events += "favorite" },
                    onDelete = { events += "delete" },
                    onDismiss = { events += "dismiss" },
                )
            }
        }

        compose.onNodeWithText("Alarme").assertExists()
        compose.onNodeWithText("Informações").performClick()
        compose.onNodeWithText("Editar dispositivo").performClick()
        compose.onNodeWithText("Marcar favorito").performClick()
        compose.onNodeWithText("Remover dispositivo").performClick()

        assertEquals(
            listOf("dismiss", "info", "dismiss", "edit", "dismiss", "favorite", "dismiss", "delete"),
            events,
        )
    }

    @Test
    fun confirmDeleteDialogReportsTheChoice() {
        compose.setContent {
            AppTheme { ConfirmDeleteDialog(onConfirm = { events += "confirm" }, onDismiss = { events += "cancel" }) }
        }

        compose.onNodeWithText("Você tem certeza que deseja remover este dispositivo?").assertExists()
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Cancelar").performClick()

        assertEquals(listOf("confirm", "cancel"), events)
    }
}
