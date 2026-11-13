import UIKit

/// Owns one navigation flow: it builds the screens and decides what comes next.
/// Views and view controllers never push or present anything themselves.
@MainActor
protocol Coordinator: AnyObject {
    var navigationController: UINavigationController { get }
    func start()
}

/// A screen whose Observable keeps work running (flow observation, a ViewModel scope).
/// Its coordinator calls `stopObserving()` as soon as the screen leaves the navigation stack,
/// on the main actor, instead of relying on when `deinit` happens to run.
@MainActor
protocol ObservingScreen: AnyObject {
    func stopObserving()
}
