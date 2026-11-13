import Shared
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
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(AppColors.text)
                            typeSelector(selected: state.type)
                        }

                        field("Nome", state.name, state.errors.name, observable.onNameChange)

                        switch state.type {
                        case .alarm:
                            field("MAC Address", state.macAddress, state.errors.macAddress, observable.onMacAddressChange)
                                .textInputAutocapitalization(.characters)
                            field("Senha", state.password, state.errors.password, observable.onPasswordChange, isSecure: true)
                                .keyboardType(.numberPad)
                        case .video:
                            field("Número de série", state.serial, state.errors.serial, observable.onSerialChange)
                            field("Usuário", state.username, state.errors.username, observable.onUsernameChange)
                                .textInputAutocapitalization(.never)
                            field("Senha", state.password, state.errors.password, observable.onPasswordChange, isSecure: true)
                        }
                    }
                    .padding(16)
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
                        .font(.system(size: 14, weight: .semibold))
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
        _ onChange: @escaping (String) -> Void,
        isSecure: Bool = false
    ) -> some View {
        OutlinedField(label: label, text: text, errorText: error?.text, isSecure: isSecure, onChange: onChange)
    }
}
