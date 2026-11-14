@preconcurrency import Shared
import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private var appCoordinator: AppCoordinator?
    private let backgroundTasks = BackgroundTaskRunner()

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

    /// The app may be suspended or killed in the background: send what telemetry has buffered now,
    /// asking iOS for the time to finish. Without it the app is suspended mid-request and, if it
    /// is then closed, the buffered data is lost.
    func sceneDidEnterBackground(_ scene: UIScene) {
        backgroundTasks.run("telemetry.flush") {
            try? await AppContainer.shared.telemetry.flushAndWait()
        }
    }
}

/// Keeps the app running in the background until some work finishes or iOS's time runs out,
/// whichever comes first, and ends the background task exactly once.
@MainActor
final class BackgroundTaskRunner {
    typealias Begin = @MainActor (_ name: String, _ expiration: @escaping @MainActor @Sendable () -> Void)
        -> UIBackgroundTaskIdentifier
    typealias End = @MainActor (UIBackgroundTaskIdentifier) -> Void

    private let begin: Begin
    private let end: End

    init(
        begin: @escaping Begin = { name, expiration in
            UIApplication.shared.beginBackgroundTask(withName: name, expirationHandler: expiration)
        },
        end: @escaping End = { UIApplication.shared.endBackgroundTask($0) }
    ) {
        self.begin = begin
        self.end = end
    }

    @discardableResult
    func run(_ name: String, _ work: @escaping @MainActor () async -> Void) -> Task<Void, Never> {
        let task = RunningTask(end: end)
        task.id = begin(name) { task.finish() }
        return Task { @MainActor in
            await work()
            task.finish()
        }
    }
}

@MainActor
private final class RunningTask {
    var id = UIBackgroundTaskIdentifier.invalid
    private let end: BackgroundTaskRunner.End

    init(end: @escaping BackgroundTaskRunner.End) {
        self.end = end
    }

    /// Called when the work finishes and when iOS's time runs out; only the first call ends the task.
    func finish() {
        guard id != .invalid else { return }
        end(id)
        id = .invalid
    }
}
