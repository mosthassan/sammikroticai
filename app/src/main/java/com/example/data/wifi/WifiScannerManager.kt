package com.example.data.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiScannerManager(private val context: Context) {

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /**
     * Checks whether mandatory runtime location/nearby permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocationGranted || nearbyGranted
    }

    /**
     * Checks if WiFi radio is currently active on device
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager?.isWifiEnabled == true
    }

    /**
     * Scans for WiFi Access Points and returns an explicit ScanState.
     * Strictly uses real hardware scan results with zero mock or fake telemetry.
     */
    fun scan(): ScanState {
        if (!hasRequiredPermissions()) {
            return ScanState.NoPermission
        }

        val wm = wifiManager ?: return ScanState.Error("خدمة فحص الواي فاي غير متوفرة على هذا الجهاز")

        if (!wm.isWifiEnabled) {
            return ScanState.WifiDisabled
        }

        return try {
            val rawResults: List<ScanResult> = wm.scanResults ?: emptyList()
            if (rawResults.isEmpty()) {
                ScanState.Empty
            } else {
                val mapped = mapScanResults(rawResults)
                if (mapped.isEmpty()) ScanState.Empty else ScanState.Ready(mapped)
            }
        } catch (e: SecurityException) {
            ScanState.NoPermission
        } catch (e: Exception) {
            ScanState.Error(e.message ?: "فشل فحص الترددات اللاسلكية")
        }
    }

    /**
     * Performs a scan trigger and returns raw Access Points.
     */
    fun getScannedAccessPoints(): List<WifiAccessPoint> {
        return when (val state = scan()) {
            is ScanState.Ready -> state.accessPoints
            else -> emptyList()
        }
    }

    /**
     * Triggers active hardware scan if supported
     */
    fun triggerHardwareScan(): Boolean {
        return try {
            wifiManager?.startScan() ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Listens to hardware WiFi scan updates via BroadcastReceiver emitting ScanState
     */
    fun observeScanState(): Flow<ScanState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(scan())
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)

        // Send initial state immediately
        trySend(scan())

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    /**
     * Listens to hardware WiFi scan updates via BroadcastReceiver
     */
    fun observeScanResults(): Flow<List<WifiAccessPoint>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val aps = getScannedAccessPoints()
                trySend(aps)
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)

        // Send initial state immediately
        trySend(getScannedAccessPoints())

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    private fun mapScanResults(results: List<ScanResult>): List<WifiAccessPoint> {
        return results.map { result ->
            val freq = result.frequency
            val band = WifiFrequencyHelper.frequencyToBand(freq)
            val channel = WifiFrequencyHelper.frequencyToChannel(freq)
            val channelWidth = WifiFrequencyHelper.getChannelWidthMhz(result)
            val security = WifiFrequencyHelper.parseSecurityType(result.capabilities ?: "")

            WifiAccessPoint(
                bssid = result.BSSID ?: "00:00:00:00:00:00",
                ssid = result.SSID ?: "",
                frequencyMhz = freq,
                channel = channel,
                channelWidthMhz = channelWidth,
                rssiDbm = result.level,
                securityType = security,
                band = band,
                capabilities = result.capabilities ?: "",
                timestamp = System.currentTimeMillis()
            )
        }.sortedByDescending { it.rssiDbm }
    }
}
