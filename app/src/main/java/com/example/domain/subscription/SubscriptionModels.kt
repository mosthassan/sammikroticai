package com.example.domain.subscription

import org.json.JSONObject

/**
 * Subscription Tiers for SamMikrotik ISP Financial Management.
 */
enum class SubscriptionTier(val titleAr: String, val badge: String) {
    FREE("الخطة المجانية", "FREE"),
    PRO("الخطة الاحترافية", "PRO")
}

/**
 * Constants governing Freemium limits.
 */
object SubscriptionLimits {
    const val FREE_MAX_INVOICES = 100
    const val FREE_MAX_DEVICES = 50
    const val ACTIVATION_WHATSAPP_NUMBER = "770446040"
    const val ACTIVATION_WHATSAPP_MESSAGE_AR =
        "للتفعيل راسلنا وتساب على الرقم 770446040"
    const val LIMIT_EXCEEDED_MESSAGE_AR =
        "لقد وصلت للحد الأقصى المسموح به في الخطة المجانية (100 فاتورة / 50 جهازاً). يرجى ترقية اشتراكك للاستمرار.\n$ACTIVATION_WHATSAPP_MESSAGE_AR"
    const val WIFI_SPECTRUM_LOCKED_MESSAGE_AR =
        "ميزة محلل طيف الواي فاي متاحة حصرياً لمشتركي الخطة الاحترافية (Pro). يرجى الترقية لتفعيل أداة فحص الترددات والقنوات.\n$ACTIVATION_WHATSAPP_MESSAGE_AR"

    fun buildActivationWhatsAppMessage(hardwareDeviceId: String): String {
        return "مرحباً، أود تفعيل ترخيص تطبيق SamMikrotik PRO.\nمعرف جهازي:\n$hardwareDeviceId"
    }
}

/**
 * Snapshot of current subscription state.
 */
data class SubscriptionStatus(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val isActive: Boolean = true,
    val isPro: Boolean = false,
    val expirationEpochMs: Long? = null, // null means lifetime or not applicable
    val activationKey: String? = null,
    val hardwareDeviceId: String = "",
    val issuedAtEpochMs: Long? = null,
    val customerName: String? = null
) {
    val isLifetime: Boolean
        get() = isPro && (expirationEpochMs == null || expirationEpochMs <= 0L)

    val isExpired: Boolean
        get() {
            if (!isPro) return false
            if (isLifetime) return false
            val expiry = expirationEpochMs ?: return false
            return System.currentTimeMillis() > expiry
        }

    val remainingDays: Long?
        get() {
            if (!isPro || isLifetime) return null
            val expiry = expirationEpochMs ?: return null
            val diff = expiry - System.currentTimeMillis()
            return if (diff > 0) diff / (1000 * 60 * 60 * 24) else 0L
        }
}

/**
 * Decoded payload of an offline license token.
 */
data class LicenseTokenPayload(
    val deviceId: String,
    val tier: SubscriptionTier,
    val expirationEpochMs: Long?, // -1 or null for lifetime
    val issuedAtEpochMs: Long,
    val customerName: String = "",
    val issuer: String = "SamMikrotik License Authority"
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("deviceId", deviceId)
        json.put("tier", tier.name)
        json.put("expirationEpochMs", expirationEpochMs ?: -1L)
        json.put("issuedAtEpochMs", issuedAtEpochMs)
        json.put("customerName", customerName)
        json.put("issuer", issuer)
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): LicenseTokenPayload {
            val json = JSONObject(jsonStr)
            val tierStr = json.optString("tier", SubscriptionTier.FREE.name)
            val tier = runCatching { SubscriptionTier.valueOf(tierStr) }.getOrDefault(SubscriptionTier.FREE)
            val expiry = json.optLong("expirationEpochMs", -1L)
            return LicenseTokenPayload(
                deviceId = json.optString("deviceId"),
                tier = tier,
                expirationEpochMs = if (expiry > 0L) expiry else null,
                issuedAtEpochMs = json.optLong("issuedAtEpochMs", System.currentTimeMillis()),
                customerName = json.optString("customerName", ""),
                issuer = json.optString("issuer", "SamMikrotik License Authority")
            )
        }
    }
}
