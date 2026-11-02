package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.RateZone
import com.example.core.time.AppClock
import com.example.core.time.FiscalCalendar
import com.example.core.time.FixedAppClock
import com.example.data.ledger.LedgerInvariants
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentVoucherType
import com.example.data.ledger.PurchaseItemSpec
import com.example.data.ledger.SalesItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FiscalPeriodEntity
import com.example.data.local.entity.OrganizationEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.TreasuryAccountEntity
import com.example.domain.report.CashFlowCategory
import com.example.domain.report.generateCashFlowStatement
import com.example.domain.report.generateMonthlyProfitReport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountingCoreSecurityRefactorTest {

    private lateinit var db: AppDatabase
    private lateinit var writer: LedgerWriter
    private lateinit var invariants: LedgerInvariants

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
        AppDatabase.seedDefaultData(db.openHelper.writableDatabase)

        writer = LedgerWriter(db, enableInvariantValidation = true)
        invariants = LedgerInvariants(db)

        runBlocking {
            // Seed customer and treasuries
            db.partyDao().insertParty(
                PartyEntity(
                    id = "CUST_TEST_01",
                    name = "عميل تجريبي",
                    isCustomer = true
                )
            )
            db.treasuryDao().insertTreasury(
                TreasuryAccountEntity(
                    id = "TR_TEST_CASH",
                    name = "خزينة تجريبية 1",
                    glAccountCode = "1101",
                    currency = "YER",
                    allowNegative = true
                )
            )
            db.treasuryDao().insertTreasury(
                TreasuryAccountEntity(
                    id = "TR_TEST_CASH_2",
                    name = "خزينة تجريبية 2",
                    glAccountCode = "1101",
                    currency = "YER",
                    allowNegative = true
                )
            )

            // Seed 2026 fiscal periods
            for (m in 1..12) {
                db.fiscalPeriodDao().insertPeriod(
                    FiscalPeriodEntity(
                        id = "FP_2026_${m.toString().padStart(2, '0')}",
                        year = 2026,
                        month = m,
                        isClosed = false
                    )
                )
            }
            // Seed 2027 fiscal periods
            for (m in 1..12) {
                db.fiscalPeriodDao().insertPeriod(
                    FiscalPeriodEntity(
                        id = "FP_2027_${m.toString().padStart(2, '0')}",
                        year = 2027,
                        month = m,
                        isClosed = false
                    )
                )
            }
        }

        AppClock.setClock(
            FixedAppClock(
                Instant.parse("2026-06-15T10:00:00Z"),
                ZoneId.of("Asia/Aden")
            )
        )
    }

    @After
    fun tearDown() {
        AppClock.resetToDefault()
        db.close()
    }

    @Test
    fun testTask1_LedgerWriterPositiveValueGuardrails() {
        runBlocking {
            val dateEpoch = LocalDate.of(2026, 3, 15).toEpochDay()
            val parity = ExchangeRate.parity(CurrencyCode.FUNCTIONAL)

        // 1. Sales Invoice with quantity <= 0 must fail with Arabic exception
        val ex1 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postSalesInvoice(
                    partyId = "CUST_TEST_01",
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRate = parity,
                    cardItems = listOf(
                        SalesItemSpec(
                            description = "كارت 500",
                            quantity = 0,
                            unitPriceMinor = 500_00L
                        )
                    ),
                    serviceItems = emptyList()
                )
            }
        }
        assertTrue(ex1.message!!.contains("الكمية") || ex1.message!!.contains("كمية"))

        // 2. Sales Invoice with unitPrice <= 0 must fail
        val ex2 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postSalesInvoice(
                    partyId = "CUST_TEST_01",
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRate = parity,
                    cardItems = listOf(
                        SalesItemSpec(
                            description = "كارت 500",
                            quantity = 5,
                            unitPriceMinor = -100_00L
                        )
                    ),
                    serviceItems = emptyList()
                )
            }
        }
        assertTrue(ex2.message!!.contains("سعر الوحدة") || ex2.message!!.contains("سعر"))

        // 3. Purchase Invoice with invalid usefulLifeMonths when isAsset == true
        val vendor = PartyEntity(
            id = "VENDOR_TEST",
            name = "مورد أجهزة",
            isVendor = true
        )
        db.partyDao().insertParty(vendor)

        val ex3 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postPurchaseInvoice(
                    vendorPartyId = vendor.id,
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRate = parity,
                    items = listOf(
                        PurchaseItemSpec(
                            description = "راوتر مايكروتك",
                            accountCode = "1501",
                            quantity = 1,
                            unitPriceMinor = 100_000_00L,
                            isAsset = true,
                            usefulLifeMonths = 0 // Invalid: must be 1..600
                        )
                    )
                )
            }
        }
        assertTrue(ex3.message!!.contains("العمر الإنتاجي"))

        // 4. Customer Receipt with zero amount
        val ex4 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postCustomerReceipt(
                    partyId = "CUST_TEST_01",
                    treasuryId = "TR_TEST_CASH",
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch,
                    amountOrigMinor = 0L,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRate = parity
                )
            }
        }
        assertTrue(ex4.message!!.contains("مبلغ") || ex4.message!!.contains("المبلغ"))

        // 5. Payment Voucher with negative amount
        val ex5 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postPaymentVoucher(
                    recipientPartyId = vendor.id,
                    treasuryId = "TR_TEST_CASH",
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch,
                    amountOrigMinor = -50_00L,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRate = parity,
                    paymentType = PaymentVoucherType.VENDOR_SETTLEMENT
                )
            }
        }
        assertTrue(ex5.message!!.contains("مبلغ") || ex5.message!!.contains("المبلغ"))

        // 6. Treasury transfer with <= 0 amount
        val ex6 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.postTreasuryTransfer(
                    sourceTreasuryId = "TR_TEST_CASH",
                    sourceAmountOrigMinor = 0L,
                    sourceCurrency = CurrencyCode.FUNCTIONAL,
                    sourceRate = parity,
                    destTreasuryId = "TR_TEST_CASH_2",
                    destAmountOrigMinor = 0L,
                    destCurrency = CurrencyCode.FUNCTIONAL,
                    destRate = parity,
                    fiscalYear = 2026,
                    dateEpochDay = dateEpoch
                )
            }
        }
        assertTrue(ex6.message!!.contains("مبلغ التحويل"))

        // 7. Verify zero corrupted lines inserted from failed operations
        invariants.verifyAll()
        }
    }

    @Test
    fun testTask2_CashFlowCorrectionsAndVoidedDocuments() {
        runBlocking {
            val dateEpoch = LocalDate.of(2026, 3, 10).toEpochDay()
            val parity = ExchangeRate.parity(CurrencyCode.FUNCTIONAL)

            // Post a valid receipt voucher (inflow)
            val validReceipt = writer.postCustomerReceipt(
                partyId = "CUST_TEST_01",
                treasuryId = "TR_TEST_CASH",
                fiscalYear = 2026,
                dateEpochDay = dateEpoch,
                amountOrigMinor = 50_000_00L,
                currency = CurrencyCode.FUNCTIONAL,
                exchangeRate = parity,
                notes = "تحصيل نقدي صالح"
            )

            // Post another receipt voucher that is subsequently marked VOIDED via voidDocument
            val voidedReceipt = writer.postCustomerReceipt(
                partyId = "CUST_TEST_01",
                treasuryId = "TR_TEST_CASH",
                fiscalYear = 2026,
                dateEpochDay = dateEpoch,
                amountOrigMinor = 200_000_00L,
                currency = CurrencyCode.FUNCTIONAL,
                exchangeRate = parity,
                notes = "سند تم إلغاؤه"
            )
            val voidSuccess = writer.voidDocument(voidedReceipt.id, dateEpoch, "إلغاء سند")
            assertTrue("Void document must succeed", voidSuccess)

            // Generate Cash Flow Statement for the period
            val statement = generateCashFlowStatement(
                db = db,
                fromEpochDay = LocalDate.of(2026, 3, 1).toEpochDay(),
                toEpochDay = LocalDate.of(2026, 3, 31).toEpochDay()
            )

            // Verify voided document does not duplicate or contaminate the cash flows
            val allLines = statement.operatingSection.lines + statement.investingSection.lines + statement.financingSection.lines
            val voidedLineFound = allLines.any { it.docId == voidedReceipt.id }
            assertEquals("Voided document lines must not appear in Cash Flow Statement", false, voidedLineFound)

            // Verify valid receipt is properly included in Operating Inflows (CUSTOMER_COLLECTIONS)
            val validLine = allLines.find { it.docId == validReceipt.id }
            assertNotNull("Valid receipt must appear in cash flow", validLine)
            assertEquals(CashFlowCategory.CUSTOMER_COLLECTIONS, validLine!!.category)
            assertEquals(50_000_00L, validLine.inflowMinor)

            // Cash flow dynamic balance identity must hold: Opening + Net == Closing
            assertEquals(statement.closingCashMinor, statement.openingCashMinor + statement.netChangeInCashMinor)
            assertEquals(0L, statement.unclassifiedAmountMinor)
        }
    }

    @Test
    fun testTask3_DateAnchoredPeriodicRevaluation() {
        runBlocking {
            // Setup USD treasury
            val usdTreasury = TreasuryAccountEntity(
                id = "VAULT_USD",
                name = "صندوق الدولار",
                glAccountCode = "1101",
                currency = "USD",
                allowNegative = true
            )
            db.treasuryDao().insertTreasury(usdTreasury)

            val jan15 = LocalDate.of(2026, 1, 15).toEpochDay()
            val jan31 = LocalDate.of(2026, 1, 31).toEpochDay()
            val feb10 = LocalDate.of(2026, 2, 10).toEpochDay()

            val rateJan15 = ExchangeRate(CurrencyCode.USD, CurrencyCode.FUNCTIONAL, 530_000_000L) // 530 YER

            // Jan 15: Inflow of 1,000 USD @ 530 YER/USD = 530,000 YER
            writer.postCustomerReceipt(
                partyId = "CUST_TEST_01",
                treasuryId = usdTreasury.id,
                fiscalYear = 2026,
                dateEpochDay = jan15,
                amountOrigMinor = 1_000_00L,
                currency = CurrencyCode.USD,
                exchangeRate = rateJan15,
                notes = "إيداع دولاري في يناير"
            )

            // Feb 10: Inflow of another 2,000 USD @ 535 YER/USD
            val rateFeb10 = ExchangeRate(CurrencyCode.USD, CurrencyCode.FUNCTIONAL, 535_000_000L) // 535 YER
            writer.postCustomerReceipt(
                partyId = "CUST_TEST_01",
                treasuryId = usdTreasury.id,
                fiscalYear = 2026,
                dateEpochDay = feb10,
                amountOrigMinor = 2_000_00L,
                currency = CurrencyCode.USD,
                exchangeRate = rateFeb10,
                notes = "إيداع دولاري في فبراير"
            )

            // Now perform Revaluation as of Jan 31 @ 540 YER/USD
            // As of Jan 31: balance was strictly 1,000 USD with book base 530,000 YER.
            // Market value at 540 = 540,000 YER. Expected delta = +10,000 YER = 10,000_00 minor.
            // If revaluation wasn't anchored to Jan 31, it would wrongly evaluate 3,000 USD!
            val revalRateJan31 = ExchangeRate(CurrencyCode.USD, CurrencyCode.FUNCTIONAL, 540_000_000L) // 540 YER
            val revalDoc = writer.postPeriodicRevaluation(
                accountCode = AccountConstants.CASH_VAULT,
                treasuryId = usdTreasury.id,
                fiscalYear = 2026,
                dateEpochDay = jan31,
                foreignCurrency = CurrencyCode.USD,
                exchangeRate = revalRateJan31,
                rateZone = RateZone.SANAA
            )

            assertNotNull("Revaluation document must be generated", revalDoc)
            // Delta must be strictly based on Jan 31 balance of 1,000 USD: (540 - 530) * 1000 = 10,000 YER = 10,000_00 minor
            assertEquals(10_000_00L, revalDoc!!.totalMinor)
            assertEquals(10_000_00L, revalDoc.totalBaseMinor)
        }
    }

    @Test
    fun testTask4_FiscalYearStartMonthAwareness() {
        runBlocking {
            // Set organization fiscalYearStartMonth = 7 (July)
            val seededOrg = db.organizationDao().getOrganizationSync()
            requireNotNull(seededOrg) { "Seeded organization must exist" }
            db.organizationDao().updateOrganization(seededOrg.copy(fiscalYearStartMonth = 7))

            val report = generateMonthlyProfitReport(
                db = db,
                fiscalYear = 2026
            )

            assertEquals("Annual summary label must match fiscal year", "السنة المالية 2026", report.annualSummaryLabel)
            assertEquals("Must generate 12 fiscal months", 12, report.months.size)

            // First fiscal month (index 1) must be July 2026
            val firstMonth = report.months[0]
            assertEquals(1, firstMonth.monthIndex)
            assertEquals(LocalDate.of(2026, 7, 1).toEpochDay(), firstMonth.fromEpochDay)

            // Seventh fiscal month (index 7) must be January 2027 of next calendar year
            val seventhMonth = report.months[6]
            assertEquals(7, seventhMonth.monthIndex)
            assertEquals(LocalDate.of(2027, 1, 1).toEpochDay(), seventhMonth.fromEpochDay)

            // Twelfth fiscal month (index 12) must be June 2027
            val twelfthMonth = report.months[11]
            assertEquals(12, twelfthMonth.monthIndex)
            assertEquals(LocalDate.of(2027, 6, 1).toEpochDay(), twelfthMonth.fromEpochDay)

            // Reconciliation checks
            assertTrue("Monthly profit report reconciliation checks must pass", report.reconciliation.reconciled)
        }
    }

    @Test
    fun testTask5_Migration3To4WithImmutableTrigger() {
        // Build a raw SQLite database at version 3 with the trigger installed
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_3_4.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        // Create v3 schema subset
                        sdb.execSQL("""
                            CREATE TABLE organizations (
                                id TEXT PRIMARY KEY NOT NULL,
                                name TEXT NOT NULL,
                                taxNumber TEXT NOT NULL DEFAULT '',
                                functionalCurrency TEXT NOT NULL DEFAULT 'YER',
                                fiscalYearStartMonth INTEGER NOT NULL DEFAULT 1,
                                isInitialized INTEGER NOT NULL DEFAULT 0,
                                primaryRateZone TEXT NOT NULL DEFAULT 'SANAA',
                                equityShareMode TEXT NOT NULL DEFAULT 'DERIVED_FROM_CAPITAL',
                                createdAt INTEGER NOT NULL DEFAULT 0
                            )
                        """)
                        sdb.execSQL("""
                            CREATE TABLE documents (
                                id TEXT PRIMARY KEY NOT NULL,
                                type TEXT NOT NULL,
                                fiscalYear INTEGER NOT NULL,
                                docNumber INTEGER NOT NULL,
                                partyId TEXT NOT NULL,
                                dateEpochDay INTEGER NOT NULL,
                                currency TEXT NOT NULL,
                                exchangeRateMicros INTEGER NOT NULL,
                                rateZone TEXT NOT NULL DEFAULT 'DEFAULT',
                                rateSource TEXT NOT NULL DEFAULT 'SYSTEM_DAILY',
                                totalMinor INTEGER NOT NULL,
                                totalBaseMinor INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                notes TEXT NOT NULL,
                                pdfUri TEXT,
                                createdAt INTEGER NOT NULL
                            )
                        """)
                        sdb.execSQL("""
                            CREATE TABLE journal_entries (
                                id TEXT PRIMARY KEY NOT NULL,
                                docId TEXT NOT NULL,
                                entryNumber INTEGER NOT NULL,
                                entryDateEpochDay INTEGER NOT NULL,
                                fiscalYear INTEGER NOT NULL,
                                type TEXT NOT NULL,
                                notes TEXT NOT NULL,
                                createdAt INTEGER NOT NULL
                            )
                        """)
                        sdb.execSQL("""
                            CREATE TABLE number_sequences (
                                docType TEXT NOT NULL,
                                fiscalYear INTEGER NOT NULL,
                                nextVal INTEGER NOT NULL,
                                PRIMARY KEY(docType, fiscalYear)
                            )
                        """)

                        // Install immutable update trigger that v3 has
                        sdb.execSQL("""
                            CREATE TRIGGER prevent_journal_entries_update
                            BEFORE UPDATE ON journal_entries
                            BEGIN
                                SELECT RAISE(ABORT, 'Journal entries are immutable and cannot be updated');
                            END;
                        """)
                    }

                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val rawDb = helper.writableDatabase
        try {
            // Insert 2 entries with duplicate entryNumber = 100
            rawDb.execSQL("INSERT INTO journal_entries VALUES ('JE1', 'DOC1', 100, 20000, 2026, 'GENERAL', 'Notes 1', 1000)")
            rawDb.execSQL("INSERT INTO journal_entries VALUES ('JE2', 'DOC2', 100, 20001, 2026, 'GENERAL', 'Notes 2', 2000)")

            // Run MIGRATION_3_4
            AppDatabase.MIGRATION_3_4.migrate(rawDb)

            // Verify entryNumbers are deduplicated and valid
            val cursor = rawDb.query("SELECT id, entryNumber FROM journal_entries ORDER BY entryNumber ASC")
            assertTrue(cursor.moveToFirst())
            val num1 = cursor.getLong(1)
            assertTrue(cursor.moveToNext())
            val num2 = cursor.getLong(1)
            cursor.close()

            assertTrue("Numbers must be deduplicated", num1 != num2)

            // Verify that the prevent_journal_entries_update trigger was re-installed and still protects immutability
            val updateEx = assertThrows(Exception::class.java) {
                rawDb.execSQL("UPDATE journal_entries SET notes = 'Hacked' WHERE id = 'JE1'")
            }
            assertTrue("Trigger must prevent updates", updateEx.message!!.contains("Journal entries are immutable"))
        } finally {
            rawDb.close()
            context.deleteDatabase("test_migration_3_4.db")
        }
    }
}
