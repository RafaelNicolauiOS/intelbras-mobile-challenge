import Shared
import SwiftUI

/// Bridges the shared Kotlin `DashboardViewModel` to SwiftUI: it republishes the
/// ViewModel's StateFlow as `@Published` state and forwards user actions.
/// `ObservableObject` (not `@Observable`) because the deployment target is iOS 16.
@MainActor
final class DashboardObservable: ObservableObject {
    @Published private(set) var state: DashboardUiState

    private let viewModel: DashboardViewModel
    private var observation: Task<Void, Never>?

    init(viewModel: DashboardViewModel) {
        self.viewModel = viewModel
        state = viewModel.uiState.value
        observation = Task { [weak self] in
            for await state in viewModel.uiState {
                self?.state = state
            }
        }
    }

    deinit {
        observation?.cancel()
        viewModel.clear()
    }

    func onQueryChange(_ value: String) {
        viewModel.onQueryChange(value: value)
    }

    func onFilterSelected(_ filter: DeviceFilter) {
        viewModel.onFilterSelected(value: filter)
    }

    func refresh() {
        viewModel.refresh()
    }

    func delete(_ device: Device) {
        viewModel.delete(device: device)
    }

    func toggleFavorite(_ device: Device) {
        viewModel.toggleFavorite(device: device)
    }
}
