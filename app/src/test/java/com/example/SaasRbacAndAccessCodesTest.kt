package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AccessCodeEntry
import com.example.data.auth.AgentPermissions
import com.example.data.auth.AppUserRole
import com.example.data.auth.AuthManager
import com.example.data.auth.RoleAccessPolicy
import com.example.data.auth.UserSession
import com.example.data.local.AppDatabase
import com.example.data.sync.FirestoreAdapter
import com.example.data.sync.FirestoreSyncManager
import com.example.domain.usecase.BackupRestoreUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SaasRbacAndAccessCodesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private class InMemoryFirestoreAdapter : FirestoreAdapter {
        val docs = mutableMapOf<String, Map<String, Any?>>()

        override fun isAvailable(): Boolean = true

        override suspend fun get(path: String): Map<String, Any?>? = docs[path]

        override suspend fun set(path: String, data: Map<String, Any?>, merge: Boolean) {
            if (merge && docs.containsKey(path)) {
                val merged = docs[path]!!.toMutableMap()
                merged.putAll(data)
                docs[path] = merged
            } else {
                docs[path] = data
            }
        }

        override suspend fun delete(path: String) {
            docs.remove(path)
        }

        override suspend fun listCollection(collectionPath: String): List<Pair<String, Map<String, Any?>>> {
            val prefix = collectionPath.trimEnd('/') + "/"
            return docs.entries
                .filter { (k, _) ->
                    k.startsWith(prefix) && !k.removePrefix(prefix).contains("/")
                }
                .map { (k, v) -> k.removePrefix(prefix) to v }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("sammikrotik_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `default UserSession and AuthManager preserve NETWORK_OWNER role`() {
        val defaultSession = UserSession(
            email = "owner@sammikrotik.com",
            displayName = "مالك الشبكة"
        )
        assertEquals(AppUserRole.NETWORK_OWNER, defaultSession.role)
        assertEquals(AgentPermissions.FULL_OWNER, defaultSession.agentPermissions)

        val authManager = AuthManager(context)
        assertEquals(AppUserRole.NETWORK_OWNER, authManager.activeRole.value)
        assertTrue(RoleAccessPolicy.canAccessRouterOsSettings(authManager.activeRole.value))
        assertTrue(RoleAccessPolicy.canAccessPartnerEquityAndCapital(authManager.activeRole.value))
        assertTrue(RoleAccessPolicy.canAccessNetProfitReports(authManager.activeRole.value))
    }

    @Test
    fun `RoleAccessPolicy enforces hard restrictions and granular agent permissions`() {
        // 1. NETWORK_OWNER has 100% full access
        val owner = AppUserRole.NETWORK_OWNER
        assertTrue(RoleAccessPolicy.canAccessRouterOsSettings(owner))
        assertTrue(RoleAccessPolicy.canAccessWifiSpectrum(owner))
        assertTrue(RoleAccessPolicy.canAccessPartnerEquityAndCapital(owner))
        assertTrue(RoleAccessPolicy.canAccessNetProfitReports(owner))
        assertTrue(RoleAccessPolicy.canAccessPurchasesAndAssets(owner))
        assertTrue(RoleAccessPolicy.canAccessTeamAndCodesManager(owner))

        // 2. DISTRIBUTOR_AGENT is strictly blocked from Owner-only capabilities
        val agent = AppUserRole.DISTRIBUTOR_AGENT
        assertFalse(RoleAccessPolicy.canAccessRouterOsSettings(agent))
        assertFalse(RoleAccessPolicy.canAccessWifiSpectrum(agent))
        assertFalse(RoleAccessPolicy.canAccessPartnerEquityAndCapital(agent))
        assertFalse(RoleAccessPolicy.canAccessNetProfitReports(agent))
        assertFalse(RoleAccessPolicy.canAccessPurchasesAndAssets(agent))
        assertFalse(RoleAccessPolicy.canAccessTeamAndCodesManager(agent))
        assertFalse(RoleAccessPolicy.canAccessCardStudioGeneration(agent))

        // Default AgentPermissions
        val defaultPerms = AgentPermissions()
        assertTrue(RoleAccessPolicy.canCreateSalesInvoices(agent, defaultPerms))
        assertTrue(RoleAccessPolicy.canCollectReceipts(agent, defaultPerms))
        assertFalse(RoleAccessPolicy.canCreateDisbursements(agent, defaultPerms))
        assertTrue(RoleAccessPolicy.canViewAssignedStock(agent, defaultPerms))
        assertTrue(RoleAccessPolicy.canGeneratePosCodes(agent, defaultPerms))

        // Customized AgentPermissions
        val customPerms = AgentPermissions(
            canCreateSalesInvoices = false,
            canCollectReceipts = true,
            canCreateDisbursements = true,
            canViewAssignedStock = false,
            canGeneratePosCodes = false
        )
        assertFalse(RoleAccessPolicy.canCreateSalesInvoices(agent, customPerms))
        assertTrue(RoleAccessPolicy.canCollectReceipts(agent, customPerms))
        assertTrue(RoleAccessPolicy.canCreateDisbursements(agent, customPerms))
        assertFalse(RoleAccessPolicy.canViewAssignedStock(agent, customPerms))
        assertFalse(RoleAccessPolicy.canGeneratePosCodes(agent, customPerms))

        // 3. POS_MERCHANT is strictly read-only for their own boundPartyId
        val pos = AppUserRole.POS_MERCHANT
        assertFalse(RoleAccessPolicy.canAccessRouterOsSettings(pos))
        assertFalse(RoleAccessPolicy.canAccessWifiSpectrum(pos))
        assertFalse(RoleAccessPolicy.canAccessPartnerEquityAndCapital(pos))
        assertFalse(RoleAccessPolicy.canAccessNetProfitReports(pos))
        assertFalse(RoleAccessPolicy.canAccessPurchasesAndAssets(pos))
        assertFalse(RoleAccessPolicy.canCreateSalesInvoices(pos, AgentPermissions.FULL_OWNER))
        assertFalse(RoleAccessPolicy.canCollectReceipts(pos, AgentPermissions.FULL_OWNER))
        assertFalse(RoleAccessPolicy.canCreateDisbursements(pos, AgentPermissions.FULL_OWNER))
        assertFalse(RoleAccessPolicy.canViewAssignedStock(pos, AgentPermissions.FULL_OWNER))
        assertFalse(RoleAccessPolicy.canGeneratePosCodes(pos, AgentPermissions.FULL_OWNER))
        assertTrue(RoleAccessPolicy.canViewPartyStatement(pos, targetPartyId = "PARTY_101", boundPartyId = "PARTY_101"))
        assertFalse(RoleAccessPolicy.canViewPartyStatement(pos, targetPartyId = "PARTY_202", boundPartyId = "PARTY_101"))
    }

    @Test
    fun `AccessCodeEntry generates valid AGT and POS formats and official WhatsApp message`() {
        val agentCode4 = AccessCodeEntry.generateAgentCodeFormat()
        assertTrue("Agent code should match AGT-XXXX: $agentCode4", agentCode4.matches(Regex("^AGT-[A-Z0-9]{4}$")))

        val agentCode8 = AccessCodeEntry.generateAgentCodeFormat(suffixLength = 8)
        assertTrue("Agent 8-char code should match AGT-XXXX-XXXX: $agentCode8", agentCode8.matches(Regex("^AGT-[A-Z0-9]{4}-[A-Z0-9]{4}$")))

        val posCode = AccessCodeEntry.generatePosCodeFormat()
        assertTrue("POS code should match POS-XXXXXX: $posCode", posCode.matches(Regex("^POS-[A-Z0-9]{6}$")))

        val msg = AccessCodeEntry.buildPosWhatsAppMessage("شبكة النور الذكية", "POS-7K9M2P")
        assertEquals(
            "مرحباً بك في بوابة الشفافية الرقمية لشبكة شبكة النور الذكية. كود الدخول الخاص بنقطة البيع لمتابعة كشف حسابك وفواتيرك لحظياً هو: POS-7K9M2P",
            msg
        )
    }

    @Test
    fun `MainTab visibleTabsForRole dynamically gates tabs per role and permissions`() {
        val ownerTabs = MainTab.visibleTabsForRole(AppUserRole.NETWORK_OWNER)
        assertEquals(10, ownerTabs.size)

        val defaultAgentTabs = MainTab.visibleTabsForRole(AppUserRole.DISTRIBUTOR_AGENT, AgentPermissions())
        val defaultAgentTabObjects = defaultAgentTabs.map { it.second }
        assertEquals(
            listOf(MainTab.Sales, MainTab.Vouchers, MainTab.CardsAndPackages, MainTab.Parties),
            defaultAgentTabObjects
        )
        assertFalse(defaultAgentTabObjects.contains(MainTab.Operations))
        assertFalse(defaultAgentTabObjects.contains(MainTab.Dashboard))
        assertFalse(defaultAgentTabObjects.contains(MainTab.Purchases))
        assertFalse(defaultAgentTabObjects.contains(MainTab.Reports))
        assertFalse(defaultAgentTabObjects.contains(MainTab.Audit))

        val restrictedAgentTabs = MainTab.visibleTabsForRole(
            AppUserRole.DISTRIBUTOR_AGENT,
            AgentPermissions(
                canCreateSalesInvoices = true,
                canCollectReceipts = false,
                canCreateDisbursements = false,
                canViewAssignedStock = false,
                canGeneratePosCodes = false
            )
        ).map { it.second }
        assertEquals(listOf(MainTab.Sales, MainTab.Parties), restrictedAgentTabs)

        val posTabs = MainTab.visibleTabsForRole(AppUserRole.POS_MERCHANT)
        assertTrue(posTabs.isEmpty())
    }

    @Test
    fun `AuthManager and FirestoreSyncManager sync and authenticate Agent and POS codes`() = runBlocking {
        val authManager = AuthManager(context)
        val adapter = InMemoryFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = BackupRestoreUseCase(db, context = context),
            firestoreAdapter = adapter
        )

        val agentEntry = authManager.createOrUpdateAgentInviteCode(
            agentName = "أحمد الموزع",
            agentPin = "4321",
            agentPhone = "771122334",
            permissions = AgentPermissions(
                canCreateSalesInvoices = true,
                canCollectReceipts = true,
                canCreateDisbursements = true,
                canViewAssignedStock = true,
                canGeneratePosCodes = false
            ),
            networkId = "NET_01",
            networkName = "شبكة المجد"
        )
        assertTrue(agentEntry.code.startsWith("AGT-"))

        val posEntry = authManager.generateOrGetPosCode(
            partyId = "PARTY_GROCERY_1",
            partyName = "بقالة الأمانة",
            partyPhone = "775566778",
            networkId = "NET_01",
            networkName = "شبكة المجد"
        )
        assertTrue(posEntry.code.startsWith("POS-"))
        assertEquals("PARTY_GROCERY_1", posEntry.partyId)

        // Sync both codes to Firestore under networks/NET_01/access_codes/{code}
        assertTrue(syncManager.syncAccessCodeToCloud("NET_01", agentEntry).isSuccess)
        assertTrue(syncManager.syncAccessCodeToCloud("NET_01", posEntry).isSuccess)
        assertNotNull(adapter.docs["networks/NET_01/access_codes/${agentEntry.code}"])
        assertNotNull(adapter.docs["networks/NET_01/access_codes/${posEntry.code}"])

        // Authenticate with Agent Code (wrong PIN should fail, correct PIN should succeed)
        val failedAgentLogin = authManager.loginWithAgentInviteCode(agentEntry.code, "0000")
        assertTrue(failedAgentLogin.isFailure)

        val successAgentLogin = authManager.loginWithAgentInviteCode(agentEntry.code, "4321")
        assertTrue(successAgentLogin.isSuccess)
        assertEquals(AppUserRole.DISTRIBUTOR_AGENT, authManager.activeRole.value)
        assertEquals("أحمد الموزع", authManager.currentUser.value?.agentName)
        assertTrue(authManager.activeAgentPermissions.value.canCreateDisbursements)
        assertFalse(authManager.activeAgentPermissions.value.canGeneratePosCodes)

        // Authenticate with POS Code
        val successPosLogin = authManager.loginWithPosTransparencyCode(posEntry.code)
        assertTrue(successPosLogin.isSuccess)
        assertEquals(AppUserRole.POS_MERCHANT, authManager.activeRole.value)
        assertEquals("PARTY_GROCERY_1", authManager.currentUser.value?.boundPartyId)
        assertEquals("بقالة الأمانة", authManager.currentUser.value?.boundPartyName)

        // Switch back to NETWORK_OWNER
        authManager.switchToNetworkOwnerRole()
        assertEquals(AppUserRole.NETWORK_OWNER, authManager.activeRole.value)
        assertEquals(AgentPermissions.FULL_OWNER, authManager.activeAgentPermissions.value)
    }

    @Test
    fun `Phase 2 - DocumentVerificationStatus transitions have ZERO side effects on LedgerWriter balances`() = runBlocking {
        val ledgerDb = AppDatabase.createInMemory(context)
        try {
            AppDatabase.installTriggers(ledgerDb.openHelper.writableDatabase)
            AppDatabase.seedDefaultData(ledgerDb.openHelper.writableDatabase)

            val writer = com.example.data.ledger.LedgerWriter(ledgerDb, enableInvariantValidation = true)
            val invariants = com.example.data.ledger.LedgerInvariants(ledgerDb)
            val authManager = AuthManager(context)

            val posParty = com.example.data.local.entity.PartyEntity(
                id = "POS_GROCERY_202",
                name = "بقالة التوفيق",
                phone = "770123456",
                isCustomer = true
            )
            ledgerDb.partyDao().insertParty(posParty)
            val rate = com.example.core.model.ExchangeRate.parity(com.example.core.model.CurrencyCode.YER)

            // 1. Post Sales Invoice: 50,000 YER (5,000,000 minor)
            val invoice = writer.postSalesInvoice(
                partyId = posParty.id,
                fiscalYear = 2026,
                dateEpochDay = 20100L,
                currency = com.example.core.model.CurrencyCode.YER,
                exchangeRate = rate,
                cardItems = listOf(com.example.data.ledger.SalesItemSpec("كروت 2 جيجا", 50, 100000L)),
                serviceItems = emptyList()
            )

            // 2. Post Receipt Voucher: 30,000 YER (3,000,000 minor)
            val receipt = writer.postCustomerReceipt(
                partyId = posParty.id,
                treasuryId = "TR_MAIN_YER",
                fiscalYear = 2026,
                dateEpochDay = 20101L,
                amountOrigMinor = 3000000L,
                currency = com.example.core.model.CurrencyCode.YER,
                exchangeRate = rate,
                allocations = listOf(com.example.data.ledger.InvoiceAllocationSpec(invoice.id, 3000000L))
            )

            // Snapshot ledger balances BEFORE any POS verification or dispute
            val arBefore = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.ACCOUNTS_RECEIVABLE)
            val revBefore = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CARD_SALES_REVENUE)
            val cashBefore = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CASH_VAULT)
            val entriesCountBefore = ledgerDb.journalDao().getAllEntriesSync().size
            assertEquals(2000000L, arBefore) // 50,000 - 30,000 = 20,000 YER (2,000,000 minor)

            // 3. Initial state is PENDING_POS_CONFIRMATION
            val initialInvVer = authManager.getDocumentVerification(invoice.id, posParty.id, "NET_01")
            assertEquals(com.example.data.auth.DocumentVerificationStatus.PENDING_POS_CONFIRMATION, initialInvVer.status)
            assertEquals("⏳ بانتظار تأكيد البقالة", initialInvVer.status.posBadgeAr)

            // 4. Transition Invoice: PENDING -> CONFIRMED_BY_POS
            val confirmedInv = authManager.confirmDocumentByPos(
                documentId = invoice.id,
                partyId = posParty.id,
                networkId = "NET_01",
                accessCode = "POS-998877",
                verifiedAtEpochMs = 1760000001000L
            )
            assertEquals(com.example.data.auth.DocumentVerificationStatus.CONFIRMED_BY_POS, confirmedInv.status)
            assertEquals("✅ مؤكد من نقطة البيع", confirmedInv.status.posBadgeAr)
            assertEquals("✅ مؤكد من البقالة", confirmedInv.status.ownerBadgeAr)
            assertEquals(1760000001000L, confirmedInv.verifiedAtEpochMs)

            // 5. Transition Receipt: PENDING -> DISPUTED_BY_POS with note
            val disputedReceipt = authManager.disputeDocumentByPos(
                documentId = receipt.id,
                partyId = posParty.id,
                disputeNote = "المبلغ المسلم للموزع 50,000 وليس 30,000",
                networkId = "NET_01",
                accessCode = "POS-998877",
                verifiedAtEpochMs = 1760000002000L
            )
            assertEquals(com.example.data.auth.DocumentVerificationStatus.DISPUTED_BY_POS, disputedReceipt.status)
            assertEquals("المبلغ المسلم للموزع 50,000 وليس 30,000", disputedReceipt.disputeNote)
            assertEquals("⚠️ يوجد ملاحظة / اعتراض", disputedReceipt.status.posBadgeAr)
            assertEquals("⚠️ اعتراض من البقالة", disputedReceipt.status.ownerBadgeAr)

            // 6. Verify ZERO mutation to LedgerWriter balances, journal lines, or accounting invariants
            val arAfter = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.ACCOUNTS_RECEIVABLE)
            val revAfter = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CARD_SALES_REVENUE)
            val cashAfter = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CASH_VAULT)
            val entriesCountAfter = ledgerDb.journalDao().getAllEntriesSync().size

            assertEquals(arBefore, arAfter)
            assertEquals(revBefore, revAfter)
            assertEquals(cashBefore, cashAfter)
            assertEquals(entriesCountBefore, entriesCountAfter)
            assertTrue(invariants.verifyAll(failFast = true).isValid)
        } finally {
            ledgerDb.close()
        }
    }

    @Test
    fun `Phase 2 - FirestoreSyncManager syncs POS verifications under networks_networkId_parties_partyId_verifications_docId with local fallback`() = runBlocking {
        val adapter = InMemoryFirestoreAdapter()
        val syncManager = FirestoreSyncManager(
            context = context,
            db = db,
            backupRestoreUseCase = BackupRestoreUseCase(db, context = context),
            firestoreAdapter = adapter
        )
        val authManager = AuthManager(context)

        val confirmedRecord = authManager.confirmDocumentByPos(
            documentId = "DOC_INV_501",
            partyId = "PARTY_POS_1",
            networkId = "NET_MAIN",
            accessCode = "POS-ABC123",
            verifiedAtEpochMs = 1765000000000L
        )
        val disputedRecord = authManager.disputeDocumentByPos(
            documentId = "DOC_RCV_502",
            partyId = "PARTY_POS_1",
            disputeNote = "فرق 10,000 ريال في السند",
            networkId = "NET_MAIN",
            accessCode = "POS-ABC123",
            verifiedAtEpochMs = 1765000005000L
        )

        // Sync to Firestore under networks/{networkId}/parties/{partyId}/verifications/{documentId}
        assertTrue(syncManager.syncDocumentVerificationToCloud("NET_MAIN", "PARTY_POS_1", confirmedRecord).isSuccess)
        assertTrue(syncManager.syncDocumentVerificationToCloud("NET_MAIN", "PARTY_POS_1", disputedRecord).isSuccess)

        assertNotNull(adapter.docs["networks/NET_MAIN/parties/PARTY_POS_1/verifications/DOC_INV_501"])
        assertNotNull(adapter.docs["networks/NET_MAIN/parties/PARTY_POS_1/verifications/DOC_RCV_502"])

        val pulled = syncManager.pullPartyDocumentVerifications("NET_MAIN", "PARTY_POS_1")
        assertEquals(2, pulled.size)
        val pulledDispute = pulled.first { it.documentId == "DOC_RCV_502" }
        assertEquals(com.example.data.auth.DocumentVerificationStatus.DISPUTED_BY_POS, pulledDispute.status)
        assertEquals("فرق 10,000 ريال في السند", pulledDispute.disputeNote)
    }

    @Test
    fun `Phase 3 - PosRestockOrder and PosSupportTicket lifecycle transitions and Firestore sync have ZERO side effects on LedgerWriter`() = runBlocking {
        val ledgerDb = AppDatabase.createInMemory(context)
        try {
            AppDatabase.installTriggers(ledgerDb.openHelper.writableDatabase)
            AppDatabase.seedDefaultData(ledgerDb.openHelper.writableDatabase)

            val writer = com.example.data.ledger.LedgerWriter(ledgerDb, enableInvariantValidation = true)
            val invariants = com.example.data.ledger.LedgerInvariants(ledgerDb)
            val authManager = AuthManager(context)
            val adapter = InMemoryFirestoreAdapter()
            val syncManager = FirestoreSyncManager(
                context = context,
                db = ledgerDb,
                backupRestoreUseCase = BackupRestoreUseCase(ledgerDb, context = context),
                firestoreAdapter = adapter
            )

            val posParty = com.example.data.local.entity.PartyEntity(
                id = "POS_GROCERY_303",
                name = "بقالة البركة",
                phone = "773344556",
                isCustomer = true
            )
            ledgerDb.partyDao().insertParty(posParty)
            val rate = com.example.core.model.ExchangeRate.parity(com.example.core.model.CurrencyCode.YER)

            // Post initial invoice to establish baseline ledger state
            writer.postSalesInvoice(
                partyId = posParty.id,
                fiscalYear = 2026,
                dateEpochDay = 20110L,
                currency = com.example.core.model.CurrencyCode.YER,
                exchangeRate = rate,
                cardItems = listOf(com.example.data.ledger.SalesItemSpec("كرت 500 ريال", 20, 50000L)),
                serviceItems = emptyList()
            )

            val arBefore = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.ACCOUNTS_RECEIVABLE)
            val revBefore = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CARD_SALES_REVENUE)
            val entriesBefore = ledgerDb.journalDao().getAllEntriesSync().size
            assertEquals(1000000L, arBefore) // 10,000 YER = 1,000,000 minor

            // 1. Create POS Restock Order (PENDING)
            val orderItems = listOf(
                com.example.data.auth.PosRestockOrderItem(
                    packageId = "PKG_200",
                    packageName = "كرت 200 ريال - 1 جيجا",
                    unitPrice = 20000L,
                    requestedQuantity = 50
                ),
                com.example.data.auth.PosRestockOrderItem(
                    packageId = "PKG_500",
                    packageName = "كرت 500 ريال - 3 جيجا",
                    unitPrice = 50000L,
                    requestedQuantity = 20
                )
            )
            val createdOrder = authManager.createPosRestockOrder(
                partyId = posParty.id,
                partyName = posParty.name,
                items = orderItems,
                note = "يرجى إرسالها مع الموزع صباحاً",
                networkId = "NET_MAIN"
            )
            assertEquals(com.example.data.auth.RestockOrderStatus.PENDING, createdOrder.status)
            assertEquals("⏳ قيد الانتظار", createdOrder.status.badgeAr)
            assertEquals(2000000L, createdOrder.estimatedTotal) // (50*200 + 20*500) YER = 20,000 YER = 2,000,000 minor

            // Sync order to Firestore under networks/NET_MAIN/pos_orders/{orderId}
            assertTrue(syncManager.syncPosRestockOrderToCloud("NET_MAIN", createdOrder).isSuccess)
            assertNotNull(adapter.docs["networks/NET_MAIN/pos_orders/${createdOrder.orderId}"])

            // 2. Transition Order: PENDING -> PREPARING -> FULFILLED
            val preparingOrder = authManager.updatePosRestockOrderStatus(
                orderId = createdOrder.orderId,
                newStatus = com.example.data.auth.RestockOrderStatus.PREPARING,
                handledBy = "المالك"
            )
            assertNotNull(preparingOrder)
            assertEquals(com.example.data.auth.RestockOrderStatus.PREPARING, preparingOrder?.status)
            assertEquals("📦 جاري التجهيز", preparingOrder?.status?.badgeAr)

            val fulfilledOrder = authManager.updatePosRestockOrderStatus(
                orderId = createdOrder.orderId,
                newStatus = com.example.data.auth.RestockOrderStatus.FULFILLED,
                handledBy = "أحمد الموزع"
            )
            assertNotNull(fulfilledOrder)
            assertEquals(com.example.data.auth.RestockOrderStatus.FULFILLED, fulfilledOrder?.status)
            assertEquals("✅ تم التسليم", fulfilledOrder?.status?.badgeAr)
            assertEquals("أحمد الموزع", fulfilledOrder?.handledBy)

            assertTrue(syncManager.syncPosRestockOrderToCloud("NET_MAIN", fulfilledOrder!!).isSuccess)
            val pulledOrders = syncManager.pullPosRestockOrders("NET_MAIN")
            assertEquals(1, pulledOrders.size)
            assertEquals(com.example.data.auth.RestockOrderStatus.FULFILLED, pulledOrders.first().status)

            // 3. Create POS Support Ticket (OPEN) & transition to IN_PROGRESS -> RESOLVED with adminReply
            val createdTicket = authManager.createPosSupportTicket(
                partyId = posParty.id,
                partyName = posParty.name,
                category = com.example.data.auth.TicketCategory.VOUCHER_ISSUE,
                message = "الكرت لا يعمل لدى الزبون",
                voucherCode = "77889911",
                networkId = "NET_MAIN"
            )
            assertEquals(com.example.data.auth.TicketStatus.OPEN, createdTicket.status)
            assertEquals("77889911", createdTicket.voucherCode)
            assertEquals("مشكلة في كرت", createdTicket.category.labelAr)

            assertTrue(syncManager.syncPosSupportTicketToCloud("NET_MAIN", createdTicket).isSuccess)
            assertNotNull(adapter.docs["networks/NET_MAIN/pos_tickets/${createdTicket.ticketId}"])

            val resolvedTicket = authManager.updatePosSupportTicketStatus(
                ticketId = createdTicket.ticketId,
                newStatus = com.example.data.auth.TicketStatus.RESOLVED,
                adminReply = "تم تفعيل الكرت وإعادة ضبط الجلسة في المايكروتك"
            )
            assertNotNull(resolvedTicket)
            assertEquals(com.example.data.auth.TicketStatus.RESOLVED, resolvedTicket?.status)
            assertEquals("✅ تم الحل", resolvedTicket?.status?.badgeAr)
            assertEquals("تم تفعيل الكرت وإعادة ضبط الجلسة في المايكروتك", resolvedTicket?.adminReply)

            assertTrue(syncManager.syncPosSupportTicketToCloud("NET_MAIN", resolvedTicket!!).isSuccess)
            val pulledTickets = syncManager.pullPosSupportTickets("NET_MAIN")
            assertEquals(1, pulledTickets.size)
            assertEquals(com.example.data.auth.TicketStatus.RESOLVED, pulledTickets.first().status)
            assertEquals("تم تفعيل الكرت وإعادة ضبط الجلسة في المايكروتك", pulledTickets.first().adminReply)

            // 4. Verify ZERO side effects on LedgerWriter balances and journal entries
            val arAfter = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.ACCOUNTS_RECEIVABLE)
            val revAfter = ledgerDb.journalDao().getNetDebitBalanceForAccount(com.example.core.ledger.AccountConstants.CARD_SALES_REVENUE)
            val entriesAfter = ledgerDb.journalDao().getAllEntriesSync().size

            assertEquals(arBefore, arAfter)
            assertEquals(revBefore, revAfter)
            assertEquals(entriesBefore, entriesAfter)
            assertTrue(invariants.verifyAll(failFast = true).isValid)
        } finally {
            ledgerDb.close()
        }
    }
}


