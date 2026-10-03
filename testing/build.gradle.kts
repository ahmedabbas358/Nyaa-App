plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(project(":domain"))
    testImplementation(project(":core:common"))
    testImplementation(project(":core:logging"))
    testImplementation(project(":core:network"))
    testImplementation(project(":provider:core"))
    testImplementation(project(":download:core"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> {
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
