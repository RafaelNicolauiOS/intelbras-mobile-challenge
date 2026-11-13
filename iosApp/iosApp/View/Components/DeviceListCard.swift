import Shared
import SwiftUI

/// The square with the device-type icon that leads a device row.
struct DeviceTypeIcon: View {
    let type: DeviceType

    var body: some View {
        Image(systemName: type.iconName)
            .font(.app(16))
            .foregroundStyle(AppColors.text)
            .frame(width: 40, height: 40)
            .background(AppColors.background, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(AppColors.surfaceStrong, lineWidth: 1))
            .accessibilityLabel(type.sheetTitle)
    }
}

/// All devices inside one card, separated by dividers, as in the prototype.
struct DeviceListCard: View {
    let devices: [Device]
    let onOpenDevice: (Device) -> Void
    let onMore: (Device) -> Void

    var body: some View {
        AppCard {
            VStack(spacing: 0) {
                ForEach(Array(devices.enumerated()), id: \.element.id) { index, device in
                    if index > 0 {
                        Rectangle()
                            .fill(AppColors.surfaceStrong)
                            .frame(height: 1)
                            .padding(.horizontal, 16)
                    }
                    row(device)
                }
            }
        }
    }

    private func row(_ device: Device) -> some View {
        HStack(spacing: 12) {
            DeviceTypeIcon(type: device.type)
            VStack(alignment: .leading, spacing: 2) {
                Text(device.name)
                    .font(.app(16, .semibold))
                Text(device.identifier)
                    .font(.app(14))
            }
            .foregroundStyle(AppColors.text)
            .lineLimit(1)
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                onMore(device)
            } label: {
                Image(systemName: "ellipsis")
                    .rotationEffect(.degrees(90))
                    .foregroundStyle(AppColors.text)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Mais ações de \(device.name)")
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
        .onTapGesture { onOpenDevice(device) }
    }
}
