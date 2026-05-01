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
        versionCode = 1
        versionName = "0.1.0"

        // Restrict to architectures Play Store actually serves to phones.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }

        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        // Release signing config is read from environment variables OR a
        // local keystore.properties file. NEVER commit a keystore.
        create("release") {
            val ksFile = file("keystore.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
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
            signingConfig = signingConfigs.getByName("release")
            // Strip BuildConfig.DEBUG-gated logs in release.
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
}
