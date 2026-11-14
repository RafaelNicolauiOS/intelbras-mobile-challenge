@preconcurrency import Shared
import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        InitializeApp_iosKt.initializeApp(config: Self.apiConfig(), telemetry: Self.telemetryConfig())
        return true
    }

    /// OTLP endpoint and headers (`name=value,…`) come from the xcconfig files, like the API token.
    /// An empty endpoint sends nothing; debug builds also print to the console ("[TELEMETRY]").
    private static func telemetryConfig() -> TelemetryConfig {
        let info = Bundle.main.infoDictionary ?? [:]
        return TelemetryConfig(
            serviceName: "devices-app",
            serviceVersion: info["CFBundleShortVersionString"] as? String ?? "",
            environment: logsRequests ? "debug" : "release",
            otlpEndpoint: info["OTLP_ENDPOINT"] as? String ?? "",
            otlpHeaders: TelemetryConfigKt.parseOtlpHeaders(text: info["OTLP_HEADERS"] as? String ?? ""),
            console: logsRequests
        )
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
