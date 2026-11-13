import Shared
import SwiftUI
import UIKit

/// Hosts the SwiftUI dashboard inside UIKit navigation and owns its navigation bar.
final class DashboardViewController: UIHostingController<DashboardView> {
    private let onAddDevice: () -> Void

    init(
        observable: DashboardObservable,
        onAddDevice: @escaping () -> Void,
        onOpenDevice: @escaping (Device) -> Void,
        onEditDevice: @escaping (Device) -> Void
    ) {
        self.onAddDevice = onAddDevice
        super.init(rootView: DashboardView(observable: observable, onOpenDevice: onOpenDevice, onEditDevice: onEditDevice))
        title = "Dispositivos"
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .add,
            target: self,
            action: #selector(addTapped)
        )
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    @objc private func addTapped() {
        onAddDevice()
    }
}
