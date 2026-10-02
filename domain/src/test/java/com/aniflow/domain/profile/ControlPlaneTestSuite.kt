package com.aniflow.domain.profile

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.AndNode
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.NotNode
import com.aniflow.domain.controlplane.models.OrNode
import com.aniflow.domain.controlplane.models.RulePrecedenceLevel
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.model.aggregate.organization.RuleScope
import com.aniflow.domain.profile.model.EntityPreference
import com.aniflow.domain.profile.model.ProfilePreferences
import com.aniflow.domain.profile.model.ProfileTemplateType
import com.aniflow.domain.profile.model.ResolutionPreference
import com.aniflow.domain.profile.model.SizePreference
import com.aniflow.domain.profile.model.SizeRange
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.profile.resolver.ProfileResolutionContext
import com.aniflow.domain.profile.resolver.ProfileResolutionSource
import com.aniflow.domain.profile.resolver.ProfileResolver
import com.aniflow.domain.profile.template.ProfileTemplates
import com.aniflow.domain.profile.validator.InvalidProfileConfigurationException
import com.aniflow.domain.profile.validator.ProfileValidator
import com.aniflow.domain.rules.conflict.RuleConflictDetector
import com.aniflow.domain.rules.conflict.RuleConflictSeverity
import com.aniflow.domain.rules.conflict.RuleResolutionPolicy
import com.aniflow.domain.rules.simulation.RuleSimulator
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Test Suite for AniFlow Control Plane (STEP 27).
 * Tests Profile Validation, Precedence Resolvers, Rule ASTs, Conflict Detection, and Non-destructive Simulations.
 */
class ControlPlaneTestSuite {

    // -------------------------------------------------------------
    // 1. Profile Validation & Contradiction Rejection Tests
    // -------------------------------------------------------------

    @Test
    fun `testProfileValidation ValidProfile Passes`() {
        val valid = ProfileTemplates.createBalanced(ProfileId("prof_1"))
        val result = ProfileValidator.validate(valid)
        assertTrue("Valid profile must pass validation", result.isValid)
        assertTrue("No violations expected", result.violations.isEmpty())
    }

    @Test
    fun `testProfileValidation RequiredAndForbiddenContradiction Fails`() {
        val contradictory = ProfileTemplates.createBalanced(ProfileId("prof_2")).copy(
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    required = Resolution.R1080p,
                    forbidden = setOf(Resolution.R1080p)
                )
            )
        )

        val result = ProfileValidator.validate(contradictory)
        assertFalse("Profile with Required + Forbidden contradiction must fail", result.isValid)
        assertTrue(result.violations.any { it.field == "resolution" })

        assertThrows(InvalidProfileConfigurationException::class.java) {
            ProfileValidator.validateOrThrow(contradictory)
        }
    }

    @Test
    fun `testProfileValidation PreferredAndForbiddenContradiction Fails`() {
        val contradictory = ProfileTemplates.createBalanced(ProfileId("prof_3")).copy(
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R720p,
                    forbidden = setOf(Resolution.R720p)
                )
            )
        )

        val result = ProfileValidator.validate(contradictory)
        assertFalse("Preferred + Forbidden contradiction must fail", result.isValid)
        assertTrue(result.violations.any { it.field == "resolution" })
    }

    @Test
    fun `testProfileValidation InvalidSizeRange Fails`() {
        val invalidSize = ProfileTemplates.createBalanced(ProfileId("prof_4")).copy(
            preferences = ProfilePreferences(
                size = SizePreference(
                    minBytes = 5000L,
                    maxBytes = 2000L // min > max
                )
            )
        )

        val result = ProfileValidator.validate(invalidSize)
        assertFalse("Min size > Max size must fail validation", result.isValid)
        assertTrue(result.violations.any { it.field == "size" })
    }

    @Test
    fun `testProfileValidation ForbiddenAndPreferredEntity Fails`() {
        val invalidEntity = ProfileTemplates.createBalanced(ProfileId("prof_5")).copy(
            preferences = ProfilePreferences(
                releaseGroup = EntityPreference(
                    preferred = listOf("SubsPlease"),
                    forbidden = setOf("SubsPlease")
                )
            )
        )

        val result = ProfileValidator.validate(invalidEntity)
        assertFalse("Same entity in Preferred and Forbidden must fail", result.isValid)
        assertTrue(result.violations.any { it.field == "releaseGroup" })
    }

    // -------------------------------------------------------------
    // 2. ProfileResolver Precedence Hierarchy Tests (Section 5, 6)
    // -------------------------------------------------------------

    @Test
    fun `testProfileResolver Hierarchy ManualOverrideWins`() {
        val global = ProfileTemplates.createBalanced(ProfileId("prof_global"))
        val anime = ProfileTemplates.createHighQuality(ProfileId("prof_anime"))
        val automation = ProfileTemplates.createSmallSize(ProfileId("prof_auto"))
        val manual = ProfileTemplates.createArchive(ProfileId("prof_manual"))

        val resolver = ProfileResolver()
        val context = ProfileResolutionContext(
            manualOverrideProfile = manual,
            automationProfile = automation,
            animeProfile = anime,
            globalDefaultProfile = global
        )

        val result = resolver.resolve(context)
        assertEquals(ProfileResolutionSource.ManualOverride, result.winningSource)
        assertEquals(manual.id, result.profile.id)
        assertEquals(manual.name, result.profile.name)
    }

    @Test
    fun `testProfileResolver Hierarchy AutomationWinsOverAnimeAndGlobal`() {
        val global = ProfileTemplates.createBalanced(ProfileId("prof_global"))
        val anime = ProfileTemplates.createHighQuality(ProfileId("prof_anime"))
        val automation = ProfileTemplates.createSmallSize(ProfileId("prof_auto"))

        val resolver = ProfileResolver()
        val context = ProfileResolutionContext(
            manualOverrideProfile = null,
            automationProfile = automation,
            animeProfile = anime,
            globalDefaultProfile = global
        )

        val result = resolver.resolve(context)
        assertEquals(ProfileResolutionSource.Automation, result.winningSource)
        assertEquals(automation.id, result.profile.id)
    }

    @Test
    fun `testProfileResolver Hierarchy AnimeWinsOverGlobal`() {
        val global = ProfileTemplates.createBalanced(ProfileId("prof_global"))
        val anime = ProfileTemplates.createHighQuality(ProfileId("prof_anime"))

        val resolver = ProfileResolver()
        val context = ProfileResolutionContext(
            manualOverrideProfile = null,
            automationProfile = null,
            savedSearchProfile = null,
            animeProfile = anime,
            globalDefaultProfile = global
        )

        val result = resolver.resolve(context)
        assertEquals(ProfileResolutionSource.AnimeSpecific, result.winningSource)
        assertEquals(anime.id, result.profile.id)
    }

    @Test
    fun `testProfileResolver Hierarchy GlobalDefaultFallback`() {
        val global = ProfileTemplates.createBalanced(ProfileId("prof_global"))

        val resolver = ProfileResolver()
        val context = ProfileResolutionContext(
            manualOverrideProfile = null,
            automationProfile = null,
            savedSearchProfile = null,
            animeProfile = null,
            globalDefaultProfile = global
        )

        val result = resolver.resolve(context)
        assertEquals(ProfileResolutionSource.GlobalDefault, result.winningSource)
        assertEquals(global.id, result.profile.id)
    }

    @Test
    fun `testProfileSnapshot CapturesImmutably`() {
        val global = ProfileTemplates.createHighQuality(ProfileId("prof_hq_snap"))
        val resolver = ProfileResolver()
        val result = resolver.resolve(ProfileResolutionContext(globalDefaultProfile = global))

        val snapshot = result.snapshot
        assertEquals(global.id, snapshot.profileId)
        assertEquals(global.name, snapshot.profileName)
        assertEquals(global.version, snapshot.version)
        assertNotNull(snapshot.resolvedAt)
        assertEquals(global.preferences, snapshot.preferences)
    }

    // -------------------------------------------------------------
    // 3. Predefined Templates Verification (Section 37)
    // -------------------------------------------------------------

    @Test
    fun `testProfileTemplates GenerateExpectedCharacteristics`() {
        val balanced = ProfileTemplates.createFromTemplate(ProfileTemplateType.Balanced)
        assertEquals(Resolution.R1080p, balanced.preferences.resolution.preferred)
        assertEquals(VideoCodec.HEVC, balanced.preferences.codec.preferred)

        val hq = ProfileTemplates.createFromTemplate(ProfileTemplateType.HighQuality)
        assertTrue(hq.preferences.audio.requireLossless)

        val small = ProfileTemplates.createFromTemplate(ProfileTemplateType.SmallSize)
        assertEquals(Resolution.R720p, small.preferences.resolution.preferred)

        val archive = ProfileTemplates.createFromTemplate(ProfileTemplateType.Archive)
        assertEquals(Resolution.R1080p, archive.preferences.resolution.required)
    }

    // -------------------------------------------------------------
    // 4. Rule Precedence & Resolution Policy (Section 28)
    // -------------------------------------------------------------

    @Test
    fun `testRuleResolutionPolicy SortsByPrecedenceAndScope`() {
        val rule1 = AdvancedRule(
            id = RuleId("r1"),
            name = "Global General",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.EpisodeIsMissing),
            actions = listOf(AdvancedRuleAction.QueueDownload),
            precedence = RulePrecedenceLevel.GeneralRule,
            customPriority = 50
        )
        val rule2 = AdvancedRule(
            id = RuleId("r2"),
            name = "Anime Specific",
            scope = RuleScope.Anime,
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.QueueDownload),
            precedence = RulePrecedenceLevel.SpecificScopedRule,
            customPriority = 70
        )
        val rule3 = AdvancedRule(
            id = RuleId("r3"),
            name = "Manual Override Lock",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.QueueDownload),
            precedence = RulePrecedenceLevel.ManualOverride,
            customPriority = 100
        )

        val sorted = RuleResolutionPolicy.sortRules(listOf(rule1, rule3, rule2))
        assertEquals("Highest priority manual override must come first", rule3.id, sorted[0].id)
        assertEquals("Specific scoped rule comes second", rule2.id, sorted[1].id)
        assertEquals("General global rule comes third", rule1.id, sorted[2].id)
    }

    // -------------------------------------------------------------
    // 5. Rule Conflict Detection (Section 29)
    // -------------------------------------------------------------

    @Test
    fun `testRuleConflictDetector DetectsOppositeActionsOnSameCondition`() {
        val ruleQueue = AdvancedRule(
            id = RuleId("r_queue"),
            name = "Auto Queue One Piece",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.QueueDownload),
            customPriority = 60
        )

        val ruleReject = AdvancedRule(
            id = RuleId("r_reject"),
            name = "Reject One Piece",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.Reject("Blacklisted anime")),
            customPriority = 60
        )

        val detector = RuleConflictDetector()
        val conflicts = detector.detectConflicts(listOf(ruleQueue, ruleReject))

        assertEquals(1, conflicts.size)
        assertEquals(RuleConflictSeverity.Critical, conflicts[0].severity)
        assertTrue(conflicts[0].description.contains("Contradictory actions"))
    }

    @Test
    fun `testRuleConflictDetector DetectsAmbiguousProfileSelection`() {
        val ruleA = AdvancedRule(
            id = RuleId("r_a"),
            name = "Rule Select Profile A",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.EpisodeIsMissing),
            actions = listOf(AdvancedRuleAction.SelectProfile(DownloadProfileId("prof_a"))),
            customPriority = 50
        )

        val ruleB = AdvancedRule(
            id = RuleId("r_b"),
            name = "Rule Select Profile B",
            scope = RuleScope.Global,
            root = ConditionNode(AdvancedRuleCondition.EpisodeIsMissing),
            actions = listOf(AdvancedRuleAction.SelectProfile(DownloadProfileId("prof_b"))),
            customPriority = 50
        )

        val detector = RuleConflictDetector()
        val conflicts = detector.detectConflicts(listOf(ruleA, ruleB))

        assertEquals(1, conflicts.size)
        assertEquals(RuleConflictSeverity.Potential, conflicts[0].severity)
        assertTrue(conflicts[0].description.contains("Ambiguous profile selection"))
    }

    // -------------------------------------------------------------
    // 6. Rule Simulator & Non-Destructive Dry Run (Section 30, 31, 32)
    // -------------------------------------------------------------

    @Test
    fun `testRuleSimulator MatchesAndSafetyCheckPasses`() {
        val rule = AdvancedRule(
            id = RuleId("r_sim_1"),
            name = "1080p HEVC One Piece",
            root = AndNode(
                listOf(
                    ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
                    ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R1080p)),
                    ConditionNode(AdvancedRuleCondition.CodecIs(VideoCodec.HEVC))
                )
            ),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate = CandidateReleaseContext(
            title = "[SubsPlease] One Piece - 1090 (1080p) [HEVC].mkv",
            animeTitle = "One Piece",
            resolution = Resolution.R1080p,
            codec = VideoCodec.HEVC,
            sizeBytes = 1000L * 1024L * 1024L,
            freeSpaceBytes = 20L * 1024L * 1024L * 1024L,
            duplicateExists = false
        )

        val simulator = RuleSimulator()
        val report = simulator.simulate(rule, candidate)

        assertTrue("All conditions must match", report.overallMatched)
        assertTrue("Safety gate should pass with ample storage", report.safetyPreview.passed)
        assertEquals(1, report.appliedActions.size)
        assertTrue(report.explanation.contains("matched all conditions"))
    }

    @Test
    fun `testRuleSimulator FailsConditionAccurately`() {
        val rule = AdvancedRule(
            id = RuleId("r_sim_2"),
            name = "Only 1080p",
            root = ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R1080p)),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate720p = CandidateReleaseContext(
            title = "[Erai-raws] One Piece - 1090 [720p].mkv",
            animeTitle = "One Piece",
            resolution = Resolution.R720p
        )

        val simulator = RuleSimulator()
        val report = simulator.simulate(rule, candidate720p)

        assertFalse("720p candidate must fail 1080p condition", report.overallMatched)
        assertTrue(report.appliedActions.isEmpty())
        assertTrue(report.explanation.contains("failed to match"))
    }

    @Test
    fun `testRuleSimulator BlocksOnLowDiskSpaceWithoutMutatingState`() {
        val rule = AdvancedRule(
            id = RuleId("r_sim_3"),
            name = "Auto Queue",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidateLowSpace = CandidateReleaseContext(
            title = "[SubsPlease] One Piece - 1090.mkv",
            animeTitle = "One Piece",
            sizeBytes = 3L * 1024L * 1024L * 1024L,
            freeSpaceBytes = 1L * 1024L * 1024L * 1024L // Only 1GB free (less than 3GB + 2GB buffer)
        )

        val simulator = RuleSimulator()
        val report = simulator.simulate(rule, candidateLowSpace)

        assertTrue(report.overallMatched)
        assertFalse("Safety gate must block execution when disk space is insufficient", report.safetyPreview.passed)
        assertTrue(report.appliedActions.isEmpty())
        assertTrue(report.explanation.contains("BLOCKED by Safety Gate"))
    }

    // -------------------------------------------------------------
    // 7. Recursive AST Short-Circuit Evaluation (Section 24)
    // -------------------------------------------------------------

    @Test
    fun `testRuleTreeEvaluator NestedAndOrNotShortCircuit`() {
        val evaluator = RuleTreeEvaluator()

        // Expression: (Anime == "Bleach" OR Anime == "Naruto") AND NOT (Resolution == 480p)
        val ast = AndNode(
            listOf(
                OrNode(
                    listOf(
                        ConditionNode(AdvancedRuleCondition.AnimeIs("Bleach")),
                        ConditionNode(AdvancedRuleCondition.AnimeIs("Naruto"))
                    )
                ),
                NotNode(
                    ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R480p))
                )
            )
        )

        val naruto1080p = CandidateReleaseContext(
            title = "Naruto Ep 1 1080p",
            animeTitle = "Naruto",
            resolution = Resolution.R1080p
        )
        assertTrue(evaluator.evaluateNode(ast, naruto1080p))

        val naruto480p = CandidateReleaseContext(
            title = "Naruto Ep 1 480p",
            animeTitle = "Naruto",
            resolution = Resolution.R480p
        )
        assertFalse("480p must be excluded by NOT condition", evaluator.evaluateNode(ast, naruto480p))

        val onePiece1080p = CandidateReleaseContext(
            title = "One Piece Ep 1 1080p",
            animeTitle = "One Piece",
            resolution = Resolution.R1080p
        )
        assertFalse("One Piece does not match Bleach or Naruto", evaluator.evaluateNode(ast, onePiece1080p))
    }
}
