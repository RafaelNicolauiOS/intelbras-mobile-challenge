import Shared
import UIKit

/// Navigation for the devices flow: dashboard, details and add/edit form.
@MainActor
final class DevicesCoordinator: Coordinator {
    let navigationController: UINavigationController

    init(navigationController: UINavigationController) {
        self.navigationController = navigationController
    }

    func start() {
        let observable = DashboardObservable(viewModel: AppContainer.shared.dashboardViewModel())
        let viewController = DashboardViewController(
            observable: observable,
            onAddDevice: { [weak self] in self?.showAddDevice() },
            onOpenDevice: { [weak self] device in self?.showDeviceDetails(deviceId: device.id) }
        )
        navigationController.setViewControllers([viewController], animated: false)
    }

    func showDeviceDetails(deviceId: String) {
        let observable = DeviceDetailsObservable(
            viewModel: AppContainer.shared.deviceDetailsViewModel(deviceId: deviceId)
        )
        let viewController = DeviceDetailsViewController(
            observable: observable,
            onEdit: { [weak self] in self?.showEditDevice(deviceId: deviceId) }
        )
        navigationController.pushViewController(viewController, animated: true)
    }

    func showAddDevice() {
        navigationController.pushViewController(DeviceFormViewController(deviceId: nil), animated: true)
    }

    func showEditDevice(deviceId: String) {
        navigationController.pushViewController(DeviceFormViewController(deviceId: deviceId), animated: true)
    }
}
