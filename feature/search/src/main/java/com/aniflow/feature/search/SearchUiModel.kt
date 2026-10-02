package com.aniflow.feature.search

import com.aniflow.core.ui.model.ReleaseUiModel

/**
 * Presentation models for search feature (Section 47).
 */
typealias SearchReleaseItem = ReleaseUiModel

data class FilterOption(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class SortOptionUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)
