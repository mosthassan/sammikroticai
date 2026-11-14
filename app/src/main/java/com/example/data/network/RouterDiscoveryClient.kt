package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

data class DiscoveredRouter(
    val ipAddress: String,
    val macAddress: String = "",
    val identity: String = "",
    val boardName: String = "",
    val routerOsVersion: String = ""
)

/**
 * Discovers MikroTik routers on the local network using MNDP (UDP port 5678).
 */
class RouterDiscoveryClient(private val context: Context) {

    companion object {
        private const val MNDP_PORT = 5678
        private const val DEFAULT_TIMEOUT_MS = 2_500L
        private const val MAX_PACKET_SIZE = 2048
    }

    suspend fun discover(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Result<List<DiscoveredRouter>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val multicastLock = runCatching {
                    wifiManager?.createMulticastLock("sammikrotik_mndp")?.apply {
                        setReferenceCounted(false)
                        acquire()
                    }
                }.getOrNull()

                val found = linkedMapOf<String, DiscoveredRouter>()
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        bind(InetSocketAddress(MNDP_PORT))
                        soTimeout = 400
                    }

                    // Send empty MNDP discovery probe to local broadcast
                    val probePayload = byteArrayOf(0, 0, 0, 0)
                    runCatching {
                        val broadcastAddr = InetAddress.getByName("255.255.255.255")
                        socket.send(DatagramPacket(probePayload, probePayload.size, broadcastAddr, MNDP_PORT))
                    }

                    val buffer = ByteArray(MAX_PACKET_SIZE)
                    val deadline = System.currentTimeMillis() + timeoutMs
                    while (System.currentTimeMillis() < deadline) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val parsed = parseMndpPacket(
                                data = packet.data,
                                length = packet.length,
                                senderIp = packet.address?.hostAddress.orEmpty()
                            )
                            if (parsed != null && parsed.ipAddress.isNotBlank()) {
                                found[parsed.ipAddress] = parsed
                            }
                        } catch (_: SocketTimeoutException) {
                            // Continue listening until overall discovery deadline
                        }
                    }
                } finally {
                    runCatching { socket?.close() }
                    runCatching {
                        if (multicastLock?.isHeld == true) multicastLock.release()
                    }
                }

                found.values.toList()
            }
        }

    private fun parseMndpPacket(data: ByteArray, length: Int, senderIp: String): DiscoveredRouter? {
        if (length < 4) return null

        var macAddress = ""
        var identity = ""
        var version = ""
        var boardName = ""
        var tlvIpAddress = ""

        // MNDP header is 4 bytes (2 bytes type/ttl + 2 bytes seq) followed by TLVs (2-byte type, 2-byte length)
        var offset = 4
        while (offset + 4 <= length) {
            val tlvType = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val tlvLen = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            offset += 4
            if (tlvLen < 0 || offset + tlvLen > length) break

            when (tlvType) {
                1 -> if (tlvLen == 6) {
                    macAddress = (0 until 6).joinToString(":") { idx ->
                        "%02X".format(data[offset + idx])
                    }
                }
                5 -> identity = String(data, offset, tlvLen, Charsets.UTF_8).trim()
                7 -> version = String(data, offset, tlvLen, Charsets.UTF_8).trim()
                12 -> boardName = String(data, offset, tlvLen, Charsets.UTF_8).trim()
                16 -> if (tlvLen == 4) {
                    tlvIpAddress = (0 until 4).joinToString(".") { idx ->
                        (data[offset + idx].toInt() and 0xFF).toString()
                    }
                }
            }
            offset += tlvLen
        }

        val resolvedIp = tlvIpAddress.ifBlank { senderIp }
        if (resolvedIp.isBlank() || resolvedIp == "0.0.0.0") return null

        return DiscoveredRouter(
            ipAddress = resolvedIp,
            macAddress = macAddress,
            identity = identity.ifBlank { "MikroTik ($resolvedIp)" },
            boardName = boardName,
            routerOsVersion = version
        )
    }
}
