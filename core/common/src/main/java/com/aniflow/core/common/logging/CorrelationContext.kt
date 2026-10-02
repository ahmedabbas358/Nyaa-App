package com.aniflow.core.common.logging

import java.util.UUID

/**
 * CorrelationContext (Sections 64, 65).
 * Propagates end-to-end trace correlation IDs across Search, Selection, Planning, Download Runtime,
 * Storage Finalization, and Library indexing for complete observability and diagnostic tracing.
 */
data class CorrelationContext(
    val searchSessionId: String? = null,
    val downloadPlanId: String? = null,
    val downloadTaskId: String? = null,
    val automationExecutionId: String? = null
) {
    companion object {
        fun newSearchSession(): CorrelationContext =
            CorrelationContext(searchSessionId = "search-${UUID.randomUUID()}")

        fun newPlan(searchContext: CorrelationContext? = null): CorrelationContext =
            CorrelationContext(
                searchSessionId = searchContext?.searchSessionId,
                downloadPlanId = "plan-${UUID.randomUUID()}"
            )

        fun newTask(planContext: CorrelationContext? = null): CorrelationContext =
            CorrelationContext(
                searchSessionId = planContext?.searchSessionId,
                downloadPlanId = planContext?.downloadPlanId,
                downloadTaskId = "task-${UUID.randomUUID()}"
            )

        fun newAutomation(): CorrelationContext =
            CorrelationContext(automationExecutionId = "auto-${UUID.randomUUID()}")
    }
}

data class StructuredLogEntry(
    val category: String,
    val event: String,
    val level: String = "INFO",
    val timestampMillis: Long = System.currentTimeMillis(),
    val correlation: CorrelationContext = CorrelationContext(),
    val message: String,
    val metadata: Map<String, Any> = emptyMap()
)
