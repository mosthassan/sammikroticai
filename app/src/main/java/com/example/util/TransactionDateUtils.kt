package com.example.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Standardized transaction and document date utility for SamMikrotik.
 * Ensures consistent date formatting with uppercase "MM" (Month) and Locale.US
 * across all financial entities, documents, vouchers, invoices, and reports.
 */
object TransactionDateUtils {

    /**
     * Standard date format pattern: yyyy/MM/dd (Year / Month / Day)
     * NOTE: Uppercase 'MM' is strictly used for month to avoid minute ('mm') collision.
     */
    const val PATTERN_DATE = "yyyy/MM/dd"

    /**
     * Standard date-time format pattern: yyyy/MM/dd HH:mm
     */
    const val PATTERN_DATETIME = "yyyy/MM/dd HH:mm"

    /**
     * Standard date-time format with seconds: yyyy/MM/dd HH:mm:ss
     */
    const val PATTERN_DATETIME_SEC = "yyyy/MM/dd HH:mm:ss"

    /**
     * Creates a new [SimpleDateFormat] instance with Locale.US for the standard date pattern.
     */
    fun createDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(PATTERN_DATE, Locale.US)
    }

    /**
     * Creates a new [SimpleDateFormat] instance with Locale.US for the date-time pattern.
     */
    fun createDateTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat(PATTERN_DATETIME, Locale.US)
    }

    /**
     * Formats an epoch-day (days since 1970-01-01) to "yyyy/MM/dd" using [Locale.US].
     */
    fun formatEpochDay(epochDay: Long): String {
        return try {
            val localDate = LocalDate.ofEpochDay(epochDay)
            String.format(Locale.US, "%04d/%02d/%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
        } catch (e: Exception) {
            val date = Date(epochDay * 86_400_000L)
            createDateFormat().format(date)
        }
    }

    /**
     * Formats a millisecond timestamp to "yyyy/MM/dd" using [Locale.US].
     */
    fun formatDate(timestampMs: Long): String {
        val ts = if (timestampMs > 0L) timestampMs else System.currentTimeMillis()
        return createDateFormat().format(Date(ts))
    }

    /**
     * Formats a millisecond timestamp to "yyyy/MM/dd HH:mm" using [Locale.US].
     */
    fun formatDateTime(timestampMs: Long): String {
        val ts = if (timestampMs > 0L) timestampMs else System.currentTimeMillis()
        return createDateTimeFormat().format(Date(ts))
    }

    /**
     * Returns current date string formatted as "yyyy/MM/dd".
     */
    fun todayDateString(): String {
        return createDateFormat().format(Date())
    }

    /**
     * Converts a millisecond timestamp to epoch-day.
     */
    fun timestampToEpochDay(timestampMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate().toEpochDay()
    }
}
