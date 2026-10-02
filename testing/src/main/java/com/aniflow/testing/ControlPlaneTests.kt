package com.aniflow.testing

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.AndExpression
import com.aniflow.domain.controlplane.models.AndNode
import com.aniflow.domain.controlplane.models.AutomationSafetyLevel
import com.aniflow.domain.controlplane.models.AutomationTrigger
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.ExtendedDownloadProfile
import com.aniflow.domain.controlplane.models.NotNode
import com.aniflow.domain.controlplane.models.OrNode
import com.aniflow.domain.controlplane.models.ProfileBackupDto
import com.aniflow.domain.controlplane.models.RuleBackupDto
import com.aniflow.domain.controlplane.models.RulePrecedenceLevel
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.controlplane.service.AutomationEngine
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.ConfigBackupService
import com.aniflow.domain.controlplane.service.ExportedConfigBundle
import com.aniflow.domain.controlplane.service.ImportConflictPolicy
import com.aniflow.domain.controlplane.service.PreferenceResolver
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.controlplane.service.ScopedPreferenceInput
import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.RuleId
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ControlPlaneTests (Sections 55, 56).
 * Unit tests verifying PreferenceResolver precedence, nested RuleTree AST evaluation,
 * AutomationEngine dry-run simulations, safety gates, idempotency locks, and versioned config backup.
 */
class ControlPlaneTests {

    private lateinit var preferenceResolver: PreferenceResolver
    private lateinit var ruleTreeEvaluator: RuleTreeEvaluator
    private lateinit var automationEngine: AutomationEngine
    private lateinit var backupService: ConfigBackupService

    @Before
    fun setUp() {
        preferenceResolver = PreferenceResolver()
        ruleTreeEvaluator = RuleTreeEvaluator()
        automationEngine = AutomationEngine(ruleTreeEvaluator)
        backupService = ConfigBackupService()
    }

    // ==========================================
    // 1. PREFERENCE RESOLVER & PRECEDENCE TESTS
    // ==========================================

    @Test
    fun `test PreferenceResolver honors ManualOverride over Anime and Profile scopes`() {
        val profile = ExtendedDownloadProfile(
            id = DownloadProfileId("prof-1080p"),
            name = "Anime 1080p",
            preferredResolution = Resolution.R1080p,
            preferredCodec = VideoCodec.HEVC
        )

        val input = ScopedPreferenceInput(
            animeTitle = "One Piece",
            manualOverrideResolution = Resolution.R2160p,
            animeSpecificResolution = Resolution.R720p,
            activeProfile = profile
        )

        val result = preferenceResolver.resolve(input)

        // Manual override wins highest precedence
        assertEquals(Resolution.R2160p, result.targetResolution)
        assertEquals(VideoCodec.HEVC, result.targetCodec)
        assertEquals("ManualOverride", result.winningScope)
        assertTrue(result.explanationTrace.any { it.contains("Manual User Override") })
    }

    @Test
    fun `test PreferenceResolver picks Anime Specific when manual override is absent`() {
        val profile = ExtendedDownloadProfile(
            id = DownloadProfileId("prof-1080p"),
            name = "Anime 1080p",
            preferredResolution = Resolution.R1080p
        )

        val input = ScopedPreferenceInput(
            animeTitle = "One Piece",
            manualOverrideResolution = null,
            animeSpecificResolution = Resolution.R720p,
            activeProfile = profile
        )

        val result = preferenceResolver.resolve(input)

        assertEquals(Resolution.R720p, result.targetResolution)
        assertTrue(result.explanationTrace.any { it.contains("Anime-Specific Preference") })
    }

    // ==========================================
    // 2. RULE TREE & NESTED BOOLEAN AST TESTS
    // ==========================================

    @Test
    fun `test RuleTree evaluates nested AND and OR expressions accurately`() {
        // Construct AST: (1080p AND HEVC) OR (720p AND Size < 1.5GB)
        val ast = OrNode(
            children = listOf(
                AndNode(
                    children = listOf(
                        ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R1080p)),
                        ConditionNode(AdvancedRuleCondition.CodecIs(VideoCodec.HEVC))
                    )
                ),
                AndNode(
                    children = listOf(
                        ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R720p)),
                        ConditionNode(AdvancedRuleCondition.SizeLessThan(ByteSize.fromGigabytes(1.5)))
                    )
                )
            )
        )

        val matchCandidate1080p = CandidateReleaseContext(
            title = "One Piece - 1000 [1080p][HEVC]",
            animeTitle = "One Piece",
            resolution = Resolution.R1080p,
            codec = VideoCodec.HEVC,
            sizeBytes = 2_000_000_000L
        )

        val matchCandidate720p = CandidateReleaseContext(
            title = "One Piece - 1000 [720p][AVC]",
            animeTitle = "One Piece",
            resolution = Resolution.R720p,
            codec = VideoCodec.AVC,
            sizeBytes = 800_000_000L // 800 MB < 1.5 GB
        )

        val nonMatchCandidate = CandidateReleaseContext(
            title = "One Piece - 1000 [720p][AVC] Large",
            animeTitle = "One Piece",
            resolution = Resolution.R720p,
            codec = VideoCodec.AVC,
            sizeBytes = 2_000_000_000L // 2 GB > 1.5 GB
        )

        assertTrue("1080p HEVC should match the first OR branch", ruleTreeEvaluator.evaluateNode(ast, matchCandidate1080p))
        assertTrue("720p with small size should match second OR branch", ruleTreeEvaluator.evaluateNode(ast, matchCandidate720p))
        assertFalse("720p with large size should not match any branch", ruleTreeEvaluator.evaluateNode(ast, nonMatchCandidate))
    }

    @Test
    fun `test RuleTree respects Precedence Priority when multiple rules match`() {
        val specificRule = AdvancedRule(
            id = RuleId("rule-anime-specific"),
            name = "One Piece Erai-raws Priority Rule",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.QueueDownload),
            precedence = RulePrecedenceLevel.SpecificScopedRule,
            customPriority = 70
        )

        val generalRule = AdvancedRule(
            id = RuleId("rule-general"),
            name = "General Global Fallback Rule",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("One Piece")),
            actions = listOf(AdvancedRuleAction.Notify("Global fallback")),
            precedence = RulePrecedenceLevel.GeneralRule,
            customPriority = 50
        )

        val candidate = CandidateReleaseContext(
            title = "One Piece - 1000",
            animeTitle = "One Piece"
        )

        val evalResult = ruleTreeEvaluator.evaluateAll(listOf(generalRule, specificRule), candidate)

        assertTrue(evalResult.isMatched)
        assertEquals("Specific Scoped Rule (priority 70) must win over General Rule (priority 50)", specificRule, evalResult.winningRule)
        assertTrue(evalResult.appliedActions.contains(AdvancedRuleAction.QueueDownload))
    }

    // ==========================================
    // 3. AUTOMATION SIMULATION & SAFETY GATES
    // ==========================================

    @Test
    fun `test Automation dry run simulation detects eligible candidates and estimates batch size`() {
        val rule = AdvancedRule(
            id = RuleId("rule-1080p"),
            name = "Auto-Download 1080p",
            root = ConditionNode(AdvancedRuleCondition.ResolutionIs(Resolution.R1080p)),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidates = listOf(
            CandidateReleaseContext(
                title = "Release A (1080p)",
                animeTitle = "Naruto",
                resolution = Resolution.R1080p,
                sizeBytes = 1_500_000_000L
            ),
            CandidateReleaseContext(
                title = "Release B (720p)",
                animeTitle = "Naruto",
                resolution = Resolution.R720p,
                sizeBytes = 800_000_000L
            )
        )

        val simulation = automationEngine.simulate(
            trigger = AutomationTrigger.Manual,
            candidates = candidates,
            rules = listOf(rule)
        )

        assertEquals(1, simulation.selectedReleases.size)
        assertEquals("Release A (1080p)", simulation.selectedReleases.first())
        assertEquals(1, simulation.rejectedReleases.size)
        assertEquals(1_500_000_000L, simulation.estimatedSizeBytes)
        assertFalse("Size under 10GB should not require confirmation", simulation.requiresConfirmation)
    }

    @Test
    fun `test Automation Safety Gate blocks execution when target storage is critically low`() {
        val rule = AdvancedRule(
            id = RuleId("rule-auto"),
            name = "Auto Rule",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("Bleach")),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate = CandidateReleaseContext(
            title = "Bleach - 01",
            animeTitle = "Bleach",
            episodeNumber = 1.0,
            sizeBytes = 1_000_000_000L
        )

        // Storage space is only 100 MB (< 500 MB safety threshold)
        val plan = automationEngine.evaluateTrigger(
            trigger = AutomationTrigger.NewReleaseDetected("Bleach"),
            candidates = listOf(candidate),
            rules = listOf(rule),
            storageFreeBytes = 100L * 1024 * 1024
        )

        assertTrue("Execution must be blocked when storage is critically low", plan.isBlockedBySafety)
        assertEquals(AutomationSafetyLevel.AlwaysConfirm, plan.safetyLevel)
        assertEquals("BLOCKED_BY_SAFETY", plan.auditLog.outcome)
    }

    @Test
    fun `test Automation Idempotency prevents creating duplicate tasks for same episode`() {
        val rule = AdvancedRule(
            id = RuleId("rule-auto"),
            name = "Auto Rule",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("Attack on Titan")),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate = CandidateReleaseContext(
            title = "Attack on Titan - 05",
            animeTitle = "Attack on Titan",
            episodeNumber = 5.0
        )

        // First execution runs successfully
        val plan1 = automationEngine.evaluateTrigger(
            trigger = AutomationTrigger.NewReleaseDetected("Attack on Titan"),
            candidates = listOf(candidate),
            rules = listOf(rule)
        )
        assertEquals(1, plan1.selectedReleases.size)

        // Repeated trigger with identical candidate must be skipped by idempotency lock
        val plan2 = automationEngine.evaluateTrigger(
            trigger = AutomationTrigger.NewReleaseDetected("Attack on Titan"),
            candidates = listOf(candidate),
            rules = listOf(rule)
        )
        assertEquals(0, plan2.selectedReleases.size)
        assertEquals("NO_ACTION", plan2.auditLog.outcome)
    }

    // ==========================================
    // 4. CONFIGURATION IMPORT / EXPORT & SCHEMA
    // ==========================================

    @Test
    fun `test ConfigBackupService export and import with conflict resolution policies`() {
        val profiles = listOf(ProfileBackupDto("prof-1", "1080p Profile", "1080p", "HEVC"))
        val rules = listOf(RuleBackupDto("rule-1", "Auto 1080p", 70))

        val exported = backupService.exportConfiguration(profiles, rules)
        assertEquals(1, exported.schemaVersion)
        assertEquals("AniFlow", exported.app)

        // Import when no existing conflict exists
        val reportMerge = backupService.importConfiguration(
            bundle = exported,
            existingProfileIds = emptySet(),
            existingRuleIds = emptySet(),
            policy = ImportConflictPolicy.Merge
        )
        assertTrue(reportMerge.isSuccess)
        assertEquals(1, reportMerge.importedProfilesCount)
        assertEquals(0, reportMerge.conflictsDetected)

        // Import with conflict and Skip policy
        val reportSkip = backupService.importConfiguration(
            bundle = exported,
            existingProfileIds = setOf("prof-1"),
            existingRuleIds = setOf("rule-1"),
            policy = ImportConflictPolicy.Skip
        )
        assertTrue(reportSkip.isSuccess)
        assertEquals(0, reportSkip.importedProfilesCount)
        assertEquals(2, reportSkip.conflictsDetected)
    }

    @Test
    fun `test ConfigBackupService rejects future schema versions safely`() {
        val futureBundle = ExportedConfigBundle(
            schemaVersion = 99,
            app = "AniFlow"
        )

        val report = backupService.importConfiguration(
            bundle = futureBundle,
            existingProfileIds = emptySet(),
            existingRuleIds = emptySet()
        )

        assertFalse("Future schema versions must not be imported blindly", report.isSuccess)
        assertNotNull(report.errorMessage)
        assertTrue(report.errorMessage!!.contains("Unsupported schema version 99"))
    }

    // ==========================================
    // 5. SEARCH EXPRESSION AST COMPILATION
    // ==========================================

    @Test
    fun `test SearchExpression AST compiles into explainable query string`() {
        val expression = SearchExpression(
            root = AndExpression(
                listOf(
                    ComparisonExpression(SearchField.Anime, ComparisonOperator.Equals, "One Piece"),
                    ComparisonExpression(SearchField.Resolution, ComparisonOperator.GreaterThanOrEqual, "1080p"),
                    ComparisonExpression(SearchField.Codec, ComparisonOperator.Equals, "HEVC")
                )
            )
        )

        val queryStr = expression.toQueryString()
        assertTrue(queryStr.contains("Anime = \"One Piece\""))
        assertTrue(queryStr.contains("Resolution >= \"1080p\""))
        assertTrue(queryStr.contains("Codec = \"HEVC\""))
        assertTrue(queryStr.contains("AND"))
    }
}
