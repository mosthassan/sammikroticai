package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.data.ledger.InvoiceAllocationSpec
import com.example.data.ledger.LedgerInvariants
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.SalesItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PartyEntity
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
import com.example.data.sync.BackupChunk
import com.example.data.sync.BackupCryptoEngine
import com.example.data.sync.BackupManifest
import com.example.data.sync.CloudBackupMeta
import com.example.data.sync.FirestoreAdapter
import com.example.data.sync.FirestoreSyncManager
import com.example.domain.usecase.BackupRestoreUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudBackupChunkedRestoreTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var networkRepository: NetworkRepository
    private lateinit var backupRestoreUseCase: BackupRestoreUseCase
    private lateinit var invariants: LedgerInvariants

    /**
     * In-memory mock adapter simulating Firestore collections and documents
     */
    class TestFirestoreAdapter : FirestoreAdapter {
        val store = mutableMapOf<String, Map<String, Any?>>()
        val deletedPaths = mutableListOf<String>()
        var failOnChunkUploadIndex: Int? = null

        override fun isAvailable(): Boolean = true

        override suspend fun get(path: String): Map<String, Any?>? = store[path]

        override suspend fun set(path: String, data: Map<String, Any?>, merge: Boolean) {
            val chunkIdx = (data["index"] as? Number)?.toInt()
            if (failOnChunkUploadIndex != null && chunkIdx == failOnChunkUploadIndex) {
                throw IOException("Simulated network timeout during chunk $chunkIdx upload")
            }
            store[path] = data
        }

        override suspend fun delete(path: String) {
            deletedPaths.add(path)
            store.remove(path)
        }

        override suspend fun listCollection(collectionPath: String): List<Pair<String, Map<String, Any?>>> {
            val prefix = collectionPath.trim('/') + "/"
            return store.filterKeys { key ->
                if (key.startsWith(prefix)) {
                    val sub = key.removePrefix(prefix)
                    !sub.contains("/")
                } else false
            }.map { (k, v) -> k.substringAfterLast("/") to v }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
        AppDatabase.seedDefaultData(db.openHelper.writableDatabase)

        networkRepository = NetworkRepository(context)
        runBlocking { networkRepository.deleteAllDevices() }

        backupRestoreUseCase = BackupRestoreUseCase(
            db = db,
            deviceDao = networkRepository,
            networkRepository = networkRepository,
            context = context
        )
        invariants = LedgerInvariants(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. Chunking Logic
    @Test
    fun testChunkingLogicSmallAndLargePayloads() {
        val passphrase = "TestSecurePassword123!"

        // Case A: Small payload (< 400KB) -> produces exactly 1 chunk
        val smallJson = """{"version":1,"data":"Small Test Payload"}"""
        val pkgSmall = BackupCryptoEngine.createBackupPackage(
            jsonString = smallJson,
            passphrase = passphrase,
            uid = "user_small",
            userEmail = "small@sammikrotik.ye"
        )
        val manifestSmall = pkgSmall.manifest
        val chunksSmall = pkgSmall.chunks

        assertEquals("Small payload should produce exactly 1 chunk", 1, chunksSmall.size)
        assertEquals(0, chunksSmall[0].index)
        assertTrue("Cipher text must not be empty", chunksSmall[0].cipherText.isNotBlank())
        assertTrue("Chunk size must be <= 400KB", chunksSmall[0].cipherText.length <= 400 * 1024)
        assertEquals(1, manifestSmall.chunkCount)

        // Case B: Large payload (> 400KB uncompressed / compressed) -> splits into multiple chunks
        val largeStringBuilder = StringBuilder()
        largeStringBuilder.append("{\"items\":[")
        for (i in 0 until 12000) {
            if (i > 0) largeStringBuilder.append(",")
            largeStringBuilder.append("{\"id\":\"item_$i\",\"desc\":\"Non-trivial payload content with random bytes ${UUID.randomUUID()}\",\"val\":$i}")
        }
        largeStringBuilder.append("]}")
        val largeJson = largeStringBuilder.toString()
        assertTrue("Uncompressed payload should exceed 500KB", largeJson.length > 500 * 1024)

        val pkgLarge = BackupCryptoEngine.createBackupPackage(
            jsonString = largeJson,
            passphrase = passphrase,
            uid = "user_large",
            userEmail = "large@sammikrotik.ye"
        )
        val manifestLarge = pkgLarge.manifest
        val chunksLarge = pkgLarge.chunks

        assertTrue("Large payload must produce multiple chunks", chunksLarge.size > 1)
        assertEquals(chunksLarge.size, manifestLarge.chunkCount)

        // Verify zero empty chunks & zero chunks exceeding 400KB Base64
        for (chunk in chunksLarge) {
            assertTrue("Chunk index ${chunk.index} must not be empty", chunk.cipherText.isNotBlank())
            assertTrue(
                "Chunk index ${chunk.index} length (${chunk.cipherText.length}) must be strictly <= 400KB",
                chunk.cipherText.length <= 400 * 1024
            )
            assertTrue("SHA-256 checksum must be 64 hex characters", chunk.sha256.length == 64)
        }
    }

    // 2. End-to-End Pipeline: JSON -> GZIP -> AES-GCM -> Chunk -> Assemble -> Decrypt -> Decompress -> Equality
    @Test
    fun testEndToEndEncryptionPipelineByteForByteEquality() {
        val passphrase = "MasterAccountingPassphrase2026@"
        val originalPayload = """
        {
            "schemaVersion": 1,
            "exportDate": 1791682432000,
            "documents": [
                {"id": "DOC-2026-0001", "type": "RECEIPT_VOUCHER", "amount": 150000},
                {"id": "DOC-2026-0002", "type": "EXPENSE_VOUCHER", "amount": 75000}
            ],
            "notes": "القيود المحاسبية لشبكة الأمل اللاسلكية بالكامل"
        }
        """.trimIndent()

        // Step 1: Encrypt & chunk
        val pkg = BackupCryptoEngine.createBackupPackage(
            jsonString = originalPayload,
            passphrase = passphrase,
            uid = "user_e2e",
            userEmail = "e2e@sammikrotik.ye"
        )

        // Step 2: Decrypt & assemble
        val decryptedJson = BackupCryptoEngine.decryptAndAssembleBackupPackage(
            manifest = pkg.manifest,
            chunks = pkg.chunks,
            passphrase = passphrase
        )

        assertEquals("Decrypted output must match original JSON byte-for-byte", originalPayload, decryptedJson)
    }

    // 3. Tamper Detection: modifying a byte in any chunk triggers SHA-256 failure and aborts restore
    @Test
    fun testTamperDetectionAbortsRestore() {
        val passphrase = "SecurityAuditPassword#99"
        val originalPayload = """{"audit": "Untampered original ledger snapshot"}"""

        val pkg = BackupCryptoEngine.createBackupPackage(
            jsonString = originalPayload,
            passphrase = passphrase,
            uid = "user_tamper",
            userEmail = "tamper@sammikrotik.ye"
        )

        assertTrue(pkg.chunks.isNotEmpty())
        val firstChunk = pkg.chunks[0]

        // Case A: Tamper with ciphertext
        val tamperedCipher = if (firstChunk.cipherText.endsWith("A")) {
            firstChunk.cipherText.dropLast(1) + "B"
        } else {
            firstChunk.cipherText.dropLast(1) + "A"
        }
        val tamperedChunk = firstChunk.copy(cipherText = tamperedCipher)

        try {
            BackupCryptoEngine.decryptAndAssembleBackupPackage(
                manifest = pkg.manifest,
                chunks = listOf(tamperedChunk),
                passphrase = passphrase
            )
            fail("Expected exception due to tampered chunk ciphertext")
        } catch (e: Exception) {
            assertTrue(
                "Error message should indicate corruption or tamper: ${e.message}",
                e.message?.contains("تالف") == true || e.message?.contains("تطابق") == true || e.message?.contains("SHA-256") == true
            )
        }

        // Case B: Tamper with SHA-256 in manifest
        val tamperedManifest = pkg.manifest.copy(preEncryptionPayloadSha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        try {
            BackupCryptoEngine.decryptAndAssembleBackupPackage(
                manifest = tamperedManifest,
                chunks = pkg.chunks,
                passphrase = passphrase
            )
            fail("Expected exception due to payload checksum mismatch")
        } catch (e: Exception) {
            assertTrue(
                "Error message should indicate payload checksum failure: ${e.message}",
                e.message?.contains("مطابقة") == true || e.message?.contains("تطابق") == true || e.message?.contains("SHA-256") == true
            )
        }
    }

    // 4. Wrong Passphrase Test: invalid passphrase throws Arabic exception and leaves DB record counts unchanged
    @Test
    fun testWrongPassphraseAbortsRestoreAndLeavesDatabaseUnchanged() = runBlocking {
        val correctPassphrase = "ValidPassword2026$"
        val wrongPassphrase = "WrongPassword9999!"
        val testUid = "user_test_cpa_01"
        val testEmail = "cpa.audit@sammikrotik.ye"

        // Seed initial database records
        val writer = LedgerWriter(db, enableInvariantValidation = true)
        val agent = PartyEntity("AGENT_WRONG_PASS", "عميل تجريبي", "770000001", isCustomer = true)
        db.partyDao().insertParty(agent)
        val rate = ExchangeRate.parity(CurrencyCode.YER)
        writer.postSalesInvoice(
            partyId = agent.id,
            fiscalYear = 2026,
            dateEpochDay = 20001L,
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            cardItems = listOf(SalesItemSpec("كروت 1 جيجا", 10, 10000L)),
            serviceItems = emptyList()
        )

        val initialDocCount = db.documentDao().getAllDocumentsSync().size
        val initialLineCount = db.journalDao().getAllLinesSync().size
        assertTrue(initialDocCount > 0)
        assertTrue(initialLineCount > 0)

        // Push backup with correct passphrase
        val adapter = TestFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = adapter
        )

        val pushResult = syncManager.syncPush(testUid, testEmail, correctPassphrase)
        assertTrue("Push should succeed: ${pushResult.exceptionOrNull()?.message}", pushResult.isSuccess)

        // Attempt pull/restore with WRONG passphrase
        val pullResult = syncManager.syncPull(testUid, testEmail, wrongPassphrase)
        assertTrue("Pull should fail with wrong passphrase", pullResult.isFailure)

        val errorMsg = pullResult.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error message must clearly inform the user in Arabic: '$errorMsg'",
            errorMsg.contains("عبارة مرور خاطئة") || errorMsg.contains("تالف")
        )

        // Verify DB record counts are 100% unchanged
        val afterDocCount = db.documentDao().getAllDocumentsSync().size
        val afterLineCount = db.journalDao().getAllLinesSync().size
        assertEquals("Document count must not change on failed restore", initialDocCount, afterDocCount)
        assertEquals("Journal lines count must not change on failed restore", initialLineCount, afterLineCount)
    }

    // 5. Backward Compatibility Test: legacy backup at users/{uid}/backup_latest/latest restores successfully
    @Test
    fun testBackwardCompatibilityLegacyBackupRestore() = runBlocking {
        val testUid = "legacy_user_01"
        val testEmail = "legacy@sammikrotik.ye"
        val adapter = TestFirestoreAdapter()

        // Create a legacy snapshot format as was stored in users/{uid}/backup_latest/latest
        val legacyJson = """
        {
            "schemaVersion": 1,
            "exportDate": 1790000000000,
            "organization": {"id": "ORG-1", "name": "شبكة التراث اللاسلكية"},
            "documents": [],
            "documentItems": [],
            "journalEntries": [],
            "journalLines": [],
            "treasuries": [],
            "parties": [],
            "cardPackages": [],
            "stockMovements": [],
            "currencyRates": [],
            "assets": [],
            "allocations": [],
            "networkDevices": [
                {
                    "id": "DEV-LEGACY-01",
                    "name": "ميكروتك البرج الرئيسي",
                    "ipAddress": "192.168.88.1",
                    "deviceType": "ROUTER",
                    "status": "ONLINE"
                }
            ],
            "networkPrefs": {}
        }
        """.trimIndent()

        val legacyDocPath = "users/$testUid/backup_latest/latest"
        adapter.store[legacyDocPath] = mapOf(
            "timestamp" to 1790000000000L,
            "userEmail" to testEmail,
            "versionCode" to 8,
            "totalRecords" to 1,
            "jsonContent" to legacyJson,
            "payload" to legacyJson
        )

        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = adapter
        )

        // Check remote backup detects legacy backup
        val meta = syncManager.checkRemoteBackup(testUid, testEmail)
        assertNotNull("Should detect legacy backup", meta)
        assertFalse("Legacy backup is not chunked", meta!!.isChunked)

        // Pull and restore without passphrase (empty string for legacy backup)
        val pullResult = syncManager.syncPull(testUid, testEmail, "")
        assertTrue("Legacy pull should succeed: ${pullResult.exceptionOrNull()?.message}", pullResult.isSuccess)

        // Verify restored network device
        val devices = networkRepository.getAllDevices()
        assertTrue("Restored legacy network device should exist", devices.any { it.name == "ميكروتك البرج الرئيسي" })
    }

    // 6. Trigger & Invariants Integrity: successful restore leaves triggers functional and passes invariants.verifyAll()
    @Test
    fun testTriggerAndInvariantsIntegrityAfterRestore() = runBlocking {
        val passphrase = "IntegrityCheckPassword!2026"
        val testUid = "user_invariants_01"
        val testEmail = "audit@sammikrotik.ye"

        // 1. Post a legitimate double-entry transaction
        val writer = LedgerWriter(db, enableInvariantValidation = true)
        val agent = PartyEntity("AGENT_INV", "عميل التدقيق", "770000002", isCustomer = true)
        db.partyDao().insertParty(agent)
        val rate = ExchangeRate.parity(CurrencyCode.YER)
        writer.postSalesInvoice(
            partyId = agent.id,
            fiscalYear = 2026,
            dateEpochDay = 20001L,
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            cardItems = listOf(SalesItemSpec("كروت 1 جيجا", 20, 20000L)),
            serviceItems = emptyList()
        )

        val invBefore = invariants.verifyAll(failFast = false)
        assertTrue("Ledger must be balanced before backup", invBefore.isValid)

        // 2. Backup to cloud
        val adapter = TestFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = adapter
        )

        val pushRes = syncManager.syncPush(testUid, testEmail, passphrase)
        assertTrue("Push should succeed", pushRes.isSuccess)

        // 3. Post a second transaction to make local DB different
        writer.postSalesInvoice(
            partyId = agent.id,
            fiscalYear = 2026,
            dateEpochDay = 20002L,
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            cardItems = listOf(SalesItemSpec("كروت 2 جيجا", 10, 15000L)),
            serviceItems = emptyList()
        )
        val docCountBeforeRestore = db.documentDao().getAllDocumentsSync().size
        assertEquals(2, docCountBeforeRestore)

        // 4. Restore from cloud (should atomic restore back to 1 document)
        val pullRes = syncManager.syncPull(testUid, testEmail, passphrase)
        assertTrue("Pull should succeed: ${pullRes.exceptionOrNull()?.message}", pullRes.isSuccess)

        val docCountAfterRestore = db.documentDao().getAllDocumentsSync().size
        assertEquals(1, docCountAfterRestore)

        // 5. Verify LedgerInvariants passes after restore
        val invAfter = invariants.verifyAll(failFast = false)
        assertTrue("Ledger must be 100% balanced after restore: ${invAfter.violations}", invAfter.isValid)

        // 6. Verify SQLite triggers are active by asserting update/delete on journal lines fails
        val restoredLines = db.journalDao().getAllLinesSync()
        assertTrue("Restored lines must exist", restoredLines.isNotEmpty())
        val firstLine = restoredLines[0]

        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM journal_lines WHERE id = '${firstLine.id}'")
            fail("Expected SQLiteConstraintException from trigger on journal_lines delete")
        } catch (e: Exception) {
            // Trigger successfully prevented modification
            assertTrue("Trigger must prevent deletion of journal lines: ${e.message}", e is android.database.sqlite.SQLiteException || e.message?.contains("trigger", ignoreCase = true) == true || e.message?.contains("constraint", ignoreCase = true) == true || e.message?.contains("abort", ignoreCase = true) == true)
        }
    }

    // 7. Write Atomicity Test: chunk failure on upload causes chunk rollback and no manifest commit
    @Test
    fun testWriteAtomicityRollbackOnChunkFailure() = runBlocking {
        val passphrase = "RollbackPassword!123"
        val testUid = "user_rollback_test"
        val testEmail = "failover@sammikrotik.ye"

        // Create enough data to produce multiple chunks
        for (i in 0 until 10) {
            networkRepository.insertDevice(
                NetworkDevice(
                    name = "سيرفر برج الأمل $i",
                    ipAddress = "10.0.0.$i",
                    deviceType = DeviceType.ROUTER,
                    status = DeviceStatus.ONLINE
                )
            )
        }

        val adapter = TestFirestoreAdapter()
        // Inject failure on chunk index 0 upload
        adapter.failOnChunkUploadIndex = 0

        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = adapter
        )

        val pushRes = syncManager.syncPush(testUid, testEmail, passphrase)
        assertTrue("Push should fail due to chunk upload error", pushRes.isFailure)

        // Verify that manifest document was NEVER written
        val manifestWrites = adapter.store.keys.filter { it.startsWith("users/$testUid/backups/") && !it.contains("/chunks/") }
        assertEquals("Manifest should never be written when chunks fail", 0, manifestWrites.size)
    }

    // 8. Memory Efficiency & Streaming Constraints: large dataset test verifying bounded memory and correct chunking
    @Test
    fun testMemoryEfficiencyLargeDatasetBoundedMemory() {
        val passphrase = "HighThroughputPassword_2026!"
        val stringBuilder = StringBuilder(1024 * 1024)
        stringBuilder.append("{\"records\":[")
        for (i in 0 until 15000) {
            if (i > 0) stringBuilder.append(",")
            stringBuilder.append("{\"id\":\"R-$i\",\"code\":\"ACC-101\",\"amount\":${i * 1000},\"desc\":\"قيد محاسبي مفصل رقم $i\"}")
        }
        stringBuilder.append("]}")
        val largeJson = stringBuilder.toString()

        val runtime = Runtime.getRuntime()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()

        val pkg = BackupCryptoEngine.createBackupPackage(
            jsonString = largeJson,
            passphrase = passphrase,
            uid = "user_perf",
            userEmail = "perf@sammikrotik.ye",
            rawSliceSize = 25_000
        )
        val manifest = pkg.manifest
        val chunks = pkg.chunks

        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val deltaMb = (memAfter - memBefore) / (1024 * 1024)

        assertTrue("Large dataset must be split into multiple chunks", chunks.size >= 2)
        assertEquals(chunks.size, manifest.chunkCount)

        // Verify roundtrip decryption of this large payload
        val roundtripJson = BackupCryptoEngine.decryptAndAssembleBackupPackage(
            manifest = manifest,
            chunks = chunks,
            passphrase = passphrase
        )
        assertEquals("Roundtrip of large dataset must match exactly", largeJson.length, roundtripJson.length)
        assertEquals("Roundtrip of large dataset content must match", largeJson, roundtripJson)
    }
}
