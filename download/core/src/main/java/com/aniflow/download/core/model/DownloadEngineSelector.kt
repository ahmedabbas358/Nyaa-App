package com.aniflow.download.core.model

/**
 * Maps download source and runtime capabilities to the appropriate download engine (Section 13, 14, 15).
 */
class DownloadEngineSelector {

    fun selectEngine(source: DownloadSource, capabilities: RuntimeCapabilities): DownloadEngineType {
        return when (source) {
            is DownloadSource.HttpSource -> {
                if (!capabilities.supportsHttp) {
                    throw IllegalStateException("HTTP engine is unsupported in this runtime environment")
                }
                DownloadEngineType.Http
            }
            is DownloadSource.MagnetSource, is DownloadSource.TorrentFileSource -> {
                if (!capabilities.supportsTorrent) {
                    throw IllegalStateException("Torrent engine is unsupported in this runtime environment")
                }
                DownloadEngineType.Torrent
            }
            is DownloadSource.DirectFileSource -> {
                DownloadEngineType.DirectFile
            }
        }
    }
}
