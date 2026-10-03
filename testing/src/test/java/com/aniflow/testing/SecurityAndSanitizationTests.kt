package com.aniflow.testing

import com.aniflow.core.common.sanitization.PathSanitizer
import com.aniflow.core.logging.LogRedactor
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.GroupConditionNode
import com.aniflow.domain.controlplane.models.LogicalOperator
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.controlplane.service.ConfigurationSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Enforces STEP 13:
 * - Section 26 (Filename Safety)
 * - Section 27 (Path Traversal Protection)
 * - Section 48 (Untrusted Provider Data)
 * - Section 49 (Logging Security)
 * - Section 50 (Export Security)
 * - Section 51 (Import Security)
 * - Section 52 (File Security)
 */
class SecurityAndSanitizationTests {

    @Test
    fun testPathTraversalRejection() {
        val rootDir = File(System.getProperty("java.io.tmpdir"), "aniflow_safe_storage").apply { mkdirs() }

        // Attack 1: Classic traversal
        try {
            PathSanitizer.ensureInsideStorageRoot(rootDir, "../../etc/passwd")
            fail("Traversal attack 1 must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal"))
        }

        // Attack 2: Windows backslash traversal
        try {
            PathSanitizer.ensureInsideStorageRoot(rootDir, "..\\..\\Windows\\System32")
            fail("Traversal attack 2 must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal"))
        }

        // Attack 3: Subdirectory with embedded escape
        try {
            PathSanitizer.ensureInsideStorageRoot(rootDir, "Anime/Season 1/../../../../escape.mkv")
            fail("Traversal attack 3 must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal"))
        }

        rootDir.deleteRecursively()
    }

    @Test
    fun testFilenameSanitization_ForbiddenCharsAndReservedNames() {
        // Forbidden chars: / \ : * ? " < > |
        val dirtyTitle = "One / Piece : Season * 1 ? <Best> | [1080p].mkv"
        val cleanTitle = PathSanitizer.sanitizeFilename(dirtyTitle)
        assertFalse("Clean title must not contain colons", cleanTitle.contains(":"))
        assertFalse("Clean title must not contain slashes", cleanTitle.contains("/"))
        assertFalse("Clean title must not contain asterisks", cleanTitle.contains("*"))
        assertFalse("Clean title must not contain angle brackets", cleanTitle.contains("<"))

        // Windows reserved device names
        val conFile = PathSanitizer.sanitizeFilename("CON.mkv")
        assertEquals("_CON.mkv", conFile)

        val prnFile = PathSanitizer.sanitizeFilename("prn.mp4")
        assertEquals("_prn.mp4", prnFile)

        val auxFile = PathSanitizer.sanitizeFilename("AUX")
        assertEquals("_AUX", auxFile)
    }

    @Test
    fun testLogRedactorRemovesSensitiveCredentials() {
        val messageWithAuth = "Requesting provider using Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val redactedAuth = LogRedactor.redact(messageWithAuth)
        assertFalse(redactedAuth.contains("eyJhbGciOi"))
        assertTrue(redactedAuth.contains("[REDACTED]"))

        val messageWithPasskey = "Announcing torrent to https://tracker.nyaa.si:443/announce?passkey=abcdef1234567890"
        val redactedPasskey = LogRedactor.redact(messageWithPasskey)
        assertFalse(redactedPasskey.contains("abcdef1234567890"))
        assertTrue(redactedPasskey.contains("[REDACTED]"))

        val messageWithPassword = "Login attempt with password = \"super_secret_p@ss!\""
        val redactedPassword = LogRedactor.redact(messageWithPassword)
        assertFalse(redactedPassword.contains("super_secret_p@ss!"))
        assertTrue(redactedPassword.contains("[REDACTED]"))
    }

    @Test
    fun testImportValidationSizeLimits() {
        val emptyBytes = ByteArray(0)
        val emptyResult = ConfigurationSecurity.validateRawImport(emptyBytes)
        assertTrue(emptyResult is ConfigurationSecurity.ValidationResult.Invalid)

        val oversizedBytes = ByteArray(6 * 1024 * 1024) // 6 MB exceeds 5 MB
        val oversizedResult = ConfigurationSecurity.validateRawImport(oversizedBytes)
        assertTrue(oversizedResult is ConfigurationSecurity.ValidationResult.Invalid)

        val validBytes = ByteArray(1024) // 1 KB
        val validResult = ConfigurationSecurity.validateRawImport(validBytes)
        assertTrue(validResult is ConfigurationSecurity.ValidationResult.Valid)
    }

    @Test
    fun testRuleAstDepthLimiter() {
        val leaf = ComparisonExpression(SearchField.Resolution, ComparisonOperator.Equals, "1080p")

        // Build a malicious 15-level deeply nested rule tree
        var currentTree: com.aniflow.domain.controlplane.models.ConditionNode = leaf
        for (i in 1..14) {
            currentTree = GroupConditionNode(LogicalOperator.And, listOf(currentTree))
        }

        val validation = ConfigurationSecurity.validateRuleDepth(currentTree)
        assertTrue("Nesting depth > 10 must be rejected to prevent StackOverflowError", validation is ConfigurationSecurity.ValidationResult.Invalid)
    }
}
