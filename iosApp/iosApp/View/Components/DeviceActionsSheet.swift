import Shared
import SwiftUI

/// The options behind a device's "more actions" button. Every choice also dismisses the sheet.
struct DeviceActionsSheet: View {
    let device: Device
    let onEdit: () -> Void
    let onInfo: () -> Void
    let onToggleFavorite: () -> Void
    let onDelete: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(device.type.sheetTitle)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppColors.text)
                .frame(maxWidth: .infinity)
                .padding(.top, 28)
                .padding(.bottom, 24)
            action("Informações", icon: "info.circle", perform: onInfo)
            action("Editar dispositivo", icon: "square.and.pencil", perform: onEdit)
            action(
                device.isFavorite ? "Desmarcar favorito" : "Marcar favorito",
                icon: device.isFavorite ? "star.slash" : "star",
                perform: onToggleFavorite
            )
            Rectangle()
                .fill(AppColors.borderMedium)
                .frame(height: 1)
                .padding(.horizontal, 16)
            action("Remover dispositivo", icon: "trash", color: AppColors.error, perform: onDelete)
            Spacer(minLength: 0)
        }
        .background(AppColors.background)
        .presentationDetents([.height(340)])
    }

    private func action(
        _ title: String,
        icon: String,
        color: Color = AppColors.text,
        perform: @escaping () -> Void
    ) -> some View {
        Button {
            dismiss()
            perform()
        } label: {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .frame(width: 24)
                Text(title)
                    .font(.system(size: 16))
            }
            .foregroundStyle(color)
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .padding(.horizontal, 20)
            .contentShape(Rectangle())
        }
    }
}
