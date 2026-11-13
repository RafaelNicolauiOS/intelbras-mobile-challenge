@testable import iosApp
import Shared
import XCTest

@MainActor
final class DashboardObservableTests: XCTestCase {
    private func makeObservable(
        _ repository: InMemoryDeviceRepository = Fixtures.repository(),
        messages: MessageCenter = MessageCenter()
    ) -> DashboardObservable {
        DashboardObservable(viewModel: DashboardViewModel(repository: repository, messages: messages))
    }

    private func ids(_ observable: DashboardObservable) -> [String] {
        observable.state.devices.map(\.id)
    }

    func testStartsLoadingThenPublishesTheStoredDevices() async {
        let observable = makeObservable()
        XCTAssertTrue(observable.state.isLoading)

        await waitUntil { !observable.state.isLoading }

        XCTAssertEqual(ids(observable), ["a1", "v1"])
        XCTAssertEqual(observable.state.totalCount, 2)
        XCTAssertTrue(observable.state.hasAnyDevice)
    }

    func testQueryNarrowsTheList() async {
        let observable = makeObservable()
        await waitUntil { !observable.state.isLoading }

        observable.onQueryChange("camera")

        await waitUntil { observable.state.query == "camera" }
        XCTAssertEqual(ids(observable), ["v1"])
    }

    func testFilterNarrowsTheList() async {
        let observable = makeObservable()
        await waitUntil { !observable.state.isLoading }

        observable.onFilterSelected(.alarms)

        await waitUntil { observable.state.filter == .alarms }
        XCTAssertEqual(ids(observable), ["a1"])
    }

    func testToggleFavoriteIsPublished() async {
        let observable = makeObservable()
        await waitUntil { !observable.state.isLoading }

        observable.toggleFavorite(Fixtures.alarm)

        await waitUntil { observable.state.devices.first { $0.id == "a1" }?.isFavorite == true }
    }

    func testDeleteRemovesTheDeviceAndReportsIt() async {
        let messages = MessageCenter()
        let observable = makeObservable(messages: messages)
        await waitUntil { !observable.state.isLoading }

        observable.delete(Fixtures.alarm)

        await waitUntil { self.ids(observable) == ["v1"] }
        let message = await firstMessage(of: messages)
        XCTAssertEqual((message as? UserMessageSuccess)?.kind, .deviceDeleted)
    }

    func testFailedRefreshIsPublishedAsAnError() async {
        let repository = Fixtures.repository()
        let observable = makeObservable(repository)
        await waitUntil { !observable.state.isRefreshing }

        repository.nextOutcome = OutcomeFailure(error: .network)
        observable.refresh()

        await waitUntil { observable.state.error == .network }
        XCTAssertEqual(ids(observable), ["a1", "v1"], "cached devices stay")
    }
}
