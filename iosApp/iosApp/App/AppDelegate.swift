import Shared
import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        InitializeApp_iosKt.initializeApp(config: Self.apiConfig())
        return true
    }

    /// Values come from Config.xcconfig / the git-ignored Secrets.xcconfig, through Info.plist.
    private static func apiConfig() -> ApiConfig {
        let info = Bundle.main.infoDictionary ?? [:]
        return ApiConfig(
            baseUrl: info["API_BASE_URL"] as? String ?? "",
            token: info["API_TOKEN"] as? String ?? "",
            logRequests: logsRequests
        )
    }

    /// Requests and responses are printed to the Xcode console (filter by "[HTTP]") in
    /// debug builds only. The API token is never printed.
    private static var logsRequests: Bool {
        #if DEBUG
        return true
        #else
        return false
        #endif
    }
}
