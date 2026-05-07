import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.transferrate.app"
    compileSdk = 35   // Android 15 SDK to compile against

    defaultConfig {
        applicationId = "com.transferrate.app"
        // ANDROID 14+ ONLY — per project requirement. Reduces install base
        // significantly but gives us the strict modern security posture.
        minSdk = 34
        targetSdk = 34
        // versionCode bumped on every release. App stores use it as the
        // canonical "is this newer?" comparison.
        versionCode = 33
        versionName = "0.21.0"

        resourceConfigurations += listOf("en")
    }

    // Per-architecture APK splits. Each user device only downloads the
    // APK for its CPU; significantly smaller install. Universal APK
    // (the one we currently produce) is also kept for sideloading.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        // Release signing reads from one of two sources, in priority order:
        //   1. keystore.properties file at the project root (gitignored)
        //   2. environment variables (KEYSTORE_*, KEY_*)  — for CI use
        // If neither is configured, release builds fall back to debug
        // signing (won't be Play-acceptable but allows local builds).
        create("release") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties()
                propsFile.inputStream().use { props.load(it) }
                storeFile = file(props.getProperty("storeFile") ?: "keystore.jks")
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                val envStore = System.getenv("KEYSTORE_FILE")
                if (envStore != null) {
                    storeFile = file(envStore)
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use the release signing config IF it has a storeFile;
            // otherwise fall back to debug so the build doesn't fail
            // when no keystore is configured (useful for first-time
            // local builds + reproducible-builds verification).
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
            isDebuggable = false
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Treat all warnings as errors for the production sourceset.
        // Keeps suspicious code from sneaking in.
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // Disable features we don't use — smaller attack surface.
        aidl = false
        renderScript = false
        shaders = false
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)

    // MapLibre Native - vector-capable map renderer for the mosque finder.
    // We render OSM raster tiles via inline style JSON (no API key, no
    // paid tier, no Google Play Services dep).  Annotation plugin
    // provides SymbolManager for adding/removing markers cleanly.
    implementation(libs.maplibre.android.sdk)
    implementation(libs.maplibre.android.annotation)
}
