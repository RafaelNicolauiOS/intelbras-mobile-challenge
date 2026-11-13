@testable import iosApp
import Shared
import SwiftUI
import XCTest

/// SwiftUI has no public API to inspect a view tree, so these tests render each view
/// in a hosting controller and check it lays out with a real size in every state.
/// Behavior behind the views is covered by the observable and shared tests.
@MainActor
final class ViewRenderingTests: XCTestCase {
    private func assertRenders<V: View>(_ view: V, file: StaticString = #filePath, line: UInt = #line) {
        let host = UIHostingController(rootView: view)
        let size = host.sizeThatFits(in: CGSize(width: 390, height: 844))
        host.view.frame = CGRect(origin: .zero, size: CGSize(width: 390, height: max(size.height, 1)))
        host.view.layoutIfNeeded()
        XCTAssertGreaterThan(size.width, 0, file: file, line: line)
        XCTAssertGreaterThan(size.height, 0, file: file, line: line)
    }

    func testComponentsRender() {
        assertRenders(StatusMessage(message: "Nenhum dispositivo cadastrado."))
        assertRenders(StatusMessage(message: "Erro", onRetry: {}))
        assertRenders(ToastBanner(text: "Copiado"))
        assertRenders(SearchField(onQueryChange: { _ in }))
        assertRenders(FilterChipsRow(selected: .favorites, onSelected: { _ in }))
        assertRenders(DeviceTypeIcon(type: .alarm))
        assertRenders(AppCard { Text("dentro") })
        assertRenders(DeviceListCard(devices: [Fixtures.alarm, Fixtures.camera], onOpenDevice: { _ in }, onMore: { _ in }))
        assertRenders(DeviceActionsSheet(device: Fixtures.camera, onEdit: {}, onInfo: {}, onToggleFavorite: {}, onDelete: {}))
    }

    func testOutlinedFieldRendersInEveryVariant() {
        assertRenders(OutlinedField(label: "Nome", text: "", errorText: nil, onChange: { _ in }))
        assertRenders(OutlinedField(label: "Nome", text: "Casa", errorText: "Campo obrigatório.", onChange: { _ in }))
        assertRenders(OutlinedField(label: "Senha", text: "1234", errorText: nil, isSecure: true, onChange: { _ in }))
    }

    func testOutlinedFieldErrorMakesItTaller() {
        let plain = UIHostingController(rootView: OutlinedField(label: "Nome", text: "", errorText: nil, onChange: { _ in }))
        let withError = UIHostingController(
            rootView: OutlinedField(label: "Nome", text: "", errorText: "Campo obrigatório.", onChange: { _ in })
        )
        let bounds = CGSize(width: 390, height: 844)

        XCTAssertGreaterThan(withError.sizeThatFits(in: bounds).height, plain.sizeThatFits(in: bounds).height)
    }

    func testScreensRender() async {
        let dashboard = DashboardObservable(
            viewModel: DashboardViewModel(repository: Fixtures.repository(), messages: MessageCenter())
        )
        await waitUntil { !dashboard.state.isLoading }
        assertRenders(DashboardView(observable: dashboard, onAddDevice: {}, onOpenDevice: { _ in }, onEditDevice: { _ in }))

        let details = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "a1", repository: Fixtures.repository(), messages: MessageCenter())
        )
        await waitUntil { details.device != nil }
        assertRenders(DeviceDetailsView(observable: details))

        let form = DeviceFormObservable(
            viewModel: DeviceFormViewModel(deviceId: nil, repository: Fixtures.repository([]), messages: MessageCenter()),
            onSaved: {}
        )
        assertRenders(DeviceFormView(observable: form))
    }

    func testEmptyAndMissingStatesRender() async {
        let empty = DashboardObservable(
            viewModel: DashboardViewModel(repository: Fixtures.repository([]), messages: MessageCenter())
        )
        await waitUntil { !empty.state.isLoading }
        assertRenders(DashboardView(observable: empty, onAddDevice: {}, onOpenDevice: { _ in }, onEditDevice: { _ in }))

        let missing = DeviceDetailsObservable(
            viewModel: DeviceDetailsViewModel(deviceId: "x", repository: Fixtures.repository([]), messages: MessageCenter())
        )
        assertRenders(DeviceDetailsView(observable: missing))
    }
}
