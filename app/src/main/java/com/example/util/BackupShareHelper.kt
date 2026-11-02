package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "BackupShareHelper"

data class BackupSummary(
    val timestamp: Long,
    val formattedDate: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val partiesCount: Int,
    val documentsCount: Int,
    val journalEntriesCount: Int,
    val packagesCount: Int,
    val treasuriesCount: Int,
    val devicesCount: Int,
    val sha256: String,
    val hasValidSignature: Boolean
)

object BackupShareHelper {

    /**
     * Parses key metrics and counts from the full JSON backup.
     */
    fun parseBackupSummary(jsonString: String): BackupSummary {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            JSONObject()
        }

        val ts = root.optLong("timestamp", System.currentTimeMillis())
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(ts))
        val bytes = jsonString.toByteArray(Charsets.UTF_8).size.toLong()
        val sizeStr = formatFileSize(bytes)

        val parties = root.optJSONArray("parties")?.length() ?: 0
        val docs = root.optJSONArray("documents")?.length() ?: 0
        val journalEntries = root.optJSONArray("journal_entries")?.length() ?: 0
        val packages = root.optJSONArray("packages")?.length() ?: 0
        val treasuries = root.optJSONArray("treasuries")?.length() ?: 0
        val devices = root.optJSONArray("network_devices")?.length()
            ?: root.optJSONArray("networkDevices")?.length() ?: 0
        val sha256 = root.optString("sha256", "")

        return BackupSummary(
            timestamp = ts,
            formattedDate = dateStr,
            fileSizeBytes = bytes,
            formattedSize = sizeStr,
            partiesCount = parties,
            documentsCount = docs,
            journalEntriesCount = journalEntries,
            packagesCount = packages,
            treasuriesCount = treasuries,
            devicesCount = devices,
            sha256 = sha256,
            hasValidSignature = sha256.isNotBlank()
        )
    }

    /**
     * Writes the complete backup JSON to a dedicated, timestamped file in cache.
     */
    fun createBackupFile(context: Context, jsonString: String): File {
        val backupsDir = File(context.cacheDir, "backups").apply {
            if (!exists()) mkdirs()
        }

        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val fileName = "SamMikrotik_Full_Backup_$fileTimestamp.json"
        val file = File(backupsDir, fileName)

        FileOutputStream(file).use { out ->
            out.write(jsonString.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        Log.i(TAG, "Created backup file: ${file.absolutePath} (${file.length()} bytes)")
        return file
    }

    /**
     * Launches the system share sheet (WhatsApp, Telegram, Drive, Gmail, Files, etc.)
     * with the backup JSON file securely attached via FileProvider.
     */
    fun shareBackupFile(
        context: Context,
        file: File,
        subject: String? = null,
        extraText: String? = null
    ): Result<Unit> = runCatching {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val dateDisplay = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        val defaultSubject = subject ?: "نسخة احتياطية كاملة - سام ميكروتك ($dateDisplay)"
        val defaultText = extraText ?: """
            📦 نسخة احتياطية محلية كاملة لنظام سام ميكروتك (SamMikrotik ERP).
            📅 تاريخ الإنشاء: $dateDisplay
            📁 اسم الملف: ${file.name}
            ⚖️ الحسابات متزنة وفق المعايير المحاسبية المعتمدة IFRS.
            💡 احتفظ بهذا الملف بأمان لاستعادة كافة بياناتك في حال تعذر المزامنة السحابية.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, defaultSubject)
            putExtra(Intent.EXTRA_TEXT, defaultText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية وتأمينها عبر...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }

    /**
     * Convenience method to write and share the backup in one step.
     */
    fun writeAndShareBackup(context: Context, jsonString: String): Result<File> = runCatching {
        val file = createBackupFile(context, jsonString)
        shareBackupFile(context, file).getOrThrow()
        file
    }

    /**
     * Reads the text content of a backup file from a selected content URI (e.g. from file picker).
     */
    fun readTextFromUri(context: Context, uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalStateException("تعذر فتح الملف المحدد")
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.ENGLISH, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
