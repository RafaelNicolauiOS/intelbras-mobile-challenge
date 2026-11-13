import Shared
import SwiftUI
import UIKit

/// Hosts the SwiftUI dashboard. The prototype draws its own header, so the navigation
/// bar is hidden while this screen is visible.
final class DashboardViewController: UIHostingController<DashboardView> {
    init(
        observable: DashboardObservable,
        onAddDevice: @escaping () -> Void,
        onOpenDevice: @escaping (Device) -> Void,
        onEditDevice: @escaping (Device) -> Void
    ) {
        super.init(rootView: DashboardView(
            observable: observable,
            onAddDevice: onAddDevice,
            onOpenDevice: onOpenDevice,
            onEditDevice: onEditDevice
        ))
        navigationItem.backButtonDisplayMode = .minimal
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
}
