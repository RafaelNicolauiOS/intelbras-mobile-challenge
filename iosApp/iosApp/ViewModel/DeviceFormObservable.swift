@preconcurrency import Shared
import SwiftUI

/// Bridges the shared Kotlin `DeviceFormViewModel` to SwiftUI. Text field changes arrive
/// here on the main actor and are published synchronously, on the main actor too.
@MainActor
final class DeviceFormObservable: ObservableObject {
    @Published private(set) var state: DeviceFormUiState

    private let viewModel: DeviceFormViewModel
    private nonisolated let bag: ObservationBag

    /// `onSaved` runs once the device was stored, so the coordinator can close the form.
    init(viewModel: DeviceFormViewModel, onSaved: @escaping @MainActor () -> Void) {
        self.viewModel = viewModel
        state = viewModel.uiState.value
        bag = ObservationBag(release: viewModel.clear)
        bag.add(Task { @MainActor [weak self] in
            // The state `apply` already published comes back through the flow; it is
            // skipped here, so each keystroke renders once instead of twice.
            for await state in viewModel.uiState {
                guard let self, state != self.state else { continue }
                self.state = state
                Signposts.published("Device form state")
            }
        })
        bag.add(Task { @MainActor in
            for await _ in viewModel.saved {
                onSaved()
            }
        })
    }

    /// Stops observing and clears the ViewModel. Called by the coordinator when the screen leaves.
    func stop() {
        bag.stop()
    }

    /// True once the observation was stopped (screen left, or `stop()` called).
    var isStopped: Bool { bag.isStopped }

    deinit {
        bag.stop()
    }

    func onTypeChange(_ type: DeviceType) { apply { $0.onTypeChange(type: type) } }
    func onNameChange(_ value: String) { apply { $0.onNameChange(value: value) } }
    func onMacAddressChange(_ value: String) { apply { $0.onMacAddressChange(value: value) } }
    func onSerialChange(_ value: String) { apply { $0.onSerialChange(value: value) } }
    func onUsernameChange(_ value: String) { apply { $0.onUsernameChange(value: value) } }
    func onPasswordChange(_ value: String) { apply { $0.onPasswordChange(value: value) } }
    func submit() { apply { $0.submit() } }

    /// Runs an action and publishes the resulting state right away, on the main actor.
    /// Waiting for the flow would leave SwiftUI rendering the previous text for a moment,
    /// and a text field bound to stale text can overwrite what the user is typing. So a
    /// field's text must never be published asynchronously.
    private func apply(_ action: (DeviceFormViewModel) -> Void) {
        action(viewModel)
        let new = viewModel.uiState.value
        guard new != state else { return }
        state = new
        Signposts.published("Device form state")
    }
}
