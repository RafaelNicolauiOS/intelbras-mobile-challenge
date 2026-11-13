import Shared
import SwiftUI

/// Search by device name. The text being typed is kept here and only reported upwards:
/// the screen state arrives asynchronously, and echoing it back into the field would
/// fight with fast typing.
struct SearchField: View {
    let onQueryChange: (String) -> Void

    @State private var text = ""

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(AppColors.text)
                .accessibilityHidden(true)
            TextField("", text: $text, prompt: Text("Pesquisar").foregroundColor(AppColors.textSecondary))
                .font(.system(size: 14))
                .foregroundStyle(AppColors.text)
                .tint(AppColors.primary)
                .autocorrectionDisabled()
                .submitLabel(.search)
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
        .background(AppColors.surface, in: Capsule())
        .overlay(Capsule().stroke(AppColors.surfaceStrong, lineWidth: 1))
        .onChange(of: text) { onQueryChange($0) }
    }
}

struct FilterChipsRow: View {
    let selected: DeviceFilter
    let onSelected: (DeviceFilter) -> Void

    private let filters: [DeviceFilter] = [.all, .favorites, .cameras, .alarms]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(filters, id: \.self) { filter in
                    let isSelected = filter == selected
                    Button(filter.label) { onSelected(filter) }
                        .font(.system(size: 14))
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
