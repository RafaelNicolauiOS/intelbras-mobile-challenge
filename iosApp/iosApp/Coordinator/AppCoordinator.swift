import UIKit

/// Root coordinator: sets up the window and hands over to the first flow.
@MainActor
final class AppCoordinator: Coordinator {
    let navigationController = UINavigationController()
    private let window: UIWindow
    private var childCoordinators: [Coordinator] = []

    init(window: UIWindow) {
        self.window = window
    }

    func start() {
        let devices = DevicesCoordinator(navigationController: navigationController)
        childCoordinators.append(devices)
        devices.start()

        window.rootViewController = navigationController
        window.makeKeyAndVisible()
    }
}
