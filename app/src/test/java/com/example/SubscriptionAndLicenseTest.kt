package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.subscription.LicenseTokenPayload
import com.example.domain.subscription.SubscriptionLimits
import com.example.domain.subscription.SubscriptionTier
import com.example.util.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionAndLicenseTest {

    private lateinit var context: Context
    private lateinit var licenseManager: LicenseManager
    private lateinit var privateKey: PrivateKey

    // Private key matching the public key in LicenseManager.kt (for testing key generation)
    private val testPrivateKeyBase64 =
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDNAqwycTvrmUTL" +
        "Sloj14v5dyLbyXoi6vIoPxgqVMwEDbI2wwOGAqtxYkCJAZP8FtvhCRBkdTeGGtli" +
        "X7Rd0cMEjEsDkNmMh//5U5FpgfqgsjODBWDjAeqn1H4J3vCmpo5hZ2/GGaUyPa65" +
        "9DaQZ6SDeJuPP7YwQcGiXAfC0OkwfwYvlmcegYHOgJdh2Zlp1k78aYtgSF7lgJAX" +
        "9D7DnrDI2JAl2V0vxprISqy8iiAlnwtaIpqMBr8CJL4reyntpAQQ+ixFFNs9EEQS" +
        "Zn7ne9zy/DTb/Kj+eWMiVChLbpYALZRV5WUhcuvexQeApOhqLDXzo8P97PvN9jiA" +
        "PcMFGhG5AgMBAAECggEAEYKqmJMJY4HaLrmcDLcAEqpgBNZ8Tc00KnHb1jRwnzat" +
        "mSB7sDo8JdlsL7I9zz4DQSutWxc+oIAHP0VzZMFiNITocgEw7mA+DIVcbzRAkZyG" +
        "6n2kHTTwXli3pzoujd1oszMgMz3E2WS2rtVdb7oagsw4uo8zjYL1pLDS94sxJQjH" +
        "/KLkXJHMsBo0FCoY7K6XFwnx2U1Lu5ixsjZSPlqR1yCzrWoA78xbvs7IV06mfdlG" +
        "shkKFlrTmtzQSfAb0lovLJ8/OXVc4/kLFr49aGLxOU7aXfPzIIYec+vo0+cv2bw3" +
        "Ci9k1j9OYa+gt30bMVQ0vQ+xo66nD2PoxjspuLYEnQKBgQDpQCMhSBHCe3nTs9r4" +
        "YH98q7DWj8sVCHQV2kzKQhWRhpsy2piWlRU4/pXGdHOYAWatdvTPG5rf+a/7Yy2K" +
        "EXfc8TrhPYiC9YL2QpZSHhw6n8KKFuzi+7lTkJSmyY3ScP6a6A6d4dnX/nfk+WoP" +
        "dzNi56LX9JFJncCPgH8scugg7wKBgQDhAW3UpEpHrNYgVXZLPE7ri+3qINIu6o0Q" +
        "94tLAp5wsXQhoxsqvSWoMcWGQLHhxXDaXyiqAVqxTHUqYTuKUFW7GHDtq7Q9JO9S" +
        "q1Vb2RrmQhZE4n/Y1MrPKOFUXKKX05j6sPiPGrMpoV8uXitfqN2gDqbPfzrqwi01" +
        "CxbXJv4n1wKBgDjrjucbSEI2kadI98GvAs1wtInXs05hrrp6eEzPTaYivBLv51av" +
        "8OrkrbYtStwoU183bUxxRx0rOHfXXxCXbLG9KIo40uxqb1gbLcXCVU81F2tNZueJ" +
        "WFN9L9l3zCUzr94rjQ2YROoBzk5RU0GM7DxRnJqosx/M4GXkjKRTMjR/AoGAF7lM" +
        "2UTRSCljElmwdVJQh+ljFnKLdsLoSMew+iG2ju9G7V0DOkhjNg65YuRjXQiyU3QB" +
        "F5u48kS7fdJusrCxuV5kWcrm4COK0E6cW2jSu0BwlKbFEBmvoJQQ3hI+Wn/G+3e5" +
        "XW1VQqhvHrZzIEzvwTaKRAqILcR3e8ZFjD3iQlUCgYBTbsAy9YrGZ9YbCfXysPNh" +
        "iJWTxiLJtxTiiWKrZRlzWPi4uU+Ysj0F+/7b9KIzk8AWsKqbFCrSLC3H4eEe1UbJ" +
        "ZeXhcIp/ZE0pvgKbU1gXK8pufskJ/uoyzvkH6/OcsEVzQ+LWn//3swbS1okv9VYc" +
        "y+2y27Tmo0zV4obC5bDU5w=="

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        licenseManager = LicenseManager.getInstance(context)
        licenseManager.deactivateLicense()

        val keyBytes = Base64.decode(testPrivateKeyBase64, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun generateTestToken(
        deviceId: String,
        tier: SubscriptionTier = SubscriptionTier.PRO,
        expiryMs: Long? = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
        customerName: String = "Test Customer"
    ): String {
        val payload = LicenseTokenPayload(
            deviceId = deviceId,
            tier = tier,
            expirationEpochMs = expiryMs,
            issuedAtEpochMs = System.currentTimeMillis(),
            customerName = customerName
        )
        val payloadJson = payload.toJsonString()
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val payloadB64 = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(payloadBytes)
        val signatureBytes = signer.sign()
        val signatureB64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        return "$payloadB64.$signatureB64"
    }

    @Test
    fun testFreeTierLimitsEnforcement() {
        assertFalse("Free tier should not be pro", licenseManager.isPro())
        assertEquals(SubscriptionTier.FREE, licenseManager.subscriptionStatus.value.tier)

        // Invoice limits: 100 max
        assertTrue("99 invoices should be allowed", licenseManager.canCreateInvoice(99))
        assertFalse("100 invoices should reach limit", licenseManager.canCreateInvoice(100))
        assertFalse("105 invoices should be blocked", licenseManager.canCreateInvoice(105))

        // Device limits: 50 max
        assertTrue("49 devices should be allowed", licenseManager.canCreateDevice(49))
        assertFalse("50 devices should reach limit", licenseManager.canCreateDevice(50))
        assertFalse("55 devices should be blocked", licenseManager.canCreateDevice(55))

        // Wi-Fi Spectrum Analyzer is locked on Free tier
        assertFalse("Wi-Fi Spectrum must be locked on Free tier", licenseManager.isWifiSpectrumAllowed())
    }

    @Test
    fun testValidOfflineKeyActivatesProTier() {
        val deviceId = licenseManager.hardwareDeviceId
        val token = generateTestToken(deviceId = deviceId)

        val result = licenseManager.activateLicenseKey(token)
        assertTrue("Activation should succeed", result.isSuccess)

        val status = result.getOrThrow()
        assertTrue("Status must be PRO", status.isPro)
        assertEquals(SubscriptionTier.PRO, status.tier)
        assertFalse("Status must not be expired", status.isExpired)

        // Now Pro limits must be unlimited
        assertTrue("Pro should allow 150 invoices", licenseManager.canCreateInvoice(150))
        assertTrue("Pro should allow 500 invoices", licenseManager.canCreateInvoice(500))
        assertTrue("Pro should allow 80 devices", licenseManager.canCreateDevice(80))
        assertTrue("Pro should allow Wi-Fi Spectrum", licenseManager.isWifiSpectrumAllowed())
    }

    @Test
    fun testWildcardKeyActivatesAnyDevice() {
        val token = generateTestToken(deviceId = "*")

        val result = licenseManager.activateLicenseKey(token)
        assertTrue("Wildcard key should activate on any device", result.isSuccess)
        assertTrue(licenseManager.isPro())
    }

    @Test
    fun testKeyForDifferentDeviceIsRejected() {
        val differentDeviceId = "SAM-OTHER-DEVICE-9999"
        val token = generateTestToken(deviceId = differentDeviceId)

        val result = licenseManager.activateLicenseKey(token)
        assertTrue("Activation for different device must fail", result.isFailure)
        assertFalse("Tier must remain free", licenseManager.isPro())
    }

    @Test
    fun testTamperedKeyIsRejected() {
        val deviceId = licenseManager.hardwareDeviceId
        val token = generateTestToken(deviceId = deviceId)

        // Tamper with payload
        val parts = token.split(".")
        val tamperedPayloadB64 = Base64.encodeToString("tampered_json".toByteArray(), Base64.NO_WRAP)
        val tamperedToken = "$tamperedPayloadB64.${parts[1]}"

        val result = licenseManager.activateLicenseKey(tamperedToken)
        assertTrue("Tampered token must fail verification", result.isFailure)
        assertFalse("Tier must remain free", licenseManager.isPro())
    }

    @Test
    fun testExpiredKeyIsRejected() {
        val deviceId = licenseManager.hardwareDeviceId
        val pastDate = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000) // 10 days ago
        val token = generateTestToken(deviceId = deviceId, expiryMs = pastDate)

        val result = licenseManager.activateLicenseKey(token)
        assertTrue("Expired token must fail activation", result.isFailure)
        assertFalse("Tier must remain free", licenseManager.isPro())
    }

    @Test
    fun testDeactivateLicenseRevertsToFreeTier() {
        val deviceId = licenseManager.hardwareDeviceId
        val token = generateTestToken(deviceId = deviceId)
        licenseManager.activateLicenseKey(token)
        assertTrue(licenseManager.isPro())

        licenseManager.deactivateLicense()
        assertFalse("After deactivation, must revert to Free tier", licenseManager.isPro())
        assertEquals(SubscriptionTier.FREE, licenseManager.subscriptionStatus.value.tier)
        assertFalse("Limits must be reinstated", licenseManager.canCreateInvoice(100))
    }

    @Test
    fun testCloudSubscriptionApply() {
        val futureExpiry = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)
        licenseManager.applyCloudSubscription("PRO", futureExpiry, "Cloud Customer")

        assertTrue("Cloud subscription PRO must be active", licenseManager.isPro())
        assertEquals("Cloud Customer", licenseManager.subscriptionStatus.value.customerName)
    }

    @Test
    fun testNetworkDeviceLimitEnforcementInRepository() = kotlinx.coroutines.runBlocking {
        val repo = com.example.data.network.NetworkRepository(context)
        repo.deleteAllDevices()

        // Seed exactly 50 devices
        val dummyList = (1..50).map { i ->
            com.example.data.network.NetworkDevice(
                id = "dev_$i",
                name = "Device $i",
                ipAddress = "10.10.10.$i",
                deviceType = com.example.data.network.DeviceType.ROUTER
            )
        }
        repo.insertAll(dummyList)
        assertEquals(50, repo.devices.value.size)

        // Attempting to add 51st device on Free tier must return LimitReached
        licenseManager.deactivateLicense()
        val device51 = com.example.data.network.NetworkDevice(
            id = "dev_51",
            name = "Device 51",
            ipAddress = "10.10.10.51",
            deviceType = com.example.data.network.DeviceType.ACCESS_POINT
        )
        val result = repo.addOrUpdateDevice(device51)
        assertTrue("Device #51 must be blocked with LimitReached", result is com.example.data.network.DeviceSaveResult.LimitReached)
        assertEquals(50, repo.devices.value.size)

        // Now activate Pro tier
        val token = generateTestToken(deviceId = licenseManager.hardwareDeviceId)
        licenseManager.activateLicenseKey(token)
        assertTrue(licenseManager.isPro())

        // In Pro tier, device 51 must be added successfully
        val proResult = repo.addOrUpdateDevice(device51)
        assertTrue("In Pro tier, device #51 must succeed", proResult is com.example.data.network.DeviceSaveResult.Success)
        assertEquals(51, repo.devices.value.size)
    }
}
