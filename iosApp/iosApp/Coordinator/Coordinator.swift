import UIKit

/// Owns one navigation flow: it builds the screens and decides what comes next.
/// Views and view controllers never push or present anything themselves.
@MainActor
protocol Coordinator: AnyObject {
    var navigationController: UINavigationController { get }
    func start()
}
