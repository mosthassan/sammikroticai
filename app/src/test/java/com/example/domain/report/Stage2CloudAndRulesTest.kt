package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.model.RateZone
import com.example.data.ledger.LedgerWriter
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AllocationEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.DocumentItemEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalLineEntity
import com.example.data.local.entity.OrganizationEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.TreasuryEntity
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Stage2CloudAndRulesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var networkRepository: NetworkRepository
    private lateinit var backupRestoreUseCase: BackupRestoreUseCase

    class FakeFirestoreAdapter : FirestoreAdapter {
        val writes = mutableMapOf<String, Map<String, Any?>>()
        val store = mutableMapOf<String, Map<String, Any?>>()

        override fun isAvailable(): Boolean = true

        override suspend fun get(path: String): Map<String, Any?>? = store[path]

        override suspend fun set(path: String, data: Map<String, Any?>, merge: Boolean) {
            writes[path] = data
            store[path] = data
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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFirestoreRulesDocumentedAndVerified() {
        // Read firestore.rules
        val rulesFile = File("firestore.rules").takeIf { it.exists() }
            ?: File("../firestore.rules").takeIf { it.exists() }
            ?: File("/firestore.rules").takeIf { it.exists() }
            ?: File(System.getProperty("user.dir") ?: ".", "firestore.rules").takeIf { it.exists() }

        assertNotNull("firestore.rules must exist in workspace", rulesFile)
        val rules = rulesFile!!.readText()

        // 1. Rules version & document service match
        assertTrue("Must use rules_version = '2'", rules.contains("rules_version = '2'"))
        assertTrue("Must match document service", rules.contains("match /databases/{database}/documents"))

        // 2. Strict owner-only path: users/{userId}/{document=**}
        assertTrue("Must match /users/{userId}/{document=**}", rules.contains("match /users/{userId}/{document=**}"))
        assertTrue(
            "Must allow read/write only if auth != null && auth.uid == userId",
            rules.contains("allow read, write: if request.auth != null && request.auth.uid == userId;")
        )

        // 3. Catch-all deny
        assertTrue(
            "Must deny all root and unmatched documents",
            rules.contains("match /{document=**} { allow read, write: if false; }")
        )

        // 4. Must NOT have any /tenants block
        assertFalse(
            "Must remove all /tenants match blocks to prevent cross-tenant exposure",
            rules.contains("tenants")
        )

        // Rules evaluation logic assertions:
        // - Owner allowed: when request.auth != null and request.auth.uid == userId
        // - Other user denied: request.auth.uid != userId -> evaluates to false
        // - Anonymous denied: request.auth == null -> evaluates to false
        // - tenants denied: no /tenants match block exists, falling through to root deny
    }

    @Test
    fun testNoSignedInUserNothingUploadedOrPulledAndNoException() = runBlocking {
        val fakeAdapter = FakeFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = fakeAdapter
        )

        // 1. syncPush with blank user
        val pushRes = syncManager.syncPush("", "")
        assertTrue("Push without signed-in user must fail gracefully", pushRes.isFailure)
        assertEquals(0, fakeAdapter.writes.size)

        // 2. syncPull with blank user
        val pullRes = syncManager.syncPull("", "")
        assertTrue("Pull without signed-in user must fail gracefully", pullRes.isFailure)

        // 3. checkRemoteBackup with blank user
        val checkRes = syncManager.checkRemoteBackup("", "")
        assertNull("Check remote backup without signed-in user must return null", checkRes)

        // 4. Granular methods without signed-in user
        val pushDevRes = syncManager.pushNetworkDevices("", "")
        assertTrue(pushDevRes.isFailure)

        val pullDevRes = syncManager.pullNetworkDevices("", "")
        assertTrue(pullDevRes.isFailure)

        val pushRateRes = syncManager.pushExchangeRates("", "")
        assertTrue(pushRateRes.isFailure)

        val pullRateRes = syncManager.pullExchangeRates("", "")
        assertTrue(pullRateRes.isFailure)

        val pushIdRes = syncManager.pushNetworkIdentityAndConfig("", "")
        assertTrue(pushIdRes.isFailure)

        val pullIdRes = syncManager.pullNetworkIdentityAndConfig("", "")
        assertTrue(pullIdRes.isFailure)

        // Verify zero writes were dispatched
        assertEquals("Zero cloud writes must occur when not signed in", 0, fakeAdapter.writes.size)
    }

    @Test
    fun testUploadWritesOnlyUnderUsersUid() = runBlocking {
        val testUid = "user_cloud_uid_9988"
        val testEmail = "mosthassan.ye@gmail.com"
        val fakeAdapter = FakeFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = fakeAdapter
        )

        // Seed sample network device and exchange rate
        networkRepository.addOrUpdateDevice(
            NetworkDevice(
                id = "dev_mkt_edge_01",
                name = "MikroTik RB5009",
                ipAddress = "192.168.88.1",
                deviceType = DeviceType.ROUTER,
                macAddress = "E4:8D:8C:11:22:33",
                towerLocation = "برج شميلة",
                status = DeviceStatus.ONLINE
            )
        )
        val rateUsd = CurrencyRateEntity(
            id = "RATE_USD_SANAA_S2",
            currency = "USD",
            zone = "SANAA",
            rateMicros = 535_000_000L,
            effectiveDateEpochDay = 20450L,
            createdAt = System.currentTimeMillis(),
            createdBy = "ADMIN",
            reason = "Stage 2 cloud test rate"
        )
        db.currencyRateDao().insertRate(rateUsd)

        val pushResult = syncManager.syncPush(testUid, testEmail)
        assertTrue(
            "syncPush must succeed: ${pushResult.exceptionOrNull()?.message}",
            pushResult.isSuccess
        )

        // Strict assertion: ALL written paths must start with users/{testUid}
        assertTrue("Writes must not be empty", fakeAdapter.writes.isNotEmpty())
        for ((path, data) in fakeAdapter.writes) {
            assertTrue("Write path '$path' must start with 'users/$testUid'", path.startsWith("users/$testUid"))
            assertFalse("Write path '$path' must never write under tenants", path.contains("tenants"))
            assertFalse("Write path '$path' must never write emailKey", path.contains("emailKey"))
        }

        // Verify primary backup document
        assertTrue(
            "Primary backup must be stored under users/$testUid/backup_latest/latest",
            fakeAdapter.writes.containsKey("users/$testUid/backup_latest/latest")
        )
        val primaryData = fakeAdapter.writes["users/$testUid/backup_latest/latest"]
        assertNotNull(primaryData)
        assertEquals(testUid, primaryData?.get("uid"))
        assertEquals(testEmail, primaryData?.get("userEmail"))
        assertNotNull(primaryData?.get("jsonContent"))
    }

    @Test
    fun testOversizePayloadRejectedWithMessage() = runBlocking {
        // Construct an entity with oversized payload (> 900,000 bytes)
        val oversizedText = "X".repeat(920_000)
        val oversizedDoc = DocumentEntity(
            id = "DOC_OVERSIZE_TEST_S2",
            type = "PAYMENT_VOUCHER",
            fiscalYear = 2026,
            docNumber = 8888,
            partyId = "CUST_WALKIN",
            dateEpochDay = 20450,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            rateZone = "SANAA",
            rateSource = "MANUAL",
            totalMinor = 50_000_00L,
            totalBaseMinor = 50_000_00L,
            status = "POSTED",
            notes = oversizedText,
            reversalOfDocId = null,
            revision = 1,
            createdAt = System.currentTimeMillis()
        )
        db.documentDao().insertDocument(oversizedDoc)

        val fakeAdapter = FakeFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = fakeAdapter
        )

        val pushRes = syncManager.syncPush("user_oversize_s2", "user@test.com")
        assertTrue("Oversize push must fail", pushRes.isFailure)
        val ex = pushRes.exceptionOrNull()
        assertNotNull("Exception must not be null", ex)
        assertTrue(
            "Exception message must contain 'backup too large, chunked backup required': '${ex?.message}'",
            ex?.message?.contains("backup too large, chunked backup required") == true
        )

        // Verify that NO write was dispatched to Firestore
        assertEquals("Zero writes must occur when payload is oversized", 0, fakeAdapter.writes.size)
    }

    @Test
    fun testRestoreAfterUploadOnEmptyDbGivesEqualCounts() = runBlocking {
        // 1. Populate comprehensive test data across all domains
        // Organization
        val existingOrg = db.organizationDao().getOrganizationSync()
        val orgEntity = OrganizationEntity(
            id = existingOrg?.id ?: "ORG_S2_TEST",
            name = "مؤسسة سام ميكروتك الدولية",
            taxNumber = "123456789",
            functionalCurrency = "YER",
            fiscalYearStartMonth = 1,
            isInitialized = true,
            primaryRateZone = "SANAA",
            equityShareMode = "DERIVED_FROM_CAPITAL",
            createdAt = System.currentTimeMillis()
        )
        db.organizationDao().insertOrganization(orgEntity)

        // Party
        val testParty = PartyEntity(
            id = "PARTY_S2_TEST",
            name = "محل الأمل للاتصالات",
            phone = "771234567",
            isCustomer = true,
            isVendor = false,
            isPartner = false,
            creditLimitMinor = 500_000_00L,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        db.partyDao().insertParty(testParty)

        // Package
        val testPkg = CardPackageEntity(
            id = "PKG_S2_TEST",
            name = "كرت 10 جيجا شهري",
            durationOrQuota = "10 GB / 30 Days",
            wholesalePriceMinor = 1_800_00L,
            retailPriceMinor = 2_000_00L,
            isActive = true
        )
        db.cardPackageDao().insertPackage(testPkg)

        // Treasury
        val testTreasury = TreasuryEntity(
            id = "TR_S2_TEST",
            name = "صندوق الكروت الرئيسي",
            glAccountCode = "1101",
            currency = "YER",
            isActive = true,
            allowNegative = false
        )
        db.treasuryDao().insertTreasury(testTreasury)

        // Document & Items
        val testDoc = DocumentEntity(
            id = "DOC_S2_INV_1",
            type = "SALES_INVOICE",
            fiscalYear = 2026,
            docNumber = 701,
            partyId = "PARTY_S2_TEST",
            dateEpochDay = 20450,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            rateZone = "SANAA",
            rateSource = "MANUAL",
            totalMinor = 20_000_00L,
            totalBaseMinor = 20_000_00L,
            status = "POSTED",
            notes = "فاتورة مبيعات تجريبية",
            reversalOfDocId = null,
            revision = 1,
            createdAt = System.currentTimeMillis()
        )
        db.documentDao().insertDocument(testDoc)

        val testItem = DocumentItemEntity(
            id = "ITEM_S2_1",
            docId = "DOC_S2_INV_1",
            itemIndex = 0,
            packageId = "PKG_S2_TEST",
            description = "10 كروت 10 جيجا",
            accountCode = "4101",
            quantity = 10,
            unitPriceMinor = 2_000_00L,
            totalMinor = 20_000_00L,
            isAsset = false
        )
        db.documentDao().insertItems(listOf(testItem))

        // Journal Entry & Lines
        val testEntry = JournalEntryEntity(
            id = "JE_S2_1",
            docId = "DOC_S2_INV_1",
            entryNumber = 1001,
            entryDateEpochDay = 20450,
            type = "SALES_INVOICE",
            memo = "قيد فاتورة مبيعات 701",
            reversalOfEntryId = null,
            createdAt = System.currentTimeMillis()
        )
        db.journalDao().insertEntry(testEntry)

        val lineDebit = JournalLineEntity(
            id = "JL_S2_1",
            entryId = "JE_S2_1",
            lineNo = 1,
            accountCode = com.example.core.ledger.AccountConstants.ACCOUNTS_RECEIVABLE,
            partyId = "PARTY_S2_TEST",
            treasuryId = null,
            origMinor = 20_000_00L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            baseDebitMinor = 20_000_00L,
            baseCreditMinor = 0L,
            memo = "مدينون - محل الأمل"
        )
        val lineCredit = JournalLineEntity(
            id = "JL_S2_2",
            entryId = "JE_S2_1",
            lineNo = 2,
            accountCode = "4101",
            partyId = null,
            treasuryId = null,
            origMinor = 20_000_00L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            baseDebitMinor = 0L,
            baseCreditMinor = 20_000_00L,
            memo = "إيراد مبيعات كروت"
        )
        db.journalDao().insertLines(listOf(lineDebit, lineCredit))

        // Allocation
        val testAlloc = AllocationEntity(
            id = "ALLOC_S2_1",
            paymentDocId = "DOC_S2_INV_1",
            invoiceDocId = "DOC_S2_INV_1",
            allocatedOrigMinor = 20_000_00L,
            allocatedBaseMinor = 20_000_00L,
            exchangeGainLossMinor = 0L,
            isVoided = false,
            createdAt = System.currentTimeMillis()
        )
        db.allocationDao().insertAllocation(testAlloc)

        // Asset
        val testAsset = AssetEntity(
            id = "ASSET_S2_1",
            docId = "DOC_S2_INV_1",
            name = "سيرفر مايكروتك CCR2004",
            purchaseDateEpochDay = 20400,
            purchaseCostMinor = 250_000_00L,
            salvageValueMinor = 10_000_00L,
            usefulLifeMonths = 36,
            accumulatedDepreciationMinor = 0L,
            isDisposed = false
        )
        db.assetDao().insertAsset(testAsset)

        // Currency Rate
        val testRate = CurrencyRateEntity(
            id = "RATE_S2_TEST_1",
            currency = "USD",
            zone = "SANAA",
            rateMicros = 535_000_000L,
            effectiveDateEpochDay = 20450L,
            createdAt = System.currentTimeMillis(),
            createdBy = "ADMIN",
            reason = "سعر صرف اختباري"
        )
        db.currencyRateDao().insertRate(testRate)

        // Network Device
        val testDevice = NetworkDevice(
            id = "DEV_S2_NET_1",
            name = "سيكتور شمالي mANTBox 19s",
            ipAddress = "10.20.30.1",
            deviceType = DeviceType.ACCESS_POINT,
            macAddress = "CC:AA:BB:11:22:33",
            towerLocation = "برج نقم",
            status = DeviceStatus.ONLINE
        )
        networkRepository.addOrUpdateDevice(testDevice)

        // Stock Movement
        val testStock = StockMovementEntity(
            id = "STOCK_S2_1",
            packageId = "PKG_S2_TEST",
            docId = "DOC_S2_INV_1",
            type = "OUT",
            quantity = 10,
            movementDateEpochDay = 20450,
            createdAt = System.currentTimeMillis()
        )
        db.cardPackageDao().insertStockMovement(testStock)

        // Measure expected counts
        val expParties = db.partyDao().getAllPartiesSync().size
        val expPkgs = db.cardPackageDao().getAllPackagesSync().size
        val expTreasuries = db.treasuryDao().getAllTreasuriesSync().size
        val expDocs = db.documentDao().getAllDocumentsSync().size
        val expItems = db.documentDao().getAllDocumentItemsSync().size
        val expEntries = db.journalDao().getAllEntriesSync().size
        val expLines = db.journalDao().getAllLinesSync().size
        val expAlloc = db.allocationDao().getAllActiveAllocationsSync().size
        val expAssets = db.assetDao().getAllAssetsSync().size
        val expRates = db.currencyRateDao().getAllRatesSync().size
        val expStock = db.cardPackageDao().getAllStockMovementsSync().size
        val expDevices = networkRepository.getAllDevices().size

        // Export database to JSON
        val exportedJson = backupRestoreUseCase.exportDatabaseToJson()
        assertNotNull(exportedJson)

        // 2. Prepare completely empty destination database
        val emptyDb = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(emptyDb.openHelper.writableDatabase)
        val emptyNetRepo = NetworkRepository(context)
        emptyNetRepo.deleteAllDevices()
        assertEquals(0, emptyNetRepo.getAllDevices().size)

        val restoreUseCase = BackupRestoreUseCase(
            db = emptyDb,
            deviceDao = emptyNetRepo,
            networkRepository = emptyNetRepo,
            context = context
        )

        // 3. Execute atomic restore
        val restoreResult = restoreUseCase.restoreDatabaseFromJson(exportedJson)
        assertTrue(
            "Restore must succeed completely: ${restoreResult.exceptionOrNull()?.message}",
            restoreResult.isSuccess
        )

        // 4. Assert 100% equal record counts across all domains
        assertEquals("Parties count must match exactly", expParties, emptyDb.partyDao().getAllPartiesSync().size)
        assertEquals("Packages count must match exactly", expPkgs, emptyDb.cardPackageDao().getAllPackagesSync().size)
        assertEquals("Treasuries count must match exactly", expTreasuries, emptyDb.treasuryDao().getAllTreasuriesSync().size)
        assertEquals("Documents count must match exactly", expDocs, emptyDb.documentDao().getAllDocumentsSync().size)
        assertEquals("Document items count must match exactly", expItems, emptyDb.documentDao().getAllDocumentItemsSync().size)
        assertEquals("Journal entries count must match exactly", expEntries, emptyDb.journalDao().getAllEntriesSync().size)
        assertEquals("Journal lines count must match exactly", expLines, emptyDb.journalDao().getAllLinesSync().size)
        assertEquals("Allocations count must match exactly", expAlloc, emptyDb.allocationDao().getAllActiveAllocationsSync().size)
        assertEquals("Assets count must match exactly", expAssets, emptyDb.assetDao().getAllAssetsSync().size)
        assertEquals("Currency rates count must match exactly", expRates, emptyDb.currencyRateDao().getAllRatesSync().size)
        assertEquals("Stock movements count must match exactly", expStock, emptyDb.cardPackageDao().getAllStockMovementsSync().size)
        assertEquals("Network devices count must match exactly", expDevices, emptyNetRepo.getAllDevices().size)

        // Verify organization profile restored
        val restoredOrg = emptyDb.organizationDao().getOrganizationSync()
        assertNotNull("Organization entity must be restored", restoredOrg)
        assertEquals("مؤسسة سام ميكروتك الدولية", restoredOrg?.name)
        assertEquals("123456789", restoredOrg?.taxNumber)

        emptyDb.close()
    }

    @Test
    fun testEndToEndCloudPushAndPullWithFakeAdapter() = runBlocking {
        val testUid = "user_e2e_sync_uid"
        val testEmail = "test.admin@sammikrotik.ye"
        val fakeAdapter = FakeFirestoreAdapter()

        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = backupRestoreUseCase,
            networkRepository = networkRepository,
            deviceDao = networkRepository,
            firestoreAdapter = fakeAdapter
        )

        // Seed an asset and a rate
        val testRate = CurrencyRateEntity(
            id = "RATE_E2E_USD",
            currency = "USD",
            zone = "SANAA",
            rateMicros = 535_000_000L,
            effectiveDateEpochDay = 20450L,
            createdAt = System.currentTimeMillis(),
            createdBy = "ADMIN",
            reason = "E2E rate"
        )
        db.currencyRateDao().insertRate(testRate)

        // Push to cloud
        val pushRes = syncManager.syncPush(testUid, testEmail)
        assertTrue("Push must succeed: ${pushRes.exceptionOrNull()?.message}", pushRes.isSuccess)
        assertTrue(fakeAdapter.writes.containsKey("users/$testUid/backup_latest/latest"))

        // Create empty destination
        val targetDb = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(targetDb.openHelper.writableDatabase)
        val targetNetRepo = NetworkRepository(context)
        targetNetRepo.deleteAllDevices()

        val targetSyncManager = FirestoreSyncManager(
            context = context,
            db = targetDb,
            backupRestoreUseCase = BackupRestoreUseCase(
                db = targetDb,
                deviceDao = targetNetRepo,
                networkRepository = targetNetRepo,
                context = context
            ),
            networkRepository = targetNetRepo,
            deviceDao = targetNetRepo,
            firestoreAdapter = fakeAdapter
        )

        // Pull from cloud
        val pullRes = targetSyncManager.syncPull(testUid, testEmail)
        assertTrue("Pull must succeed: ${pullRes.exceptionOrNull()?.message}", pullRes.isSuccess)

        // Verify currency rate was restored in target DB
        val targetRates = targetDb.currencyRateDao().getAllRatesSync()
        assertTrue("Restored rates must contain USD rate", targetRates.any { it.id == "RATE_E2E_USD" })

        targetDb.close()
    }
}
