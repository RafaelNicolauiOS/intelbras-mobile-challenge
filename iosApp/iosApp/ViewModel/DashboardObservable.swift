@preconcurrency import Shared
import SwiftUI

/// Bridges the shared Kotlin `DashboardViewModel` to SwiftUI: it republishes the
/// ViewModel's StateFlow as `@Published` state and forwards user actions.
/// `ObservableObject` (not `@Observable`) because the deployment target is iOS 16.
/// Everything here runs on the main actor; the filtering itself happens in the shared
/// ViewModel, off the main thread.
@MainActor
final class DashboardObservable: ObservableObject {
    @Published private(set) var state: DashboardUiState

    private let viewModel: DashboardViewModel
    private nonisolated let bag: ObservationBag

    init(viewModel: DashboardViewModel) {
        self.viewModel = viewModel
        state = viewModel.uiState.value
        bag = ObservationBag(release: viewModel.clear)
        bag.add(Task { @MainActor [weak self] in
            for await state in viewModel.uiState {
                guard let self, state != self.state else { continue }
                self.state = state
                Signposts.published("Dashboard state")
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
