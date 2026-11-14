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
     * Generates a complete, fault-tolerant RouterOS .rsc script dynamically mapped
     * to the SaaS Plan Object (NetworkPlanSpec) with native limiters and :do on-error={} wrapper.
     */
    fun generateRouterOsScript(batch: CardBatch): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val formattedDate = dateFormat.format(Date(batch.createdAt))
        val plan = batch.getEffectivePlan()
        val sb = StringBuilder()

        sb.appendLine("# ================================================================")
        sb.appendLine("# SamMikrotik Dynamic SaaS Hotspot Batch #${batch.batchNumber}")
        sb.appendLine("# Batch Title: ${batch.batchTitle}")
        sb.appendLine("# Plan: ${plan.name} | Profile: ${plan.profileName}")
        sb.appendLine("# Quota: ${if (plan.dataQuotaMb > 0) "${plan.dataQuotaMb} MB" else "Unlimited"} | Uptime Limit: ${if (plan.uptimeHours > 0) "${plan.uptimeHours} Hours" else "Unlimited"}")
        sb.appendLine("# Total Cards: ${batch.cardCount} | Generated At: $formattedDate")
        sb.appendLine("# ================================================================")
        sb.appendLine()

        val comment = escapeRsc(batch.technicalMikrotikComment)
        val quotaParam = if (plan.dataQuotaMb > 0) "limit-bytes-total=${plan.dataQuotaMb * 1048576L} " else ""
        val uptimeParam = if (plan.uptimeHours > 0) "limit-uptime=${plan.uptimeHours}h " else ""
        val profileParam = escapeRsc(plan.profileName)

        for (card in batch.cards) {
            val u = escapeRsc(card.username)
            val p = escapeRsc(card.password)
            sb.appendLine(":do { /ip hotspot user add name=\"$u\" password=\"$p\" profile=\"$profileParam\" $quotaParam$uptimeParam server=all comment=\"$comment\" } on-error={}")
        }

        sb.appendLine()
        sb.appendLine("# Log batch import to RouterOS system log")
        sb.appendLine(":log info \"[SamMikrotik] Injected Batch #${batch.batchNumber} (${batch.cardCount} cards) for profile '$profileParam'\"")
        sb.appendLine("# End of SamMikrotik Script")

        return sb.toString()
    }

    /**
     * Copies the .rsc script directly to system clipboard
     */
    fun copyScriptToClipboard(context: Context, batch: CardBatch): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboard != null) {
                val clip = android.content.ClipData.newPlainText("RouterOS Script", generateRouterOsScript(batch))
                clipboard.setPrimaryClip(clip)
                true
            } else {
                false
            }
        }.getOrDefault(false)
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
        val comment = escapeRsc(batch.technicalMikrotikComment)
        for (card in batch.cards) {
            sb.appendLine("add username=\"${escapeRsc(card.username)}\" password=\"${escapeRsc(card.password)}\" customer=admin comment=\"$comment\"")
        }
        return sb.toString()
    }

    /**
     * Generates a rollback script to safely remove all users from this specific batch
     */
    fun generateRemovalScript(batch: CardBatch): String {
        val comment = escapeRsc(batch.technicalMikrotikComment)
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
        val plan = batch.getEffectivePlan()
        val dir = File(context.cacheDir, "mikrotik_scripts").apply { mkdirs() }
        val filename = "sammikrotik_batch_${batch.batchNumber}_${plan.profileName}.rsc"
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
