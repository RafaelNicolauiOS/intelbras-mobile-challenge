@testable import iosApp
@preconcurrency import Shared
import XCTest

/// Screens stop observing their ViewModel as soon as they leave the navigation stack.
@MainActor
final class LifecycleTests: XCTestCase {
    private var window: UIWindow!
    private var navigationController: UINavigationController!
    private var coordinator: DevicesCoordinator!

    override func setUp() async throws {
        navigationController = UINavigationController()
        coordinator = DevicesCoordinator(navigationController: navigationController)
        // Attached to the host app's scene, so UIKit really shows it and runs transitions.
        let scene = try XCTUnwrap(UIApplication.shared.connectedScenes.first as? UIWindowScene)
        window = UIWindow(windowScene: scene)
        window.rootViewController = navigationController
        window.makeKeyAndVisible()
        coordinator.start()
    }

    /// Waits until no navigation animation is running: UIKit ignores a pop issued during a push.
    private func settle() async {
        await waitUntil {
            self.navigationController.transitionCoordinator == nil
                && self.navigationController.topViewController?.viewIfLoaded?.window != nil
        }
    }

    override func tearDown() async throws {
        window.isHidden = true
        window = nil
    }

    func testStopIsIdempotentAndMarksTheObservableStopped() {
        let observable = DashboardObservable(
            viewModel: DashboardViewModel(repository: Fixtures.repository(), messages: MessageCenter())
        )

        observable.stop()
        observable.stop()

        XCTAssertTrue(observable.isStopped)
    }

    func testStoppedObservableIgnoresLaterChanges() async throws {
        let repository = Fixtures.repository()
        let observable = DashboardObservable(
            viewModel: DashboardViewModel(repository: repository, messages: MessageCenter())
        )
        await waitUntil { !observable.state.isLoading }

        observable.stop()
        let other = DashboardViewModel(repository: repository, messages: MessageCenter())
        other.toggleFavorite(device: Fixtures.alarm)
        try await Task.sleep(nanoseconds: 200_000_000)

        XCTAssertFalse(observable.state.devices.contains { $0.id == "a1" && $0.isFavorite })
        other.clear()
    }

    func testLeavingTheDetailsStopsItsObservation() async {
        coordinator.showDeviceDetails(deviceId: "v1")
        let details = navigationController.topViewController as! DeviceDetailsViewController
        await settle()

        navigationController.popViewController(animated: false)
        coordinator.stopScreensThatLeft()

        XCTAssertTrue(details.observable.isStopped)
    }

    func testClosingTheFormStopsItsObservation() async {
        coordinator.showAddDevice()
        let form = navigationController.topViewController as! DeviceFormViewController
        await settle()

        navigationController.popViewController(animated: false)
        coordinator.stopScreensThatLeft()

        XCTAssertTrue(form.observable.isStopped)
    }

    func testScreensStillInTheStackKeepObserving() {
        coordinator.showDeviceDetails(deviceId: "v1")
        let dashboard = navigationController.viewControllers.first as! DashboardViewController

        coordinator.stopScreensThatLeft()

        XCTAssertFalse(dashboard.observable.isStopped)
        XCTAssertFalse((navigationController.topViewController as! DeviceDetailsViewController).observable.isStopped)
    }

    /// The navigation controller itself reports the pop; no manual call needed.
    func testThePopIsDetectedThroughTheNavigationDelegate() async {
        coordinator.showDeviceDetails(deviceId: "v1")
        let details = navigationController.topViewController as! DeviceDetailsViewController
        await settle()

        navigationController.popViewController(animated: true)

        await waitUntil(timeout: 3) { details.observable.isStopped }
    }
}
