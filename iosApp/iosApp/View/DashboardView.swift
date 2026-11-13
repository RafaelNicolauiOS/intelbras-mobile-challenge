import Shared
import SwiftUI

/// Renders the dashboard state. Navigation is reported through closures and handled by the coordinator.
struct DashboardView: View {
    @ObservedObject var observable: DashboardObservable
    let onOpenDevice: (Device) -> Void
    let onEditDevice: (Device) -> Void

    // Ids, not devices: the sheet and dialog always show the current data from the state.
    @State private var sheetDeviceId: String?
    @State private var deleteDeviceId: String?

    var body: some View {
        content
            .sheet(isPresented: isPresenting($sheetDeviceId)) {
                if let device = device(withId: sheetDeviceId) {
                    DeviceActionsSheet(
                        device: device,
                        onEdit: { onEditDevice(device) },
                        onInfo: { onOpenDevice(device) },
                        onToggleFavorite: { observable.toggleFavorite(device) },
                        onDelete: { deleteDeviceId = device.id }
                    )
                }
            }
            .confirmationDialog(
                "Remover dispositivo?",
                isPresented: isPresenting($deleteDeviceId),
                titleVisibility: .visible,
                presenting: device(withId: deleteDeviceId)
            ) { device in
                Button("Remover", role: .destructive) { observable.delete(device) }
                Button("Cancelar", role: .cancel) {}
            } message: { device in
                Text("\"\(device.name)\" será removido do servidor. Essa ação não pode ser desfeita.")
            }
    }

    @ViewBuilder
    private var content: some View {
        let state = observable.state
        if state.isLoading {
            ProgressView()
        } else if !state.hasAnyDevice, let error = state.error {
            // Nothing cached and the sync failed: the error takes the whole screen.
            StatusMessage(message: error.message, onRetry: observable.refresh)
        } else if !state.hasAnyDevice {
            StatusMessage(message: "Nenhum dispositivo cadastrado.", onRetry: observable.refresh)
        } else {
            VStack(spacing: 8) {
                // Search and chips stay on screen even when the search matches nothing.
                SearchField(onQueryChange: observable.onQueryChange)
                    .padding(.horizontal, 16)
                FilterChipsRow(selected: state.filter, onSelected: observable.onFilterSelected)
                List {
                    // Cached devices stay visible; the failed sync is only a notice.
                    if let error = state.error {
                        Text("\(error.message) Mostrando os últimos dados salvos.")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                    if state.devices.isEmpty {
                        Text("Nenhum dispositivo encontrado.")
                            .frame(maxWidth: .infinity)
                            .foregroundStyle(.secondary)
                    }
                    ForEach(state.devices, id: \.id) { device in
                        DeviceCard(device: device) { sheetDeviceId = device.id }
                            .contentShape(Rectangle())
                            .onTapGesture { onOpenDevice(device) }
                    }
                }
                .refreshable { observable.refresh() }
            }
            .padding(.top, 8)
            .background(Color(.systemGroupedBackground))
        }
    }

    private func device(withId id: String?) -> Device? {
        observable.state.devices.first { $0.id == id }
    }

    private func isPresenting(_ id: Binding<String?>) -> Binding<Bool> {
        Binding(get: { id.wrappedValue != nil }, set: { if !$0 { id.wrappedValue = nil } })
    }
}
