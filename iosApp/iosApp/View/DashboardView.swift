@preconcurrency import Shared
import SwiftUI

/// Renders the dashboard state. Navigation is reported through closures and handled by the coordinator.
struct DashboardView: View {
    @ObservedObject var observable: DashboardObservable
    let onAddDevice: @MainActor () -> Void
    let onOpenDevice: @MainActor (Device) -> Void
    let onEditDevice: @MainActor (Device) -> Void

    // Ids, not devices: the sheet and dialog always show the current data from the state.
    @State private var sheetDeviceId: String?
    @State private var deleteDeviceId: String?

    @FocusState private var isSearchFocused: Bool
    /// Where the search field is, so a tap can be told apart as inside or outside it.
    @State private var searchFrame: CGRect = .zero
    private static let space = "dashboard"

    var body: some View {
        let state = observable.state
        VStack(alignment: .leading, spacing: 0) {
            Text("Meus dispositivos")
                .font(.app(24, .semibold))
                .accessibilityAddTraits(.isHeader)
                .padding(.top, 24)
            Text(deviceCount(Int(state.totalCount)))
                .font(.app(14))

            // Search and chips stay on screen whenever there is something to search in,
            // including when the current search matches nothing.
            if state.hasAnyDevice {
                SearchField(onQueryChange: observable.onQueryChange, isFocused: $isSearchFocused)
                    .background(
                        GeometryReader { proxy in
                            Color.clear.preference(key: SearchFrameKey.self, value: proxy.frame(in: .named(Self.space)))
                        }
                    )
                    .padding(.top, 16)
                FilterChipsRow(selected: state.filter, onSelected: observable.onFilterSelected)
                    .padding(.top, 12)
            }

            content(state)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            addButton
        }
        .foregroundStyle(AppColors.text)
        .padding(.horizontal, 16)
        .background(AppColors.background)
        .coordinateSpace(name: Self.space)
        .onPreferenceChange(SearchFrameKey.self) { frame in
            // SwiftUI calls this on the main thread, but the closure is typed as @Sendable.
            Task { @MainActor in searchFrame = frame }
        }
        // A tap anywhere outside the search field closes the keyboard. Simultaneous, so
        // buttons, chips and rows still get their tap; scrolling is not a tap and keeps it open.
        .simultaneousGesture(
            SpatialTapGesture(coordinateSpace: .named(Self.space)).onEnded { tap in
                // Until the field's frame is known (right after it appears) no tap can be
                // classified, so none closes the keyboard; otherwise it could never open.
                guard searchFrame != .zero, !searchFrame.contains(tap.location) else { return }
                isSearchFocused = false
            }
        )
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
        // Platform adaptation: the prototype's custom modal becomes the native confirmation
        // dialog, which is what iOS users expect for a destructive action.
        .confirmationDialog(
            "Remover dispositivo?",
            isPresented: isPresenting($deleteDeviceId),
            titleVisibility: .visible,
            presenting: device(withId: deleteDeviceId)
        ) { device in
            Button("Confirmar", role: .destructive) { observable.delete(device) }
            Button("Cancelar", role: .cancel) {}
        } message: { _ in
            Text("Você tem certeza que deseja remover este dispositivo?")
        }
    }

    @ViewBuilder
    private func content(_ state: DashboardUiState) -> some View {
        if state.isLoading {
            ProgressView()
        } else if !state.hasAnyDevice, let error = state.error {
            // Nothing cached and the sync failed: the error takes the whole screen.
            StatusMessage(message: error.message, onRetry: observable.refresh)
        } else if !state.hasAnyDevice {
            StatusMessage(message: "Nenhum dispositivo cadastrado.", onRetry: observable.refresh)
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    // Cached devices stay visible; the failed sync is only a notice.
                    if let error = state.error {
                        Text("\(error.message) Mostrando os últimos dados salvos.")
                            .font(.app(14))
                            .foregroundStyle(AppColors.error)
                    }
                    if state.devices.isEmpty {
                        StatusMessage(message: "Nenhum dispositivo encontrado.")
                    } else {
                        DeviceListCard(
                            devices: state.devices,
                            onOpenDevice: onOpenDevice,
                            onMore: { sheetDeviceId = $0.id }
                        )
                    }
                }
                .padding(.vertical, 16)
            }
            .refreshable { observable.refresh() }
        }
    }

    private var addButton: some View {
        Button(action: onAddDevice) {
            HStack(spacing: 12) {
                Image(systemName: "plus")
                Text("Adicionar")
                    .font(.app(16))
            }
            .foregroundStyle(AppColors.text)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(AppColors.surfaceStrong, in: Capsule())
        }
        .padding(.vertical, 16)
    }

    private func deviceCount(_ count: Int) -> String {
        switch count {
        case 0: return "Nenhum dispositivo"
        case 1: return "1 dispositivo"
        default: return "\(count) dispositivos"
        }
    }

    private func device(withId id: String?) -> Device? {
        observable.state.devices.first { $0.id == id }
    }

    private func isPresenting(_ id: Binding<String?>) -> Binding<Bool> {
        Binding(get: { id.wrappedValue != nil }, set: { if !$0 { id.wrappedValue = nil } })
    }
}

/// Carries the search field's frame up to the dashboard.
struct SearchFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero

    /// SwiftUI also calls this for siblings that never set the key, passing `defaultValue`.
    /// Only a real frame may replace the value; otherwise those siblings erase it, every tap
    /// counts as "outside" and the keyboard closes the moment it opens.
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        let next = nextValue()
        if next != defaultValue { value = next }
    }
}
