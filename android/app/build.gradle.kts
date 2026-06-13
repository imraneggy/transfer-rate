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
        versionCode = 70
        versionName = "0.33.0"


        // Locales whose resource folders are bundled into the APK.  Any
        // values-XX/ that isn't listed here is stripped at build time —
        // ship-list semantics, not a hint.  v0.30 added Tamil; v0.30.3
        // completes the UAE-India language trio with Hindi + Malayalam.
        // All three first-cuts pending native-speaker review.
        resourceConfigurations += listOf("en", "ta", "hi", "ml")

        // Cloudflare Worker that proxies a workflow_dispatch to the
        // scrape workflow.  The Worker holds the GitHub PAT in its
        // encrypted env; the app only knows the Worker URL and a
        // shared bearer secret.
        //
        // Both values are baked into the APK at build time and exposed
        // via BuildConfig.  The shared secret is extractable with
        // apktool — that's an accepted abuse-prevention layer, NOT a
        // strong security boundary.  The strong boundary is the PAT
        // staying inside Cloudflare.  If the secret leaks we rotate
        // it in the Worker's env + ship a new APK.
        //
        // v0.28.1: the secret is no longer a string literal in this
        // file (a public repo, indexed by github.com/search the moment
        // it was committed).  Resolution order:
        //
        //   1. Environment variable REFRESH_TRIGGER_SECRET — used by
        //      CI; the value lives in GitHub Actions repo secrets and
        //      is injected into the build by the workflow.
        //   2. secrets.properties at the android/ root (gitignored) —
        //      used by maintainer for local debug builds.
        //   3. Empty string — F-Droid reproducible builds, contributors
        //      who don't need the refresh-trigger feature.  The empty
        //      string is gracefully handled by the existing guard in
        //      RatesRepository (`takeIf { it.isNotBlank() }`), which
        //      disables the refresh-button-to-Worker path entirely.
        val refreshSecret: String =
            System.getenv("REFRESH_TRIGGER_SECRET")
                ?: run {
                    val secretsFile = rootProject.file("secrets.properties")
                    if (secretsFile.exists()) {
                        Properties().apply {
                            secretsFile.inputStream().use { load(it) }
                        }.getProperty("REFRESH_TRIGGER_SECRET", "")
                    } else ""
                }

        buildConfigField(
            "String",
            "REFRESH_TRIGGER_URL",
            "\"https://transfer-rate-refresh.imranbatchait.workers.dev\"",
        )
        buildConfigField(
            "String",
            "REFRESH_TRIGGER_SECRET",
            "\"$refreshSecret\"",
        )
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
            // Use the release signing config IF it has a storeFile.  In
            // CI we MUST refuse to fall back to debug signing — a debug
            // keystore is shared across all SDK installations, so a
            // release APK signed with it could be over-installed by
            // anyone.  Locally (no CI) we still permit the fallback so
            // first-time contributors and reproducible-build verifiers
            // can run `assembleRelease` without a keystore at hand.
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile?.exists() == true) {
                releaseConfig
            } else {
                val isCi = System.getenv("CI") == "true" ||
                    System.getenv("GITHUB_ACTIONS") == "true"
                val requestedReleaseBuild = gradle.startParameter.taskNames.any { taskName ->
                    taskName.contains("Release", ignoreCase = true) ||
                        taskName.contains("Bundle", ignoreCase = true)
                }
                if (isCi && requestedReleaseBuild) {
                    error(
                        "CI release build attempted with no signing keystore configured. " +
                        "Provide KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD " +
                        "in the workflow env, or a keystore.properties file at the project root.",
                    )
                }
                logger.warn(
                    "WARNING: release build is falling back to the debug-signing " +
                    "keystore — local-only.  Do not distribute this APK.",
                )
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
}
