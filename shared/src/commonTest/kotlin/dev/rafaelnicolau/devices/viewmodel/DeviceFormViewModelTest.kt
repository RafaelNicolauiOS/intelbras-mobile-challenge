package dev.rafaelnicolau.devices.viewmodel

import dev.rafaelnicolau.devices.models.Device
import dev.rafaelnicolau.devices.models.DeviceError
import dev.rafaelnicolau.devices.models.DeviceType
import dev.rafaelnicolau.devices.models.FieldError
import dev.rafaelnicolau.devices.models.Outcome
import dev.rafaelnicolau.devices.models.SuccessKind
import dev.rafaelnicolau.devices.models.UserMessage
import dev.rafaelnicolau.devices.repository.InMemoryDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFormViewModelTest {
    private val alarm = Device.Alarm(id = "a1", name = "Casa", isFavorite = true, macAddress = "1A2B3C4D5E6F", password = "1234")
    private val messages = MessageCenter()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun DeviceFormViewModel.fillValidAlarm() {
        onTypeChange(DeviceType.ALARM)
        onNameChange("Portão")
        onMacAddressChange("aa:bb:cc:dd:ee:ff")
        onPasswordChange("123456")
    }

    @Test
    fun invalidFormShowsFieldErrorsAndSavesNothing() = runTest {
        val repository = InMemoryDeviceRepository()
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.onTypeChange(DeviceType.ALARM)
        viewModel.onMacAddressChange("123")

        viewModel.submit()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.errors
        assertEquals(FieldError.REQUIRED, errors.name)
        assertEquals(FieldError.INVALID_MAC, errors.macAddress)
        assertEquals(FieldError.REQUIRED, errors.password)
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(repository.observeDevices().first().isEmpty())
    }

    @Test
    fun editingAFieldClearsItsError() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.submit()
        assertEquals(FieldError.REQUIRED, viewModel.uiState.value.errors.name)

        viewModel.onNameChange("P")

        assertNull(viewModel.uiState.value.errors.name)
    }

    @Test
    fun resendingTheSameTextKeepsTheError() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.onTypeChange(DeviceType.ALARM)
        viewModel.onMacAddressChange("123")
        viewModel.submit()

        // What SwiftUI does when it re-renders a text field.
        viewModel.onMacAddressChange("123")
        viewModel.onTypeChange(DeviceType.ALARM)

        assertEquals(FieldError.INVALID_MAC, viewModel.uiState.value.errors.macAddress)
    }

    @Test
    fun validFormCreatesTheDeviceAndSignalsSaved() = runTest {
        val repository = InMemoryDeviceRepository()
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidAlarm()

        viewModel.submit()
        viewModel.saved.first()

        val created = assertIs<Device.Alarm>(repository.observeDevices().first().single())
        assertEquals("Portão", created.name)
        assertEquals("AABBCCDDEEFF", created.macAddress, "MAC is normalized before it is sent")
        assertEquals(UserMessage.Success(SuccessKind.DEVICE_CREATED), messages.messages.first())
    }

    @Test
    fun editStartsPrefilledAndUpdatesTheSameDevice() = runTest {
        val repository = InMemoryDeviceRepository(listOf(alarm))
        val viewModel = DeviceFormViewModel("a1", repository, messages)

        val prefilled = viewModel.uiState.first { !it.isLoading }
        assertTrue(prefilled.isEditing)
        assertEquals(DeviceType.ALARM, prefilled.type)
        assertEquals("Casa", prefilled.name)
        assertEquals("1A2B3C4D5E6F", prefilled.macAddress)

        viewModel.onNameChange("Casa nova")
        viewModel.submit()
        viewModel.saved.first()

        val updated = repository.observeDevices().first().single()
        assertEquals("a1", updated.id)
        assertEquals("Casa nova", updated.name)
        assertTrue(updated.isFavorite, "editing must not drop the favorite mark")
        assertEquals(UserMessage.Success(SuccessKind.DEVICE_UPDATED), messages.messages.first())
    }

    @Test
    fun typeCannotChangeWhileEditing() = runTest {
        val viewModel = DeviceFormViewModel("a1", InMemoryDeviceRepository(listOf(alarm)), messages)
        viewModel.uiState.first { !it.isLoading }

        viewModel.onTypeChange(DeviceType.VIDEO)

        assertEquals(DeviceType.ALARM, viewModel.uiState.value.type)
    }

    @Test
    fun conflictPointsAtTheDuplicatedFieldAndKeepsTheFormOpen() = runTest {
        val repository = InMemoryDeviceRepository()
        repository.nextOutcome = Outcome.Failure(DeviceError.CONFLICT)
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidAlarm()

        viewModel.submit()

        assertEquals(UserMessage.Failure(DeviceError.CONFLICT), messages.messages.first())
        val state = viewModel.uiState.value
        assertEquals(FieldError.DUPLICATE, state.errors.macAddress)
        assertFalse(state.isSaving)
        assertEquals("Portão", state.name, "what the user typed is kept")
    }
}
