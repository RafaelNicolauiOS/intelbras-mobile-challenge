import Shared
import SwiftUI
import UIKit

/// Bridges the shared Kotlin `DeviceDetailsViewModel` to SwiftUI.
@MainActor
final class DeviceDetailsObservable: ObservableObject {
    @Published private(set) var device: Device?

    private let viewModel: DeviceDetailsViewModel
    private var observation: Task<Void, Never>?

    init(viewModel: DeviceDetailsViewModel) {
        self.viewModel = viewModel
        device = viewModel.device.value
        observation = Task { [weak self] in
            for await device in viewModel.device {
                self?.device = device
            }
        }
    }

    deinit {
        observation?.cancel()
        viewModel.clear()
    }

    func copy(_ value: String) {
        UIPasteboard.general.string = value
        viewModel.onCopied()
    }
}
