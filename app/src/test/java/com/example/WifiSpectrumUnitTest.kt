package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.wifi.ChannelRating
import com.example.data.wifi.ChannelRecommendationEngine
import com.example.data.wifi.ScanState
import com.example.data.wifi.WifiAccessPoint
import com.example.data.wifi.WifiBand
import com.example.data.wifi.WifiFrequencyHelper
import com.example.data.wifi.WifiScannerManager
import com.example.data.wifi.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WifiSpectrumUnitTest {

    @Test
    fun testFrequencyToChannel24Ghz() {
        assertEquals(1, WifiFrequencyHelper.frequencyToChannel(2412))
        assertEquals(6, WifiFrequencyHelper.frequencyToChannel(2437))
        assertEquals(11, WifiFrequencyHelper.frequencyToChannel(2462))
        assertEquals(13, WifiFrequencyHelper.frequencyToChannel(2472))
        assertEquals(14, WifiFrequencyHelper.frequencyToChannel(2484))
    }

    @Test
    fun testFrequencyToChannel5Ghz() {
        assertEquals(36, WifiFrequencyHelper.frequencyToChannel(5180))
        assertEquals(40, WifiFrequencyHelper.frequencyToChannel(5200))
        assertEquals(48, WifiFrequencyHelper.frequencyToChannel(5240))
        assertEquals(149, WifiFrequencyHelper.frequencyToChannel(5745))
        assertEquals(165, WifiFrequencyHelper.frequencyToChannel(5825))
    }

    @Test
    fun testChannelToFrequencyMapping() {
        assertEquals(2412, WifiFrequencyHelper.channelToFrequency(1, WifiBand.BAND_2_4_GHZ))
        assertEquals(2437, WifiFrequencyHelper.channelToFrequency(6, WifiBand.BAND_2_4_GHZ))
        assertEquals(2462, WifiFrequencyHelper.channelToFrequency(11, WifiBand.BAND_2_4_GHZ))
        assertEquals(5180, WifiFrequencyHelper.channelToFrequency(36, WifiBand.BAND_5_GHZ))
    }

    @Test
    fun testParseSecurityType() {
        assertEquals(
            WifiSecurityType.WPA3,
            WifiFrequencyHelper.parseSecurityType("[WPA3-SAE-CCMP][ESS]")
        )
        assertEquals(
            WifiSecurityType.WPA2,
            WifiFrequencyHelper.parseSecurityType("[WPA2-PSK-CCMP][ESS]")
        )
        assertEquals(
            WifiSecurityType.OPEN,
            WifiFrequencyHelper.parseSecurityType("[ESS]")
        )
        assertEquals(
            WifiSecurityType.WEP,
            WifiFrequencyHelper.parseSecurityType("[WEP][ESS]")
        )
    }

    @Test
    fun testChannelRecommendationEngine24Ghz() {
        // Given APs heavily congesting channel 1 and channel 6
        val congestedAps = listOf(
            WifiAccessPoint(
                bssid = "AA:BB:CC:DD:EE:01",
                ssid = "Busy-AP-Ch1",
                frequencyMhz = 2412,
                channel = 1,
                channelWidthMhz = 20,
                rssiDbm = -40,
                securityType = WifiSecurityType.WPA2,
                band = WifiBand.BAND_2_4_GHZ,
                capabilities = "[WPA2-PSK-CCMP]"
            ),
            WifiAccessPoint(
                bssid = "AA:BB:CC:DD:EE:02",
                ssid = "Busy-AP-Ch6",
                frequencyMhz = 2437,
                channel = 6,
                channelWidthMhz = 20,
                rssiDbm = -45,
                securityType = WifiSecurityType.WPA2,
                band = WifiBand.BAND_2_4_GHZ,
                capabilities = "[WPA2-PSK-CCMP]"
            )
        )

        val recommendation = ChannelRecommendationEngine.evaluateBand(
            congestedAps,
            WifiBand.BAND_2_4_GHZ
        )

        assertNotNull(recommendation)
        // Channel 11 must be chosen as the cleanest non-overlapping channel
        assertEquals(11, recommendation.recommendedChannel)
        assertEquals(ChannelRating.EXCELLENT, recommendation.rating)
        assertTrue(recommendation.rationale.contains("11"))
    }

    @Test
    fun testChannelRecommendationEngine5Ghz() {
        val aps5G = listOf(
            WifiAccessPoint(
                bssid = "00:11:22:33:44:01",
                ssid = "Mikrotik-5G",
                frequencyMhz = 5180,
                channel = 36,
                channelWidthMhz = 40,
                rssiDbm = -50,
                securityType = WifiSecurityType.WPA3,
                band = WifiBand.BAND_5_GHZ,
                capabilities = "[WPA3-SAE-CCMP]"
            )
        )

        val recommendation = ChannelRecommendationEngine.evaluateBand(
            aps5G,
            WifiBand.BAND_5_GHZ
        )

        assertNotNull(recommendation)
        assertTrue(recommendation.recommendedChannel > 0)
        assertEquals(WifiBand.BAND_5_GHZ, recommendation.band)
    }

    @Test
    fun testSignalPercentageClamping() {
        val apStrong = WifiAccessPoint(
            bssid = "00:00:00:00:00:01",
            ssid = "StrongAP",
            frequencyMhz = 2412,
            channel = 1,
            channelWidthMhz = 20,
            rssiDbm = -30,
            securityType = WifiSecurityType.OPEN,
            band = WifiBand.BAND_2_4_GHZ,
            capabilities = ""
        )
        assertEquals(100, apStrong.signalPercent)

        val apWeak = WifiAccessPoint(
            bssid = "00:00:00:00:00:02",
            ssid = "WeakAP",
            frequencyMhz = 2412,
            channel = 1,
            channelWidthMhz = 20,
            rssiDbm = -105,
            securityType = WifiSecurityType.OPEN,
            band = WifiBand.BAND_2_4_GHZ,
            capabilities = ""
        )
        assertEquals(0, apWeak.signalPercent)
    }

    @Test
    fun testHiddenSsidFallback() {
        val hiddenAp = WifiAccessPoint(
            bssid = "AA:BB:CC:11:22:33",
            ssid = "",
            frequencyMhz = 2412,
            channel = 1,
            channelWidthMhz = 20,
            rssiDbm = -65,
            securityType = WifiSecurityType.WPA2,
            band = WifiBand.BAND_2_4_GHZ,
            capabilities = ""
        )
        assertTrue(hiddenAp.displaySsid.contains("مخفية"))
        assertTrue(hiddenAp.displaySsid.contains("AA:BB:CC:11:22:33"))
    }

    @Test
    fun testScanStateHierarchy() {
        val readyState: ScanState = ScanState.Ready(emptyList())
        assertTrue(readyState is ScanState.Ready)

        val noPermState: ScanState = ScanState.NoPermission
        assertEquals(ScanState.NoPermission, noPermState)

        val disabledState: ScanState = ScanState.WifiDisabled
        assertEquals(ScanState.WifiDisabled, disabledState)

        val emptyState: ScanState = ScanState.Empty
        assertEquals(ScanState.Empty, emptyState)

        val errorState: ScanState = ScanState.Error("Test error")
        assertTrue(errorState is ScanState.Error)
        assertEquals("Test error", (errorState as ScanState.Error).message)
    }

    @Test
    fun testScannerManagerNeverEmitsMockOrFakeTelemetries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = WifiScannerManager(context)
        val state = scanner.scan()
        // Must never return 9 hardcoded fake APs
        assertTrue(state is ScanState.Empty || state is ScanState.NoPermission || state is ScanState.WifiDisabled)
        val aps = scanner.getScannedAccessPoints()
        assertTrue("Scanned access points must be empty when no physical APs broadcast", aps.isEmpty())
    }

    @Test
    fun test10StarChannelRatingComputation() {
        val aps = listOf(
            WifiAccessPoint(
                bssid = "AA:BB:CC:DD:EE:01",
                ssid = "AP-1",
                frequencyMhz = 2412,
                channel = 1,
                channelWidthMhz = 20,
                rssiDbm = -50,
                securityType = WifiSecurityType.WPA2,
                band = WifiBand.BAND_2_4_GHZ,
                capabilities = ""
            )
        )
        val rec = ChannelRecommendationEngine.evaluateBand(aps, WifiBand.BAND_2_4_GHZ)
        assertNotNull(rec)
        assertTrue(rec.allChannelScores.isNotEmpty())
        
        // Channel 1 has an AP so its score/star rating will be lower than empty channel 11
        val ch1 = rec.allChannelScores.firstOrNull { it.channel == 1 }
        val ch11 = rec.allChannelScores.firstOrNull { it.channel == 11 }
        assertNotNull(ch1)
        assertNotNull(ch11)
        assertTrue("Star rating must be between 1 and 10", ch1!!.starRating in 1..10)
        assertTrue("Star rating must be between 1 and 10", ch11!!.starRating in 1..10)
        assertTrue("Channel 11 should have >= stars than congested channel 1", ch11.starRating >= ch1.starRating)
        assertTrue("Channel scores must include channel width 20 MHz", ch1.channelWidthMhz == 20)
    }

    @Test
    fun testCurrentWifiConnectionModel() {
        val conn = com.example.data.wifi.CurrentWifiConnection(
            isConnected = true,
            ssid = "Talqa-Net 26",
            bssid = "ec:31:4a:1a:44:f9",
            frequencyMhz = 2462,
            channel = 11,
            rssiDbm = -60,
            linkSpeedMbps = 97,
            ipAddress = "10.50.7.251",
            distanceMetersApprox = 9.7
        )
        assertTrue(conn.isConnected)
        assertEquals("Talqa-Net 26", conn.ssid)
        assertEquals(11, conn.channel)
        assertEquals(97, conn.linkSpeedMbps)
        assertEquals("10.50.7.251", conn.ipAddress)
    }
}
