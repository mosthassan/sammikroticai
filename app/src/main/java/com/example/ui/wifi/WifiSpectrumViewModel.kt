package com.example.ui.wifi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.wifi.ChannelRecommendation
import com.example.data.wifi.ChannelRecommendationEngine
import com.example.data.wifi.ScanRefreshInterval
import com.example.data.wifi.ScanState
import com.example.data.wifi.SignalHistoryPoint
import com.example.data.wifi.WifiAccessPoint
import com.example.data.wifi.WifiBand
import com.example.data.wifi.WifiScannerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WifiSpectrumUiState(
    val scanState: ScanState = ScanState.Empty,
    val isScanning: Boolean = false,
    val allAccessPoints: List<WifiAccessPoint> = emptyList(),
    val filteredAccessPoints: List<WifiAccessPoint> = emptyList(),
    val selectedBand: WifiBand = WifiBand.BAND_2_4_GHZ,
    val recommendation: ChannelRecommendation? = null,
    val selectedApForMeter: WifiAccessPoint? = null,
    val signalHistory: List<SignalHistoryPoint> = emptyList(),
    val refreshInterval: ScanRefreshInterval = ScanRefreshInterval.SECONDS_5,
    val searchQuery: String = "",
    val hasPermissions: Boolean = false,
    val isWifiEnabled: Boolean = true,
    val lastScanTime: Long = 0L,
    val minRssiDbm: Int = -100,
    val maxRssiDbm: Int = -30,
    val avgRssiDbm: Int = -60,
    val currentConnection: com.example.data.wifi.CurrentWifiConnection = com.example.data.wifi.CurrentWifiConnection(),
    val selectedViewTab: WifiSpectrumViewTab = WifiSpectrumViewTab.CHANNEL_RATING,
    val isScanThrottlingEnabled: Boolean = true
)

enum class WifiSpectrumViewTab(val titleAr: String, val titleEn: String) {
    CHANNEL_RATING("تقييم القنوات", "Channel Rating"),
    CHANNEL_GRAPH("مخطط الطيف", "Channel Graph"),
    TIME_GRAPH("مقياس الإشارة", "Time Graph"),
    ACCESS_POINTS("نقاط البث", "Access Points")
}

class WifiSpectrumViewModel(application: Application) : AndroidViewModel(application) {

    private val scannerManager = WifiScannerManager(application)

    private val _uiState = MutableStateFlow(
        WifiSpectrumUiState(
            hasPermissions = scannerManager.hasRequiredPermissions(),
            isWifiEnabled = scannerManager.isWifiEnabled()
        )
    )
    val uiState: StateFlow<WifiSpectrumUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        checkPermissionsAndScan()
        startAutoRefreshLoop()
    }

    fun checkPermissionsAndScan() {
        val hasPerms = scannerManager.hasRequiredPermissions()
        val wifiEnabled = scannerManager.isWifiEnabled()
        _uiState.update {
            it.copy(
                hasPermissions = hasPerms,
                isWifiEnabled = wifiEnabled
            )
        }
        performScan()
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(hasPermissions = true) }
        performScan()
    }

    fun setSelectedBand(band: WifiBand) {
        _uiState.update { currentState ->
            val recommendation = ChannelRecommendationEngine.evaluateBand(
                currentState.allAccessPoints,
                band
            )
            val filtered = filterAndSortAps(currentState.allAccessPoints, band, currentState.searchQuery)
            val selectedMeterAp = if (currentState.selectedApForMeter?.band != band) {
                filtered.firstOrNull()
            } else {
                currentState.selectedApForMeter
            }

            currentState.copy(
                selectedBand = band,
                recommendation = recommendation,
                filteredAccessPoints = filtered,
                selectedApForMeter = selectedMeterAp
            )
        }
    }

    fun setRefreshInterval(interval: ScanRefreshInterval) {
        _uiState.update { it.copy(refreshInterval = interval) }
        startAutoRefreshLoop()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { currentState ->
            val filtered = filterAndSortAps(currentState.allAccessPoints, currentState.selectedBand, query)
            currentState.copy(
                searchQuery = query,
                filteredAccessPoints = filtered
            )
        }
    }

    fun selectApForMeter(ap: WifiAccessPoint) {
        _uiState.update { currentState ->
            val initialHistory = listOf(SignalHistoryPoint(System.currentTimeMillis(), ap.rssiDbm))
            currentState.copy(
                selectedApForMeter = ap,
                signalHistory = initialHistory,
                minRssiDbm = ap.rssiDbm,
                maxRssiDbm = ap.rssiDbm,
                avgRssiDbm = ap.rssiDbm
            )
        }
    }

    fun performScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            scannerManager.triggerHardwareScan()

            // Small delay for hardware scanner settling
            delay(300)

            val state = scannerManager.scan()
            val hasPerms = scannerManager.hasRequiredPermissions()
            val wifiEnabled = scannerManager.isWifiEnabled()
            val currentBand = _uiState.value.selectedBand

            val rawAps = when (state) {
                is ScanState.Ready -> state.accessPoints
                else -> emptyList()
            }

            val recommendation = if (rawAps.isNotEmpty()) {
                ChannelRecommendationEngine.evaluateBand(rawAps, currentBand)
            } else {
                null
            }
            val filtered = filterAndSortAps(rawAps, currentBand, _uiState.value.searchQuery)

            val currentSelected = _uiState.value.selectedApForMeter
            val updatedSelected = if (currentSelected != null) {
                rawAps.find { it.bssid == currentSelected.bssid } ?: currentSelected
            } else {
                filtered.firstOrNull()
            }

            // Update signal history for the live technician gauge using actual measured RSSI
            val now = System.currentTimeMillis()
            val newHistory = _uiState.value.signalHistory.toMutableList()
            if (updatedSelected != null) {
                newHistory.add(SignalHistoryPoint(now, updatedSelected.rssiDbm))
                while (newHistory.size > 25) {
                    newHistory.removeAt(0)
                }
            }

            val minRssi = newHistory.minOfOrNull { it.rssiDbm } ?: -95
            val maxRssi = newHistory.maxOfOrNull { it.rssiDbm } ?: -30
            val avgRssi = if (newHistory.isNotEmpty()) newHistory.map { it.rssiDbm }.average().toInt() else -60
            val currentConn = scannerManager.getCurrentConnectionInfo()

            _uiState.update {
                it.copy(
                    scanState = state,
                    isScanning = false,
                    hasPermissions = hasPerms,
                    isWifiEnabled = wifiEnabled,
                    allAccessPoints = rawAps,
                    filteredAccessPoints = filtered,
                    recommendation = recommendation,
                    selectedApForMeter = updatedSelected,
                    signalHistory = newHistory,
                    lastScanTime = now,
                    minRssiDbm = minRssi,
                    maxRssiDbm = maxRssi,
                    avgRssiDbm = avgRssi,
                    currentConnection = currentConn
                )
            }
        }
    }

    fun selectViewTab(tab: WifiSpectrumViewTab) {
        _uiState.update { it.copy(selectedViewTab = tab) }
    }

    private fun startAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        val interval = _uiState.value.refreshInterval
        if (interval == ScanRefreshInterval.MANUAL) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(interval.seconds * 1000L)
                performScan()
            }
        }
    }

    private fun filterAndSortAps(
        all: List<WifiAccessPoint>,
        band: WifiBand,
        query: String
    ): List<WifiAccessPoint> {
        return all
            .filter { it.band == band }
            .filter {
                if (query.isBlank()) true
                else it.ssid.contains(query, ignoreCase = true) ||
                        it.bssid.contains(query, ignoreCase = true) ||
                        it.channel.toString() == query.trim()
            }
            .sortedByDescending { it.rssiDbm }
    }
}
