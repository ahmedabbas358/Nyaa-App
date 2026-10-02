package com.aniflow.download.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStrategySelectorTest {

    @Test
    fun api34_selectsUserInitiatedDataTransferForUserDownloads() {
        val selector = RuntimeStrategySelector(apiLevel = 34)
        val requirements = RuntimeTaskRequirements(
            isUserInitiated = true,
            isLongRunningDataTransfer = true
        )

        val strategy = selector.selectStrategy(requirements)
        assertEquals(AndroidExecutionMechanism.UserInitiatedDataTransfer, strategy)
    }

    @Test
    fun api33_selectsForegroundServiceForDownloads() {
        val selector = RuntimeStrategySelector(apiLevel = 33)
        val requirements = RuntimeTaskRequirements(
            isUserInitiated = true,
            isLongRunningDataTransfer = true
        )

        val strategy = selector.selectStrategy(requirements)
        assertEquals(AndroidExecutionMechanism.ForegroundService, strategy)
    }

    @Test
    fun deferredOrCleanup_selectsWorkManagerAcrossAllApis() {
        val selector34 = RuntimeStrategySelector(apiLevel = 34)
        val requirements = RuntimeTaskRequirements(
            isDeferredOrCleanup = true
        )

        val strategy = selector34.selectStrategy(requirements)
        assertEquals(AndroidExecutionMechanism.WorkManagerDeferred, strategy)
    }
}
