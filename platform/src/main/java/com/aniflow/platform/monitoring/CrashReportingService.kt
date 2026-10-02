package com.aniflow.platform.monitoring

import com.aniflow.core.logging.LogRedactor
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enforces STEP 14 Section 33 (Crash Reporting Architecture), Section 35 (Error Reporting),
 * and Section 36 (Offline Crash / Error Queue).
 *
 * Guarantees privacy:
 * 1. Stack traces and messages are passed through LogRedactor to strip passwords, tokens, and passkeys.
 * 2. Stores up to 20 crash events locally in a bounded FIFO queue.
 * 3. Never captures personal filenames or provider credentials.
 */
enum class ErrorSeverity {
    FatalCrash,
    NonFatalError,
    Recoverable
}

data class CrashEvent(
    val eventId: String = UUID.randomUUID().toString().take(8),
    val appVersion: String = "1.0.0",
    val severity: ErrorSeverity,
    val exceptionClass: String,
    val sanitizedMessage: String,
    val sanitizedStackTrace: String,
    val timestamp: Instant = Instant.now()
)

class CrashReportingService(
    private val maxQueuedEvents: Int = 20
) {

    private val localEventQueue = ConcurrentLinkedQueue<CrashEvent>()

    fun recordException(throwable: Throwable, severity: ErrorSeverity = ErrorSeverity.NonFatalError) {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val rawTrace = stringWriter.toString()

        val sanitizedTrace = LogRedactor.redact(rawTrace)
        val sanitizedMessage = LogRedactor.redact(throwable.message ?: "No message provided")

        val event = CrashEvent(
            severity = severity,
            exceptionClass = throwable.javaClass.simpleName,
            sanitizedMessage = sanitizedMessage,
            sanitizedStackTrace = sanitizedTrace
        )

        localEventQueue.offer(event)
        while (localEventQueue.size > maxQueuedEvents) {
            localEventQueue.poll()
        }
    }

    fun getPendingEvents(): List<CrashEvent> {
        return localEventQueue.toList()
    }

    fun clearEvents() {
        localEventQueue.clear()
    }
}
