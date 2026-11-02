package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BackupShareHelper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupShareHelperTest {

    @Test
    fun testParseBackupSummaryWithCompletePayload() {
        val json = JSONObject().apply {
            put("timestamp", 1700000000000L)
            put("sha256", "abc123def4567890abcdef")
            put("parties", JSONArray().put(JSONObject()).put(JSONObject()))
            put("documents", JSONArray().put(JSONObject()).put(JSONObject()).put(JSONObject()))
            put("journal_entries", JSONArray().put(JSONObject()))
            put("packages", JSONArray().put(JSONObject()).put(JSONObject()))
            put("treasuries", JSONArray().put(JSONObject()))
            put("network_devices", JSONArray().put(JSONObject()).put(JSONObject()).put(JSONObject()).put(JSONObject()))
        }.toString()

        val summary = BackupShareHelper.parseBackupSummary(json)

        assertEquals(1700000000000L, summary.timestamp)
        assertEquals(2, summary.partiesCount)
        assertEquals(3, summary.documentsCount)
        assertEquals(1, summary.journalEntriesCount)
        assertEquals(2, summary.packagesCount)
        assertEquals(1, summary.treasuriesCount)
        assertEquals(4, summary.devicesCount)
        assertEquals("abc123def4567890abcdef", summary.sha256)
        assertTrue(summary.hasValidSignature)
        assertTrue(summary.formattedSize.isNotBlank())
    }

    @Test
    fun testParseBackupSummaryWithInvalidJsonDoesNotCrash() {
        val summary = BackupShareHelper.parseBackupSummary("{ this is broken json ...")

        assertEquals(0, summary.partiesCount)
        assertEquals(0, summary.documentsCount)
        assertEquals(0, summary.journalEntriesCount)
        assertEquals("", summary.sha256)
        assertFalse(summary.hasValidSignature)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 B", BackupShareHelper.formatFileSize(500))
        assertEquals("2.0 KB", BackupShareHelper.formatFileSize(2048))
        assertEquals("1.50 MB", BackupShareHelper.formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCreateBackupFileInCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testJson = "{\"version\": 1, \"appName\": \"SamMikrotik\"}"

        val file = BackupShareHelper.createBackupFile(context, testJson)

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.name.startsWith("SamMikrotik_Full_Backup_"))
        assertTrue(file.name.endsWith(".json"))
        assertEquals(testJson, file.readText())
    }
}
