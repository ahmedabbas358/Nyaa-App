package com.aniflow.domain.controlplane.usecase

import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AutomationSimulationResult
import com.aniflow.domain.controlplane.models.AutomationTrigger
import com.aniflow.domain.controlplane.service.AutomationEngine
import com.aniflow.domain.controlplane.service.AutomationPlan
import com.aniflow.domain.controlplane.service.ProfileBackupDto
import com.aniflow.domain.controlplane.service.RuleBackupDto
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
import com.aniflow.domain.controlplane.service.ConfigBackupService
import com.aniflow.domain.controlplane.service.ExportedConfigBundle
import com.aniflow.domain.controlplane.service.ImportConflictPolicy
import com.aniflow.domain.controlplane.service.ImportReport
import com.aniflow.domain.controlplane.service.PreferenceResolver
import com.aniflow.domain.controlplane.service.ResolvedPreferences
import com.aniflow.domain.controlplane.service.RuleTreeEvaluationResult
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.controlplane.service.ScopedPreferenceInput

/**
 * Control Plane Use Cases (Section 38).
 * Decouples presentation from backend repositories and direct algorithmic rules.
 */
class ResolvePreferencesUseCase(
    private val resolver: PreferenceResolver = PreferenceResolver()
) {
    operator fun invoke(input: ScopedPreferenceInput): ResolvedPreferences {
        return resolver.resolve(input)
    }
}

class EvaluateRuleTreeUseCase(
    private val evaluator: RuleTreeEvaluator = RuleTreeEvaluator()
) {
    operator fun invoke(rules: List<AdvancedRule>, context: CandidateReleaseContext): RuleTreeEvaluationResult {
        return evaluator.evaluateAll(rules, context)
    }
}

class RunAutomationSimulationUseCase(
    private val engine: AutomationEngine = AutomationEngine()
) {
    operator fun invoke(
        trigger: AutomationTrigger,
        candidates: List<CandidateReleaseContext>,
        rules: List<AdvancedRule>
    ): AutomationSimulationResult {
        return engine.simulate(trigger, candidates, rules)
    }
}

class ExecuteAutomationUseCase(
    private val engine: AutomationEngine = AutomationEngine()
) {
    operator fun invoke(
        trigger: AutomationTrigger,
        candidates: List<CandidateReleaseContext>,
        rules: List<AdvancedRule>,
        storageFreeBytes: Long = Long.MAX_VALUE
    ): AutomationPlan {
        return engine.evaluateTrigger(trigger, candidates, rules, storageFreeBytes = storageFreeBytes)
    }
}

class ExportConfigUseCase(
    private val service: ConfigBackupService = ConfigBackupService()
) {
    operator fun invoke(profiles: List<ProfileBackupDto>, rules: List<RuleBackupDto>): ExportedConfigBundle {
        return service.exportConfiguration(profiles, rules)
    }
}

class ImportConfigUseCase(
    private val service: ConfigBackupService = ConfigBackupService()
) {
    operator fun invoke(
        bundle: ExportedConfigBundle,
        existingProfiles: Set<String>,
        existingRules: Set<String>,
        policy: ImportConflictPolicy = ImportConflictPolicy.Merge
    ): ImportReport {
        return service.importConfiguration(bundle, existingProfiles, existingRules, policy)
    }
}
