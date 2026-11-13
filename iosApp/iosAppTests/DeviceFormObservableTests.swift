@testable import iosApp
import Shared
import XCTest

@MainActor
final class DeviceFormObservableTests: XCTestCase {
    private var savedCount = 0

    private func makeObservable(
        deviceId: String? = nil,
        repository: InMemoryDeviceRepository = Fixtures.repository([])
    ) -> DeviceFormObservable {
        DeviceFormObservable(
            viewModel: DeviceFormViewModel(deviceId: deviceId, repository: repository, messages: MessageCenter()),
            onSaved: { [weak self] in self?.savedCount += 1 }
        )
    }

    func testAddModeStartsEmpty() {
        let observable = makeObservable()

        XCTAssertFalse(observable.state.isEditing)
        XCTAssertFalse(observable.state.isLoading)
        XCTAssertEqual(observable.state.name, "")
    }

    /// Text fields must see their new text right away, not after a coroutine hop.
    func testEditsArePublishedSynchronously() {
        let observable = makeObservable()

        observable.onNameChange("Portão")
        XCTAssertEqual(observable.state.name, "Portão")

        observable.onTypeChange(.alarm)
        XCTAssertEqual(observable.state.type, .alarm)

        observable.onMacAddressChange("AA")
        XCTAssertEqual(observable.state.macAddress, "AA")

        observable.onPasswordChange("1234")
        XCTAssertEqual(observable.state.password, "1234")
    }

    func testVideoFieldsArePublished() {
        let observable = makeObservable()

        observable.onSerialChange("S1")
        observable.onUsernameChange("admin")

        XCTAssertEqual(observable.state.serial, "S1")
        XCTAssertEqual(observable.state.username, "admin")
    }

    func testInvalidSubmitShowsErrorsImmediately() {
        let observable = makeObservable()

        observable.submit()

        XCTAssertEqual(observable.state.errors.name, .required)
        XCTAssertEqual(observable.state.errors.serial, .required)
        XCTAssertEqual(savedCount, 0)
    }

    func testValidSubmitStoresTheDeviceAndCallsOnSaved() async {
        let repository = Fixtures.repository([])
        let observable = makeObservable(repository: repository)
        observable.onNameChange("Garagem")
        observable.onSerialChange("S1")
        observable.onUsernameChange("admin")
        observable.onPasswordChange("x")

        observable.submit()
        XCTAssertTrue(observable.state.isSaving)

        await waitUntil { self.savedCount == 1 }
    }

    func testEditModePrefillsFromTheRepository() async {
        // On iOS the ViewModel's scope dispatches immediately on the main thread, and the
        // in-memory repository answers without suspending, so the prefill may already be done here.
        let observable = makeObservable(deviceId: "a1", repository: Fixtures.repository())

        await waitUntil { !observable.state.isLoading }

        XCTAssertTrue(observable.state.isEditing)
        XCTAssertEqual(observable.state.type, .alarm)
        XCTAssertEqual(observable.state.macAddress, "1A:2B:3C:4D:5E:6F")
    }
}
