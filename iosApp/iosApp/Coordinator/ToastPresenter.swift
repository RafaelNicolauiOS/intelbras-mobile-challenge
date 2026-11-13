import SwiftUI
import UIKit

/// Shows `ToastBanner`s above whatever screen is visible, one at a time.
@MainActor
final class ToastPresenter {
    private weak var window: UIWindow?
    private var current: UIViewController?
    private var dismissal: Task<Void, Never>?

    init(window: UIWindow) {
        self.window = window
    }

    func show(text: String, isFailure: Bool) {
        guard let window else { return }
        removeCurrent()

        let host = UIHostingController(rootView: ToastBanner(text: text, isFailure: isFailure))
        host.view.backgroundColor = .clear
        host.view.translatesAutoresizingMaskIntoConstraints = false
        window.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: window.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: window.trailingAnchor),
            host.view.bottomAnchor.constraint(equalTo: window.safeAreaLayoutGuide.bottomAnchor, constant: -16),
        ])
        current = host
        UIAccessibility.post(notification: .announcement, argument: text)

        dismissal = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            guard !Task.isCancelled else { return }
            self?.removeCurrent()
        }
    }

    private func removeCurrent() {
        dismissal?.cancel()
        current?.view.removeFromSuperview()
        current = nil
    }
}
