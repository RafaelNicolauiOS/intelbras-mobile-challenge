@preconcurrency import Shared
import SwiftUI
import UIKit

/// Bridges the shared Kotlin `DeviceDetailsViewModel` to SwiftUI, on the main actor.
@MainActor
final class DeviceDetailsObservable: ObservableObject {
    @Published private(set) var device: Device?

    private let viewModel: DeviceDetailsViewModel
    private nonisolated let bag: ObservationBag

    init(viewModel: DeviceDetailsViewModel) {
        self.viewModel = viewModel
        device = viewModel.device.value
        bag = ObservationBag(release: viewModel.clear)
        bag.add(Task { @MainActor [weak self] in
            for await device in viewModel.device {
                guard let self, !Self.isSame(device, self.device) else { continue }
                self.device = device
                Signposts.published("Device details state")
            }
        })
    }

    /// Stops observing and clears the ViewModel. Called by the coordinator when the screen leaves.
    func stop() {
        bag.stop()
    }

    /// True once the observation was stopped (screen left, or `stop()` called).
    var isStopped: Bool { bag.isStopped }

    deinit {
        bag.stop()
    }

    /// `Device` is a Kotlin interface, so it is compared through `isEqual` (the data class `equals`).
    private static func isSame(_ lhs: Device?, _ rhs: Device?) -> Bool {
        switch (lhs, rhs) {
        case (nil, nil): return true
        case let (lhs?, rhs?): return (lhs as AnyObject).isEqual(rhs)
        default: return false
        }
    }

    func copy(_ value: String) {
        UIPasteboard.general.string = value
        viewModel.onCopied()
    }
}
