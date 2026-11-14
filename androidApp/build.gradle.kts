import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Secrets live in the git-ignored local.properties; CI can supply them as environment variables.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secret(name: String): String = localProperties.getProperty(name) ?: System.getenv(name) ?: ""

// Not a secret: falls back to the challenge API so only the token has to be configured.
val apiBaseUrl = secret("API_BASE_URL").ifEmpty { "http://squadapps.ddns-intelbras.com.br:37025" }

android {
    namespace = "dev.rafaelnicolau.devices"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.rafaelnicolau.devices"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_TOKEN", "\"${secret("API_TOKEN")}\"")
        // Telemetry: an OTLP/HTTP receiver (e.g. http://10.0.2.2:4318 for a collector on this machine).
        // Empty means nothing leaves the device. Headers as name=value,name2=value2 (e.g. api-key=…).
        buildConfigField("String", "OTLP_ENDPOINT", "\"${secret("OTLP_ENDPOINT")}\"")
        buildConfigField("String", "OTLP_HEADERS", "\"${secret("OTLP_HEADERS")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric needs the merged resources (fonts, strings) to render Compose screens on the JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
}
