plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aniflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aniflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 103
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (!keystoreFile.isNullOrEmpty() && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // Fallback to debug keystore for local non-CI release builds without credentials
                val debugKeystore = signingConfigs.getByName("debug").storeFile
                if (debugKeystore != null && debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(project(":core:logging"))

    implementation(project(":domain"))
    implementation(project(":domain:release-intelligence"))
    implementation(project(":domain:selection"))
    implementation(project(":data"))
    implementation(libs.okhttp)

    implementation(project(":provider:core"))
    implementation(project(":provider:nyaa"))

    implementation(project(":download:core"))
    implementation(project(":download:http"))
    implementation(project(":download:torrent"))
    implementation(project(":download:service"))

    implementation(project(":platform:notifications"))
    implementation(project(":platform:share"))
    implementation(project(":platform:network"))
    implementation(project(":platform:storage"))

    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:release"))
    implementation(project(":feature:collections"))
    implementation(project(":feature:downloads"))
    implementation(project(":feature:library"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:automation"))
    implementation(project(":feature:watchlist"))
    implementation(project(":feature:saved-search"))

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // AndroidX & Hilt
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(project(":testing"))
}
