import Shared
import SwiftUI

/// Search by device name. The text being typed is kept here and only reported upwards:
/// the screen state arrives asynchronously, and echoing it back into the field would
/// fight with fast typing.
struct SearchField: View {
    let onQueryChange: (String) -> Void

    @State private var text = ""

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            TextField("Buscar por nome", text: $text)
                .autocorrectionDisabled()
                .submitLabel(.search)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel("Limpar busca")
            }
        }
        .padding(.horizontal, 12)
        .frame(minHeight: 44)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
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
                        .font(.subheadline)
                        .padding(.horizontal, 14)
                        .frame(minHeight: 36)
                        .foregroundStyle(isSelected ? Color.white : Color.primary)
                        .background(
                            isSelected ? Color.accentColor : Color(.secondarySystemGroupedBackground),
                            in: Capsule()
                        )
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
            .padding(.horizontal, 16)
        }
    }
}
