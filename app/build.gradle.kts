import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

// =============================================================================
// :app module build script.
// -----------------------------------------------------------------------------
// Single application module that hosts the entire TickLog app. Package-level
// separation (core / data / domain / ui / di) inside this module mirrors a
// Clean Architecture layering and keeps the project easy to split into Gradle
// modules later, should the codebase grow.
// =============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// -----------------------------------------------------------------------------
// Release signing.
// -----------------------------------------------------------------------------
// Signing material is read from a git-ignored `keystore.properties` at the repo
// root (see keystore.properties.example), or from environment variables in CI.
// Nothing is hardcoded and no keystore is committed. When no signing config is
// present the release build is simply left unsigned, so the project still
// configures and builds for contributors who don't hold the keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

fun signingProperty(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val hasReleaseSigning: Boolean =
    signingProperty("storeFile", "TICKLOG_KEYSTORE_FILE") != null

android {
    namespace = "com.ticklog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ticklog"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Material 3 / Compose vector assets render correctly on API 28+.
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // Only defined when signing material is available (keystore.properties or
        // CI env vars); otherwise the release build is left unsigned.
        if (hasReleaseSigning) {
            create("release") {
                // storeFile is resolved relative to the repo root, where
                // keystore.properties lives (see keystore.properties.example).
                storeFile = rootProject.file(signingProperty("storeFile", "TICKLOG_KEYSTORE_FILE")!!)
                storePassword = signingProperty("storePassword", "TICKLOG_KEYSTORE_PASSWORD")
                keyAlias = signingProperty("keyAlias", "TICKLOG_KEY_ALIAS")
                keyPassword = signingProperty("keyPassword", "TICKLOG_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Distinct id so a debug build can sit next to a release install.
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            // Shrink, obfuscate and strip unused resources for a lean, hardened
            // store build. Keep rules live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signs the APK/AAB when a keystore is configured; null (unsigned)
            // otherwise so contributors can still build a release artifact.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged manifest/resources to host an in-memory
            // Room database in plain JVM unit tests.
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Treat lint findings strictly: any remaining warning fails the build,
        // so the codebase stays genuinely warning-free.
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = true

        // Disabled checks, each a deliberate, documented exception:
        //  - GradleDependency / AndroidGradlePluginVersion: purely-informational
        //    "a newer version is available" notices. Not correctness problems;
        //    they drift over time and upgrades here are made deliberately via the
        //    version catalog, not in response to lint.
        //  - ObsoleteSdkInt: a false-positive for our launcher icon. <adaptive-icon>
        //    is an API 26 feature that AAPT only accepts in a `-v26` folder, so the
        //    qualifier is mandatory even though minSdk is 28. lint cannot model this.
        disable += setOf(
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "ObsoleteSdkInt",
        )
    }
}

// Modern Kotlin compiler configuration (replaces the deprecated kotlinOptions).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Room annotation processor configuration: export the schema for every version
// so database migrations can be written and verified against a known history.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // --- AndroidX core -------------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    // --- Compose (versions governed by the BOM) ------------------------------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)

    // --- Navigation ----------------------------------------------------------
    implementation(libs.androidx.navigation.compose)

    // --- Splash screen -------------------------------------------------------
    implementation(libs.androidx.core.splashscreen)

    // --- Glance (home-screen widget) -----------------------------------------
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // --- Dependency Injection ------------------------------------------------
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // --- Persistence ---------------------------------------------------------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // --- Serialization (offline backup format) -------------------------------
    implementation(libs.kotlinx.serialization.json)

    // --- Unit tests ----------------------------------------------------------
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    // Room's generated DAO/database code is exercised by the repository tests.
    testImplementation(libs.androidx.room.runtime)

    // --- Instrumented tests --------------------------------------------------
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // --- Debug-only tooling --------------------------------------------------
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
