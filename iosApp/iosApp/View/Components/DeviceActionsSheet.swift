import Shared
import SwiftUI

/// The options behind a device's "more" button. Every choice also dismisses the sheet.
struct DeviceActionsSheet: View {
    let device: Device
    let onEdit: () -> Void
    let onInfo: () -> Void
    let onToggleFavorite: () -> Void
    let onDelete: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(device.name)
                .font(.headline)
                .padding()
            action("Editar dispositivo", icon: "pencil", perform: onEdit)
            action("Informações", icon: "info.circle", perform: onInfo)
            action(
                device.isFavorite ? "Desmarcar favorito" : "Marcar favorito",
                icon: device.isFavorite ? "star.slash" : "star",
                perform: onToggleFavorite
            )
            action("Remover dispositivo", icon: "trash", role: .destructive, perform: onDelete)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .presentationDetents([.height(300)])
        .presentationDragIndicator(.visible)
    }

    private func action(
        _ title: String,
        icon: String,
        role: ButtonRole? = nil,
        perform: @escaping () -> Void
    ) -> some View {
        Button(role: role) {
            dismiss()
            perform()
        } label: {
            Label(title, systemImage: icon)
                .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                .padding(.horizontal)
        }
    }
}
