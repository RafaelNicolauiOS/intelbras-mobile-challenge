import Shared
import SwiftUI

/// Full-screen message for empty and error states, with an optional retry action.
struct StatusMessage: View {
    let message: String
    var onRetry: (() -> Void)?

    var body: some View {
        VStack(spacing: 16) {
            Text(message)
                .multilineTextAlignment(.center)
            if let onRetry {
                Button("Tentar novamente", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(32)
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
