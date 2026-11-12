package dev.rafaelnicolau.devices

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.SDK_INT}"
