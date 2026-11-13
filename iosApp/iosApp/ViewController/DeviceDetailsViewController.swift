import SwiftUI
import UIKit

final class DeviceDetailsViewController: UIHostingController<DeviceDetailsView> {
    init(observable: DeviceDetailsObservable) {
        super.init(rootView: DeviceDetailsView(observable: observable))
        title = "Informações"
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
