import SwiftUI
import UIKit

final class DeviceFormViewController: UIHostingController<DeviceFormView> {
    /// A nil `deviceId` opens the form in "add" mode.
    init(deviceId: String?) {
        super.init(rootView: DeviceFormView(deviceId: deviceId))
        title = deviceId == nil ? "Adicionar dispositivo" : "Editar dispositivo"
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
