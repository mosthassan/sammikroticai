package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.domain.subscription.LicenseTokenPayload
import com.example.domain.subscription.SubscriptionLimits
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

/**
 * High-security License Manager supporting offline RSA-2048 cryptographically signed tokens
 * and EncryptedSharedPreferences persistence for SamMikrotik ISP Financial Management.
 */
class LicenseManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "sammikrotik_secure_license_prefs"
        private const val FALLBACK_PREFS_FILE = "sammikrotik_license_prefs_fallback"

        private const val KEY_ACTIVATION_KEY = "activation_key"
        private const val KEY_PAYLOAD_JSON = "payload_json"
        private const val KEY_TIER = "tier"
        private const val KEY_EXPIRY_MS = "expiry_ms"
        private const val KEY_DEVICE_ID = "hardware_device_id"
        private const val KEY_CUSTOMER_NAME = "customer_name"
        private const val KEY_ISSUED_AT = "issued_at_ms"

        // Embedded Public Key (X.509 SubjectPublicKeyInfo Base64)
        // Private Key stays strictly on the Admin Control Panel / Backend!
        const val PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzQKsMnE765lEy0paI9eL" +
            "+Xci28l6IuryKD8YKlTMBA2yNsMDhgKrcWJAiQGT/Bbb4QkQZHU3hhrZYl+0XdH" +
            "DBIxLA5DZjIf/+VORaYH6oLIzgwVg4wHqp9R+Cd7wpqaOYWdvxhmlMj2uufQ2kG" +
            "ekg3ibjz+2MEHBolwHwtDpMH8GL5ZnHoGBzoCXYdmZadZO/GmLYEhe5YCQF/Q+w" +
            "56wyNiQJdldL8aayEqsvIogJZ8LWiKajAa/AiS+K3sp7aQEEPosRRTbPRBEEmZ+" +
            "53vc8vw02/yo/nljIlQoS26WAC2UVeVlIXLr3sUHgKToaiw186PD/ez7zfY4gD3" +
            "DBRoRuQIDAQAB"

        @Volatile
        private var instance: LicenseManager? = null

        fun getInstance(context: Context): LicenseManager {
            return instance ?: synchronized(this) {
                instance ?: LicenseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val rsaPublicKey: PublicKey by lazy {
        val keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private val prefs: SharedPreferences by lazy {
        initPreferences()
    }

    val hardwareDeviceId: String by lazy {
        resolveHardwareDeviceId()
    }

    private val _subscriptionStatus = MutableStateFlow(loadInitialStatus())
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    private fun initPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            // Safe fallback for unit tests or legacy hardware lacking AndroidKeyStore provider
            context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    @SuppressLint("HardwareIds")
    private fun resolveHardwareDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        // Try getting ANDROID_ID
        var id = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }

        if (id.isNullOrBlank() || id == "9774d56d682e549c") {
            // Fallback to random deterministic UUID
            id = UUID.randomUUID().toString().replace("-", "").take(16)
        }

        val formatted = "SAM-" + id.uppercase().take(12).chunked(4).joinToString("-")
        prefs.edit().putString(KEY_DEVICE_ID, formatted).apply()
        return formatted
    }

    private fun loadInitialStatus(): SubscriptionStatus {
        val key = prefs.getString(KEY_ACTIVATION_KEY, null)
        val payloadJson = prefs.getString(KEY_PAYLOAD_JSON, null)
        val tierStr = prefs.getString(KEY_TIER, SubscriptionTier.FREE.name)
        val expiry = prefs.getLong(KEY_EXPIRY_MS, -1L)
        val customerName = prefs.getString(KEY_CUSTOMER_NAME, null)
        val issuedAt = prefs.getLong(KEY_ISSUED_AT, -1L)

        val tier = runCatching { SubscriptionTier.valueOf(tierStr ?: "") }.getOrDefault(SubscriptionTier.FREE)

        val status = SubscriptionStatus(
            tier = tier,
            isActive = true,
            isPro = tier == SubscriptionTier.PRO,
            expirationEpochMs = if (expiry > 0L) expiry else null,
            activationKey = key,
            hardwareDeviceId = hardwareDeviceId,
            issuedAtEpochMs = if (issuedAt > 0L) issuedAt else null,
            customerName = customerName
        )

        // If saved license is expired, verify and downgrade if needed
        if (status.isExpired) {
            return status.copy(tier = SubscriptionTier.FREE, isPro = false)
        }

        return status
    }

    /**
     * Activates an offline digital license key.
     * Token format: <Base64Payload>.<Base64Signature>
     */
    @Synchronized
    fun activateLicenseKey(token: String): Result<SubscriptionStatus> {
        return runCatching {
            val cleanToken = token.trim().replace("\n", "").replace("\r", "")
            if (cleanToken.isBlank()) {
                throw IllegalArgumentException("يرجى إدخال رمز تفعيل صالح.")
            }

            val parts = cleanToken.split(".")
            if (parts.size != 2) {
                throw IllegalArgumentException("تنسيق رمز التفعيل غير صحيح. يجب أن يتكون من مقطعين مشفرين.")
            }

            val payloadBase64 = parts[0]
            val signatureBase64 = parts[1]

            val payloadBytes = try {
                Base64.decode(payloadBase64, Base64.DEFAULT)
            } catch (e: Exception) {
                throw IllegalArgumentException("فشل قراءة بيانات الترخيص المشفرة.")
            }

            val signatureBytes = try {
                Base64.decode(signatureBase64, Base64.DEFAULT)
            } catch (e: Exception) {
                throw IllegalArgumentException("فشل قراءة التوقيع الرقمي.")
            }

            val payloadJson = String(payloadBytes, Charsets.UTF_8)

            // 1. Cryptographic Signature Verification
            val sigVerifier = Signature.getInstance("SHA256withRSA")
            sigVerifier.initVerify(rsaPublicKey)
            sigVerifier.update(payloadBytes)
            val isValidSignature = sigVerifier.verify(signatureBytes)

            if (!isValidSignature) {
                throw SecurityException("التوقيع الرقمي للترخيص غير صالح أو تم التلاعب به!")
            }

            // 2. Parse Payload
            val payload = LicenseTokenPayload.fromJsonString(payloadJson)

            // 3. Hardware Fingerprint Verification
            val tokenDeviceId = payload.deviceId.trim()
            val myDeviceId = hardwareDeviceId.trim()
            val isWildcard = tokenDeviceId == "*" || tokenDeviceId.equals("ALL", ignoreCase = true)

            if (!isWildcard && !tokenDeviceId.equals(myDeviceId, ignoreCase = true)) {
                throw SecurityException("رمز التفعيل هذا مخصص لجهاز آخر ($tokenDeviceId) ولا يطابق هذا الجهاز ($myDeviceId).")
            }

            // 4. Expiration Verification
            val now = System.currentTimeMillis()
            if (payload.expirationEpochMs != null && payload.expirationEpochMs > 0 && now > payload.expirationEpochMs) {
                throw IllegalStateException("رمز التفعيل هذا منتهي الصلاحية بتاريخ سابق.")
            }

            // 5. Save Valid License
            prefs.edit()
                .putString(KEY_ACTIVATION_KEY, cleanToken)
                .putString(KEY_PAYLOAD_JSON, payloadJson)
                .putString(KEY_TIER, payload.tier.name)
                .putLong(KEY_EXPIRY_MS, payload.expirationEpochMs ?: -1L)
                .putString(KEY_CUSTOMER_NAME, payload.customerName)
                .putLong(KEY_ISSUED_AT, payload.issuedAtEpochMs)
                .apply()

            val newStatus = SubscriptionStatus(
                tier = payload.tier,
                isActive = true,
                isPro = payload.tier == SubscriptionTier.PRO,
                expirationEpochMs = payload.expirationEpochMs,
                activationKey = cleanToken,
                hardwareDeviceId = myDeviceId,
                issuedAtEpochMs = payload.issuedAtEpochMs,
                customerName = payload.customerName
            )

            _subscriptionStatus.value = newStatus
            newStatus
        }
    }

    /**
     * Deactivates the current license and returns to Free tier.
     */
    @Synchronized
    fun deactivateLicense() {
        prefs.edit()
            .remove(KEY_ACTIVATION_KEY)
            .remove(KEY_PAYLOAD_JSON)
            .putString(KEY_TIER, SubscriptionTier.FREE.name)
            .remove(KEY_EXPIRY_MS)
            .remove(KEY_CUSTOMER_NAME)
            .remove(KEY_ISSUED_AT)
            .apply()

        _subscriptionStatus.value = SubscriptionStatus(
            tier = SubscriptionTier.FREE,
            isActive = true,
            isPro = false,
            expirationEpochMs = null,
            activationKey = null,
            hardwareDeviceId = hardwareDeviceId
        )
    }

    /**
     * Synchronize online subscription status from Firebase Firestore.
     */
    @Synchronized
    fun applyCloudSubscription(
        tierName: String,
        expirationEpochMs: Long?,
        customerName: String? = null
    ) {
        val tier = runCatching { SubscriptionTier.valueOf(tierName) }.getOrDefault(SubscriptionTier.FREE)
        val now = System.currentTimeMillis()
        val isExpired = expirationEpochMs != null && expirationEpochMs > 0 && now > expirationEpochMs

        if (isExpired || tier == SubscriptionTier.FREE) {
            deactivateLicense()
            return
        }

        prefs.edit()
            .putString(KEY_TIER, tier.name)
            .putLong(KEY_EXPIRY_MS, expirationEpochMs ?: -1L)
            .putString(KEY_CUSTOMER_NAME, customerName ?: "")
            .apply()

        _subscriptionStatus.value = SubscriptionStatus(
            tier = tier,
            isActive = true,
            isPro = true,
            expirationEpochMs = expirationEpochMs,
            activationKey = "CLOUD_MANAGED",
            hardwareDeviceId = hardwareDeviceId,
            customerName = customerName
        )
    }

    var currentUserEmail: String? = null
        set(value) {
            field = value
            val isAdmin = com.example.domain.subscription.AdminAuthPolicy.isAdmin(value)
            if (isAdmin) {
                _subscriptionStatus.value = _subscriptionStatus.value.copy(
                    tier = SubscriptionTier.PRO,
                    isPro = true
                )
            } else {
                _subscriptionStatus.value = loadInitialStatus()
            }
        }

    /**
     * Checks if current tier is Pro and not expired.
     * System administrators automatically receive perpetual PRO privileges.
     */
    fun isPro(): Boolean {
        if (com.example.domain.subscription.AdminAuthPolicy.isAdmin(currentUserEmail)) {
            return true
        }
        val current = _subscriptionStatus.value
        return current.isPro && !current.isExpired
    }

    /**
     * Invoice creation limit check (Sales + Purchases combined).
     */
    fun canCreateInvoice(currentInvoicesCount: Int): Boolean {
        if (isPro()) return true
        return currentInvoicesCount < SubscriptionLimits.FREE_MAX_INVOICES
    }

    /**
     * Network devices creation limit check.
     */
    fun canCreateDevice(currentDevicesCount: Int): Boolean {
        if (isPro()) return true
        return currentDevicesCount < SubscriptionLimits.FREE_MAX_DEVICES
    }

    /**
     * Wi-Fi Spectrum screen gate.
     */
    fun isWifiSpectrumAllowed(): Boolean {
        return isPro()
    }
}
