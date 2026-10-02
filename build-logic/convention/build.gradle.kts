plugins {
    `kotlin-dsl`
}

group = "com.aniflow.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "aniflow.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "aniflow.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "aniflow.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "aniflow.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("testingConvention") {
            id = "aniflow.testing"
            implementationClass = "TestingConventionPlugin"
        }
    }
}
