package com.aniflow.core.logging

import java.util.regex.Pattern

/**
 * Log Redactor enforcing STEP 13 Section 49 & 68 (Logging Security).
 * Automatically masks tokens, bearer credentials, API keys, passwords,
 * authorization headers, and private URIs before writing to logs.
 */
object LogRedactor {

    private val SENSITIVE_PATTERNS = listOf(
        // Bearer tokens and Authorization headers
        Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.~+/]+=*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(authorization:\\s*)[^\\r\\n,]+", Pattern.CASE_INSENSITIVE),
        // Passwords & API keys in key-value or query strings
        Pattern.compile("(?i)(password|secret|apikey|api_key|token|auth_token)\\s*[=:]\\s*[\"']?([^\"'&\\s]+)[\"']?", Pattern.CASE_INSENSITIVE),
        // Private passkeys in torrent/tracker URLs
        Pattern.compile("(?i)([?&]passkey=)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)([?&]auth=)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
    )

    fun redact(message: String): String {
        if (message.isBlank()) return message
        var sanitized = message
        for (pattern in SENSITIVE_PATTERNS) {
            val matcher = pattern.matcher(sanitized)
            val sb = StringBuffer()
            while (matcher.find()) {
                val prefix = if (matcher.groupCount() >= 1) matcher.group(1) else ""
                matcher.appendReplacement(sb, "$prefix[REDACTED]")
            }
            matcher.appendTail(sb)
            sanitized = sb.toString()
        }
        return sanitized
    }
}
