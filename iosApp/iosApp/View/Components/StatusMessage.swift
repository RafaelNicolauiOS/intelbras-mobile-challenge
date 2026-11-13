import SwiftUI

/// Message for empty and error states, with an optional retry action.
struct StatusMessage: View {
    let message: String
    var onRetry: (() -> Void)?

    var body: some View {
        VStack(spacing: 8) {
            Text(message)
                .font(.app(16))
                .foregroundStyle(AppColors.text)
                .multilineTextAlignment(.center)
            if let onRetry {
                Button("Tentar novamente", action: onRetry)
                    .font(.app(14, .semibold))
                    .foregroundStyle(AppColors.primary)
                    .frame(minHeight: 44)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity)
    }
}
