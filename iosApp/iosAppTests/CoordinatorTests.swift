@testable import iosApp
import Shared
import SwiftUI
import XCTest

/// The coordinators build screens through `AppContainer`, which the host app's
/// `AppDelegate` initializes before any test runs.
@MainActor
final class DevicesCoordinatorTests: XCTestCase {
    private var navigationController: UINavigationController!
    private var coordinator: DevicesCoordinator!

    override func setUp() async throws {
        navigationController = UINavigationController()
        coordinator = DevicesCoordinator(navigationController: navigationController)
        coordinator.start()
    }

    func testStartShowsOnlyTheDashboard() {
        XCTAssertEqual(navigationController.viewControllers.count, 1)
        XCTAssertTrue(navigationController.viewControllers.first is DashboardViewController)
    }

    func testShowDeviceDetailsPushesTheDetails() {
        coordinator.showDeviceDetails(deviceId: "v1")

        XCTAssertTrue(navigationController.topViewController is DeviceDetailsViewController)
        XCTAssertEqual(navigationController.topViewController?.title, "Informações")
    }

    func testShowAddDevicePushesAnAddForm() {
        coordinator.showAddDevice()

        XCTAssertTrue(navigationController.topViewController is DeviceFormViewController)
        XCTAssertEqual(navigationController.topViewController?.title, "Novo dispositivo")
    }

    func testShowEditDevicePushesAnEditForm() {
        coordinator.showEditDevice(deviceId: "a1")

        XCTAssertTrue(navigationController.topViewController is DeviceFormViewController)
        XCTAssertEqual(navigationController.topViewController?.title, "Editar dispositivo")
    }

    func testClosingTheFormReturnsToTheDashboard() {
        coordinator.showAddDevice()
        let form = navigationController.topViewController as? DeviceFormViewController

        _ = form?.navigationItem.leftBarButtonItem.map { item in
            UIApplication.shared.sendAction(item.action!, to: item.target, from: item, for: nil)
        }

        XCTAssertTrue(navigationController.viewControllers.last is DashboardViewController)
    }
}

@MainActor
final class AppCoordinatorTests: XCTestCase {
    func testStartInstallsTheNavigationStackInALightThemedWindow() {
        let window = UIWindow(frame: UIScreen.main.bounds)
        let coordinator = AppCoordinator(window: window)

        coordinator.start()

        XCTAssertTrue(window.rootViewController === coordinator.navigationController)
        XCTAssertTrue(coordinator.navigationController.viewControllers.first is DashboardViewController)
        XCTAssertEqual(window.overrideUserInterfaceStyle, .light)
        XCTAssertEqual(window.tintColor, UIColor(AppColors.primary))
        XCTAssertNotNil(coordinator.navigationController.navigationBar.standardAppearance.titleTextAttributes[.font])
    }
}

@MainActor
final class ToastPresenterTests: XCTestCase {
    private var window: UIWindow!

    override func setUp() async throws {
        window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = UIViewController()
        window.makeKeyAndVisible()
        baseline = window.subviews
    }

    override func tearDown() async throws {
        window.isHidden = true
        window = nil
    }

    /// What the window held before any toast, so only banners are counted.
    private var baseline: [UIView] = []

    private var toastCount: Int {
        window.subviews.filter { view in !baseline.contains { $0 === view } }.count
    }

    func testShowAddsTheBannerToTheWindow() {
        let presenter = ToastPresenter(window: window)

        presenter.show(text: "Copiado")

        XCTAssertEqual(toastCount, 1)
    }

    func testANewToastReplacesTheCurrentOne() {
        let presenter = ToastPresenter(window: window)

        presenter.show(text: "Primeiro")
        presenter.show(text: "Segundo")

        XCTAssertEqual(toastCount, 1)
    }

    func testToastGoesAwayByItself() async {
        let presenter = ToastPresenter(window: window)

        presenter.show(text: "Copiado")

        await waitUntil(timeout: 5) { self.toastCount == 0 }
    }
}
