package com.localdownloader.downloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.localdownloader.domain.models.DownloadNetworkMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

data class DownloadNetworkStatus(
    val isConnected: Boolean,
    val isUnmetered: Boolean,
    val hasWifiLikeTransport: Boolean,
) {
    fun matches(mode: DownloadNetworkMode): Boolean {
        return when (mode) {
            DownloadNetworkMode.ANY -> isConnected
            DownloadNetworkMode.WIFI_ONLY -> isConnected && hasWifiLikeTransport
            DownloadNetworkMode.UNMETERED -> isConnected && isUnmetered
        }
    }

    companion object {
        val Disconnected = DownloadNetworkStatus(
            isConnected = false,
            isUnmetered = false,
            hasWifiLikeTransport = false,
        )
    }
}

@Singleton
class DownloadNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentStatus(): DownloadNetworkStatus {
        val network = connectivityManager.activeNetwork ?: return DownloadNetworkStatus.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return DownloadNetworkStatus.Disconnected
        val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val wifiLikeTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        return DownloadNetworkStatus(
            isConnected = connected,
            isUnmetered = unmetered,
            hasWifiLikeTransport = wifiLikeTransport,
        )
    }

    fun observeStatus(): Flow<DownloadNetworkStatus> {
        return callbackFlow {
            trySend(currentStatus())

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(currentStatus())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(currentStatus())
                }

                override fun onLost(network: Network) {
                    trySend(currentStatus())
                }
            }

            runCatching {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    callback,
                )
            }.onFailure {
                trySend(currentStatus())
                close(it)
            }

            awaitClose {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }
            .conflate()
            .distinctUntilChanged()
    }
}
