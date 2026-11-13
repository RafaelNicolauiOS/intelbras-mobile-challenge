@testable import iosApp
import Shared
import XCTest

@MainActor
final class DeviceDetailsObservableTests: XCTestCase {
    func testPublishesTheDevice() async {
        let observable = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "v1", repository: Fixtures.repository(), messages: MessageCenter())
        )

        await waitUntil { observable.device != nil }

        XCTAssertEqual(observable.device?.name, "Câmera Garagem")
    }

    func testUnknownDeviceStaysNil() async throws {
        let observable = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "missing", repository: Fixtures.repository(), messages: MessageCenter())
        )

        try await Task.sleep(nanoseconds: 200_000_000)

        XCTAssertNil(observable.device)
    }

    func testFollowsUpdatesToTheDevice() async {
        let repository = Fixtures.repository()
        let observable = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "a1", repository: repository, messages: MessageCenter())
        )
        await waitUntil { observable.device != nil }

        try? await repository.setFavorite(id: "a1", favorite: true)

        await waitUntil { observable.device?.isFavorite == true }
    }

    func testCopyPutsTheValueOnThePasteboardAndConfirms() async {
        let messages = MessageCenter()
        let observable = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "v1", repository: Fixtures.repository(), messages: messages)
        )

        observable.copy("SN-001")

        XCTAssertEqual(UIPasteboard.general.string, "SN-001")
        let message = await firstMessage(of: messages)
        XCTAssertEqual((message as? UserMessageSuccess)?.kind, .copied)
    }
}
