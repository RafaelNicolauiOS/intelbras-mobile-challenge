@preconcurrency import Shared
import SwiftUI

struct DeviceDetailsView: View {
    @ObservedObject var observable: DeviceDetailsObservable

    var body: some View {
        VStack(spacing: 12) {
            if let device = observable.device {
                AppCard {
                    HStack(spacing: 12) {
                        DeviceTypeIcon(type: device.type)
                        Text(device.name)
                            .font(.app(16, .semibold))
                    }
                    .padding(16)
                }
                AppCard {
                    VStack(spacing: 8) {
                        row("Tipo", device.type.label)
                        switch onEnum(of: device) {
                        case .alarm(let alarm):
                            row("MAC Address", alarm.identifier, canCopy: true)
                        case .video(let video):
                            row("Número de série", video.serial, canCopy: true)
                            row("Usuário", video.username)
                        }
                    }
                    .padding(16)
                }
            } else {
                StatusMessage(message: "Dispositivo não encontrado.")
            }
            Spacer(minLength: 0)
        }
        .foregroundStyle(AppColors.text)
        .padding(16)
        .background(AppColors.background)
    }

    private func row(_ label: String, _ value: String, canCopy: Bool = false) -> some View {
        HStack(spacing: 8) {
            Text(label)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
            if canCopy {
                Button {
                    observable.copy(value)
                } label: {
                    Image(systemName: "doc.on.doc")
                        .font(.app(14))
                        .frame(width: 32, height: 32)
                }
                .accessibilityLabel("Copiar \(label)")
            }
        }
        .font(.app(16))
    }
}
