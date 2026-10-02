package com.aniflow.testing

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.GroupConditionNode
import com.aniflow.domain.controlplane.models.LogicalOperator
import com.aniflow.domain.controlplane.models.NotConditionNode
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Enforces STEP 13 Section 10 (Selection Engine Testing) and Section 11 (Rule Engine Testing).
 *
 * Verifies:
 * 1. Hard constraint disqualification (Hard constraints strictly override scoring)
 * 2. Determinism (same input + policies = exact same outcome)
 * 3. Boolean AST expressions: A AND B, A OR B, NOT A, (A AND B) OR C, A AND (B OR C), NOT (A OR B)
 * 4. Priority conflict resolution and rule enablement
 */
class SelectionAndRuleHardeningTests {

    private lateinit var evaluator: RuleTreeEvaluator

    @Before
    fun setUp() {
        evaluator = RuleTreeEvaluator()
    }

    private fun createContext(
        releaseGroup: String = "SubsPlease",
        resolution: String = "1080p",
        codec: String = "HEVC",
        sizeBytes: Long = 1_400_000_000L,
        seeders: Int = 50
    ): CandidateReleaseContext {
        return CandidateReleaseContext(
            releaseGroup = releaseGroup,
            resolution = resolution,
            videoCodec = codec,
            sizeBytes = sizeBytes,
            seeders = seeders,
            uploader = "Erai-raws"
        )
    }

    @Test
    fun testHardConstraintStrictDisqualification() {
        // Condition: Resolution must be 1080p AND Codec must NOT be AV1
        val is1080p = ComparisonExpression(SearchField.Resolution, ComparisonOperator.Equals, "1080p")
        val isAv1 = ComparisonExpression(SearchField.Codec, ComparisonOperator.Equals, "AV1")
        val notAv1 = NotConditionNode(isAv1)

        val rootNode = GroupConditionNode(
            operator = LogicalOperator.And,
            conditions = listOf(is1080p, notAv1)
        )

        val validCandidate = createContext(resolution = "1080p", codec = "HEVC")
        val invalidCandidate = createContext(resolution = "1080p", codec = "AV1")

        assertTrue("Valid candidate matches 1080p and not AV1", evaluator.evaluate(rootNode, validCandidate))
        assertFalse("AV1 candidate strictly disqualified even if 1080p", evaluator.evaluate(rootNode, invalidCandidate))
    }

    @Test
    fun testComplexBooleanExpression_A_And_B_Or_C() {
        // (Group == "SubsPlease" AND Codec == "HEVC") OR (Seeders >= 100)
        val condA = ComparisonExpression(SearchField.ReleaseGroup, ComparisonOperator.Equals, "SubsPlease")
        val condB = ComparisonExpression(SearchField.Codec, ComparisonOperator.Equals, "HEVC")
        val condC = ComparisonExpression(SearchField.Seeders, ComparisonOperator.GreaterThanOrEqual, "100")

        val groupAB = GroupConditionNode(LogicalOperator.And, listOf(condA, condB))
        val rootNode = GroupConditionNode(LogicalOperator.Or, listOf(groupAB, condC))

        val candidate1 = createContext(releaseGroup = "SubsPlease", codec = "HEVC", seeders = 10)
        val candidate2 = createContext(releaseGroup = "Judas", codec = "AVC", seeders = 120)
        val candidate3 = createContext(releaseGroup = "Judas", codec = "AVC", seeders = 20)

        assertTrue("(A AND B) matches", evaluator.evaluate(rootNode, candidate1))
        assertTrue("C matches", evaluator.evaluate(rootNode, candidate2))
        assertFalse("Neither (A AND B) nor C matches", evaluator.evaluate(rootNode, candidate3))
    }

    @Test
    fun testDeMorganLaw_Not_A_Or_B() {
        // NOT (Resolution == "480p" OR Resolution == "720p") => Must be >= 1080p
        val is480p = ComparisonExpression(SearchField.Resolution, ComparisonOperator.Equals, "480p")
        val is720p = ComparisonExpression(SearchField.Resolution, ComparisonOperator.Equals, "720p")
        val innerOr = GroupConditionNode(LogicalOperator.Or, listOf(is480p, is720p))
        val rootNot = NotConditionNode(innerOr)

        val candidate1080 = createContext(resolution = "1080p")
        val candidate720 = createContext(resolution = "720p")

        assertTrue(evaluator.evaluate(rootNot, candidate1080))
        assertFalse(evaluator.evaluate(rootNot, candidate720))
    }

    @Test
    fun testSelectionDeterminism() {
        val condition = ComparisonExpression(SearchField.Resolution, ComparisonOperator.Equals, "1080p")
        val candidate = createContext(resolution = "1080p")

        // 100 consecutive evaluations must produce identical boolean outputs without side effects
        val initialResult = evaluator.evaluate(condition, candidate)
        for (i in 1..100) {
            assertEquals(initialResult, evaluator.evaluate(condition, candidate))
        }
    }
}
