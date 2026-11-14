package com.example.data.network

enum class HotspotUserStatus(val labelArabic: String) {
    ACTIVE("نشط"),
    UNUSED("جديد / غير مستخدم"),
    EXPIRED("منتهي")
}

data class HotspotUser(
    val id: String,
    val username: String,
    val profile: String = "default",
    val uptimeSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val limitBytesTotal: Long = 0L,
    val limitUptimeSeconds: Long = 0L,
    val comment: String = "",
    val isOnline: Boolean = false
) {
    val totalBytesUsed: Long
        get() = if (bytesIn < 0L || bytesOut < 0L || Long.MAX_VALUE - bytesIn < bytesOut) {
            Long.MAX_VALUE
        } else {
            bytesIn + bytesOut
        }

    val status: HotspotUserStatus
        get() = when {
            !isOnline && (
                (limitBytesTotal > 0L && totalBytesUsed >= limitBytesTotal) ||
                    (limitUptimeSeconds > 0L && uptimeSeconds >= limitUptimeSeconds) ||
                    comment.contains("expired", ignoreCase = true) ||
                    comment.contains("منتهي", ignoreCase = true)
                ) -> HotspotUserStatus.EXPIRED
            isOnline || uptimeSeconds > 0L || totalBytesUsed > 0L -> HotspotUserStatus.ACTIVE
            else -> HotspotUserStatus.UNUSED
        }
}

internal fun parseNonNegativeLong(raw: String?): Long {
    val parsed = raw?.trim()?.toLongOrNull() ?: return 0L
    return parsed.coerceAtLeast(0L)
}

internal fun parseRouterOsDurationSeconds(raw: String?): Long {
    val input = raw?.trim()?.lowercase().orEmpty()
    if (input.isEmpty() || input == "0" || input == "0s") return 0L

    input.toLongOrNull()?.let { return it.coerceAtLeast(0L) }

    var remaining = input
    var totalSeconds = 0L

    val unitRegex = Regex("""(\d+)\s*([wdhms])""")
    val matches = unitRegex.findAll(remaining).toList()
    if (matches.isNotEmpty()) {
        for (m in matches) {
            val value = m.groupValues[1].toLongOrNull() ?: 0L
            val multiplier = when (m.groupValues[2]) {
                "w" -> 604_800L
                "d" -> 86_400L
                "h" -> 3_600L
                "m" -> 60L
                "s" -> 1L
                else -> 0L
            }
            totalSeconds += value * multiplier
        }
        remaining = unitRegex.replace(remaining, "").trim()
    }

    if (remaining.contains(":")) {
        val parts = remaining.split(":").mapNotNull { it.trim().toLongOrNull() }
        totalSeconds += when (parts.size) {
            3 -> parts[0] * 3_600L + parts[1] * 60L + parts[2]
            2 -> parts[0] * 60L + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }

    return totalSeconds.coerceAtLeast(0L)
}
