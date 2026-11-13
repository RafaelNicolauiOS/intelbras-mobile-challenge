import SwiftUI
import UIKit

final class DeviceFormViewController: UIHostingController<DeviceFormView> {
    init(observable: DeviceFormObservable, isEditing: Bool) {
        super.init(rootView: DeviceFormView(observable: observable))
        title = isEditing ? "Editar dispositivo" : "Adicionar dispositivo"
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
