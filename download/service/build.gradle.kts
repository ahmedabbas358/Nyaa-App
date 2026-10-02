plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.aniflow.download.service"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(project(":download:core"))
    implementation(project(":platform:notifications"))
    implementation(project(":platform:network"))
    implementation(project(":platform:storage"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
