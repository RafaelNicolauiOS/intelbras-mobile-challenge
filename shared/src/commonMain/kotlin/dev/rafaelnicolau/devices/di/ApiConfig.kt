package dev.rafaelnicolau.devices.di

/**
 * Where the API lives and how to authenticate. Each app reads these from its own
 * untracked build configuration and hands them over at startup.
 */
data class ApiConfig(
    val baseUrl: String,
    val token: String,
    /**
     * Prints every request and response (method, URL, headers and JSON body) to the
     * console: Xcode's debug area on iOS, Logcat on Android. Meant for debug builds only,
     * since bodies include device passwords. The API token is never printed.
     */
    val logRequests: Boolean = false,
)
