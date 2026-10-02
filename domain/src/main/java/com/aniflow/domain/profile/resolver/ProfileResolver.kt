package com.aniflow.domain.profile.resolver

import com.aniflow.domain.profile.model.ProfileSnapshot
import com.aniflow.domain.profile.model.UserProfile
import java.time.Instant

enum class ProfileResolutionSource {
    ManualOverride,
    Automation,
    SavedSearch,
    AnimeSpecific,
    GlobalDefault
}

data class ProfileResolutionContext(
    val manualOverrideProfile: UserProfile? = null,
    val automationProfile: UserProfile? = null,
    val savedSearchProfile: UserProfile? = null,
    val animeProfile: UserProfile? = null,
    val globalDefaultProfile: UserProfile
)

data class ResolvedProfileResult(
    val profile: UserProfile,
    val winningSource: ProfileResolutionSource,
    val snapshot: ProfileSnapshot,
    val resolutionTrace: List<String>
)

/**
 * Deterministic Profile Resolver (Section 5, 6).
 * Enforces strict precedence hierarchy:
 * Manual Override -> Automation Profile -> Saved Search Profile -> Anime Profile -> Global Default Profile.
 */
class ProfileResolver {

    fun resolve(context: ProfileResolutionContext): ResolvedProfileResult {
        val trace = mutableListOf<String>()

        val (winningProfile, source) = when {
            context.manualOverrideProfile != null -> {
                trace.add("Profile '${context.manualOverrideProfile.name}' selected from Manual User Override (highest priority)")
                context.manualOverrideProfile to ProfileResolutionSource.ManualOverride
            }
            context.automationProfile != null -> {
                trace.add("Profile '${context.automationProfile.name}' selected from Automation Rule context")
                context.automationProfile to ProfileResolutionSource.Automation
            }
            context.savedSearchProfile != null -> {
                trace.add("Profile '${context.savedSearchProfile.name}' selected from Saved Search scope")
                context.savedSearchProfile to ProfileResolutionSource.SavedSearch
            }
            context.animeProfile != null -> {
                trace.add("Profile '${context.animeProfile.name}' selected from Anime-specific configuration")
                context.animeProfile to ProfileResolutionSource.AnimeSpecific
            }
            else -> {
                trace.add("Profile '${context.globalDefaultProfile.name}' resolved from Global Default")
                context.globalDefaultProfile to ProfileResolutionSource.GlobalDefault
            }
        }

        val snapshot = ProfileSnapshot(
            profileId = winningProfile.id,
            profileName = winningProfile.name,
            version = winningProfile.version,
            resolvedAt = Instant.now(),
            preferences = winningProfile.preferences,
            policySummary = mapOf(
                "source" to source.name,
                "networkPolicy" to winningProfile.networkPolicy.mode.name,
                "storageMinFreeSpace" to "${winningProfile.storagePolicy.minimumFreeSpaceBytes} bytes",
                "maxConcurrentDownloads" to winningProfile.downloadPolicy.maxConcurrentTasks.toString()
            )
        )

        return ResolvedProfileResult(
            profile = winningProfile,
            winningSource = source,
            snapshot = snapshot,
            resolutionTrace = trace
        )
    }
}
