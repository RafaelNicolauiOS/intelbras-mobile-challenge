@preconcurrency import Shared
import UIKit

/// Navigation for the devices flow: dashboard, details and add/edit form.
/// It is also the navigation controller's delegate, so it knows when a screen leaves the stack
/// (back button, swipe back, close, save) and stops that screen's observation right away.
@MainActor
final class DevicesCoordinator: NSObject, Coordinator, UINavigationControllerDelegate {
    let navigationController: UINavigationController

    /// Screens shown by this flow that still observe their ViewModel.
    private var screens: [WeakScreen] = []

    init(navigationController: UINavigationController) {
        self.navigationController = navigationController
        super.init()
        navigationController.delegate = self
    }

    func start() {
        let observable = DashboardObservable(viewModel: AppContainer.shared.dashboardViewModel())
        let viewController = DashboardViewController(
            observable: observable,
            onAddDevice: { [weak self] in self?.showAddDevice() },
            onOpenDevice: { [weak self] device in self?.showDeviceDetails(deviceId: device.id) },
            onEditDevice: { [weak self] device in self?.showEditDevice(deviceId: device.id) }
        )
        track(viewController)
        navigationController.setViewControllers([viewController], animated: false)
    }

    func showDeviceDetails(deviceId: String) {
        let observable = DeviceDetailsObservable(
            viewModel: AppContainer.shared.deviceDetailsViewModel(deviceId: deviceId)
        )
        let viewController = DeviceDetailsViewController(observable: observable)
        track(viewController)
        navigationController.pushViewController(viewController, animated: true)
    }

    func showAddDevice() {
        showDeviceForm(deviceId: nil)
    }

    func showEditDevice(deviceId: String) {
        showDeviceForm(deviceId: deviceId)
    }

    /// A nil `deviceId` opens the form in "add" mode. The form closes itself once saved.
    private func showDeviceForm(deviceId: String?) {
        let observable = DeviceFormObservable(
            viewModel: AppContainer.shared.deviceFormViewModel(deviceId: deviceId),
            onSaved: { [weak self] in self?.navigationController.popViewController(animated: true) }
        )
        let viewController = DeviceFormViewController(
            observable: observable,
            isEditing: deviceId != nil,
            onClose: { [weak self] in self?.navigationController.popViewController(animated: true) }
        )
        track(viewController)
        navigationController.pushViewController(viewController, animated: true)
    }

    // MARK: - Screen lifecycle

    private func track(_ screen: UIViewController & ObservingScreen) {
        screens.append(WeakScreen(screen))
    }

    /// Stops every tracked screen that is no longer in the stack. Runs after each transition
    /// completes, so a swipe back the user cancels does not stop anything.
    func stopScreensThatLeft() {
        let stack = navigationController.viewControllers
        screens.removeAll { entry in
            guard let screen = entry.screen else { return true }
            guard !stack.contains(where: { $0 === screen }) else { return false }
            screen.stopObserving()
            return true
        }
    }

    func navigationController(
        _ navigationController: UINavigationController,
        didShow viewController: UIViewController,
        animated: Bool
    ) {
        stopScreensThatLeft()
    }
}

/// A screen the coordinator tracks without keeping it alive.
private struct WeakScreen {
    weak var screen: (UIViewController & ObservingScreen)?

    init(_ screen: UIViewController & ObservingScreen) {
        self.screen = screen
    }
}
