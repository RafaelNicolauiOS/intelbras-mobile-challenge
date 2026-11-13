import Shared

extension UserMessage {
    var text: String {
        switch onEnum(of: self) {
        case .success(let success):
            switch success.kind {
            case .deviceCreated: return "Dispositivo adicionado."
            case .deviceUpdated: return "Dispositivo atualizado."
            case .deviceDeleted: return "Dispositivo removido."
            }
        case .failure(let failure):
            return failure.error.message
        }
    }

    var isFailure: Bool {
        if case .failure = onEnum(of: self) { return true }
        return false
    }
}

extension FieldError {
    var text: String {
        switch self {
        case .required: return "Campo obrigatório."
        case .invalidMac: return "Informe os 12 caracteres do MAC (0-9, A-F)."
        case .invalidAlarmPassword: return "A senha deve ter 4 ou 6 dígitos."
        case .duplicate: return "Já está em uso por outro dispositivo."
        }
    }
}

extension DeviceType {
    var label: String {
        switch self {
        case .alarm: return "Alarme"
        case .video: return "Câmera"
        }
    }
}

extension DeviceFilter {
    var label: String {
        switch self {
        case .all: return "Todos"
        case .favorites: return "Favoritos"
        case .cameras: return "Câmeras"
        case .alarms: return "Alarmes"
        }
    }
}
