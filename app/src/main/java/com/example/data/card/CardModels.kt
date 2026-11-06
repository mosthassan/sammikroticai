package com.example.data.card

import java.util.UUID

/**
 * Visual Theme Presets for Hotspot Cards
 */
enum class CardThemePreset(val displayNameAr: String) {
    CYBER_NEON("سايبر نيون (أزرق مشع)"),
    GOLDEN_LUXURY("ذهبي ملكي فاخر"),
    CLEAN_INK_SAVER("أبيض ناصع (موفر للحبر)"),
    TECH_DARK("تقني فاحم (Dark Tech)")
}

/**
 * Password generation pattern for cards
 */
enum class PasswordPattern(val displayNameAr: String) {
    SAME_AS_USERNAME("نفس اسم المستخدم"),
    NUMBERS_ONLY("أرقام سرية فقط"),
    ALPHANUMERIC("أحرف وأرقام معقدة")
}

/**
 * Printable page format
 */
enum class PrintPageFormat(val displayNameAr: String, val cardsPerPage: Int) {
    A4_GRID_10("صفحة A4 (10 كروت - مقاس قياسي)", 10),
    A4_GRID_18("صفحة A4 (18 كرت - مقاس اقتصادي)", 18),
    THERMAL_ROLL_80MM("رول فواتير حراري (80 مم)", 1)
}

/**
 * Card Template Definition
 */
data class CardTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "القالب الافتراضي",
    val networkName: String = "شبكة سام نت",
    val priceText: String = "500 ر.ي",
    val quotaText: String = "2 جيجابايت",
    val validityText: String = "صلاحية 7 أيام",
    val headline: String = "كرت إنترنت فائق السرعة",
    val footerNote: String = "للدعم الفني اتصل على خدمة العملاء",
    val hotspotLoginUrl: String = "http://10.50.7.1/login",
    val primaryColorHex: Long = 0xFF00E5FF, // Cyan
    val backgroundColorHex: Long = 0xFF0F172A, // Deep Slate
    val textColorHex: Long = 0xFFFFFFFF,
    val accentColorHex: Long = 0xFFFFD700, // Gold
    val showQrCode: Boolean = true,
    val showBarcode: Boolean = true,
    val showPrice: Boolean = true,
    val showValidity: Boolean = true,
    val showCredentials: Boolean = true,
    val themePreset: CardThemePreset = CardThemePreset.CYBER_NEON,
    val cardWidthMm: Int = 85,
    val cardHeightMm: Int = 54,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual Generated Hotspot Card
 */
data class GeneratedCard(
    val id: String = UUID.randomUUID().toString(),
    val serialNumber: String,
    val username: String,
    val password: String,
    val priceText: String,
    val quotaText: String,
    val validityText: String,
    val networkName: String,
    val loginUrlWithCredentials: String,
    val batchId: String
)

/**
 * Batch of Generated Cards
 */
data class CardBatch(
    val id: String = UUID.randomUUID().toString(),
    val batchNumber: Long,
    val batchTitle: String,
    val templateId: String,
    val templateSnapshot: CardTemplate,
    val profileName: String,
    val cardCount: Int,
    val prefix: String,
    val usernameLength: Int,
    val passwordPattern: PasswordPattern,
    val cards: List<GeneratedCard>,
    val createdAt: Long = System.currentTimeMillis(),
    val isSyncedToMikrotik: Boolean = false,
    val targetMikrotikHost: String = ""
)
