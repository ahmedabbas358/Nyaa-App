package com.aniflow.core.logging

import java.util.UUID

/**
 * Observability context tracking distributed operations across layers without leaking secrets.
 * Enforces Section 97 (Observability IDs).
 */
data class ObservabilityContext(
    val correlationId: String = UUID.randomUUID().toString().take(8),
    val errorId: String? = null,
    val taskId: String? = null,
    val providerId: String? = null
) {
    fun toLogPrefix(): String {
        val parts = mutableListOf<String>()
        parts.add("cid=$correlationId")
        if (errorId != null) parts.add("err=$errorId")
        if (taskId != null) parts.add("task=$taskId")
        if (providerId != null) parts.add("provider=$providerId")
        return "[${parts.joinToString(" ")}]"
    }
}

/**
 * Production-hardened Logger implementing Section 49, 68, 96, and 97.
 * - Redacts all sensitive credentials automatically via LogRedactor.
 * - Suppresses DEBUG messages in Release mode.
 * - Attaches structured observability identifiers.
 */
class SecureLogger(
    private val isDebugBuild: Boolean = false,
    private val sink: (level: LogLevel, tag: String, message: String, throwable: Throwable?) -> Unit = { level, tag, msg, thr ->
        when (level) {
            LogLevel.DEBUG -> println("[DEBUG][$tag] $msg")
            LogLevel.INFO -> println("[INFO][$tag] $msg")
            LogLevel.WARN -> {
                println("[WARN][$tag] $msg")
                thr?.printStackTrace()
            }
            LogLevel.ERROR -> {
                System.err.println("[ERROR][$tag] $msg")
                thr?.printStackTrace()
            }
            LogLevel.CRITICAL -> {
                System.err.println("[CRITICAL][$tag] $msg")
                thr?.printStackTrace()
            }
        }
    }
) : Logger {

    override fun debug(tag: String, message: String) {
        if (!isDebugBuild) return // Enforce Section 68: No verbose debug logging in Release build
        val sanitized = LogRedactor.redact(message)
        sink(LogLevel.DEBUG, tag, sanitized, null)
    }

    override fun info(tag: String, message: String) {
        val sanitized = LogRedactor.redact(message)
        sink(LogLevel.INFO, tag, sanitized, null)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        val sanitized = LogRedactor.redact(message)
        sink(LogLevel.WARN, tag, sanitized, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        val sanitized = LogRedactor.redact(message)
        sink(LogLevel.ERROR, tag, sanitized, throwable)
    }

    override fun critical(tag: String, message: String, throwable: Throwable?) {
        val sanitized = LogRedactor.redact(message)
        sink(LogLevel.CRITICAL, tag, sanitized, throwable)
    }

    fun logWithContext(
        level: LogLevel,
        tag: String,
        context: ObservabilityContext,
        message: String,
        throwable: Throwable? = null
    ) {
        val formatted = "${context.toLogPrefix()} $message"
        when (level) {
            LogLevel.DEBUG -> debug(tag, formatted)
            LogLevel.INFO -> info(tag, formatted)
            LogLevel.WARN -> warn(tag, formatted, throwable)
            LogLevel.ERROR -> error(tag, formatted, throwable)
            LogLevel.CRITICAL -> critical(tag, formatted, throwable)
        }
    }
}
