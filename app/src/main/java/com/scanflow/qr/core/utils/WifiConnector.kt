package com.scanflow.qr.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.Looper

enum class WifiConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}

object WifiConnector {

    private var activeCallback: ConnectivityManager.NetworkCallback? = null
    private var activeConnectivityManager: ConnectivityManager? = null

    /**
     * Memulai proses penyambungan otomatis ke jaringan Wi-Fi.
     * Menggunakan WifiNetworkSpecifier pada Android 10+ (API 29+)
     * dan WifiConfiguration fallback pada Android 9 kebawah (Pre-Q).
     */
    fun connectToWifi(
        context: Context,
        ssid: String,
        password: String?,
        securityType: String = "WPA",
        isHidden: Boolean = false,
        onStatusChange: (status: WifiConnectionStatus, message: String) -> Unit
    ) {
        cancelCurrentConnection()

        if (ssid.isBlank()) {
            onStatusChange(WifiConnectionStatus.FAILED, "Nama jaringan (SSID) tidak valid.")
            return
        }

        onStatusChange(WifiConnectionStatus.CONNECTING, "Menghubungkan ke Wi-Fi \"$ssid\"...")

        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        if (connectivityManager == null || wifiManager == null) {
            onStatusChange(WifiConnectionStatus.FAILED, "Layanan Wi-Fi sistem tidak tersedia.")
            return
        }

        // Pastikan Wi-Fi aktif pada perangkat Pre-Q
        @Suppress("DEPRECATION")
        if (!wifiManager.isWifiEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                wifiManager.isWifiEnabled = true
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectOnAndroidQAndAbove(
                connectivityManager = connectivityManager,
                wifiManager = wifiManager,
                ssid = ssid,
                password = password,
                securityType = securityType,
                isHidden = isHidden,
                onStatusChange = onStatusChange
            )
        } else {
            connectPreAndroidQ(
                wifiManager = wifiManager,
                ssid = ssid,
                password = password,
                securityType = securityType,
                onStatusChange = onStatusChange
            )
        }
    }

    private fun connectOnAndroidQAndAbove(
        connectivityManager: ConnectivityManager,
        wifiManager: WifiManager,
        ssid: String,
        password: String?,
        securityType: String,
        isHidden: Boolean,
        onStatusChange: (status: WifiConnectionStatus, message: String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        try {
            // Daftarkan saran jaringan (WifiNetworkSuggestion) agar tersimpan di OS
            try {
                val suggestionBuilder = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsHiddenSsid(isHidden)

                val normalizedSec = securityType.uppercase()
                if (!password.isNullOrEmpty()) {
                    if (normalizedSec.contains("WPA3")) {
                        suggestionBuilder.setWpa3Passphrase(password)
                    } else {
                        suggestionBuilder.setWpa2Passphrase(password)
                    }
                }
                wifiManager.addNetworkSuggestions(listOf(suggestionBuilder.build()))
            } catch (_: Exception) {}

            // Gunakan WifiNetworkSpecifier untuk memicu dialog koneksi langsung bawaan Android
            val specifierBuilder = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setIsHiddenSsid(isHidden)

            val normalizedSec = securityType.uppercase()
            if (!password.isNullOrEmpty()) {
                if (normalizedSec.contains("WPA3")) {
                    specifierBuilder.setWpa3Passphrase(password)
                } else {
                    specifierBuilder.setWpa2Passphrase(password)
                }
            }

            val specifier = specifierBuilder.build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    mainHandler.post {
                        try {
                            connectivityManager.bindProcessToNetwork(network)
                        } catch (_: Exception) {}
                        onStatusChange(
                            WifiConnectionStatus.CONNECTED,
                            "Berhasil terhubung ke Wi-Fi \"$ssid\""
                        )
                    }
                }

                override fun onUnavailable() {
                    mainHandler.post {
                        onStatusChange(
                            WifiConnectionStatus.FAILED,
                            "Permintaan koneksi Wi-Fi dibatalkan atau waktu habis."
                        )
                    }
                }

                override fun onLost(network: Network) {
                    mainHandler.post {
                        onStatusChange(
                            WifiConnectionStatus.IDLE,
                            "Koneksi Wi-Fi terputus."
                        )
                    }
                }
            }

            activeCallback = callback
            activeConnectivityManager = connectivityManager
            connectivityManager.requestNetwork(request, callback, 30000)

        } catch (e: Exception) {
            mainHandler.post {
                onStatusChange(
                    WifiConnectionStatus.FAILED,
                    "Gagal memulai koneksi: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun connectPreAndroidQ(
        wifiManager: WifiManager,
        ssid: String,
        password: String?,
        securityType: String,
        onStatusChange: (status: WifiConnectionStatus, message: String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        try {
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                val normalizedSec = securityType.uppercase()
                when {
                    password.isNullOrEmpty() || normalizedSec.contains("OPEN") || normalizedSec.contains("NONE") -> {
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    }
                    normalizedSec.contains("WEP") -> {
                        wepKeys[0] = "\"$password\""
                        wepTxKeyIndex = 0
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    }
                    else -> {
                        preSharedKey = "\"$password\""
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    }
                }
            }

            var netId = wifiManager.addNetwork(wifiConfig)
            if (netId == -1) {
                // Jika jaringan sudah terdaftar, ambil ID yang sudah ada
                val configured = wifiManager.configuredNetworks
                val existing = configured?.find { it.SSID == "\"$ssid\"" }
                if (existing != null) {
                    netId = existing.networkId
                }
            }

            if (netId != -1) {
                wifiManager.disconnect()
                val success = wifiManager.enableNetwork(netId, true)
                wifiManager.reconnect()

                if (success) {
                    mainHandler.post {
                        onStatusChange(
                            WifiConnectionStatus.CONNECTED,
                            "Wi-Fi \"$ssid\" berhasil dikonfigurasi dan diaktifkan."
                        )
                    }
                } else {
                    mainHandler.post {
                        onStatusChange(
                            WifiConnectionStatus.FAILED,
                            "Gagal mengaktifkan jaringan Wi-Fi di sistem."
                        )
                    }
                }
            } else {
                mainHandler.post {
                    onStatusChange(
                        WifiConnectionStatus.FAILED,
                        "Gagal menyimpan profil Wi-Fi pada perangkat."
                    )
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                onStatusChange(
                    WifiConnectionStatus.FAILED,
                    "Kesalahan koneksi Wi-Fi: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Membatalkan network callback aktif dan membersihkan referensi.
     */
    fun cancelCurrentConnection() {
        try {
            val cb = activeCallback
            val cm = activeConnectivityManager
            if (cb != null && cm != null) {
                cm.unregisterNetworkCallback(cb)
            }
        } catch (_: Exception) {}
        activeCallback = null
        activeConnectivityManager = null
    }
}
