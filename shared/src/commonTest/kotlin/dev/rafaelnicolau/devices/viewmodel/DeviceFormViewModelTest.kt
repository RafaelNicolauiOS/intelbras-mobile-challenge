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
import kotlinx.coroutines.withTimeoutOrNull
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
        assertEquals("1A:2B:3C:4D:5E:6F", prefilled.macAddress, "shown the way people read a MAC")

        viewModel.onNameChange("Casa nova")
        viewModel.submit()
        viewModel.saved.first()

        val updated = assertIs<Device.Alarm>(repository.observeDevices().first().single())
        assertEquals("a1", updated.id)
        assertEquals("1A2B3C4D5E6F", updated.macAddress, "sent back without separators")
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

    private val video = Device.Video(id = "v1", name = "Garagem", serial = "S1", username = "admin", password = " p ")

    private fun DeviceFormViewModel.fillValidVideo() {
        onNameChange("  Garagem  ")
        onSerialChange("ABC1234567890")
        onUsernameChange(" admin ")
        onPasswordChange(" senha ")
    }

    @Test
    fun addModeStartsEmptyOnTheVideoType() = runTest {
        val state = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages).uiState.value

        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals(DeviceType.VIDEO, state.type)
        assertEquals("", state.name)
    }

    @Test
    fun videoFormTrimsTextButKeepsThePasswordAsTyped() = runTest {
        val repository = InMemoryDeviceRepository()
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidVideo()

        viewModel.submit()
        viewModel.saved.first()

        val created = assertIs<Device.Video>(repository.observeDevices().first().single())
        assertEquals("Garagem", created.name)
        assertEquals("ABC1234567890", created.serial)
        assertEquals("admin", created.username)
        assertEquals(" senha ", created.password, "spaces may be part of a camera password")
    }

    @Test
    fun emptyVideoFormFlagsEveryField() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)

        viewModel.submit()

        val errors = viewModel.uiState.value.errors
        assertEquals(FieldError.REQUIRED, errors.name)
        assertEquals(FieldError.REQUIRED, errors.serial)
        assertEquals(FieldError.REQUIRED, errors.username)
        assertEquals(FieldError.REQUIRED, errors.password)
        assertNull(errors.macAddress)
    }

    @Test
    fun serialStopsAtThirteenCharacters() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)

        viewModel.onSerialChange("ABC1234567890XYZ")

        assertEquals("ABC1234567890", viewModel.uiState.value.serial)
    }

    @Test
    fun shortSerialBlocksSavingAndExplainsWhyInAToast() = runTest {
        val repository = InMemoryDeviceRepository()
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidVideo()
        viewModel.onSerialChange("ABC123")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(FieldError.INVALID_SERIAL, viewModel.uiState.value.errors.serial)
        assertEquals(UserMessage.Invalid(FieldError.INVALID_SERIAL), messages.messages.first())
        assertTrue(repository.observeDevices().first().isEmpty(), "nothing is saved")
    }

    @Test
    fun otherValidationErrorsDoNotShowAToast() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)

        viewModel.submit()
        advanceUntilIdle()

        assertNull(withTimeoutOrNull(100) { messages.messages.first() }, "empty fields are shown under each field only")
    }

    @Test
    fun switchingTypeEmptiesEveryField() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.fillValidVideo()

        viewModel.onTypeChange(DeviceType.ALARM)

        val alarm = viewModel.uiState.value
        assertEquals(DeviceType.ALARM, alarm.type)
        assertEquals(listOf("", "", "", "", ""), listOf(alarm.name, alarm.macAddress, alarm.serial, alarm.username, alarm.password))

        viewModel.fillValidAlarm()
        viewModel.onTypeChange(DeviceType.VIDEO)

        val video = viewModel.uiState.value
        assertEquals(DeviceType.VIDEO, video.type)
        assertEquals(listOf("", "", "", "", ""), listOf(video.name, video.macAddress, video.serial, video.username, video.password))
    }

    @Test
    fun choosingTheSameTypeAgainKeepsWhatWasTyped() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.fillValidAlarm()

        viewModel.onTypeChange(DeviceType.ALARM)

        assertEquals("Portão", viewModel.uiState.value.name)
        assertEquals("aa:bb:cc:dd:ee:ff", viewModel.uiState.value.macAddress)
    }

    @Test
    fun switchingTypeClearsTheErrors() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.submit()

        viewModel.onTypeChange(DeviceType.ALARM)

        assertEquals(DeviceType.ALARM, viewModel.uiState.value.type)
        assertFalse(viewModel.uiState.value.errors.hasErrors)
    }

    @Test
    fun everyFieldClearsOnlyItsOwnError() = runTest {
        val viewModel = DeviceFormViewModel(null, InMemoryDeviceRepository(), messages)
        viewModel.submit()

        viewModel.onSerialChange("S")
        viewModel.onUsernameChange("u")
        viewModel.onPasswordChange("p")

        val errors = viewModel.uiState.value.errors
        assertNull(errors.serial)
        assertNull(errors.username)
        assertNull(errors.password)
        assertEquals(FieldError.REQUIRED, errors.name)
    }

    @Test
    fun editingAVideoPrefillsItsFields() = runTest {
        val viewModel = DeviceFormViewModel("v1", InMemoryDeviceRepository(listOf(video)), messages)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(DeviceType.VIDEO, state.type)
        assertEquals("Garagem", state.name)
        assertEquals("S1", state.serial)
        assertEquals("admin", state.username)
        assertEquals(" p ", state.password)
    }

    @Test
    fun editStartsLoadingAndIgnoresSubmitUntilPrefilled() = runTest {
        val repository = InMemoryDeviceRepository(listOf(video))
        val viewModel = DeviceFormViewModel("v1", repository, messages)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.submit()

        assertFalse(viewModel.uiState.value.errors.hasErrors, "nothing is validated while loading")
    }

    @Test
    fun editingADeviceThatNoLongerExistsStopsLoading() = runTest {
        val viewModel = DeviceFormViewModel("gone", InMemoryDeviceRepository(), messages)

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isEditing)
        assertEquals("", state.name)
    }

    @Test
    fun secondSubmitWhileSavingIsIgnored() = runTest {
        val repository = InMemoryDeviceRepository()
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidVideo()

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.observeDevices().first().size)
    }

    @Test
    fun videoConflictPointsAtTheSerial() = runTest {
        val repository = InMemoryDeviceRepository()
        repository.nextOutcome = Outcome.Failure(DeviceError.CONFLICT)
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidVideo()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(FieldError.DUPLICATE, viewModel.uiState.value.errors.serial)
        assertNull(viewModel.uiState.value.errors.macAddress)
    }

    @Test
    fun otherFailuresOnlyReportTheErrorAndAllowRetry() = runTest {
        val repository = InMemoryDeviceRepository()
        repository.nextOutcome = Outcome.Failure(DeviceError.NETWORK)
        val viewModel = DeviceFormViewModel(null, repository, messages)
        viewModel.fillValidVideo()

        viewModel.submit()

        assertEquals(UserMessage.Failure(DeviceError.NETWORK), messages.messages.first())
        assertFalse(viewModel.uiState.value.errors.hasErrors)
        assertFalse(viewModel.uiState.value.isSaving)

        repository.nextOutcome = Outcome.Success(Unit)
        viewModel.submit()
        viewModel.saved.first()
        assertEquals(1, repository.observeDevices().first().size)
    }
}
