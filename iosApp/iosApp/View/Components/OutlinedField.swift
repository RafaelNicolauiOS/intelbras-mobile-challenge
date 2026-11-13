import SwiftUI

/// The outlined text field from the style guide: the label sits inside while empty and
/// moves onto the border once the field has focus or text.
struct OutlinedField: View {
    let label: String
    let text: String
    let errorText: String?
    var isSecure = false
    /// Called on the main actor for every edit. `@Sendable` because SwiftUI's `Binding` setter requires it.
    let onChange: @MainActor @Sendable (String) -> Void

    @FocusState private var isFocused: Bool
    @State private var isHidden = true

    private var isFloating: Bool { isFocused || !text.isEmpty }

    private var accent: Color {
        if errorText != nil { return AppColors.error }
        return isFocused ? AppColors.primary : AppColors.textSecondary
    }

    var body: some View {
        let binding = Binding(get: { text }, set: onChange)
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Group {
                    if isSecure && isHidden {
                        SecureField("", text: binding)
                    } else {
                        TextField("", text: binding)
                    }
                }
                .focused($isFocused)
                .font(.app(16))
                .foregroundStyle(AppColors.text)
                .tint(AppColors.primary)
                .accessibilityLabel(label)

                if isSecure {
                    Button {
                        isHidden.toggle()
                    } label: {
                        Image(systemName: isHidden ? "eye.slash" : "eye")
                            .foregroundStyle(AppColors.textSecondary)
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel(isHidden ? "Mostrar senha" : "Ocultar senha")
                }
            }
            .padding(.leading, 16)
            .padding(.trailing, isSecure ? 4 : 16)
            .frame(minHeight: 56)
            .overlay(RoundedRectangle(cornerRadius: 4).stroke(accent, lineWidth: isFocused ? 2 : 1))
            .overlay(alignment: .topLeading) { floatingLabel }
            .contentShape(Rectangle())
            .onTapGesture { isFocused = true }

            if let errorText {
                Text(errorText)
                    .font(.app(12))
                    .foregroundStyle(AppColors.error)
                    .padding(.leading, 16)
            }
        }
        .animation(.easeOut(duration: 0.15), value: isFloating)
    }

    private var floatingLabel: some View {
        Text(label)
            .font(.app(isFloating ? 12 : 16))
            .foregroundStyle(isFloating ? accent : AppColors.textSecondary)
            .padding(.horizontal, 4)
            .background(AppColors.background)
            .offset(x: 12, y: isFloating ? -8 : 18)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}
