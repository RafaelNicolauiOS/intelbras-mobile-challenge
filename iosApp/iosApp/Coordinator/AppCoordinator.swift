import Shared
import UIKit

/// Root coordinator: sets up the window and hands over to the first flow.
@MainActor
final class AppCoordinator: Coordinator {
    let navigationController = UINavigationController()
    private let window: UIWindow
    private var childCoordinators: [Coordinator] = []
    private let toasts: ToastPresenter
    private var messagesTask: Task<Void, Never>?

    init(window: UIWindow) {
        self.window = window
        toasts = ToastPresenter(window: window)
    }

    func start() {
        let devices = DevicesCoordinator(navigationController: navigationController)
        childCoordinators.append(devices)
        devices.start()

        window.rootViewController = navigationController
        window.makeKeyAndVisible()
        observeMessages()
    }

    /// Feedback from any screen is shown here, above the whole navigation stack, so it
    /// outlives the screen that caused it (e.g. "device added" after the form closes).
    private func observeMessages() {
        let toasts = toasts
        messagesTask = Task {
            for await message in AppContainer.shared.messageCenter.messages {
                toasts.show(text: message.text, isFailure: message.isFailure)
            }
        }
    }
}
