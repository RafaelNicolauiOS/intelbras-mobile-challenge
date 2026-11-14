@preconcurrency import Shared
import SwiftUI

struct DeviceFormView: View {
    @ObservedObject var observable: DeviceFormObservable

    var body: some View {
        let state = observable.state
        Group {
            if state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        // The type decides which API resource is used, so it is fixed once the device exists.
                        if !state.isEditing {
                            Text("Tipo de dispositivo")
                                .font(.app(14, .semibold))
                                .foregroundStyle(AppColors.text)
                            typeSelector(selected: state.type)
                        }

                        field("Nome", state.name, state.errors.name, { observable.onNameChange($0) })

                        switch state.type {
                        case .alarm:
                            field("MAC Address", state.macAddress, state.errors.macAddress, { observable.onMacAddressChange($0) })
                                .textInputAutocapitalization(.characters)
                            field("Senha", state.password, state.errors.password, { observable.onPasswordChange($0) }, isSecure: true)
                                .keyboardType(.numberPad)
                        case .video:
                            field(
                                "Número de série",
                                state.serial,
                                state.errors.serial,
                                { observable.onSerialChange($0) },
                                maxLength: Int(DeviceValidator.shared.SERIAL_LENGTH)
                            )
                            field("Usuário", state.username, state.errors.username, { observable.onUsernameChange($0) })
                                .textInputAutocapitalization(.never)
                            field("Senha", state.password, state.errors.password, { observable.onPasswordChange($0) }, isSecure: true)
                        }
                    }
                    .padding(16)
                    // Switching type starts an empty form: new fields, so no text carries over.
                    .id(state.type)
                }
                .autocorrectionDisabled()
            }
        }
        .background(AppColors.background)
        // The hosting controller takes the back button from SwiftUI, so it is hidden here;
        // the close button in the navigation bar replaces it.
        .navigationBarBackButtonHidden(true)
    }

    private func typeSelector(selected: DeviceType) -> some View {
        HStack(spacing: 0) {
            ForEach([DeviceType.alarm, DeviceType.video], id: \.self) { type in
                let isSelected = type == selected
                Button {
                    observable.onTypeChange(type)
                } label: {
                    Text(type.label)
                        .font(.app(14, .semibold))
                        .foregroundStyle(isSelected ? AppColors.primary : AppColors.text)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(
                            isSelected ? AppColors.background : AppColors.surfaceStrong,
                            in: RoundedRectangle(cornerRadius: 8)
                        )
                        .shadow(color: .black.opacity(isSelected ? 0.12 : 0), radius: 1, y: 1)
                }
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(2)
        .background(AppColors.surfaceStrong, in: RoundedRectangle(cornerRadius: 8))
    }

    private func field(
        _ label: String,
        _ text: String,
        _ error: FieldError?,
        _ onChange: @escaping @MainActor @Sendable (String) -> Void,
        isSecure: Bool = false,
        maxLength: Int? = nil
    ) -> some View {
        OutlinedField(label: label, text: text, errorText: error?.text, isSecure: isSecure, maxLength: maxLength, onChange: onChange)
    }
}
