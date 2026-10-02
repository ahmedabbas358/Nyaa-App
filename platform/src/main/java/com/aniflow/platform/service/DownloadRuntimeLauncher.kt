package com.aniflow.platform.service

import android.content.Context

/**
 * DownloadRuntimeLauncher (Section 30).
 * Platform bridge boundary isolating Android lifecycle, Foreground Service,
 * and Context from Domain and Runtime Coordinators.
 */
interface DownloadRuntimeLauncher {
    fun launchService()
    fun stopService()
}

class AndroidDownloadRuntimeLauncher(
    private val context: Context
) : DownloadRuntimeLauncher {

    override fun launchService() {
        try {
            DownloadForegroundService.start(context)
        } catch (e: Exception) {
            // Log or fallback to work manager if in background
            e.printStackTrace()
        }
    }

    override fun stopService() {
        try {
            DownloadForegroundService.stop(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
