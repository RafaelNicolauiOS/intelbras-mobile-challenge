import Shared
import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        AppContainer.shared.initialize(config: Self.apiConfig())
        return true
    }

    /// Values come from Config.xcconfig / the git-ignored Secrets.xcconfig, through Info.plist.
    private static func apiConfig() -> ApiConfig {
        let info = Bundle.main.infoDictionary ?? [:]
        return ApiConfig(
            baseUrl: info["API_BASE_URL"] as? String ?? "",
            token: info["API_TOKEN"] as? String ?? ""
        )
    }
}
