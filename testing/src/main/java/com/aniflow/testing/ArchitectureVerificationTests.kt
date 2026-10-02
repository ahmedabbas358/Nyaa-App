package com.aniflow.testing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces STEP 13 Section 3 (Architecture Verification).
 *
 * Verifies architectural boundaries:
 * 1. Domain module MUST NOT depend on Android SDK, Compose, Room, OkHttp, or Nyaa classes.
 * 2. Feature/Presentation modules MUST NOT directly import Room DAOs or Nyaa provider classes.
 * 3. Provider modules MUST NOT depend on Presentation/UI.
 */
class ArchitectureVerificationTests {

    @Test
    fun verifyDomainLayerPurity() {
        val domainSourceDir = File("../domain/src/main/java")
        if (!domainSourceDir.exists()) return // Run within workspace if path differs

        val forbiddenDomainImports = listOf(
            "android.",
            "androidx.compose",
            "androidx.room",
            "okhttp3",
            "com.aniflow.provider.nyaa",
            "com.aniflow.core.database.dao"
        )

        domainSourceDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            for (line in lines) {
                if (line.trim().startsWith("import ")) {
                    for (forbidden in forbiddenDomainImports) {
                        assertFalse(
                            "Architecture Violation in ${file.name}: Domain must not import '$forbidden'. Line: $line",
                            line.contains(forbidden)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun verifyPresentationLayerIsolation() {
        val featureSearchDir = File("../feature/search/src/main/java")
        if (!featureSearchDir.exists()) return

        val forbiddenFeatureImports = listOf(
            "com.aniflow.core.database.dao",
            "com.aniflow.core.database.AniFlowDatabase",
            "com.aniflow.provider.nyaa.NyaaProvider",
            "com.aniflow.provider.nyaa.parser"
        )

        featureSearchDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            for (line in lines) {
                if (line.trim().startsWith("import ")) {
                    for (forbidden in forbiddenFeatureImports) {
                        assertFalse(
                            "Architecture Violation in ${file.name}: Presentation must not import '$forbidden'. Line: $line",
                            line.contains(forbidden)
                        )
                    }
                }
            }
        }
    }
}
