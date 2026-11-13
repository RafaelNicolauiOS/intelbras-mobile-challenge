@preconcurrency import Shared
import SwiftUI
import UIKit

/// Hosts the SwiftUI dashboard. The prototype draws its own header, so the navigation
/// bar is hidden while this screen is visible.
final class DashboardViewController: UIHostingController<DashboardView>, ObservingScreen {
    let observable: DashboardObservable

    init(
        observable: DashboardObservable,
        onAddDevice: @escaping @MainActor () -> Void,
        onOpenDevice: @escaping @MainActor (Device) -> Void,
        onEditDevice: @escaping @MainActor (Device) -> Void
    ) {
        self.observable = observable
        super.init(rootView: DashboardView(
            observable: observable,
            onAddDevice: onAddDevice,
            onOpenDevice: onOpenDevice,
            onEditDevice: onEditDevice
        ))
        navigationItem.backButtonDisplayMode = .minimal
    }

    func stopObserving() {
        observable.stop()
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
