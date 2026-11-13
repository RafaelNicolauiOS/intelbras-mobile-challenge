@testable import iosApp
import Shared
import XCTest

/// The Portuguese copy shown to users.
final class TextsTests: XCTestCase {
    func testSuccessMessages() {
        XCTAssertEqual(UserMessageSuccess(kind: .deviceCreated).text, "Dispositivo adicionado")
        XCTAssertEqual(UserMessageSuccess(kind: .deviceUpdated).text, "Dispositivo atualizado")
        XCTAssertEqual(UserMessageSuccess(kind: .deviceDeleted).text, "Dispositivo removido")
        XCTAssertEqual(UserMessageSuccess(kind: .copied).text, "Copiado")
    }

    func testFailureMessageIsTheErrorMessage() {
        for error in [DeviceError.network, .timeout, .unauthorized, .notFound, .conflict, .unknown] {
            XCTAssertEqual(UserMessageFailure(error: error).text, error.message)
        }
    }

    func testErrorMessages() {
        XCTAssertEqual(DeviceError.network.message, "Sem conexão. Verifique sua internet.")
        XCTAssertEqual(DeviceError.timeout.message, "O servidor demorou para responder.")
        XCTAssertEqual(DeviceError.unauthorized.message, "Token de acesso inválido ou expirado.")
        XCTAssertEqual(DeviceError.notFound.message, "Dispositivo não encontrado no servidor.")
        XCTAssertEqual(DeviceError.conflict.message, "Já existe um dispositivo com esses dados.")
        XCTAssertEqual(DeviceError.unknown.message, "Algo deu errado. Tente novamente.")
    }

    func testFieldErrors() {
        XCTAssertEqual(FieldError.required.text, "Campo obrigatório.")
        XCTAssertEqual(FieldError.invalidMac.text, "Informe os 12 caracteres do MAC (0-9, A-F).")
        XCTAssertEqual(FieldError.invalidAlarmPassword.text, "A senha deve ter 4 ou 6 dígitos.")
        XCTAssertEqual(FieldError.duplicate.text, "Já está em uso por outro dispositivo.")
    }

    func testTypeNamesAndIcons() {
        XCTAssertEqual(DeviceType.alarm.label, "Alarme")
        XCTAssertEqual(DeviceType.video.label, "Vídeo")
        XCTAssertEqual(DeviceType.alarm.sheetTitle, "Alarme")
        XCTAssertEqual(DeviceType.video.sheetTitle, "Câmera")
        XCTAssertNotNil(UIImage(systemName: DeviceType.alarm.iconName), "SF Symbol must exist")
        XCTAssertNotNil(UIImage(systemName: DeviceType.video.iconName), "SF Symbol must exist")
    }

    func testFilterLabels() {
        XCTAssertEqual(
            [DeviceFilter.all, .favorites, .cameras, .alarms].map(\.label),
            ["Todos", "Favoritos", "Câmeras", "Alarmes"]
        )
    }

    /// Android and iOS must show the same text; this mirrors the Android TextsTest.
    func testDeviceIdentifiersComeFromTheSharedModel() {
        XCTAssertEqual(Fixtures.alarm.identifier, "1A:2B:3C:4D:5E:6F")
        XCTAssertEqual(Fixtures.camera.identifier, "SN-001")
    }
}
