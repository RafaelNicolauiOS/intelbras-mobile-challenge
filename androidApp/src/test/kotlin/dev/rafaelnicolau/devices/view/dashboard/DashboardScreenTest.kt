package dev.rafaelnicolau.devices.view.dashboard

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.rafaelnicolau.devices.Fixtures.alarm
import dev.rafaelnicolau.devices.Fixtures.camera
import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceFilter
import dev.rafaelnicolau.devices.view.theme.AppTheme
import dev.rafaelnicolau.devices.viewmodel.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private val loaded = DashboardUiState(
        isLoading = false,
        devices = listOf(alarm, camera),
        hasAnyDevice = true,
        totalCount = 2,
    )

    private fun show(state: DashboardUiState) = compose.setContent {
        AppTheme {
            DashboardScreen(
                state = state,
                onAddDevice = { events += "add" },
                onOpenDevice = { events += "open:${it.id}" },
                onEditDevice = { events += "edit:${it.id}" },
                onToggleFavorite = { events += "favorite:${it.id}" },
                onDeleteDevice = { events += "delete:${it.id}" },
                onRefresh = { events += "refresh" },
                onQueryChange = { events += "query:$it" },
                onFilterSelected = { events += "filter:$it" },
            )
        }
    }

    @Test
    fun listsEveryDeviceWithItsIdentifier() {
        show(loaded)

        compose.onNodeWithText("Meus dispositivos").assertExists()
        compose.onNodeWithText("2 dispositivos").assertExists()
        compose.onNodeWithText("Central da Casa").assertExists()
        compose.onNodeWithText("1A:2B:3C:4D:5E:6F").assertExists()
        compose.onNodeWithText("Câmera Garagem").assertExists()
        compose.onNodeWithText("SN-001").assertExists()
    }

    @Test
    fun countUsesSingularAndEmptyForms() {
        show(loaded.copy(totalCount = 1, devices = listOf(alarm)))
        compose.onNodeWithText("1 dispositivo").assertExists()
    }

    @Test
    fun loadingHidesSearchAndList() {
        show(DashboardUiState(isLoading = true))

        compose.onNodeWithText("Nenhum dispositivo").assertExists()
        compose.onNodeWithText("Pesquisar").assertDoesNotExist()
        compose.onNodeWithText("Nenhum dispositivo cadastrado.").assertDoesNotExist()
    }

    @Test
    fun emptyDatabaseOffersARetry() {
        show(DashboardUiState(isLoading = false))

        compose.onNodeWithText("Nenhum dispositivo cadastrado.").assertExists()
        compose.onNodeWithText("Tentar novamente").performClick()

        assertEquals(listOf("refresh"), events)
    }

    @Test
    fun failedSyncWithNothingCachedShowsTheErrorFullScreen() {
        show(DashboardUiState(isLoading = false, error = DeviceError.UNAUTHORIZED))

        compose.onNodeWithText("Token de acesso inválido ou expirado.").assertExists()
        compose.onNodeWithText("Tentar novamente").assertExists()
    }

    @Test
    fun failedSyncWithCachedDevicesKeepsTheListAndShowsANotice() {
        show(loaded.copy(error = DeviceError.NETWORK))

        compose.onNodeWithText("Sem conexão. Verifique sua internet. Mostrando os últimos dados salvos.").assertExists()
        compose.onNodeWithText("Central da Casa").assertExists()
    }

    @Test
    fun searchWithoutMatchesSaysSoButKeepsTheSearchBar() {
        show(loaded.copy(devices = emptyList(), query = "xyz"))

        compose.onNodeWithText("Nenhum dispositivo encontrado.").assertExists()
        compose.onNodeWithText("Pesquisar").assertExists()
    }

    @Test
    fun typingInTheSearchReportsTheQuery() {
        show(loaded)

        compose.onNode(hasSetTextAction()).performTextInput("cam")

        assertEquals(listOf("query:cam"), events)
    }

    @Test
    fun chipsReportTheChosenFilterAndShowTheSelectedOne() {
        show(loaded.copy(filter = DeviceFilter.ALARMS))

        compose.onNodeWithText("Alarmes").assertIsSelected()
        compose.onNodeWithText("Favoritos").performClick()

        assertEquals(listOf("filter:FAVORITES"), events)
    }

    @Test
    fun tappingADeviceOpensIt() {
        show(loaded)

        compose.onNodeWithText("Câmera Garagem").performClick()

        assertEquals(listOf("open:v1"), events)
    }

    @Test
    fun addButtonReportsAdd() {
        show(loaded)

        compose.onNodeWithText("Adicionar").performClick()

        assertEquals(listOf("add"), events)
    }

    private fun openActionsOf(device: Device) {
        compose.onNodeWithContentDescription("Mais ações de ${device.name}").performClick()
        compose.waitForIdle()
    }

    @Test
    fun actionsSheetEditsTheDevice() {
        show(loaded)
        openActionsOf(alarm)

        compose.onNodeWithText("Editar dispositivo").performClick()

        assertEquals(listOf("edit:a1"), events)
    }

    @Test
    fun actionsSheetOpensTheInformation() {
        show(loaded)
        openActionsOf(camera)

        compose.onNodeWithText("Câmera").assertExists()
        compose.onNodeWithText("Informações").performClick()

        assertEquals(listOf("open:v1"), events)
    }

    @Test
    fun actionsSheetOffersToUnmarkAFavorite() {
        show(loaded)
        openActionsOf(camera)

        compose.onNodeWithText("Desmarcar favorito").performClick()

        assertEquals(listOf("favorite:v1"), events)
    }

    @Test
    fun actionsSheetOffersToMarkANonFavorite() {
        show(loaded)
        openActionsOf(alarm)

        compose.onNodeWithText("Marcar favorito").performClick()

        assertEquals(listOf("favorite:a1"), events)
    }

    @Test
    fun deletingAsksForConfirmationFirst() {
        show(loaded)
        openActionsOf(alarm)
        compose.onNodeWithText("Remover dispositivo").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Remover dispositivo?").assertExists()
        assertEquals(emptyList<String>(), events)

        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(listOf("delete:a1"), events)
    }

    @Test
    fun cancellingTheConfirmationDeletesNothing() {
        show(loaded)
        openActionsOf(alarm)
        compose.onNodeWithText("Remover dispositivo").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Cancelar").performClick()

        compose.onNodeWithText("Remover dispositivo?").assertDoesNotExist()
        assertEquals(emptyList<String>(), events)
    }
}
