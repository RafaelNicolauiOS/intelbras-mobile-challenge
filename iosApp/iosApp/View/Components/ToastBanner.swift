import SwiftUI

/// iOS has no native Snackbar; this banner reproduces the one in the prototype.
struct ToastBanner: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(AppColors.textOnDark)
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
            .background(AppColors.surfaceDark, in: RoundedRectangle(cornerRadius: 4))
            .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
            .padding(.horizontal, 16)
            .accessibilityAddTraits(.isStaticText)
    }
}
