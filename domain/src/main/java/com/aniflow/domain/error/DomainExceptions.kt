package com.aniflow.domain.error

/**
 * Root domain exception hierarchy for AniFlow.
 * Enforces business invariants and domain boundaries without leaking infrastructure details.
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidAnimeIdentityException(message: String) : DomainException(message)

class InvalidEpisodeNumberException(message: String) : DomainException(message)

class InvalidEpisodeRangeException(message: String) : DomainException(message)

class InvalidByteSizeException(message: String) : DomainException(message)

class InvalidReleaseException(message: String) : DomainException(message)

class InvalidDownloadSourceException(message: String) : DomainException(message)

class InvalidStorageTargetException(message: String) : DomainException(message)

class InvalidStateTransitionException(
    val fromState: String,
    val toState: String,
    val reason: String
) : DomainException("Invalid transition from $fromState to $toState: $reason")

class DuplicateDownloadException(
    val releaseId: String,
    val matchedAgainstId: String,
    val explanation: String
) : DomainException("Duplicate download prevented: $explanation (Target: $releaseId matched $matchedAgainstId)")

class RuleEvaluationException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class DownloadPlanningException(message: String) : DomainException(message)
