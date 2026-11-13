import Shared
import SwiftUI

struct DeviceFormView: View {
    @ObservedObject var observable: DeviceFormObservable

    var body: some View {
        let state = observable.state
        if state.isLoading {
            ProgressView()
        } else {
            Form {
                // The type decides which API resource is used, so it is fixed once the device exists.
                if !state.isEditing {
                    Picker("Tipo", selection: Binding(get: { state.type }, set: observable.onTypeChange)) {
                        Text(DeviceType.video.label).tag(DeviceType.video)
                        Text(DeviceType.alarm.label).tag(DeviceType.alarm)
                    }
                    .pickerStyle(.segmented)
                }

                field("Nome", text: state.name, error: state.errors.name, onChange: observable.onNameChange)

                switch state.type {
                case .alarm:
                    field("Endereço MAC", text: state.macAddress, error: state.errors.macAddress,
                          onChange: observable.onMacAddressChange)
                        .textInputAutocapitalization(.characters)
                    field("Senha (4 ou 6 dígitos)", text: state.password, error: state.errors.password,
                          isSecure: true, onChange: observable.onPasswordChange)
                        .keyboardType(.numberPad)
                case .video:
                    field("Número de série", text: state.serial, error: state.errors.serial,
                          onChange: observable.onSerialChange)
                    field("Usuário", text: state.username, error: state.errors.username,
                          onChange: observable.onUsernameChange)
                        .textInputAutocapitalization(.never)
                    field("Senha", text: state.password, error: state.errors.password,
                          isSecure: true, onChange: observable.onPasswordChange)
                }

                Button(state.isSaving ? "Salvando…" : "Salvar", action: observable.submit)
                    .frame(maxWidth: .infinity)
                    .disabled(state.isSaving)
            }
            .autocorrectionDisabled()
        }
    }

    private func field(
        _ title: String,
        text: String,
        error: FieldError?,
        isSecure: Bool = false,
        onChange: @escaping (String) -> Void
    ) -> some View {
        let binding = Binding(get: { text }, set: onChange)
        return VStack(alignment: .leading, spacing: 4) {
            if isSecure {
                SecureField(title, text: binding)
            } else {
                TextField(title, text: binding)
            }
            if let error {
                Text(error.text)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
