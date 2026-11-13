import SwiftUI
import UIKit

final class DeviceDetailsViewController: UIHostingController<DeviceDetailsView>, ObservingScreen {
    let observable: DeviceDetailsObservable

    init(observable: DeviceDetailsObservable) {
        self.observable = observable
        super.init(rootView: DeviceDetailsView(observable: observable))
        title = "Informações"
    }

    func stopObserving() {
        observable.stop()
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
