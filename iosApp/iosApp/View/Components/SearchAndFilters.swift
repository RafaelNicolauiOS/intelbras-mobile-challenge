@preconcurrency import Shared
import SwiftUI

/// Search by device name. The text being typed is kept here and only reported upwards:
/// the screen state arrives asynchronously, and echoing it back into the field would
/// fight with fast typing.
///
/// Focus is owned by the screen (`isFocused`), so it can close the keyboard when the user
/// taps anywhere else. The whole pill is the touch target: tapping the icon or the padding
/// around the text opens the keyboard too, not only the line of text itself.
struct SearchField: View {
    let onQueryChange: @MainActor (String) -> Void
    var isFocused: FocusState<Bool>.Binding

    @State private var text = ""

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(AppColors.text)
                .accessibilityHidden(true)
            TextField("", text: $text, prompt: Text("Pesquisar").foregroundColor(AppColors.textSecondary))
                .font(.app(14))
                .foregroundStyle(AppColors.text)
                .tint(AppColors.primary)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .focused(isFocused)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(AppColors.text)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Limpar busca")
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, text.isEmpty ? 16 : 0)
        .frame(minHeight: 48)
        .contentShape(Capsule())
        .onTapGesture { isFocused.wrappedValue = true }
        .background(AppColors.surface, in: Capsule())
        .overlay(Capsule().stroke(AppColors.surfaceStrong, lineWidth: 1))
        .onChange(of: text) { onQueryChange($0) }
    }
}

struct FilterChipsRow: View {
    let selected: DeviceFilter
    let onSelected: @MainActor (DeviceFilter) -> Void

    private let filters: [DeviceFilter] = [.all, .favorites, .cameras, .alarms]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(filters, id: \.self) { filter in
                    let isSelected = filter == selected
                    Button(filter.label) { onSelected(filter) }
                        .font(.app(14))
                        .padding(.horizontal, 14)
                        .frame(minHeight: 36)
                        .foregroundStyle(isSelected ? AppColors.primary : AppColors.text)
                        .background(isSelected ? AppColors.primarySoft : AppColors.surfaceStrong, in: Capsule())
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
        }
    }
}
