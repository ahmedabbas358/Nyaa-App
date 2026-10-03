package com.aniflow.domain.automation.review

import com.aniflow.domain.automation.model.ReviewItem
import com.aniflow.domain.automation.model.ReviewItemState
import com.aniflow.domain.identity.AutomationExecutionId
import com.aniflow.domain.identity.ReviewItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ReviewQueueManager (Section 54, 55, 56, 110).
 * Holds ambiguous releases, large batches, or conflicting actions requiring manual confirmation.
 */
class ReviewQueueManager {

    private val items = ConcurrentHashMap<ReviewItemId, ReviewItem>()
    private val _itemsFlow = MutableStateFlow<List<ReviewItem>>(emptyList())

    fun observePendingItems(): Flow<List<ReviewItem>> = _itemsFlow.asStateFlow()

    fun createReviewItem(
        executionId: AutomationExecutionId?,
        issue: String,
        candidateReleaseTitle: String,
        reason: String,
        recommendedAction: String,
        expiresInDays: Long = 7
    ): ReviewItem {
        val id = ReviewItemId("rev_${UUID.randomUUID().toString().take(8)}")
        val item = ReviewItem(
            id = id,
            executionId = executionId,
            issue = issue,
            candidateReleaseTitle = candidateReleaseTitle,
            reason = reason,
            recommendedAction = recommendedAction,
            state = ReviewItemState.Pending,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(expiresInDays * 86400)
        )

        items[id] = item
        _itemsFlow.value = items.values.filter { it.state == ReviewItemState.Pending }
        return item
    }

    fun approve(id: ReviewItemId) {
        items[id]?.let {
            items[id] = it.copy(state = ReviewItemState.Approved)
            _itemsFlow.value = items.values.filter { it.state == ReviewItemState.Pending }
        }
    }

    fun reject(id: ReviewItemId) {
        items[id]?.let {
            items[id] = it.copy(state = ReviewItemState.Rejected)
            _itemsFlow.value = items.values.filter { it.state == ReviewItemState.Pending }
        }
    }

    fun pruneExpired(now: Instant = Instant.now()) {
        val iterator = items.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val expiresAt = entry.value.expiresAt
            if (expiresAt != null && now.isAfter(expiresAt)) {
                items[entry.key] = entry.value.copy(state = ReviewItemState.Expired)
            }
        }
        _itemsFlow.value = items.values.filter { it.state == ReviewItemState.Pending }
    }
}
