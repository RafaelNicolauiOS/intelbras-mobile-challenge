package dev.rafaelnicolau.devices.di

/**
 * Where the API lives and how to authenticate. Each app reads these from its own
 * untracked build configuration and hands them over at startup.
 */
data class ApiConfig(
    val baseUrl: String,
    val token: String,
)
