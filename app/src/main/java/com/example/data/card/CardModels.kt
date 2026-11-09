package com.example.data.card

import java.util.UUID

/**
 * Visual Theme Presets for Hotspot Cards (matching SamMikrotik Web Engine)
 */
enum class CardThemePreset(val displayNameAr: String) {
    CYBER_NEON("سايبر نيون 2026 (أزرق وبنفسجي مشع)"),
    ROYAL_GOLD("ذهب ملكي VIP (أسود وذهبي فخم)"),
    CORPORATE_BLUE("أزرق فايبر كلاسيكي (مؤسسي)"),
    SPORT_SPEED("تيربو سبيد (برتقالي وأحمر رياضي)"),
    CLEAN_WHITE("أبيض ناصع 300DPI (موفر للحبر تماماً)"),
    SCRATCH_CARD("كروت الكشط التقليدية (Scratch Foil)")
}

/**
 * Code box styling variations
 */
enum class CodeBoxStyle(val displayNameAr: String) {
    MODERN_BOX("مربع عصري بإطار ملون"),
    PILL_BADGE("كبسولة نيون مضيئة"),
    SPLIT_DIGITS("خانات أرقام مقسمة منفصلة"),
    SCRATCH_FOIL("شريط كشط أمني (امسح برفق)"),
    TICKET_DASHED("تذكرة مقطعة ومسننة")
}

/**
 * Character set for card codes (matching Yemen / Arab network standards)
 */
enum class CardCharacterSet(val displayNameAr: String, val subtitleAr: String) {
    NUMBERS_ONLY("أرقام فقط (0 - 9)", "الأسهل في الإدخال بالهاتف"),
    UPPERCASE_ALPHANUMERIC("أرقام وحروف كبيرة", "0-9 + A-Z (أمان أعلى)")
}

/**
 * Password generation pattern for cards
 */
enum class PasswordPattern(val displayNameAr: String, val subtitleAr: String = "") {
    SAME_AS_USERNAME("نفس المستخدم", "User = Password"),
    NUMBERS_ONLY("PIN منفصل", "كلمة سر مخصصة"),
    NO_PASSWORD("بدون كلمة سر", "اسم مستخدم فقط"),
    ALPHANUMERIC("أحرف وأرقام عشوائية معقدة", "أمان فائق")
}

/**
 * Printable page format for A4 sheets and POS thermal rolls
 */
enum class PrintPageFormat(
    val displayNameAr: String,
    val cardsPerPage: Int,
    val cols: Int,
    val rows: Int
) {
    A4_GRID_24("ورقة A4 (24 كرت - 3×8 اقتصادي ومرن 63×33 مم)", 24, 3, 8),
    A4_GRID_42("ورقة A4 (42 كرت - 3×14 شريط اقتصادي 63×19 مم)", 42, 3, 14),
    A4_GRID_18("ورقة A4 (18 كرت - 3×6 مقاس وسطي)", 18, 3, 6),
    A4_GRID_10("ورقة A4 (10 كروت - 2×5 مقاس بطاقة كبير)", 10, 2, 5),
    THERMAL_ROLL_80MM("رول فواتير حراري (80 مم)", 1, 1, 1)
}

/**
 * Preview Mode for Card Studio
 */
enum class CardPreviewMode(val displayNameAr: String) {
    A4_FULL_SHEET("ورقة A4 كاملة للطباعة 📄"),
    SINGLE_CARD("معاينة كرت مفرد 🎴")
}

/**
 * Touch Interactive Position, Offset & Scale for individual card elements.
 */
data class ElementLayout(
    val offsetX: Float = 0f,     // Offset in dp (-150f to 150f)
    val offsetY: Float = 0f,     // Offset in dp (-100f to 100f)
    val scale: Float = 1.0f,      // Scale multiplier (0.6f to 1.8f)
    val widthScale: Float = 1.0f, // Width / Length scale multiplier (0.4f to 2.2f)
    val isVisible: Boolean = true
)

/**
 * Card Template Definition with rich styling attributes
 */
data class CardTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "قالب سايبر نيون 2026",
    val targetPackageProfile: String = "2700M_35H", // e.g. "2700M_35H", "1G_1D", "5G_15D"
    val targetPackageName: String = "باقة 500 ريال",
    val networkName: String = "شبكة طلقة نت",
    val priceText: String = "500 ر.ي",
    val quotaText: String = "2700 ميجا",
    val validityText: String = "35 ساعة",
    val headline: String = "كرت إنترنت فائق السرعة",
    val footerNote: String = "امسح الرمز أو ادخل الكود للاتصال الفوري",
    val supportPhone: String = "770446040",
    val hotspotLoginUrl: String = "http://10.50.7.1/login",
    val dateText: String = "07/11/26",
    val primaryColorHex: Long = 0xFF00F0FF, // Neon Cyan
    val backgroundColorHex: Long = 0xFF080318, // Deep Cosmic
    val gradientEndColorHex: Long = 0xFF12062B,
    val textColorHex: Long = 0xFFFFFFFF,
    val accentColorHex: Long = 0xFFFF007F, // Neon Pink
    val badgeBgColorHex: Long = 0xFFEAB308, // Gold/Yellow badge matching sammikroticnew
    val badgeTextColorHex: Long = 0xFF000000,
    val themePreset: CardThemePreset = CardThemePreset.CYBER_NEON,
    val codeBoxStyle: CodeBoxStyle = CodeBoxStyle.MODERN_BOX,
    val customBackgroundImagePath: String? = null,
    val customBackgroundImageUri: String? = null,
    val overlayAlpha: Float = 0.12f,
    val showNetworkName: Boolean = true,
    val showCreationDate: Boolean = true,
    val dateFormatPattern: String = "yyyy/MM/dd",
    val showQrCode: Boolean = true,
    val showBarcode: Boolean = false,
    val showPrice: Boolean = true,
    val showValidity: Boolean = true,
    val showCredentials: Boolean = true,
    val showSupportPhone: Boolean = true,
    val showSerialNumber: Boolean = true,
    val showCutLines: Boolean = true,
    val cardWidthMm: Int = 63,
    val cardHeightMm: Int = 33,
    val cardsPerRow: Int = 3,
    val cardsPerCol: Int = 8,
    // Interactive Touch Drag & Scale Layouts per Element:
    val priceLayout: ElementLayout = ElementLayout(),
    val networkLayout: ElementLayout = ElementLayout(),
    val codeBoxLayout: ElementLayout = ElementLayout(),
    val qrCodeLayout: ElementLayout = ElementLayout(),
    val metaLayout: ElementLayout = ElementLayout(),
    val supportPhoneLayout: ElementLayout = ElementLayout(),
    val serialLayout: ElementLayout = ElementLayout(),
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
    val supportPhone: String,
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
    val targetMikrotikHost: String = "",
    val planSpec: NetworkPlanSpec? = null
) {
    fun getEffectivePlan(): NetworkPlanSpec {
        return planSpec ?: NetworkPlanSpec.fromTemplate(templateSnapshot, batchTitle)
    }
}

/**
 * Dynamic Network Plan Specification (SaaS Plan Object)
 * Read dynamically from tenant package configuration (No hardcoding)
 */
data class NetworkPlanSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val profileName: String,
    val dataQuotaMb: Long = 0L,
    val uptimeHours: Int = 0,
    val validityDays: Int = 0,
    val priceText: String = "",
    val retailPriceMinor: Long = 0L,
    val wholesalePriceMinor: Long = 0L,
    val targetServer: String = "all",
    val commentTag: String = ""
) {
    companion object {
        fun fromTemplate(template: CardTemplate, batchTitle: String = ""): NetworkPlanSpec {
            val rawProfile = template.targetPackageProfile.trim()
            val safeProfile = if (rawProfile.isBlank() || rawProfile.equals("default", ignoreCase = true)) {
                val quotaSlug = template.quotaText.replace(Regex("[^0-9a-zA-Z]"), "").ifBlank { "2700M" }
                val validitySlug = template.validityText.replace(Regex("[^0-9a-zA-Z]"), "").ifBlank { "35H" }
                "PKG_${quotaSlug}_${validitySlug}"
            } else {
                rawProfile
            }

            val quotaMb = parseQuotaMb(template.quotaText)
            val uptimeHours = parseUptimeHours(template.validityText)

            return NetworkPlanSpec(
                id = template.id,
                name = template.targetPackageName.ifBlank { template.name },
                profileName = safeProfile,
                dataQuotaMb = quotaMb,
                uptimeHours = uptimeHours,
                validityDays = parseValidityDays(template.validityText),
                priceText = template.priceText,
                targetServer = "all",
                commentTag = batchTitle.ifBlank { "Batch_${template.name}" }
            )
        }

        fun parseQuotaMb(text: String): Long {
            val lower = text.lowercase()
            val num = Regex("[0-9]+").find(lower)?.value?.toLongOrNull() ?: 0L
            if (num <= 0L) return 0L
            return when {
                lower.contains("g") || lower.contains("جيجا") -> num * 1024L
                lower.contains("m") || lower.contains("ميجا") -> num
                else -> num
            }
        }

        fun parseUptimeHours(text: String): Int {
            val lower = text.lowercase()
            val num = Regex("[0-9]+").find(lower)?.value?.toIntOrNull() ?: 0
            if (num <= 0) return 0
            return when {
                lower.contains("d") || lower.contains("يوم") || lower.contains("ايام") -> num * 24
                lower.contains("h") || lower.contains("ساعة") || lower.contains("ساعات") -> num
                else -> num
            }
        }

        fun parseValidityDays(text: String): Int {
            val lower = text.lowercase()
            val num = Regex("[0-9]+").find(lower)?.value?.toIntOrNull() ?: 0
            if (num <= 0) return 0
            return when {
                lower.contains("يوم") || lower.contains("ايام") || lower.contains("d") -> num
                lower.contains("شهر") -> num * 30
                lower.contains("ساعة") -> (num / 24).coerceAtLeast(1)
                else -> num
            }
        }
    }
}

