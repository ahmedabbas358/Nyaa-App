package com.aniflow.domain.identity

/**
 * Strongly-typed ID value classes for domain entities.
 * Prevents stringly-typed primitive obsession and accidental ID mixing.
 * Zero allocation runtime cost via Kotlin inline value classes.
 */

@JvmInline
value class AnimeId(val value: String) {
    init {
        require(value.isNotBlank()) { "AnimeId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class AnimeTitleId(val value: String) {
    init {
        require(value.isNotBlank()) { "AnimeTitleId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SeasonId(val value: String) {
    init {
        require(value.isNotBlank()) { "SeasonId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class EpisodeId(val value: String) {
    init {
        require(value.isNotBlank()) { "EpisodeId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ReleaseId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReleaseId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class UploaderId(val value: String) {
    init {
        require(value.isNotBlank()) { "UploaderId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ReleaseGroupId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReleaseGroupId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ProviderId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProviderId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class CollectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "CollectionId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class CollectionItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "CollectionItemId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SavedSearchId(val value: String) {
    init {
        require(value.isNotBlank()) { "SavedSearchId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class DownloadProfileId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadProfileId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class RuleId(val value: String) {
    init {
        require(value.isNotBlank()) { "RuleId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class DownloadPlanId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadPlanId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class DownloadTaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadTaskId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class DownloadFileId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadFileId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class DownloadSegmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadSegmentId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class LibraryItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "LibraryItemId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class LibraryFileId(val value: String) {
    init {
        require(value.isNotBlank()) { "LibraryFileId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class StorageId(val value: String) {
    init {
        require(value.isNotBlank()) { "StorageId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class MediaAssetId(val value: String) {
    init {
        require(value.isNotBlank()) { "MediaAssetId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class OrganizationPlanId(val value: String) {
    init {
        require(value.isNotBlank()) { "OrganizationPlanId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SavedSearchRunId(val value: String) {
    init {
        require(value.isNotBlank()) { "SavedSearchRunId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class WatchlistId(val value: String) {
    init {
        require(value.isNotBlank()) { "WatchlistId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class WatchlistItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "WatchlistItemId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class AutomationRuleId(val value: String) {
    init {
        require(value.isNotBlank()) { "AutomationRuleId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class AutomationExecutionId(val value: String) {
    init {
        require(value.isNotBlank()) { "AutomationExecutionId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SearchScheduleId(val value: String) {
    init {
        require(value.isNotBlank()) { "SearchScheduleId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ReviewItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReviewItemId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SearchHistoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "SearchHistoryId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SearchPresetId(val value: String) {
    init {
        require(value.isNotBlank()) { "SearchPresetId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class SearchSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SearchSessionId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class ProfileId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProfileId cannot be blank" }
    }
    override fun toString(): String = value
}

@JvmInline
value class LibraryMediaId(val value: String) {
    init {
        require(value.isNotBlank()) { "LibraryMediaId cannot be blank" }
    }
    override fun toString(): String = value
}

