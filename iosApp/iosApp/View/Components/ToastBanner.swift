import SwiftUI

/// iOS has no native Snackbar; this banner plays the same role at the bottom of the screen.
struct ToastBanner: View {
    let text: String
    let isFailure: Bool

    var body: some View {
        Label(text, systemImage: isFailure ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
            .font(.subheadline)
            .foregroundStyle(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isFailure ? Color.red : Color(white: 0.2), in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            .accessibilityAddTraits(.isStaticText)
    }
}
