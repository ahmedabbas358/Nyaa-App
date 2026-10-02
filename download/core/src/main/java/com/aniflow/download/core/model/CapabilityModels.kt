package com.aniflow.download.core.model

enum class NetworkType {
    Wifi,
    Cellular,
    Ethernet,
    Unavailable
}

data class RuntimeCapabilities(
    val supportsHttp: Boolean = true,
    val supportsTorrent: Boolean = true,
    val supportsUidt: Boolean = false, // Android 14+
    val supportsFgs: Boolean = true,
    val supportsWorkManager: Boolean = true,
    val availableStorageBytes: Long = Long.MAX_VALUE,
    val networkType: NetworkType = NetworkType.Wifi,
    val isMetered: Boolean = false
) {
    val isNetworkAvailable: Boolean
        get() = networkType != NetworkType.Unavailable
}

data class DownloadEngineCapabilities(
    val engineName: String,
    val supportsRangeRequests: Boolean = true,
    val supportsMultiSegment: Boolean = true,
    val supportsPauseResume: Boolean = true,
    val supportsSpeedLimit: Boolean = true,
    val supportsUploadLimit: Boolean = false,
    val supportsFileSelection: Boolean = false,
    val maxSegments: Int = 8
)
