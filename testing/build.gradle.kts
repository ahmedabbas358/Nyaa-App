plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    test {
        java.srcDirs("src/main/java")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":core:network"))
    implementation(project(":provider:core"))
    implementation(project(":download:core"))

    implementation(libs.junit)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)
}

tasks.withType<Test> {
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

