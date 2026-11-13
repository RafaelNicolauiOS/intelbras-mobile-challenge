import Shared
import SwiftUI

struct DeviceCard: View {
    let device: Device
    let onMore: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .frame(width: 32)
                .accessibilityLabel(device.type.label)
            Text(device.name)
                .frame(maxWidth: .infinity, alignment: .leading)
            if device.isFavorite {
                Image(systemName: "star.fill")
                    .foregroundStyle(.yellow)
                    .accessibilityLabel("Favorito")
            }
            Button(action: onMore) {
                Image(systemName: "ellipsis")
                    .rotationEffect(.degrees(90))
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("Mais opções de \(device.name)")
        }
    }

    private var iconName: String {
        switch device.type {
        case .alarm: return "bell"
        case .video: return "video"
        }
    }
}
