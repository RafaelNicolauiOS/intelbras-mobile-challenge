@testable import iosApp
import Shared
import XCTest

@MainActor
final class ViewControllerTests: XCTestCase {
    private func formObservable() -> DeviceFormObservable {
        DeviceFormObservable(
            viewModel: DeviceFormViewModel(deviceId: nil, repository: Fixtures.repository([]), messages: MessageCenter()),
            onSaved: {}
        )
    }

    private func tap(_ item: UIBarButtonItem?) {
        guard let item, let action = item.action else { return XCTFail("missing bar button") }
        UIApplication.shared.sendAction(action, to: item.target, from: item, for: nil)
    }

    func testFormTitleDependsOnTheMode() {
        let add = DeviceFormViewController(observable: formObservable(), isEditing: false, onClose: {})
        let edit = DeviceFormViewController(observable: formObservable(), isEditing: true, onClose: {})

        XCTAssertEqual(add.title, "Novo dispositivo")
        XCTAssertEqual(edit.title, "Editar dispositivo")
    }

    func testFormHasCloseAndSaveButtons() {
        let controller = DeviceFormViewController(observable: formObservable(), isEditing: false, onClose: {})

        XCTAssertEqual(controller.navigationItem.leftBarButtonItem?.accessibilityLabel, "Fechar")
        XCTAssertEqual(controller.navigationItem.rightBarButtonItem?.title, "Salvar")
    }

    func testCloseCallsOnClose() {
        var closed = 0
        let controller = DeviceFormViewController(observable: formObservable(), isEditing: false, onClose: { closed += 1 })

        tap(controller.navigationItem.leftBarButtonItem)

        XCTAssertEqual(closed, 1)
    }

    func testSaveSubmitsTheForm() {
        let observable = formObservable()
        let controller = DeviceFormViewController(observable: observable, isEditing: false, onClose: {})
        controller.loadViewIfNeeded()

        tap(controller.navigationItem.rightBarButtonItem)

        XCTAssertEqual(observable.state.errors.name, .required, "an empty form was validated")
    }

    func testDetailsTitle() {
        let observable = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "v1", repository: Fixtures.repository(), messages: MessageCenter())
        )

        XCTAssertEqual(DeviceDetailsViewController(observable: observable).title, "Informações")
    }

    func testDashboardHidesTheNavigationBarOnlyWhileVisible() {
        let observable = DashboardObservable(
            viewModel: DashboardViewModel(repository: Fixtures.repository(), messages: MessageCenter())
        )
        let dashboard = DashboardViewController(observable: observable, onAddDevice: {}, onOpenDevice: { _ in }, onEditDevice: { _ in })
        let navigation = UINavigationController(rootViewController: dashboard)

        dashboard.viewWillAppear(false)
        XCTAssertTrue(navigation.isNavigationBarHidden)

        dashboard.viewWillDisappear(false)
        XCTAssertFalse(navigation.isNavigationBarHidden)
        XCTAssertEqual(dashboard.navigationItem.backButtonDisplayMode, .minimal)
    }
}
