package com.aniflow.domain.model

import com.aniflow.domain.identity.ReleaseId

/**
 * Clean data model for sharing release metadata (Step 18 Section 54).
 * Strips raw HTML and ensures safe formatting for Android share intents.
 */
data class ShareableRelease(
    val id: ReleaseId,
    val title: String,
    val providerUrl: String?,
    val magnetUri: String? = null,
    val sizeText: String? = null
) {
    fun toShareText(): String = buildString {
        appendLine(title)
        if (!sizeText.isNullOrBlank()) appendLine("Size: $sizeText")
        if (!providerUrl.isNullOrBlank()) appendLine("Details: $providerUrl")
        if (!magnetUri.isNullOrBlank()) appendLine("Magnet: $magnetUri")
    }
}
