package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkStatus(
  val isOnline: Boolean = true,
  val isUnmeteredWifi: Boolean = true,
)

class NetworkMonitor(context: Context) {
  private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _status = MutableStateFlow(readCurrentStatus())
  val status: StateFlow<NetworkStatus> = _status.asStateFlow()

  private val callback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        _status.value = readCurrentStatus()
      }

      override fun onLost(network: Network) {
        _status.value = readCurrentStatus()
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        _status.value = readCurrentStatus()
      }
    }

  init {
    try {
      cm.registerDefaultNetworkCallback(callback)
    } catch (_: Exception) {
      _status.value = readCurrentStatus()
    }
  }

  fun unregister() {
    try {
      cm.unregisterNetworkCallback(callback)
    } catch (_: Exception) {
    }
  }

  private fun readCurrentStatus(): NetworkStatus {
    return try {
      val active = cm.activeNetwork ?: return NetworkStatus(isOnline = false, isUnmeteredWifi = false)
      val caps = cm.getNetworkCapabilities(active) ?: return NetworkStatus(isOnline = false, isUnmeteredWifi = false)
      val hasInternet =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      val isWifi =
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
      NetworkStatus(isOnline = hasInternet, isUnmeteredWifi = isWifi)
    } catch (_: Exception) {
      NetworkStatus(isOnline = true, isUnmeteredWifi = true)
    }
  }
}
