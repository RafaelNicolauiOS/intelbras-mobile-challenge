package dev.rafaelnicolau.devices.view.form

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.rafaelnicolau.devices.models.DeviceFormErrors
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import dev.rafaelnicolau.devices.viewmodel.DeviceFormViewModel
import dev.rafaelnicolau.devices.viewmodel.MessageCenter
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.view.theme.AppTheme
import dev.rafaelnicolau.devices.viewmodel.DeviceFormUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceFormScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions = DeviceFormActions(
        onTypeChange = { events += "type:$it" },
        onNameChange = { events += "name:$it" },
        onMacAddressChange = { events += "mac:$it" },
        onSerialChange = { events += "serial:$it" },
        onUsernameChange = { events += "username:$it" },
        onPasswordChange = { events += "password:$it" },
        onSubmit = { events += "submit" },
        onBack = { events += "back" },
    )

    private fun show(state: DeviceFormUiState) = compose.setContent {
        AppTheme { DeviceFormScreen(state = state, actions = actions) }
    }

    @Test
    fun addModeShowsTheTypeSelectorAndVideoFields() {
        show(DeviceFormUiState(type = DeviceType.VIDEO))

        compose.onNodeWithText("Novo dispositivo").assertExists()
        compose.onNodeWithText("Tipo de dispositivo").assertExists()
        compose.onNodeWithText("Vídeo").assertIsSelected()
        compose.onNodeWithText("Número de série").assertExists()
        compose.onNodeWithText("Usuário").assertExists()
        compose.onNodeWithText("MAC Address").assertDoesNotExist()
    }

    @Test
    fun alarmTypeShowsMacAndPasswordOnly() {
        show(DeviceFormUiState(type = DeviceType.ALARM))

        compose.onNodeWithText("Alarme").assertIsSelected()
        compose.onNodeWithText("MAC Address").assertExists()
        compose.onNodeWithText("Senha").assertExists()
        compose.onNodeWithText("Número de série").assertDoesNotExist()
    }

    @Test
    fun choosingATypeReportsIt() {
        show(DeviceFormUiState())

        compose.onNodeWithText("Alarme").performClick()

        assertEquals(listOf("type:ALARM"), events)
    }

    @Test
    fun editModeHidesTheTypeSelector() {
        show(DeviceFormUiState(isEditing = true, type = DeviceType.ALARM, name = "Casa"))

        compose.onNodeWithText("Editar dispositivo").assertExists()
        compose.onNodeWithText("Tipo de dispositivo").assertDoesNotExist()
        compose.onNodeWithText("Casa").assertExists()
    }

    @Test
    fun loadingShowsNoFieldsAndDisablesSave() {
        show(DeviceFormUiState(isEditing = true, isLoading = true))

        compose.onNodeWithText("Nome").assertDoesNotExist()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun savingDisablesTheButtonAndSaysSo() {
        show(DeviceFormUiState(isSaving = true))

        compose.onNodeWithText("Salvando…").assertIsNotEnabled()
    }

    @Test
    fun typingReportsEachField() {
        show(DeviceFormUiState(type = DeviceType.VIDEO))

        compose.onNodeWithText("Nome").performTextInput("G")
        compose.onNodeWithText("Número de série").performTextInput("S")
        compose.onNodeWithText("Usuário").performTextInput("u")
        compose.onNodeWithText("Senha").performTextInput("p")

        // The state never changes in this test, so fields losing focus may re-report "" too.
        assertTrue(events.containsAll(listOf("name:G", "serial:S", "username:u", "password:p")))
    }

    @Test
    fun typingTheMacReportsIt() {
        show(DeviceFormUiState(type = DeviceType.ALARM))

        compose.onNodeWithText("MAC Address").performTextInput("AA")

        assertEquals(listOf("mac:AA"), events)
    }

    @Test
    fun fieldErrorsAreShownUnderTheirFields() {
        show(
            DeviceFormUiState(
                type = DeviceType.ALARM,
                errors = DeviceFormErrors(
                    name = FieldError.REQUIRED,
                    macAddress = FieldError.DUPLICATE,
                    password = FieldError.INVALID_ALARM_PASSWORD,
                ),
            ),
        )

        compose.onNodeWithText("Campo obrigatório.").assertExists()
        compose.onNodeWithText("Já está em uso por outro dispositivo.").assertExists()
        compose.onNodeWithText("A senha deve ter 4 ou 6 dígitos.").assertExists()
    }

    @Test
    fun passwordIsHiddenUntilTheUserAsksToSeeIt() {
        show(DeviceFormUiState(type = DeviceType.VIDEO, password = "segredo"))

        assertEquals("•••••••", renderedText(hasText("segredo")))

        compose.onNodeWithContentDescription("Mostrar senha").performClick()

        assertEquals("segredo", renderedText(hasText("segredo")))
        compose.onNodeWithContentDescription("Ocultar senha").assertExists()
    }

    /** What is actually drawn: the text after the field's visual transformation. */
    private fun renderedText(matcher: SemanticsMatcher): String {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.first().layoutInput.text.text
    }

    @Test
    fun saveAndCloseReportTheirIntent() {
        show(DeviceFormUiState())

        compose.onNodeWithText("Salvar").performClick()
        compose.onNodeWithContentDescription("Fechar").performClick()

        assertEquals(listOf("submit", "back"), events)
    }

    // With the real ViewModel: the field shows what the shared rules allow.

    private fun showWithViewModel(): DeviceFormViewModel {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), MessageCenter())
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            AppTheme {
                DeviceFormScreen(
                    state = state,
                    actions = DeviceFormActions(
                        onTypeChange = viewModel::onTypeChange,
                        onNameChange = viewModel::onNameChange,
                        onMacAddressChange = viewModel::onMacAddressChange,
                        onSerialChange = viewModel::onSerialChange,
                        onUsernameChange = viewModel::onUsernameChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onSubmit = viewModel::submit,
                        onBack = {},
                    ),
                )
            }
        }
        return viewModel
    }

    @Test
    fun serialFieldStopsAtThirteenCharacters() {
        showWithViewModel()

        compose.onNodeWithText("Número de série").performTextInput("ABC1234567890XYZ")

        compose.onNodeWithText("ABC1234567890").assertExists()
        compose.onNodeWithText("ABC1234567890XYZ").assertDoesNotExist()
    }

    @Test
    fun shortSerialShowsTheLengthErrorUnderTheField() {
        showWithViewModel()

        compose.onNodeWithText("Número de série").performTextInput("ABC123")
        compose.onNodeWithText("Salvar").performClick()

        compose.onNodeWithText("O número de série deve conter 13 caracteres.").assertExists()
    }
}
