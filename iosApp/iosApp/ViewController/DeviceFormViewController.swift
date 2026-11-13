import SwiftUI
import UIKit

/// Hosts the form and owns its navigation bar: close on the left, "Salvar" on the right.
final class DeviceFormViewController: UIHostingController<DeviceFormView>, ObservingScreen {
    let observable: DeviceFormObservable
    private let onClose: @MainActor () -> Void

    init(observable: DeviceFormObservable, isEditing: Bool, onClose: @escaping @MainActor () -> Void) {
        self.observable = observable
        self.onClose = onClose
        super.init(rootView: DeviceFormView(observable: observable))
        title = isEditing ? "Editar dispositivo" : "Novo dispositivo"

        let close = UIBarButtonItem(
            image: UIImage(systemName: "xmark"),
            style: .plain,
            target: self,
            action: #selector(closeTapped)
        )
        close.accessibilityLabel = "Fechar"
        navigationItem.leftBarButtonItem = close
        // Plain style: the prototype shows "Salvar" as green text, not as a filled button.
        let save = UIBarButtonItem(title: "Salvar", style: .plain, target: self, action: #selector(saveTapped))
        save.tintColor = UIColor(AppColors.primary)
        save.setTitleTextAttributes([.font: UIFont.app(16, .semibold)], for: .normal)
        navigationItem.rightBarButtonItem = save
    }

    func stopObserving() {
        observable.stop()
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    @objc private func closeTapped() {
        onClose()
    }

    @objc private func saveTapped() {
        view.endEditing(true)
        observable.submit()
    }
}
