import Shared
import SwiftUI

/// Renders the dashboard state. Navigation is reported through closures and handled by the coordinator.
struct DashboardView: View {
    @ObservedObject var observable: DashboardObservable
    let onOpenDevice: (Device) -> Void

    var body: some View {
        if observable.state.isLoading {
            ProgressView()
        } else {
            List(observable.state.devices, id: \.id) { device in
                DeviceCard(device: device) {
                    observable.toggleFavorite(device)
                }
                .contentShape(Rectangle())
                .onTapGesture { onOpenDevice(device) }
            }
        }
    }
}
