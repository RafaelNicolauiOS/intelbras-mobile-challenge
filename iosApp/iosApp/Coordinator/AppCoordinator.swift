import Shared
import SwiftUI
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

        // Brand colors for every navigation bar: dark text, green actions, white background.
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(AppColors.background)
        appearance.shadowColor = .clear
        appearance.titleTextAttributes = [
            .foregroundColor: UIColor(AppColors.text),
            .font: UIFont.app(17, .semibold),
        ]
        navigationController.navigationBar.standardAppearance = appearance
        navigationController.navigationBar.scrollEdgeAppearance = appearance
        navigationController.navigationBar.tintColor = UIColor(AppColors.text)
        window.tintColor = UIColor(AppColors.primary)
        // The prototype defines a single, light theme.
        window.overrideUserInterfaceStyle = .light

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
                toasts.show(text: message.text)
            }
        }
    }
}
