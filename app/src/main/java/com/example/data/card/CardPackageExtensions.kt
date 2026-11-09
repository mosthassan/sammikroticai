package com.example.data.card

import com.example.data.local.entity.CardPackageEntity

/**
 * Structured specifications parsed from a package's durationOrQuota and attributes.
 */
data class PackageParsedSpecs(
    val quotaMb: Long,
    val uptimeHours: Int,
    val validityDays: Int,
    val profileName: String
)

/**
 * Formats structured specs into a clean, backwards-compatible durationOrQuota string.
 */
fun formatDurationOrQuota(
    quotaMb: Long,
    uptimeHours: Int,
    validityDays: Int,
    profileName: String = ""
): String {
    val quotaPart = if (quotaMb > 0) "${quotaMb}MB" else "مفتوحة"
    val uptimePart = if (uptimeHours > 0) "${uptimeHours}H" else "مفتوح"
    val validityPart = if (validityDays > 0) "${validityDays}D" else "غير محدد"
    val profilePart = if (profileName.isNotBlank() && !profileName.equals("default", ignoreCase = true)) {
        profileName.trim()
    } else {
        "PKG_${if (quotaMb > 0) "${quotaMb}M" else "UNL"}_${if (uptimeHours > 0) "${uptimeHours}H" else "UNL"}"
    }

    return "سعة: $quotaPart • وقت: $uptimePart • صلاحية: $validityPart • بروفايل: $profilePart"
}

val CardPackageEntity.quotaMb: Long
    get() {
        val text = durationOrQuota.lowercase()
        val mbMatch = Regex("([0-9]+)\\s*(?:mb|ميجا|م)").find(text)
        if (mbMatch != null) {
            return mbMatch.groupValues[1].toLongOrNull() ?: 0L
        }
        val gbMatch = Regex("([0-9]+)\\s*(?:gb|جيجا|g)").find(text)
        if (gbMatch != null) {
            return (gbMatch.groupValues[1].toLongOrNull() ?: 0L) * 1024L
        }
        val num = Regex("[0-9]+").find(text)?.value?.toLongOrNull() ?: 0L
        return if (num > 100L) num else num * 1024L
    }

val CardPackageEntity.uptimeHours: Int
    get() {
        val text = durationOrQuota.lowercase()
        val hMatch = Regex("([0-9]+)\\s*(?:h|ساعة|ساعات|س)").find(text)
        if (hMatch != null) {
            return hMatch.groupValues[1].toIntOrNull() ?: 0
        }
        val dMatch = Regex("([0-9]+)\\s*(?:d|يوم|ايام)").find(text)
        if (dMatch != null) {
            return (dMatch.groupValues[1].toIntOrNull() ?: 0) * 24
        }
        return 0
    }

val CardPackageEntity.validityDays: Int
    get() {
        val text = durationOrQuota.lowercase()
        val dMatch = Regex("صلاحية:\\s*([0-9]+)\\s*(?:d|يوم|ايام)?").find(text)
            ?: Regex("([0-9]+)\\s*(?:d|يوم|ايام)").find(text)
        if (dMatch != null) {
            return dMatch.groupValues[1].toIntOrNull() ?: 0
        }
        val mMatch = Regex("([0-9]+)\\s*(?:شهر|اشهر)").find(text)
        if (mMatch != null) {
            return (mMatch.groupValues[1].toIntOrNull() ?: 0) * 30
        }
        val h = uptimeHours
        return if (h > 0) (h / 24).coerceAtLeast(1) else 0
    }

val CardPackageEntity.profileName: String
    get() {
        val text = durationOrQuota
        val profileMatch = Regex("بروفايل:\\s*([0-9a-zA-Z_\\-]+)").find(text)
        if (profileMatch != null) {
            val p = profileMatch.groupValues[1].trim()
            if (p.isNotBlank() && !p.equals("default", ignoreCase = true)) {
                return p
            }
        }
        val q = quotaMb
        val u = uptimeHours
        return "PKG_${if (q > 0) "${q}M" else "UNL"}_${if (u > 0) "${u}H" else "UNL"}"
    }
