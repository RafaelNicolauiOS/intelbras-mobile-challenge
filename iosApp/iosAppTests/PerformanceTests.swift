@testable import iosApp
import Combine
@preconcurrency import Shared
import XCTest

/// Guards the main-thread budget of typing. The limits are generous for slow CI machines;
/// `measure` also records the time in the test report for comparison between runs.
@MainActor
final class PerformanceTests: XCTestCase {
    private static func devices(_ count: Int) -> [Device] {
        (0..<count).map { index -> Device in
            index.isMultiple(of: 2)
                ? DeviceAlarm(id: "a\(index)", name: "Central de Alarme Câmera Portão Nº \(index)", isFavorite: false, macAddress: "1A2B3C4D5E6F", password: "1234") as Device
                : DeviceVideo(id: "v\(index)", name: "Câmera da Garagem São João \(index)", isFavorite: false, serial: "S\(index)", username: "admin", password: "x")
        }
    }

    private func loadedDashboard(_ count: Int) async -> DashboardObservable {
        let observable = DashboardObservable(
            viewModel: DashboardViewModel(repository: InMemoryDeviceRepository(initial: Self.devices(count)), messages: MessageCenter())
        )
        await waitUntil(timeout: 10) { !observable.state.isLoading && observable.state.devices.count == count }
        return observable
    }

    /// A keystroke only records the query; filtering 1.000 devices happens off the main thread.
    func testSearchKeystrokeBarelyTouchesTheMainThread() async {
        let observable = await loadedDashboard(1_000)
        let queries = ["c", "ca", "cam", "came", "camer", "camera"]

        var elapsed: CFAbsoluteTime = 0
        for query in queries {
            let start = CFAbsoluteTimeGetCurrent()
            observable.onQueryChange(query)
            elapsed += CFAbsoluteTimeGetCurrent() - start
        }

        let perKeyMs = elapsed / Double(queries.count) * 1_000
        XCTAssertLessThan(perKeyMs, 2, "each keystroke blocked the main thread for \(perKeyMs) ms")

        measure(metrics: [XCTClockMetric()]) {
            for query in queries { observable.onQueryChange(query) }
        }
    }

    /// The filtered list still arrives, and is published on the main thread.
    func testSearchResultIsPublishedOnTheMainThread() async {
        let observable = await loadedDashboard(1_000)
        var publishedOffMain = false
        let subscription = observable.objectWillChange.sink { if !Thread.isMainThread { publishedOffMain = true } }

        observable.onQueryChange("nº 998")

        await waitUntil(timeout: 5) { observable.state.query == "nº 998" }
        subscription.cancel()
        XCTAssertFalse(publishedOffMain)
        XCTAssertEqual(observable.state.devices.map(\.id), ["a998"])
    }

    /// Typing at a human pace: each keystroke renders the form exactly once.
    func testFormRendersOncePerKeystroke() async throws {
        let observable = DeviceFormObservable(
            viewModel: DeviceFormViewModel(deviceId: nil, repository: InMemoryDeviceRepository(initial: []), messages: MessageCenter()),
            onSaved: {}
        )
        var publishes = 0
        let subscription = observable.objectWillChange.sink { publishes += 1 }

        var typed = ""
        for character in "Portão" {
            typed.append(character)
            observable.onNameChange(typed)
            try await Task.sleep(nanoseconds: 60_000_000)
        }
        subscription.cancel()

        XCTAssertEqual(publishes, typed.count)
        XCTAssertEqual(observable.state.name, "Portão")
    }

    /// SwiftUI re-sending the text a field already has must not render anything.
    func testResendingTheSameTextDoesNotRender() async throws {
        let observable = DeviceFormObservable(
            viewModel: DeviceFormViewModel(deviceId: nil, repository: InMemoryDeviceRepository(initial: []), messages: MessageCenter()),
            onSaved: {}
        )
        observable.onNameChange("Casa")
        try await Task.sleep(nanoseconds: 100_000_000)
        var publishes = 0
        let subscription = observable.objectWillChange.sink { publishes += 1 }

        observable.onNameChange("Casa")
        try await Task.sleep(nanoseconds: 100_000_000)
        subscription.cancel()

        XCTAssertEqual(publishes, 0)
    }
}
