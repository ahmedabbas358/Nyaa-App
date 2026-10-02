package com.aniflow.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface NetworkStatus {
    data object Unavailable : NetworkStatus
    data class Connected(
        val isWifi: Boolean,
        val isCellular: Boolean,
        val isMetered: Boolean
    ) : NetworkStatus
}

interface NetworkMonitor {
    val networkStatus: Flow<NetworkStatus>
    fun isConnected(): Boolean
    fun isWifi(): Boolean
    fun isMetered(): Boolean
}

class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentStatus())
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getCurrentStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(getCurrentStatus())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    override fun isConnected(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun isWifi(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun isMetered(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    private fun getCurrentStatus(): NetworkStatus {
        val active = connectivityManager.activeNetwork ?: return NetworkStatus.Unavailable
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return NetworkStatus.Unavailable

        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            NetworkStatus.Connected(
                isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                isMetered = connectivityManager.isActiveNetworkMetered
            )
        } else {
            NetworkStatus.Unavailable
        }
    }
}
