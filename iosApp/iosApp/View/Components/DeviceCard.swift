import Shared
import SwiftUI

struct DeviceCard: View {
    let device: Device
    let onToggleFavorite: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .frame(width: 32)
                .accessibilityHidden(true)
            Text(device.name)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onToggleFavorite) {
                Image(systemName: device.isFavorite ? "star.fill" : "star")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(device.isFavorite ? "Desmarcar favorito" : "Marcar favorito")
        }
        .padding(.vertical, 4)
    }

    private var iconName: String {
        switch device.type {
        case .alarm: return "bell"
        case .video: return "video"
        }
    }
}
