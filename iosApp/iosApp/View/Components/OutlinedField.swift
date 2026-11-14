import SwiftUI

/// The outlined text field from the style guide: the label sits inside while empty and
/// moves onto the border once the field has focus or text.
///
/// The field edits its own copy of the text (`draft`), starting from `text`, and reports each
/// change. Editing a local copy is what lets `maxLength` work: SwiftUI only rewrites a text
/// field when the value it is given changes, so cutting the input in the state alone
/// (13 → 13 characters) would leave the extra characters on screen.
///
/// The state does not write back into a field while it exists: its echo of each keystroke
/// arrives one step late, and applying it would overwrite what the user just typed. A screen
/// that resets its text (e.g. switching the device type) recreates the fields with `.id`.
struct OutlinedField: View {
    let label: String
    let text: String
    let errorText: String?
    var isSecure = false
    /// Characters beyond this are dropped while typing or pasting. Nil means no limit.
    var maxLength: Int?
    /// Called on the main actor for every edit.
    let onChange: @MainActor @Sendable (String) -> Void

    @FocusState private var isFocused: Bool
    @State private var isHidden = true
    @State private var draft: String

    init(
        label: String,
        text: String,
        errorText: String?,
        isSecure: Bool = false,
        maxLength: Int? = nil,
        onChange: @escaping @MainActor @Sendable (String) -> Void
    ) {
        self.label = label
        self.text = text
        self.errorText = errorText
        self.isSecure = isSecure
        self.maxLength = maxLength
        self.onChange = onChange
        _draft = State(initialValue: text)
    }

    private var isFloating: Bool { isFocused || !draft.isEmpty }

    private var accent: Color {
        if errorText != nil { return AppColors.error }
        return isFocused ? AppColors.primary : AppColors.textSecondary
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Group {
                    if isSecure && isHidden {
                        SecureField("", text: $draft)
                    } else {
                        TextField("", text: $draft)
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
        .onChange(of: draft) { typed in
            if let maxLength, typed.count > maxLength {
                // Changing the field's own value is what makes SwiftUI redraw it without the extra characters.
                draft = String(typed.prefix(maxLength))
                return
            }
            if typed != text { onChange(typed) }
        }
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
