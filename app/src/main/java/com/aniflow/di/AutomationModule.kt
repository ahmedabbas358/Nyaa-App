package com.aniflow.di

import com.aniflow.domain.controlplane.service.AutomationEngine
import com.aniflow.domain.controlplane.service.RuleTreeEvaluator
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.usecase.AutomationWorkflowCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AutomationModule (Section 4, 19, 43).
 * Binds the centralized AniFlowEventBus, RuleTreeEvaluator, AutomationEngine,
 * and AutomationWorkflowCoordinator.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutomationModule {

    @Provides
    @Singleton
    fun provideAniFlowEventBus(): AniFlowEventBus = AniFlowEventBus()

    @Provides
    @Singleton
    fun provideRuleTreeEvaluator(): RuleTreeEvaluator = RuleTreeEvaluator()

    @Provides
    @Singleton
    fun provideAutomationEngine(evaluator: RuleTreeEvaluator): AutomationEngine =
        AutomationEngine(ruleEvaluator = evaluator)

    @Provides
    @Singleton
    fun provideAutomationWorkflowCoordinator(
        automationEngine: AutomationEngine,
        downloadRepository: DownloadRepository,
        eventBus: AniFlowEventBus
    ): AutomationWorkflowCoordinator = AutomationWorkflowCoordinator(
        automationEngine = automationEngine,
        downloadRepository = downloadRepository,
        eventBus = eventBus
    )
}
