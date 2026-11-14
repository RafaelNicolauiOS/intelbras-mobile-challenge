@preconcurrency import Shared
import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private var appCoordinator: AppCoordinator?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let window = UIWindow(windowScene: windowScene)
        let coordinator = AppCoordinator(window: window)
        coordinator.start()

        self.window = window
        appCoordinator = coordinator
    }

    /// The app may be suspended or killed in the background: send what telemetry has buffered now.
    func sceneDidEnterBackground(_ scene: UIScene) {
        AppContainer.shared.telemetry.flush()
    }
}
