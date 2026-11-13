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
            onOpenDevice: { [weak self] device in self?.showDeviceDetails(deviceId: device.id) },
            onEditDevice: { [weak self] device in self?.showEditDevice(deviceId: device.id) }
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
        let viewController = DeviceFormViewController(observable: observable, isEditing: deviceId != nil)
        navigationController.pushViewController(viewController, animated: true)
    }
}
