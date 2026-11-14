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

enum class CardCutLineStyle(val labelAr: String) {
    CONTINUOUS("مستمر"),
    CORNER_MARKS("علامات زوايا"),
    NONE("بدون خطوط")
}

data class CardPrintGridSettings(
    val isCustom: Boolean = false,
    val columns: Int = 3,
    val rows: Int = 8,
    val horizontalGapMm: Float = 2.5f,
    val verticalGapMm: Float = 2.5f,
    val horizontalMarginMm: Float = 6.5f,
    val verticalMarginMm: Float = 6f,
    val autoFitCard: Boolean = true,
    val cutLineStyle: CardCutLineStyle = CardCutLineStyle.CONTINUOUS
) {
    fun normalized(): CardPrintGridSettings {
        val safeColumns = columns.coerceIn(1, 5)
        val safeRows = rows.coerceIn(1, 16)
        val safeGapX = horizontalGapMm.coerceIn(0f, 20f)
            .coerceAtMost(210f / (safeColumns - 1).coerceAtLeast(1))
        val safeGapY = verticalGapMm.coerceIn(0f, 20f)
            .coerceAtMost(297f / (safeRows - 1).coerceAtLeast(1))
        val safeMarginX = horizontalMarginMm.coerceIn(0f, 40f)
            .coerceAtMost((210f - (safeColumns - 1) * safeGapX).coerceAtLeast(0f) / 2f)
        val safeMarginY = verticalMarginMm.coerceIn(0f, 40f)
            .coerceAtMost((297f - (safeRows - 1) * safeGapY).coerceAtLeast(0f) / 2f)
        return copy(
            columns = safeColumns,
            rows = safeRows,
            horizontalGapMm = safeGapX,
            verticalGapMm = safeGapY,
            horizontalMarginMm = safeMarginX,
            verticalMarginMm = safeMarginY
        )
    }

    fun availableCellWidthMm(): Float {
        val safe = normalized()
        return ((210f - 2f * safe.horizontalMarginMm -
            (safe.columns - 1) * safe.horizontalGapMm) / safe.columns).coerceAtLeast(0f)
    }

    fun availableCellHeightMm(): Float {
        val safe = normalized()
        return ((297f - 2f * safe.verticalMarginMm -
            (safe.rows - 1) * safe.verticalGapMm) / safe.rows).coerceAtLeast(0f)
    }
}

/**
 * Preview Mode for Card Studio
 */
enum class CardPreviewMode(val displayNameAr: String) {
    A4_FULL_SHEET("ورقة A4 كاملة للطباعة 📄"),
    SINGLE_CARD("معاينة كرت مفرد 🎴")
}

/**
 * Element center and dimensions stored as normalized ratios of the card bounds.
 */
data class ElementLayout(
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f,
    val widthRatio: Float = 0.30f,
    val heightRatio: Float = 0.15f,
    val isVisible: Boolean = true,
    val textColorHex: Long = 0xFFFFFFFF,
    val fontSizeScale: Float = 1f,
    val isBold: Boolean = true
) {
    val leftRatio: Float
        get() = xRatio - widthRatio / 2f

    val topRatio: Float
        get() = yRatio - heightRatio / 2f

    val rightRatio: Float
        get() = xRatio + widthRatio / 2f

    val bottomRatio: Float
        get() = yRatio + heightRatio / 2f

    val scale: Float
        get() = (heightRatio / DEFAULT_HEIGHT_RATIO).coerceIn(0.6f, 1.8f)

    val widthScale: Float
        get() = (widthRatio / DEFAULT_WIDTH_RATIO).coerceIn(0.4f, 2.2f)

    companion object {
        const val DEFAULT_WIDTH_RATIO = 0.30f
        const val DEFAULT_HEIGHT_RATIO = 0.15f
    }
}

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
    val printGridSettings: CardPrintGridSettings = CardPrintGridSettings(),
    // Interactive Touch Drag & Scale Layouts per Element:
    val priceLayout: ElementLayout = ElementLayout(xRatio = 0.12f, yRatio = 0.12f, widthRatio = 0.22f, heightRatio = 0.15f, textColorHex = 0xFF000000, isBold = true),
    val networkLayout: ElementLayout = ElementLayout(xRatio = 0.70f, yRatio = 0.12f, widthRatio = 0.48f, heightRatio = 0.16f),
    val codeBoxLayout: ElementLayout = ElementLayout(xRatio = 0.60f, yRatio = 0.54f, widthRatio = 0.62f, heightRatio = 0.30f, isBold = true),
    val qrCodeLayout: ElementLayout = ElementLayout(xRatio = 0.12f, yRatio = 0.52f, widthRatio = 0.20f, heightRatio = 0.40f),
    val metaLayout: ElementLayout = ElementLayout(xRatio = 0.68f, yRatio = 0.42f, widthRatio = 0.45f, heightRatio = 0.15f, textColorHex = 0xFF00F0FF),
    val supportPhoneLayout: ElementLayout = ElementLayout(xRatio = 0.25f, yRatio = 0.91f, widthRatio = 0.38f, heightRatio = 0.14f, textColorHex = 0xFFE2E8F0),
    val creationDateLayout: ElementLayout = ElementLayout(xRatio = 0.80f, yRatio = 0.12f, widthRatio = 0.30f, heightRatio = 0.15f),
    val serialLayout: ElementLayout = ElementLayout(xRatio = 0.80f, yRatio = 0.91f, widthRatio = 0.30f, heightRatio = 0.15f, textColorHex = 0xFFCBD5E1),
    val createdAt: Long = System.currentTimeMillis()
)

fun defaultElementLayout(elementKey: String): ElementLayout {
    val defaults = CardTemplate()
    return when (elementKey) {
        "price" -> defaults.priceLayout
        "network" -> defaults.networkLayout
        "code_box" -> defaults.codeBoxLayout
        "qr" -> defaults.qrCodeLayout
        "meta" -> defaults.metaLayout
        "phone" -> defaults.supportPhoneLayout
        "date" -> defaults.creationDateLayout
        "serial" -> defaults.serialLayout
        else -> defaults.codeBoxLayout
    }
}

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
