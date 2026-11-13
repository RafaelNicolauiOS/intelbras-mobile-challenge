@preconcurrency import Shared

extension UserMessage {
    var text: String {
        switch onEnum(of: self) {
        case .success(let success):
            switch success.kind {
            case .deviceCreated: return "Dispositivo adicionado"
            case .deviceUpdated: return "Dispositivo atualizado"
            case .deviceDeleted: return "Dispositivo removido"
            case .copied: return "Copiado"
            }
        case .failure(let failure):
            return failure.error.message
        }
    }
}

extension DeviceError {
    var message: String {
        switch self {
        case .network: return "Sem conexão. Verifique sua internet."
        case .timeout: return "O servidor demorou para responder."
        case .unauthorized: return "Token de acesso inválido ou expirado."
        case .notFound: return "Dispositivo não encontrado no servidor."
        case .conflict: return "Já existe um dispositivo com esses dados."
        case .unknown: return "Algo deu errado. Tente novamente."
        }
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
    /// How the type is named in the form selector and in the information screen.
    var label: String {
        switch self {
        case .alarm: return "Alarme"
        case .video: return "Vídeo"
        }
    }

    /// How the type is named as the title of the actions sheet.
    var sheetTitle: String {
        switch self {
        case .alarm: return "Alarme"
        case .video: return "Câmera"
        }
    }

    var iconName: String {
        switch self {
        case .alarm: return "light.beacon.max"
        case .video: return "video"
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
