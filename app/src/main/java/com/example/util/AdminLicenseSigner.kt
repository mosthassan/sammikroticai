package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.domain.subscription.LicenseTokenPayload
import com.example.domain.subscription.SubscriptionTier
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

data class GeneratedLicenseResult(
    val token: String,
    val payload: LicenseTokenPayload,
    val expiryFormatted: String,
    val issuedAtFormatted: String
)

data class AdminIssuedLicense(
    val id: Long,
    val deviceId: String,
    val customerName: String,
    val durationMonths: Int,
    val issuedAtEpochMs: Long,
    val expirationEpochMs: Long?,
    val token: String
)

class AdminLicenseSigner(private val context: Context) {

    companion object {
        // RSA-2048 Private Key (PKCS#8 DER Base64) for signing client activation tokens
        private const val PRIVATE_KEY_BASE64 =
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

        private const val PREFS_KEY_ISSUED_HISTORY = "admin_issued_licenses_json"
    }

    private val privateKey: PrivateKey by lazy {
        val keyBytes = Base64.decode(PRIVATE_KEY_BASE64, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("sammikrotik_admin_signer_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Generates and digitally signs a license token using RSA-2048.
     */
    fun generateLicenseToken(
        deviceId: String,
        durationMonths: Int = 12,
        customerName: String = ""
    ): Result<GeneratedLicenseResult> = runCatching {
        val cleanDeviceId = deviceId.trim().uppercase()
        if (cleanDeviceId.isBlank()) {
            throw IllegalArgumentException("يرجى إدخال بصمة الجهاز (Hardware Device ID).")
        }

        val now = System.currentTimeMillis()
        val expirationEpochMs = if (durationMonths > 0) {
            now + (durationMonths.toLong() * 30L * 24L * 60L * 60L * 1000L)
        } else {
            null // Lifetime
        }

        val payload = LicenseTokenPayload(
            deviceId = cleanDeviceId,
            tier = SubscriptionTier.PRO,
            expirationEpochMs = expirationEpochMs,
            issuedAtEpochMs = now,
            customerName = customerName.trim(),
            issuer = "SamMikrotik License Authority"
        )

        val payloadJson = payload.toJsonString()
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val payloadBase64 = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)

        // Digitally sign payload
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(payloadBytes)
        val signatureBytes = signer.sign()
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        val token = "$payloadBase64.$signatureBase64"

        val expiryStr = if (expirationEpochMs != null && expirationEpochMs > 0) {
            TransactionDateUtils.formatDateTime(expirationEpochMs)
        } else {
            "دائم مدى الحياة (Lifetime)"
        }
        val issuedStr = TransactionDateUtils.formatDateTime(now)

        val record = AdminIssuedLicense(
            id = now,
            deviceId = cleanDeviceId,
            customerName = customerName.trim().ifBlank { "عميل شبكة" },
            durationMonths = durationMonths,
            issuedAtEpochMs = now,
            expirationEpochMs = expirationEpochMs,
            token = token
        )
        saveIssuedLicense(record)

        GeneratedLicenseResult(
            token = token,
            payload = payload,
            expiryFormatted = expiryStr,
            issuedAtFormatted = issuedStr
        )
    }

    fun getIssuedLicenses(): List<AdminIssuedLicense> {
        val raw = prefs.getString(PREFS_KEY_ISSUED_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AdminIssuedLicense>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val exp = obj.optLong("expirationEpochMs", -1L)
                list.add(
                    AdminIssuedLicense(
                        id = obj.optLong("id"),
                        deviceId = obj.optString("deviceId"),
                        customerName = obj.optString("customerName"),
                        durationMonths = obj.optInt("durationMonths"),
                        issuedAtEpochMs = obj.optLong("issuedAtEpochMs"),
                        expirationEpochMs = if (exp > 0) exp else null,
                        token = obj.optString("token")
                    )
                )
            }
            list.sortedByDescending { it.issuedAtEpochMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveIssuedLicense(license: AdminIssuedLicense) {
        val current = getIssuedLicenses().toMutableList()
        current.add(0, license)
        val array = JSONArray()
        current.take(100).forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("deviceId", item.deviceId)
            obj.put("customerName", item.customerName)
            obj.put("durationMonths", item.durationMonths)
            obj.put("issuedAtEpochMs", item.issuedAtEpochMs)
            obj.put("expirationEpochMs", item.expirationEpochMs ?: -1L)
            obj.put("token", item.token)
            array.put(obj)
        }
        prefs.edit().putString(PREFS_KEY_ISSUED_HISTORY, array.toString()).apply()
    }

    fun deleteIssuedLicense(id: Long) {
        val current = getIssuedLicenses().filter { it.id != id }
        val array = JSONArray()
        current.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("deviceId", item.deviceId)
            obj.put("customerName", item.customerName)
            obj.put("durationMonths", item.durationMonths)
            obj.put("issuedAtEpochMs", item.issuedAtEpochMs)
            obj.put("expirationEpochMs", item.expirationEpochMs ?: -1L)
            obj.put("token", item.token)
            array.put(obj)
        }
        prefs.edit().putString(PREFS_KEY_ISSUED_HISTORY, array.toString()).apply()
    }
}
