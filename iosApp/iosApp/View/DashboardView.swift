import Shared
import SwiftUI

/// Renders the dashboard state. Navigation is reported through closures and handled by the coordinator.
struct DashboardView: View {
    @ObservedObject var observable: DashboardObservable
    let onOpenDevice: (Device) -> Void

    var body: some View {
        let state = observable.state
        if state.isLoading {
            ProgressView()
        } else if state.devices.isEmpty, let error = state.error {
            // Nothing cached and the sync failed: the error takes the whole screen.
            StatusMessage(message: error.message, onRetry: observable.refresh)
        } else if state.devices.isEmpty {
            StatusMessage(message: "Nenhum dispositivo cadastrado.", onRetry: observable.refresh)
        } else {
            List {
                // Cached devices stay visible; the failed sync is only a notice.
                if let error = state.error {
                    Text("\(error.message) Mostrando os últimos dados salvos.")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
                ForEach(state.devices, id: \.id) { device in
                    DeviceCard(device: device) {
                        observable.toggleFavorite(device)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { onOpenDevice(device) }
                }
            }
            .refreshable { observable.refresh() }
        }
    }
}
