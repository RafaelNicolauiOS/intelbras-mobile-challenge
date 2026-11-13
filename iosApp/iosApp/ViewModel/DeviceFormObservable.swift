import Shared
import SwiftUI

/// Bridges the shared Kotlin `DeviceFormViewModel` to SwiftUI.
@MainActor
final class DeviceFormObservable: ObservableObject {
    @Published private(set) var state: DeviceFormUiState

    private let viewModel: DeviceFormViewModel
    private var tasks: [Task<Void, Never>] = []

    /// `onSaved` runs once the device was stored, so the coordinator can close the form.
    init(viewModel: DeviceFormViewModel, onSaved: @escaping () -> Void) {
        self.viewModel = viewModel
        state = viewModel.uiState.value
        tasks = [
            Task { [weak self] in
                for await state in viewModel.uiState {
                    self?.state = state
                }
            },
            Task {
                for await _ in viewModel.saved {
                    onSaved()
                }
            },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        viewModel.clear()
    }

    func onTypeChange(_ type: DeviceType) { apply { $0.onTypeChange(type: type) } }
    func onNameChange(_ value: String) { apply { $0.onNameChange(value: value) } }
    func onMacAddressChange(_ value: String) { apply { $0.onMacAddressChange(value: value) } }
    func onSerialChange(_ value: String) { apply { $0.onSerialChange(value: value) } }
    func onUsernameChange(_ value: String) { apply { $0.onUsernameChange(value: value) } }
    func onPasswordChange(_ value: String) { apply { $0.onPasswordChange(value: value) } }
    func submit() { apply { $0.submit() } }

    /// Runs an action and publishes the resulting state right away. Waiting for the flow
    /// would leave SwiftUI rendering the previous text for a moment, and a text field
    /// bound to stale text can overwrite what the user is typing.
    private func apply(_ action: (DeviceFormViewModel) -> Void) {
        action(viewModel)
        state = viewModel.uiState.value
    }
}
