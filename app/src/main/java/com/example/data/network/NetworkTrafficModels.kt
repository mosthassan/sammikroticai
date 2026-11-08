package com.example.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class NetworkInterfaceTraffic(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val displayName: String,
    val isWan: Boolean = false,
    val isRunning: Boolean = true,
    val rxMbps: Double = 1.24, // Download Speed
    val txMbps: Double = 9.20, // Upload Speed
    val totalRxGb: Double = 168.67, // Total Received Data (GB)
    val totalTxGb: Double = 1415.17, // Total Transmitted Data (GB)
    val rxHistory: List<Float> = listOf(0.2f, 0.8f, 1.4f, 4.0f, 0.1f, 1.8f, 2.6f, 1.24f),
    val txHistory: List<Float> = listOf(4.2f, 12.0f, 41.0f, 16.5f, 9.8f, 22.4f, 14.1f, 9.20f)
)

data class CardAndQuotaTelemetry(
    val totalGeneratedCards: Int = 7690,
    val soldActiveCards: Int = 1850,
    val remainingCards: Int = 5420,
    val expiredCards: Int = 420, // عدد الكروت المنتهية الصلاحية
    val totalQuotaMb: Long = 2_000_000L, // 2,000,000 MB (1,953.1 GB)
    val consumedQuotaMb: Long = 1_583_840L, // 1,583,840 MB (1,546.7 GB)
    val remainingQuotaMb: Long = 416_160L, // 416,160 MB (406.4 GB)
    val totalSalesYemenRial: Long = 1_420_000L,
    val onlineUsersNow: Int = 53,
    val broadbandUsers: String = "0 / 0",
    val hotspotUsers: Int = 7691,
    val userManagerUsers: Int = 0
) {
    val consumedGb: Double get() = consumedQuotaMb / 1024.0
    val remainingGb: Double get() = remainingQuotaMb / 1024.0
    val totalGb: Double get() = totalQuotaMb / 1024.0
    val consumedPercent: Int get() = ((consumedQuotaMb.toDouble() / totalQuotaMb.toDouble()) * 100).coerceIn(0.0, 100.0).toInt()
}

/**
 * Live Traffic Monitor Manager:
 * Handles real-time traffic polling, interface switching, and smooth waveform generation.
 */
class LiveTrafficManager(private val scope: CoroutineScope) {

    private val defaultInterfaces = listOf(
        NetworkInterfaceTraffic(
            id = "iface-bridge-lan",
            name = "bridge-LAN",
            displayName = "bridge-LAN:WAN",
            isWan = true,
            rxMbps = 1.24,
            txMbps = 9.20,
            totalRxGb = 168.67,
            totalTxGb = 1415.17
        ),
        NetworkInterfaceTraffic(
            id = "iface-ether1-wan",
            name = "ether1-WAN",
            displayName = "ether1-WAN (Starlink / Fiber)",
            isWan = true,
            rxMbps = 24.8,
            txMbps = 18.5,
            totalRxGb = 890.40,
            totalTxGb = 2340.10
        ),
        NetworkInterfaceTraffic(
            id = "iface-ether2-lan",
            name = "ether2-LAN",
            displayName = "ether2-LAN (Distribution)",
            isWan = false,
            rxMbps = 16.2,
            txMbps = 4.1,
            totalRxGb = 640.15,
            totalTxGb = 320.50
        ),
        NetworkInterfaceTraffic(
            id = "iface-ether3-sector",
            name = "ether3-Sector1",
            displayName = "ether3-AP-Sector-East",
            isWan = false,
            rxMbps = 8.5,
            txMbps = 2.4,
            totalRxGb = 245.80,
            totalTxGb = 110.20
        ),
        NetworkInterfaceTraffic(
            id = "iface-wlan1",
            name = "wlan1-5G",
            displayName = "wlan1 (5GHz Omni Hotspot)",
            isWan = false,
            rxMbps = 5.4,
            txMbps = 1.8,
            totalRxGb = 135.50,
            totalTxGb = 68.30
        )
    )

    private val _interfaces = MutableStateFlow<List<NetworkInterfaceTraffic>>(defaultInterfaces)
    val interfaces: StateFlow<List<NetworkInterfaceTraffic>> = _interfaces.asStateFlow()

    private val _selectedInterface = MutableStateFlow<NetworkInterfaceTraffic>(defaultInterfaces.first())
    val selectedInterface: StateFlow<NetworkInterfaceTraffic> = _selectedInterface.asStateFlow()

    private val _cardQuota = MutableStateFlow(CardAndQuotaTelemetry())
    val cardQuota: StateFlow<CardAndQuotaTelemetry> = _cardQuota.asStateFlow()

    private var trafficPollJob: Job? = null

    init {
        startLiveTrafficSimulation()
    }

    fun selectInterface(interfaceId: String) {
        val found = _interfaces.value.find { it.id == interfaceId } ?: return
        _selectedInterface.value = found
    }

    private fun startLiveTrafficSimulation() {
        trafficPollJob?.cancel()
        trafficPollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1200L) // Poll every 1.2s for live realistic waveforms
                val current = _selectedInterface.value
                val jitterRx = (Random.nextDouble(-0.35, 0.45)).coerceIn(-current.rxMbps * 0.4, 3.0)
                val jitterTx = (Random.nextDouble(-0.8, 1.2)).coerceIn(-current.txMbps * 0.3, 5.0)

                val newRx = (current.rxMbps + jitterRx).coerceIn(0.2, 50.0)
                val newTx = (current.txMbps + jitterTx).coerceIn(0.5, 95.0)

                val newRxHistory = (current.rxHistory.drop(1) + newRx.toFloat()).takeLast(10)
                val newTxHistory = (current.txHistory.drop(1) + newTx.toFloat()).takeLast(10)

                val updatedIface = current.copy(
                    rxMbps = kotlin.math.round(newRx * 100) / 100.0,
                    txMbps = kotlin.math.round(newTx * 100) / 100.0,
                    totalRxGb = kotlin.math.round((current.totalRxGb + 0.0002) * 100) / 100.0,
                    totalTxGb = kotlin.math.round((current.totalTxGb + 0.0011) * 100) / 100.0,
                    rxHistory = newRxHistory,
                    txHistory = newTxHistory
                )

                _selectedInterface.value = updatedIface
                _interfaces.value = _interfaces.value.map { if (it.id == updatedIface.id) updatedIface else it }
            }
        }
    }
}
