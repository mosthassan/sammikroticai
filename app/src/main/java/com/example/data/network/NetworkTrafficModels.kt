package com.example.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

enum class InterfaceCategory(val labelArabic: String) {
    ALL("الكل"),
    ETHERNET("Ethernet و SFP"),
    BRIDGE("جسور Bridges"),
    VLAN("شبكات VLAN"),
    WIRELESS("لاسلكي WiFi"),
    PPP_VPN("PPPoE و VPN"),
    DYNAMIC("عملاء ديناميكي Dynamic")
}

data class NetworkInterfaceTraffic(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "غير متصل",
    val displayName: String = "في انتظار الاتصال بالراوتر...",
    val category: InterfaceCategory = InterfaceCategory.ETHERNET,
    val isWan: Boolean = false,
    val isRunning: Boolean = false,
    val rxMbps: Double = 0.0,
    val txMbps: Double = 0.0,
    val totalRxGb: Double = 0.0,
    val totalTxGb: Double = 0.0,
    val macAddress: String = "00:00:00:00:00:00",
    val mtu: Int = 1500,
    val rxHistory: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val txHistory: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)
)

data class CardAndQuotaTelemetry(
    val totalGeneratedCards: Int = 0,
    val soldActiveCards: Int = 0,
    val remainingCards: Int = 0,
    val expiredCards: Int = 0,
    val totalQuotaMb: Long = 0L,
    val consumedQuotaMb: Long = 0L,
    val remainingQuotaMb: Long = 0L,
    val totalSalesYemenRial: Long = 0L,
    val onlineUsersNow: Int = 0,
    val broadbandUsers: String = "0 / 0",
    val hotspotUsers: Int = 0,
    val userManagerUsers: Int = 0
) {
    val consumedGb: Double get() = consumedQuotaMb / 1024.0
    val remainingGb: Double get() = remainingQuotaMb / 1024.0
    val totalGb: Double get() = totalQuotaMb / 1024.0
    val consumedPercent: Int get() = if (totalQuotaMb > 0) ((consumedQuotaMb.toDouble() / totalQuotaMb.toDouble()) * 100).coerceIn(0.0, 100.0).toInt() else 0
}

/**
 * Live Traffic Monitor Manager:
 * Handles real-time traffic polling, interface switching, and smooth waveform generation.
 * Generates the complete 114 MikroTik interfaces matching real ISP production environments.
 */
class LiveTrafficManager(private val scope: CoroutineScope) {

    private val fullMikrotikInterfaces: List<NetworkInterfaceTraffic> = buildList {
        // 1. Bridges (5)
        add(
            NetworkInterfaceTraffic(
                id = "iface-bridge-lan",
                name = "bridge-LAN",
                displayName = "bridge-LAN:WAN",
                category = InterfaceCategory.BRIDGE,
                isWan = true,
                rxMbps = 1.24,
                txMbps = 9.20,
                totalRxGb = 168.67,
                totalTxGb = 1415.17,
                macAddress = "B4:A9:4F:0C:AA:01"
            )
        )
        add(
            NetworkInterfaceTraffic(
                id = "iface-bridge-hotspot",
                name = "bridge-Hotspot",
                displayName = "bridge-Hotspot (User Pool)",
                category = InterfaceCategory.BRIDGE,
                isWan = false,
                rxMbps = 14.2,
                txMbps = 4.8,
                totalRxGb = 420.10,
                totalTxGb = 210.50,
                macAddress = "B4:A9:4F:0C:AA:02"
            )
        )
        add(
            NetworkInterfaceTraffic(
                id = "iface-bridge-vlan",
                name = "bridge-VLAN-Trunk",
                displayName = "bridge-VLAN (Distribution Trunk)",
                category = InterfaceCategory.BRIDGE,
                isWan = false,
                rxMbps = 28.5,
                txMbps = 12.1,
                totalRxGb = 812.30,
                totalTxGb = 540.20,
                macAddress = "B4:A9:4F:0C:AA:03"
            )
        )
        add(
            NetworkInterfaceTraffic(
                id = "iface-bridge-wan",
                name = "bridge-WAN-Backup",
                displayName = "bridge-WAN (Starlink / 4G Failover)",
                category = InterfaceCategory.BRIDGE,
                isWan = true,
                rxMbps = 8.1,
                txMbps = 2.9,
                totalRxGb = 112.50,
                totalTxGb = 64.10,
                macAddress = "B4:A9:4F:0C:AA:04"
            )
        )
        add(
            NetworkInterfaceTraffic(
                id = "iface-bridge-loopback",
                name = "lo-MGMT",
                displayName = "lo (Loopback Management 10.255.255.1)",
                category = InterfaceCategory.BRIDGE,
                isWan = false,
                rxMbps = 0.05,
                txMbps = 0.05,
                totalRxGb = 2.10,
                totalTxGb = 2.10,
                macAddress = "00:00:00:00:00:00"
            )
        )

        // 2. Physical Ethernet Ports 1 to 16 (CCR2004 16 ports)
        val ethLabels = listOf(
            "Starlink Main WAN (Gigabit)",
            "Bulk Fiber ISP WAN 2",
            "Core LAN Switch 1",
            "Tower North AP Main",
            "Tower South Sector AP",
            "Tower East Sector AP",
            "Tower West Sector AP",
            "Central Market AP 1",
            "Public Park AP 2",
            "Station Client Link 1",
            "Station Client Link 2",
            "CCTV Surveillance NVR",
            "Billing & Accounting Server",
            "VoIP Gateway PBX",
            "Secondary Distribution Switch",
            "Backup Failover Gateway"
        )
        for (i in 1..16) {
            val label = ethLabels.getOrNull(i - 1) ?: "Port $i"
            add(
                NetworkInterfaceTraffic(
                    id = "iface-ether-$i",
                    name = "ether$i",
                    displayName = "ether$i - $label",
                    category = InterfaceCategory.ETHERNET,
                    isWan = i <= 2,
                    rxMbps = if (i <= 2) 24.5 else 4.2 + (i % 5),
                    txMbps = if (i <= 2) 18.2 else 2.1 + (i % 3),
                    totalRxGb = 150.0 + (i * 45),
                    totalTxGb = 200.0 + (i * 65),
                    macAddress = "B4:A9:4F:0C:AA:%02X".format(i + 10)
                )
            )
        }

        // 3. SFP+ 10-Gigabit Ports (2)
        add(
            NetworkInterfaceTraffic(
                id = "iface-sfp-1",
                name = "sfp-sfpplus1",
                displayName = "sfp-sfpplus1 (10G Core Fiber Backbone)",
                category = InterfaceCategory.ETHERNET,
                isWan = true,
                rxMbps = 82.4,
                txMbps = 45.1,
                totalRxGb = 4210.8,
                totalTxGb = 2890.4,
                macAddress = "B4:A9:4F:0C:AA:30"
            )
        )
        add(
            NetworkInterfaceTraffic(
                id = "iface-sfp-2",
                name = "sfp-sfpplus2",
                displayName = "sfp-sfpplus2 (10G Aggregation Trunk)",
                category = InterfaceCategory.ETHERNET,
                isWan = false,
                rxMbps = 65.2,
                txMbps = 28.7,
                totalRxGb = 3120.5,
                totalTxGb = 1940.2,
                macAddress = "B4:A9:4F:0C:AA:31"
            )
        )

        // 4. VLANs (7)
        val vlans = listOf(
            Pair("vlan10-WAN-Tag", "vlan10 (ISP VLAN ID 10)"),
            Pair("vlan20-Hotspot", "vlan20 (Hotspot Isolated Users)"),
            Pair("vlan30-PPPoE", "vlan30 (PPPoE Subscriber Trunk)"),
            Pair("vlan40-Billing", "vlan40 (Management & Safe Billing)"),
            Pair("vlan50-VoIP", "vlan50 (VoIP SIP Priority QOS)"),
            Pair("vlan60-CCTV", "vlan60 (Security Cameras Network)"),
            Pair("vlan100-Backhaul", "vlan100 (High-Capacity Wireless Link)")
        )
        for (v in vlans) {
            add(
                NetworkInterfaceTraffic(
                    id = "iface-${v.first}",
                    name = v.first,
                    displayName = v.second,
                    category = InterfaceCategory.VLAN,
                    isWan = v.first.contains("WAN"),
                    rxMbps = 8.4,
                    txMbps = 3.6,
                    totalRxGb = 340.2,
                    totalTxGb = 180.5,
                    macAddress = "B4:A9:4F:0C:AA:40"
                )
            )
        }

        // 5. Wireless WLAN (5)
        val wlans = listOf(
            "wlan1 (2.4GHz Omni Master)",
            "wlan2 (5GHz Sector North)",
            "wlan3 (5GHz Sector South)",
            "wlan-Virtual-AP1 (Staff WiFi)",
            "wlan-Mesh-Backhaul (PTP Link)"
        )
        wlans.forEachIndexed { idx, name ->
            add(
                NetworkInterfaceTraffic(
                    id = "iface-wlan-$idx",
                    name = "wlan${idx + 1}",
                    displayName = name,
                    category = InterfaceCategory.WIRELESS,
                    isWan = false,
                    rxMbps = 6.2 + idx,
                    txMbps = 2.1 + (idx * 0.5),
                    totalRxGb = 180.0 + (idx * 40),
                    totalTxGb = 95.0 + (idx * 25),
                    macAddress = "B4:A9:4F:0C:AA:%02X".format(50 + idx)
                )
            )
        }

        // 6. PPPoE & VPN Tunnels (9)
        val pppVpns = listOf(
            Pair("pppoe-out1-Starlink", "pppoe-out1 (Starlink Direct Session)"),
            Pair("pppoe-out2-BulkFiber", "pppoe-out2 (Bulk Fiber Primary)"),
            Pair("pppoe-server-pool", "pppoe-in (PPPoE Server Access Concentrator)"),
            Pair("wg0-CloudVPN", "wg0 (WireGuard Cloud Encrypted Tunnel)"),
            Pair("eoip-tower-north", "eoip-tower1 (EoIP Layer-2 North Tower)"),
            Pair("eoip-tower-south", "eoip-tower2 (EoIP Layer-2 South Tower)"),
            Pair("gre-backup", "gre1 (GRE Tunnel Backup Link)"),
            Pair("ovpn-admin", "ovpn-client (OpenVPN Remote Admin Access)"),
            Pair("bonding1-Trunk", "bonding1 (LACP 802.3ad Fiber Trunk)")
        )
        pppVpns.forEachIndexed { idx, p ->
            add(
                NetworkInterfaceTraffic(
                    id = "iface-ppp-$idx",
                    name = p.first,
                    displayName = p.second,
                    category = InterfaceCategory.PPP_VPN,
                    isWan = idx <= 1,
                    rxMbps = 15.0 + (idx * 2),
                    txMbps = 7.0 + idx,
                    totalRxGb = 420.0 + (idx * 80),
                    totalTxGb = 280.0 + (idx * 50),
                    macAddress = "B4:A9:4F:0C:AA:%02X".format(70 + idx)
                )
            )
        }

        // 7. Dynamic Hotspot & Subscriber Interfaces (70)
        // Total so far: 5 (bridges) + 16 (ether) + 2 (sfp) + 7 (vlan) + 5 (wireless) + 9 (ppp) = 44 interfaces.
        // Adding 70 dynamic client interfaces makes exactly 114 interfaces!
        for (clientIdx in 1..70) {
            val clientName = "<hotspot-user-%02d>".format(clientIdx)
            add(
                NetworkInterfaceTraffic(
                    id = "iface-dyn-$clientIdx",
                    name = clientName,
                    displayName = "$clientName (10.0.1.${10 + clientIdx})",
                    category = InterfaceCategory.DYNAMIC,
                    isWan = false,
                    rxMbps = 0.4 + (clientIdx % 4) * 0.8,
                    txMbps = 0.1 + (clientIdx % 3) * 0.3,
                    totalRxGb = 2.4 + (clientIdx * 0.8),
                    totalTxGb = 0.8 + (clientIdx * 0.3),
                    macAddress = "D4:6E:5C:88:%02X:%02X".format(clientIdx / 256, clientIdx % 256)
                )
            )
        }
    }

    private val _interfaces = MutableStateFlow<List<NetworkInterfaceTraffic>>(emptyList())
    val interfaces: StateFlow<List<NetworkInterfaceTraffic>> = _interfaces.asStateFlow()

    private val _selectedInterface = MutableStateFlow<NetworkInterfaceTraffic>(NetworkInterfaceTraffic())
    val selectedInterface: StateFlow<NetworkInterfaceTraffic> = _selectedInterface.asStateFlow()

    private val _cardQuota = MutableStateFlow(CardAndQuotaTelemetry())
    val cardQuota: StateFlow<CardAndQuotaTelemetry> = _cardQuota.asStateFlow()

    private val _isLiveConnected = MutableStateFlow(false)
    val isLiveConnected: StateFlow<Boolean> = _isLiveConnected.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("غير متصل بأي راوتر - يرجى تسجيل الدخول أولاً")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private var trafficPollJob: Job? = null
    private var activeApiClient: MikrotikApiClient? = null

    fun selectInterface(interfaceId: String) {
        val found = _interfaces.value.find { it.id == interfaceId } ?: return
        _selectedInterface.value = found
        val client = activeApiClient
        if (_isLiveConnected.value && client != null) {
            startRealInterfacePolling(client, found.name)
        }
    }

    fun setConnectingState(msg: String) {
        _connectionStatusMessage.value = msg
    }

    fun setAuthenticatedState(msg: String) {
        _isLiveConnected.value = true
        _connectionStatusMessage.value = msg
    }

    fun setLiveSyncWarning(msg: String) {
        _isLiveConnected.value = true
        _connectionStatusMessage.value = msg
    }

    fun setOfflineState(msg: String) {
        _isLiveConnected.value = false
        _connectionStatusMessage.value = msg
        trafficPollJob?.cancel()
    }

    fun updateRealData(
        realInterfaces: List<NetworkInterfaceTraffic>,
        realCardQuota: CardAndQuotaTelemetry,
        isLive: Boolean,
        desc: String
    ) {
        _isLiveConnected.value = isLive
        _connectionStatusMessage.value = desc
        _cardQuota.value = realCardQuota

        if (realInterfaces.isNotEmpty()) {
            _interfaces.value = realInterfaces
            val match = realInterfaces.find { it.id == _selectedInterface.value.id || it.name == _selectedInterface.value.name }
                ?: realInterfaces.firstOrNull { it.isWan }
                ?: realInterfaces.first()
            _selectedInterface.value = match
        }
    }

    fun startRealInterfacePolling(client: MikrotikApiClient, interfaceName: String) {
        activeApiClient = client
        trafficPollJob?.cancel()
        trafficPollJob = scope.launch(Dispatchers.IO) {
            client.pollInterfaceTraffic(
                interfaceName = interfaceName,
                onSample = { (rx, tx) ->
                    if (!_isLiveConnected.value) return@pollInterfaceTraffic
                    val current = _selectedInterface.value
                    val newRxHistory = (current.rxHistory.drop(1) + rx.toFloat()).takeLast(10)
                    val newTxHistory = (current.txHistory.drop(1) + tx.toFloat()).takeLast(10)
                    val updatedIface = current.copy(
                        rxMbps = rx,
                        txMbps = tx,
                        rxHistory = newRxHistory,
                        txHistory = newTxHistory
                    )
                    _selectedInterface.value = updatedIface
                    _interfaces.value = _interfaces.value.map {
                        if (it.name == updatedIface.name) updatedIface else it
                    }
                },
                onFailure = { error ->
                    if (_isLiveConnected.value) {
                        _connectionStatusMessage.value =
                            "تم تسجيل الدخول؛ تعذرت قراءة سرعة الواجهة مؤقتاً: ${error.message ?: "خطأ API"}"
                    }
                }
            )
        }
    }
}
