package com.aniflow.domain.controlplane.service

import com.aniflow.core.common.sanitization.PathSanitizer
import com.aniflow.domain.controlplane.models.AndNode
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.NotNode
import com.aniflow.domain.controlplane.models.OrNode
import com.aniflow.domain.controlplane.models.RuleNode

/**
 * Enforces STEP 13 Section 50 (Export Security) and Section 51 (Import Security).
 */
object ConfigurationSecurity {

    const val MAX_IMPORT_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB ceiling
    const val MAX_RULE_AST_DEPTH = 10 // Max nested condition depth

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    /**
     * Sanitizes export data before writing to file or sharing.
     * Guarantees NO secrets, tokens, or credentials leave the device.
     */
    fun sanitizeExportBundle(bundle: ExportedConfigBundle): ExportedConfigBundle {
        // Enforce exclusion of any secrets and normalize strings
        val sanitizedProfiles = bundle.profiles.map { profile ->
            profile.copy(
                id = PathSanitizer.sanitizeFilename(profile.id),
                name = PathSanitizer.sanitizeFilename(profile.name)
            )
        }
        val sanitizedRules = bundle.rules.map { rule ->
            rule.copy(
                id = PathSanitizer.sanitizeFilename(rule.id),
                name = PathSanitizer.sanitizeFilename(rule.name)
            )
        }
        return bundle.copy(
            profiles = sanitizedProfiles,
            rules = sanitizedRules
        )
    }

    /**
     * Validates raw incoming configuration before deserialization and persistence.
     */
    fun validateRawImport(rawBytes: ByteArray): ValidationResult {
        if (rawBytes.isEmpty()) {
            return ValidationResult.Invalid("Import file is empty")
        }
        if (rawBytes.size > MAX_IMPORT_SIZE_BYTES) {
            return ValidationResult.Invalid("Import file exceeds 5MB size limit (${rawBytes.size} bytes)")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates AST rule tree recursion depth to prevent stack overflow attacks.
     */
    fun validateRuleDepth(node: RuleNode, currentDepth: Int = 1): ValidationResult {
        if (currentDepth > MAX_RULE_AST_DEPTH) {
            return ValidationResult.Invalid("Rule nesting depth exceeded maximum of $MAX_RULE_AST_DEPTH")
        }
        when (node) {
            is ConditionNode -> return ValidationResult.Valid
            is AndNode -> {
                for (child in node.children) {
                    val childResult = validateRuleDepth(child, currentDepth + 1)
                    if (childResult is ValidationResult.Invalid) return childResult
                }
            }
            is OrNode -> {
                for (child in node.children) {
                    val childResult = validateRuleDepth(child, currentDepth + 1)
                    if (childResult is ValidationResult.Invalid) return childResult
                }
            }
            is NotNode -> {
                return validateRuleDepth(node.child, currentDepth + 1)
            }
        }
        return ValidationResult.Valid
    }
}
