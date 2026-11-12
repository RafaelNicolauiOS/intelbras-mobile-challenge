import SwiftUI
import UIKit

final class DeviceDetailsViewController: UIHostingController<DeviceDetailsView> {
    private let onEdit: () -> Void

    init(observable: DeviceDetailsObservable, onEdit: @escaping () -> Void) {
        self.onEdit = onEdit
        super.init(rootView: DeviceDetailsView(observable: observable))
        title = "Informações"
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            title: "Editar",
            style: .plain,
            target: self,
            action: #selector(editTapped)
        )
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    @objc private func editTapped() {
        onEdit()
    }
}
