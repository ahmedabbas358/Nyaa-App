package com.aniflow.domain.automation

import com.aniflow.domain.automation.cooldown.CooldownManager
import com.aniflow.domain.automation.deduplication.KnownReleaseRecord
import com.aniflow.domain.automation.deduplication.NewReleaseDetector
import com.aniflow.domain.automation.deduplication.ReleaseSeenState
import com.aniflow.domain.automation.execution.AutomationRuntime
import com.aniflow.domain.automation.model.AutomationAction
import com.aniflow.domain.automation.model.AutomationExecutionState
import com.aniflow.domain.automation.model.AutomationLimits
import com.aniflow.domain.automation.model.AutomationRule
import com.aniflow.domain.automation.model.AutomationTrigger
import com.aniflow.domain.automation.model.ConfirmationMode
import com.aniflow.domain.automation.model.ConfirmationPolicy
import com.aniflow.domain.automation.model.CooldownPolicy
import com.aniflow.domain.automation.model.CooldownScope
import com.aniflow.domain.automation.model.SafetyBlockReason
import com.aniflow.domain.automation.model.SafetyDecision
import com.aniflow.domain.automation.model.ScheduleDefinition
import com.aniflow.domain.automation.model.SearchSchedule
import com.aniflow.domain.automation.review.ReviewQueueManager
import com.aniflow.domain.automation.safety.AutomationSafetyGate
import com.aniflow.domain.automation.safety.SafetyEvaluationContext
import com.aniflow.domain.automation.scheduler.AutomationScheduler
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.identity.AutomationRuleId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SearchScheduleId
import com.aniflow.domain.intelligence.model.AudioTrack
import com.aniflow.domain.intelligence.model.EpisodeMatch
import com.aniflow.domain.intelligence.model.EpisodeRangeMatch
import com.aniflow.domain.intelligence.model.FileInfo
import com.aniflow.domain.intelligence.model.MagnetLink
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseLinks
import com.aniflow.domain.intelligence.model.ReleaseSource
import com.aniflow.domain.intelligence.model.ReleaseStats
import com.aniflow.domain.intelligence.model.SubtitleTrack
import com.aniflow.domain.intelligence.model.TechnicalMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * AutomationTestSuite (Sections 158-164, 175).
 * Tests all DoD requirements for Step 24:
 * 1. New release vs stat updates vs metadata change detection.
 * 2. Search result deduplication before automation evaluation.
 * 3. Safety Gate checks (Kill Switch, Duplicate, Low Storage, Quotas, Ambiguity).
 * 4. Scoped cooldowns & expirable locks.
 * 5. Scheduler calculation & offline missed run coalescing.
 * 6. AST condition evaluation and explainability.
 * 7. Dry run simulation without side effects.
 * 8. Infinite loop protection.
 */
class AutomationTestSuite {

    private fun createSampleRelease(
        id: String,
        title: String,
        seeders: Int = 10,
        leechers: Int = 2,
        magnet: String? = "magnet:?xt=urn:btih:hash$id",
        sizeBytes: Long = 1024L * 1024 * 1024,
        resolution: String = "1080p",
        codec: String = "HEVC",
        isBatch: Boolean = false
    ): NormalizedRelease {
        return NormalizedRelease(
            releaseId = ReleaseId(id),
            providerReleaseId = id,
            source = ReleaseSource(title = title, providerName = "Nyaa", pageUrl = null),
            technical = TechnicalMetadata(
                resolution = resolution,
                videoCodec = codec,
                audio = listOf(AudioTrack("AAC", "ja")),
                subtitles = listOf(SubtitleTrack("English", true))
            ),
            episodeMatch = EpisodeMatch(
                confidence = 0.95f,
                detectedEpisode = 1.0,
                range = EpisodeRangeMatch(1.0, 1.0)
            ),
            stats = ReleaseStats(seeders = seeders, leechers = leechers, downloads = 50),
            fileInfo = FileInfo(sizeBytes = sizeBytes, fileCount = 1),
            links = ReleaseLinks(magnetUri = magnet?.let { MagnetLink(it) }),
            isBatch = isBatch
        )
    }

    // =========================================================================
    // 1. New Release Detector Tests (Section 15-20, 128, 159)
    // =========================================================================

    @Test
    fun testNewReleaseDetector_detectsNewAndDistinguishesFromStatUpdates() {
        val detector = NewReleaseDetector()

        val known = mapOf(
            "nyaa:rel_1" to KnownReleaseRecord(
                identityKey = "nyaa:rel_1",
                seeders = 10,
                leechers = 2,
                magnetUri = "magnet:?xt=urn:btih:hashrel_1",
                state = ReleaseSeenState.Seen
            )
        )

        val incoming = listOf(
            // rel_1: same magnet, only seeders changed from 10 to 25 -> Should NOT be treated as new release
            createSampleRelease("rel_1", "One Piece - 1109", seeders = 25),
            // rel_2: totally unknown -> New release
            createSampleRelease("rel_2", "One Piece - 1110", seeders = 5)
        )

        val result = detector.detect(incoming, known)

        assertEquals(1, result.newReleases.size)
        assertEquals("rel_2", result.newReleases.first().providerReleaseId)
        assertEquals(0, result.updatedReleases.size) // Only stats changed, not metadata
        assertEquals(1, result.seenCount)
    }

    @Test
    fun testNewReleaseDetector_detectsMetadataChangeWhenMagnetUpdates() {
        val detector = NewReleaseDetector()

        val known = mapOf(
            "nyaa:rel_1" to KnownReleaseRecord(
                identityKey = "nyaa:rel_1",
                seeders = 10,
                leechers = 2,
                magnetUri = "magnet:?xt=urn:btih:old_magnet",
                state = ReleaseSeenState.Seen
            )
        )

        val incoming = listOf(
            // rel_1: new magnet URI updated by uploader
            createSampleRelease("rel_1", "One Piece - 1109", magnet = "magnet:?xt=urn:btih:new_magnet")
        )

        val result = detector.detect(incoming, known)

        assertEquals(0, result.newReleases.size)
        assertEquals(1, result.updatedReleases.size)
        assertEquals("rel_1", result.updatedReleases.first().providerReleaseId)
    }

    @Test
    fun testNewReleaseDetector_deduplicatesIdenticalReleasesAcrossPagesOrRss() {
        val detector = NewReleaseDetector()

        val incoming = listOf(
            createSampleRelease("rel_10", "Bleach - 01"),
            createSampleRelease("rel_10", "Bleach - 01"), // Duplicate row from page 2 or RSS
            createSampleRelease("rel_10", "Bleach - 01")
        )

        val result = detector.detect(incoming, emptyMap())

        assertEquals(1, result.newReleases.size)
        assertEquals(1, result.totalProcessed)
    }

    // =========================================================================
    // 2. Safety Gate Tests (Section 29-35, 118-121, 163)
    // =========================================================================

    @Test
    fun testSafetyGate_blocksWhenGlobalKillSwitchActive() {
        val gate = AutomationSafetyGate(AutomationLimits(globalKillSwitch = true))
        val release = createSampleRelease("rel_1", "One Piece 1110")

        val context = SafetyEvaluationContext(
            release = release,
            availableStorageBytes = 100L * 1024 * 1024 * 1024,
            requiredSizeBytes = 1L * 1024 * 1024 * 1024
        )

        val result = gate.evaluate(context)

        assertEquals(SafetyDecision.Blocked, result.decision)
        assertEquals(SafetyBlockReason.GlobalKillSwitchActive, result.blockReason)
    }

    @Test
    fun testSafetyGate_blocksWhenDuplicateActiveTaskExists() {
        val gate = AutomationSafetyGate()
        val release = createSampleRelease("rel_1", "One Piece 1110")

        val context = SafetyEvaluationContext(
            release = release,
            isDuplicateActiveTask = true,
            availableStorageBytes = 100L * 1024 * 1024 * 1024,
            requiredSizeBytes = 1L * 1024 * 1024 * 1024
        )

        val result = gate.evaluate(context)

        assertEquals(SafetyDecision.Blocked, result.decision)
        assertEquals(SafetyBlockReason.DuplicateTaskExists, result.blockReason)
    }

    @Test
    fun testSafetyGate_blocksWhenStorageInsufficientPlusMargin() {
        val gate = AutomationSafetyGate()
        val release = createSampleRelease("rel_1", "One Piece 1110", sizeBytes = 2L * 1024 * 1024 * 1024) // 2GB

        // Only 2.5GB available: Required 2GB + 1GB safety margin = 3GB needed -> BLOCKED
        val context = SafetyEvaluationContext(
            release = release,
            availableStorageBytes = 2500L * 1024 * 1024,
            requiredSizeBytes = 2000L * 1024 * 1024
        )

        val result = gate.evaluate(context)

        assertEquals(SafetyDecision.Blocked, result.decision)
        assertEquals(SafetyBlockReason.InsufficientStorage, result.blockReason)
    }

    @Test
    fun testSafetyGate_blocksWhenDailyDownloadLimitExceeded() {
        val gate = AutomationSafetyGate(AutomationLimits(maxAutomaticDownloadsPerDay = 5))
        val release = createSampleRelease("rel_1", "One Piece 1110")

        val context = SafetyEvaluationContext(
            release = release,
            availableStorageBytes = 100L * 1024 * 1024 * 1024,
            requiredSizeBytes = 1L * 1024 * 1024 * 1024,
            downloadsTodayCount = 5 // Cap reached
        )

        val result = gate.evaluate(context)

        assertEquals(SafetyDecision.Blocked, result.decision)
        assertEquals(SafetyBlockReason.ExceedsDailyLimit, result.blockReason)
    }

    @Test
    fun testSafetyGate_requiresConfirmationOnAmbiguousParser() {
        val gate = AutomationSafetyGate()
        val release = createSampleRelease("rel_1", "Ambiguous OVA Special")

        val context = SafetyEvaluationContext(
            release = release,
            isAmbiguous = true,
            availableStorageBytes = 100L * 1024 * 1024 * 1024,
            requiredSizeBytes = 1L * 1024 * 1024 * 1024
        )

        val result = gate.evaluate(context)

        assertEquals(SafetyDecision.RequiresConfirmation, result.decision)
        assertEquals(SafetyBlockReason.AmbiguousParser, result.blockReason)
    }

    @Test
    fun testSafetyGate_requiresConfirmationOnLargeBatch() {
        val gate = AutomationSafetyGate()
        val release = createSampleRelease("rel_1", "Bleach Complete Series Batch", isBatch = true, sizeBytes = 15L * 1024 * 1024 * 1024)

        val context = SafetyEvaluationContext(
            release = release,
            isBatch = true,
            availableStorageBytes = 100L * 1024 * 1024 * 1024,
            requiredSizeBytes = 15L * 1024 * 1024 * 1024
        )

        // Default threshold is 10 GB
        val policy = ConfirmationPolicy(mode = ConfirmationMode.Confirm, sizeThresholdBytes = 10L * 1024 * 1024 * 1024)
        val result = gate.evaluate(context, policy)

        assertEquals(SafetyDecision.RequiresConfirmation, result.decision)
    }

    // =========================================================================
    // 3. Cooldown & Idempotency Tests (Section 36-38, 96, 97)
    // =========================================================================

    @Test
    fun testCooldownManager_preventsRepeatedExecutionWithinWindow() {
        val manager = CooldownManager()
        val key = com.aniflow.domain.automation.model.AutomationExecutionKey(
            ruleId = AutomationRuleId("rule_1"),
            triggerIdentity = "trigger_1",
            targetIdentity = "One Piece_1110"
        )
        val policy = CooldownPolicy(durationSeconds = 3600, scope = CooldownScope.PerEpisode)

        val now = Instant.now()

        // 1. Not cooling down initially
        assertFalse(manager.isCoolingDown(key, policy, now))

        // 2. Record execution
        manager.recordExecution(key, policy, now)

        // 3. Cooling down 5 minutes later
        assertTrue(manager.isCoolingDown(key, policy, now.plusSeconds(300)))

        // 4. Cooldown expired 61 minutes later
        assertFalse(manager.isCoolingDown(key, policy, now.plusSeconds(3660)))
    }

    @Test
    fun testCooldownManager_expirableLockPreventsConcurrentExecution() {
        val manager = CooldownManager()
        val lockKey = "exec_lock_one_piece"
        val now = Instant.now()

        // First worker acquires lock
        assertTrue(manager.tryAcquireLock(lockKey, lockDurationSeconds = 60, now = now))

        // Second worker tries to acquire same lock -> Rejected
        assertFalse(manager.tryAcquireLock(lockKey, lockDurationSeconds = 60, now = now.plusSeconds(10)))

        // First worker releases lock
        manager.releaseLock(lockKey)

        // Now can acquire again
        assertTrue(manager.tryAcquireLock(lockKey, lockDurationSeconds = 60, now = now.plusSeconds(15)))
    }

    // =========================================================================
    // 4. Scheduler Tests (Section 40-43, 131)
    // =========================================================================

    @Test
    fun testScheduler_calculatesCorrectNextRun() {
        val scheduler = AutomationScheduler()
        val now = Instant.now()

        val intervalTime = scheduler.calculateNextRun(ScheduleDefinition.IntervalMinutes(30), now)
        assertEquals(now.plusSeconds(1800), intervalTime)

        val dailyTime = scheduler.calculateNextRun(ScheduleDefinition.DailyAtHour(2), now)
        assertEquals(now.plusSeconds(86400), dailyTime)
    }

    @Test
    fun testScheduler_coalescesMissedRunsWhileDeviceWasOffline() {
        val scheduler = AutomationScheduler()
        val now = Instant.now()

        // Schedule was due 3 days ago (device offline)
        val oldDueTime = now.minusSeconds(86400 * 3)
        val schedule = SearchSchedule(
            id = SearchScheduleId("sched_1"),
            savedSearchId = SavedSearchId("ss_1"),
            enabled = true,
            schedule = ScheduleDefinition.IntervalMinutes(60),
            nextRunAt = oldDueTime
        )
        scheduler.registerSchedule(schedule)

        // 1. Should be flagged as due once
        val dueList = scheduler.getDueSchedules(now)
        assertEquals(1, dueList.size)

        // 2. On completion, schedule is advanced to next slot from completion time (not executing 72 missed runs!)
        val updated = scheduler.onScheduleCompleted(schedule.id, completedAt = now)
        assertNotNull(updated)
        assertEquals(now.plusSeconds(3600), updated!!.nextRunAt)

        // 3. Immediately checking again shows 0 due runs (successfully coalesced)
        val dueAfter = scheduler.getDueSchedules(now)
        assertEquals(0, dueAfter.size)
    }

    // =========================================================================
    // 5. Automation Runtime & AST Rule Evaluation Tests (Section 23, 24, 28, 66)
    // =========================================================================

    @Test
    fun testAutomationRuntime_evaluatesRuleASTAndDispatchesDownloadPlan() = runBlocking {
        val runtime = AutomationRuntime()

        // Rule condition: Resolution == "1080p"
        val condition = ConditionNode(
            AdvancedRuleCondition.ResolutionIs(Resolution.R1080p)
        )

        val rule = AutomationRule(
            id = AutomationRuleId("rule_1080p"),
            name = "Prefer 1080p",
            enabled = true,
            trigger = AutomationTrigger.NewRelease("One Piece"),
            conditions = condition,
            actions = listOf(AutomationAction.QueueDownload)
        )

        val candidates = listOf(
            createSampleRelease("rel_720", "One Piece - 1110 (720p)", resolution = "720p"),
            createSampleRelease("rel_1080", "One Piece - 1110 (1080p)", resolution = "1080p")
        )

        var planCreatedFor: String? = null
        val executions = runtime.processTrigger(
            trigger = AutomationTrigger.NewRelease("One Piece"),
            rules = listOf(rule),
            candidateReleases = candidates,
            availableStorageBytes = 50L * 1024 * 1024 * 1024,
            isWiFi = true,
            onQueuePlan = { rel ->
                planCreatedFor = rel.providerReleaseId
                "plan_test_123"
            }
        )

        // 720p candidate was skipped by rule condition; only 1080p was evaluated and queued
        assertEquals(1, executions.size)
        val exec = executions.first()
        assertEquals(AutomationExecutionState.Completed, exec.state)
        assertEquals("rel_1080", planCreatedFor)
        assertTrue(exec.explainabilityLog.any { it.contains("Rule conditions satisfied") })
    }

    @Test
    fun testAutomationRuntime_disabledRuleNeverExecutes() = runBlocking {
        val runtime = AutomationRuntime()

        val rule = AutomationRule(
            id = AutomationRuleId("disabled_rule"),
            name = "Disabled Rule",
            enabled = false,
            trigger = AutomationTrigger.NewRelease("One Piece")
        )

        val executions = runtime.processTrigger(
            trigger = AutomationTrigger.NewRelease("One Piece"),
            rules = listOf(rule),
            candidateReleases = listOf(createSampleRelease("rel_1", "One Piece 1110")),
            availableStorageBytes = 50L * 1024 * 1024 * 1024
        )

        assertEquals(0, executions.size)
    }

    @Test
    fun testAutomationRuntime_dryRunSimulationHasZeroSideEffects() {
        val runtime = AutomationRuntime()

        val rule = AutomationRule(
            id = AutomationRuleId("sim_rule"),
            name = "Simulation Rule",
            enabled = true,
            trigger = AutomationTrigger.NewRelease("One Piece"),
            actions = listOf(AutomationAction.QueueDownload)
        )

        val candidates = listOf(
            createSampleRelease("rel_1", "One Piece 1110", sizeBytes = 1400L * 1024 * 1024),
            createSampleRelease("rel_2", "One Piece 1111", sizeBytes = 1400L * 1024 * 1024)
        )

        val simResult = runtime.simulate(
            rule = rule,
            candidateReleases = candidates,
            availableStorageBytes = 50L * 1024 * 1024 * 1024
        )

        assertEquals(2, simResult.matchesCount)
        assertEquals(2, simResult.wouldSelectCount)
        assertEquals(2, simResult.wouldDownloadCount)
        assertEquals(0, simResult.wouldSkipCount)
        assertEquals(2800L * 1024 * 1024, simResult.estimatedSizeBytes)
    }

    // =========================================================================
    // 6. Loop Protection Test (Section 86, 162)
    // =========================================================================

    @Test
    fun testLoopProtection_downloadCompletedDoesNotTriggerDuplicatePlan() = runBlocking {
        val runtime = AutomationRuntime()
        val detector = NewReleaseDetector()

        // 1. Initial search detects new release
        val release = createSampleRelease("rel_100", "One Piece - 1100")
        val knownMap = mutableMapOf<String, KnownReleaseRecord>()

        val detection1 = detector.detect(listOf(release), knownMap)
        assertEquals(1, detection1.newReleases.size)

        // Record as known seen release
        val key = detector.buildIdentityKey(release)
        knownMap[key] = KnownReleaseRecord(
            identityKey = key,
            seeders = release.stats.seeders,
            leechers = release.stats.leechers,
            magnetUri = release.links.magnetUri?.value,
            state = ReleaseSeenState.Downloaded
        )

        // 2. Download completes -> triggers new search
        // Subsequent search returns the same release row
        val detection2 = detector.detect(listOf(release), knownMap)

        // New release detector identifies it as already seen/downloaded, NOT new!
        assertEquals(0, detection2.newReleases.size)
        assertEquals(1, detection2.seenCount)

        // No new automation executions can be triggered -> Loop terminated safely!
    }
}
