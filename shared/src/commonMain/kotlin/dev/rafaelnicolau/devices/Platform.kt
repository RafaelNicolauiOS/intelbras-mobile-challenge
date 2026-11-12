package dev.rafaelnicolau.devices

/** Temporary wiring check for the shared module; replaced by real code in the next phases. */
expect fun platformName(): String

fun greeting(): String = "Shared module running on ${platformName()}"
