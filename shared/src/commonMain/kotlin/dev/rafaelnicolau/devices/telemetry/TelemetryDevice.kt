package dev.rafaelnicolau.devices.telemetry

/**
 * The device the app runs on, read by each platform's `initializeApp` and sent as resource
 * attributes with every export, under the OpenTelemetry names (`device.*`, `os.type`).
 */
data class TelemetryDevice(
    /** `android` or `ios`. */
    val platform: String,
    /** iOS: `identifierForVendor`; Android: `Settings.Secure.ANDROID_ID`. Empty when unavailable. */
    val id: String = "",
    /** e.g. `iPhone15,2` or `Pixel 7`. */
    val model: String = "",
    /** e.g. `Apple` or `Google`. */
    val manufacturer: String = "",
)
