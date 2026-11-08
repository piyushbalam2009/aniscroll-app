package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the validated internet connectivity state of the device.
 */
enum class NetworkConnectionState {
    ONLINE,
    OFFLINE,
    RECONNECTING,
    CONNECTED_AGAIN
}

/**
 * Monitors real, validated internet connectivity using Android [ConnectivityManager]
 * and [NetworkCapabilities.NET_CAPABILITY_INTERNET] + [NetworkCapabilities.NET_CAPABILITY_VALIDATED].
 *
 * Debounces rapid transitions to prevent duplicate navigation events on unstable networks.
 */
class NetworkConnectivityObserver(
    context: Context
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null

    @Volatile
    private var isCallbackRegistered = false

    @Volatile
    private var wasEverOfflineInSession = false

    private val initialOnline = checkValidatedInternetNow()

    private val _connectionState = MutableStateFlow(
        if (initialOnline) NetworkConnectionState.ONLINE else NetworkConnectionState.OFFLINE
    )
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _isOnline = MutableStateFlow(initialOnline)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val validated = isNetworkValidated(network)
            if (validated) {
                scheduleTransitionToOnline()
            } else {
                // Network interface is up, waiting for NET_CAPABILITY_VALIDATED
                if (!_isOnline.value) {
                    _connectionState.value = NetworkConnectionState.RECONNECTING
                }
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (hasInternet && isValidated) {
                scheduleTransitionToOnline()
            } else if (hasInternet && !_isOnline.value) {
                _connectionState.value = NetworkConnectionState.RECONNECTING
            } else if (!checkValidatedInternetNow()) {
                scheduleTransitionToOffline()
            }
        }

        override fun onLost(network: Network) {
            if (!checkValidatedInternetNow()) {
                scheduleTransitionToOffline()
            }
        }

        override fun onUnavailable() {
            scheduleTransitionToOffline()
        }
    }

    init {
        if (!initialOnline) {
            wasEverOfflineInSession = true
        }
        registerCallbackSafely()
    }

    @Synchronized
    private fun registerCallbackSafely() {
        if (isCallbackRegistered || connectivityManager == null) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isCallbackRegistered = true
        } catch (_: Exception) {
            // Fallback to current snapshot if system service denies registration
        }
    }

    /**
     * Checks whether the active network currently has both
     * [NetworkCapabilities.NET_CAPABILITY_INTERNET] and [NetworkCapabilities.NET_CAPABILITY_VALIDATED].
     */
    fun checkValidatedInternetNow(): Boolean {
        val cm = connectivityManager ?: return false
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            isNetworkValidated(activeNetwork)
        } catch (_: Exception) {
            false
        }
    }

    private fun isNetworkValidated(network: Network): Boolean {
        val cm = connectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return evaluateCapabilities(
            hasInternetCapability = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            hasValidatedCapability = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )
    }

    /**
     * Manually re-checks connectivity (e.g., when user taps "Retry Connection" on the offline empty state).
     */
    fun retryConnectionCheck(): Boolean {
        if (!_isOnline.value) {
            _connectionState.value = NetworkConnectionState.RECONNECTING
        }
        val validated = checkValidatedInternetNow()
        if (validated) {
            debounceJob?.cancel()
            _isOnline.value = true
            _connectionState.value = if (wasEverOfflineInSession) {
                NetworkConnectionState.CONNECTED_AGAIN
            } else {
                NetworkConnectionState.ONLINE
            }
        } else {
            wasEverOfflineInSession = true
            _isOnline.value = false
            _connectionState.value = NetworkConnectionState.OFFLINE
        }
        return validated
    }

    /**
     * Acknowledges the "Connected Again" state when the user taps "Back to Home"
     * so normal `ONLINE` state resumes.
     */
    fun acknowledgeReconnected() {
        if (_isOnline.value) {
            _connectionState.value = NetworkConnectionState.ONLINE
        }
    }

    private fun scheduleTransitionToOnline() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(450L)
            if (checkValidatedInternetNow()) {
                _isOnline.value = true
                _connectionState.value = if (wasEverOfflineInSession) {
                    NetworkConnectionState.CONNECTED_AGAIN
                } else {
                    NetworkConnectionState.ONLINE
                }
            }
        }
    }

    private fun scheduleTransitionToOffline() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            // Debounce brief network handoffs (e.g., Wi-Fi to cellular switch)
            delay(900L)
            if (!checkValidatedInternetNow()) {
                wasEverOfflineInSession = true
                _isOnline.value = false
                _connectionState.value = NetworkConnectionState.OFFLINE
            }
        }
    }

    companion object {
        /**
         * Pure function for testing capability validation logic: requires BOTH
         * `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`.
         */
        fun evaluateCapabilities(
            hasInternetCapability: Boolean,
            hasValidatedCapability: Boolean
        ): Boolean {
            return hasInternetCapability && hasValidatedCapability
        }
    }
}
