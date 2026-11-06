package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.card.CardBatch
import com.example.data.card.GeneratedCard
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MikroTik Hotspot & User Manager Script & Provisioning Engine.
 * Generates RouterOS .rsc scripts, exports files, and formats batch commands.
 */
object MikroTikCardProvisioner {

    /**
     * Generates a complete RouterOS .rsc script ready to paste in New Terminal
     * or import via "/import batch.rsc"
     */
    fun generateRouterOsScript(batch: CardBatch): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val formattedDate = dateFormat.format(Date(batch.createdAt))
        val sb = StringBuilder()

        sb.appendLine("# ================================================================")
        sb.appendLine("# SamMikrotik Hotspot Card Batch #${batch.batchNumber}")
        sb.appendLine("# Title: ${batch.batchTitle}")
        sb.appendLine("# Generated at: $formattedDate")
        sb.appendLine("# Total Cards: ${batch.cardCount}")
        sb.appendLine("# Profile: ${batch.profileName.ifBlank { "default" }}")
        sb.appendLine("# Price: ${batch.templateSnapshot.priceText} | Quota: ${batch.templateSnapshot.quotaText}")
        sb.appendLine("# ================================================================")
        sb.appendLine()
        sb.appendLine("/ip hotspot user")

        val comment = "Batch_${batch.batchNumber}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(batch.createdAt))}"

        for (card in batch.cards) {
            sb.append("add name=\"${escapeRsc(card.username)}\" ")
            sb.append("password=\"${escapeRsc(card.password)}\" ")
            if (batch.profileName.isNotBlank()) {
                sb.append("profile=\"${escapeRsc(batch.profileName)}\" ")
            }
            sb.append("comment=\"$comment\"\n")
        }

        sb.appendLine()
        sb.appendLine("# Log success to RouterOS system log")
        sb.appendLine(":log info \"[SamMikrotik] Successfully provisioned Batch #${batch.batchNumber} with ${batch.cardCount} cards\"")
        sb.appendLine("# End of SamMikrotik Script")

        return sb.toString()
    }

    /**
     * Generates User Manager commands for RouterOS v6 / v7
     */
    fun generateUserManagerScript(batch: CardBatch): String {
        val sb = StringBuilder()
        sb.appendLine("# ================================================================")
        sb.appendLine("# SamMikrotik User Manager Script - Batch #${batch.batchNumber}")
        sb.appendLine("# ================================================================")
        sb.appendLine("/tool user-manager user")
        for (card in batch.cards) {
            sb.appendLine("add username=\"${escapeRsc(card.username)}\" password=\"${escapeRsc(card.password)}\" customer=admin")
        }
        return sb.toString()
    }

    /**
     * Generates a rollback script to safely remove all users from this specific batch
     */
    fun generateRemovalScript(batch: CardBatch): String {
        val comment = "Batch_${batch.batchNumber}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(batch.createdAt))}"
        return """
            # ================================================================
            # SamMikrotik Rollback Script for Batch #${batch.batchNumber}
            # ================================================================
            /ip hotspot user remove [find comment="$comment"]
            :log warning "[SamMikrotik] Removed all cards for Batch #${batch.batchNumber}"
        """.trimIndent()
    }

    /**
     * Saves the .rsc script to private cache/files and returns a shareable File
     */
    fun saveScriptToFile(context: Context, batch: CardBatch): File {
        val dir = File(context.cacheDir, "mikrotik_scripts").apply { mkdirs() }
        val filename = "sammikrotik_batch_${batch.batchNumber}_${batch.profileName.ifBlank { "cards" }}.rsc"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            out.write(generateRouterOsScript(batch).toByteArray(Charsets.UTF_8))
        }

        return file
    }

    /**
     * Shares the .rsc file via Android Sharesheet
     */
    fun shareScriptFile(context: Context, batch: CardBatch) {
        val file = saveScriptToFile(context, batch)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "سكربت ميكروتك - دفعة كروت #${batch.batchNumber}")
            putExtra(Intent.EXTRA_TEXT, "مرفق سكربت شحن كروت الميكروتك دفعة رقم #${batch.batchNumber} (${batch.cardCount} كرت)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة سكربت الميكروتك عبر"))
    }

    private fun escapeRsc(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
