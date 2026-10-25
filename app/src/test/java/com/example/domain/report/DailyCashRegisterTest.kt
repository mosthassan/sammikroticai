package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.core.ledger.JournalEntryType
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.UuidUtils
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentVoucherType
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalLineEntity
import com.example.data.local.entity.PartyEntity
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyCashRegisterTest {

    private lateinit var db: AppDatabase
    private lateinit var ledgerWriter: LedgerWriter
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
        AppDatabase.seedDefaultData(db.openHelper.writableDatabase)
        ledgerWriter = LedgerWriter(db, enableInvariantValidation = false)

        runBlocking {
            db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)
            db.treasuryDao().setAllowNegative("TR_USD_VAULT", true)
            db.treasuryDao().setAllowNegative("TR_SAR_VAULT", true)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Test 1: Zero-Base Positive Original Lines
     * A journal line with origMinor > 0 but baseDebitMinor == 0 and baseCreditMinor == 0.
     * The register must treat it as an Inflow in original currency (+origMinor),
     * update running balance, and maintain complete reconciliation without error.
     */
    @Test
    fun testZeroBasePositiveOriginalLines() = runBlocking {
        val fiscalYear = 2026
        val testDate = LocalDate.of(2026, 3, 10).toEpochDay()

        // 1. Create a document for zero-base test entry
        val docId = UuidUtils.newTimeOrderedId()
        db.documentDao().insertDocument(
            DocumentEntity(
                id = docId,
                type = DocumentType.RECEIPT_VOUCHER.name,
                fiscalYear = fiscalYear,
                docNumber = 9001L,
                partyId = AppDatabase.WALK_IN_CASH_PARTY_ID,
                dateEpochDay = testDate,
                currency = "USD",
                exchangeRateMicros = 0L,
                totalMinor = 50_000L,
                totalBaseMinor = 0L,
                status = "POSTED",
                notes = "حركة تجريبية صفرية الأساس بمبلغ أصلي موجب"
            )
        )

        // 2. Insert Journal Entry & Line with origMinor = 50,000 minor ($500.00), baseDebitMinor = 0, baseCreditMinor = 0
        val entryId = UuidUtils.newTimeOrderedId()
        db.journalDao().insertEntry(
            JournalEntryEntity(
                id = entryId,
                docId = docId,
                entryNumber = 9001L,
                entryDateEpochDay = testDate,
                type = JournalEntryType.NORMAL.name,
                memo = "قيد حركة صفرية الأساس"
            )
        )

        db.journalDao().insertLines(
            listOf(
                JournalLineEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    entryId = entryId,
                    lineNo = 1,
                    accountCode = AccountConstants.CASH_VAULT,
                    partyId = AppDatabase.WALK_IN_CASH_PARTY_ID,
                    treasuryId = "TR_USD_VAULT",
                    origMinor = 50_000L, // $500.00 USD
                    currency = "USD",
                    exchangeRateMicros = 0L,
                    baseDebitMinor = 0L,  // Zero base debit
                    baseCreditMinor = 0L, // Zero base credit
                    memo = "سند حركة صفرية الأساس موجبة"
                )
            )
        )

        // 3. Generate Daily Cash Register for TR_USD_VAULT
        val statement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_USD_VAULT",
            startDateEpochDay = testDate,
            endDateEpochDay = testDate
        )

        // Verify line is treated as an original inflow
        assertEquals("Should have exactly 1 transaction line", 1, statement.lines.size)
        val line = statement.lines[0]
        assertEquals("Inflow original minor must equal 50,000", 50_000L, line.inflowOrigMinor)
        assertEquals("Outflow original minor must be 0", 0L, line.outflowOrigMinor)
        assertEquals("Inflow base minor must be 0", 0L, line.inflowBaseMinor)
        assertEquals("Outflow base minor must be 0", 0L, line.outflowBaseMinor)
        assertEquals("Running balance in original currency must be 50,000", 50_000L, line.runningBalanceOrigMinor)
        assertEquals("Running balance in base currency must be 0", 0L, line.runningBalanceBaseMinor)

        // Verify totals and closing balances
        assertEquals(50_000L, statement.totalInflowOrigMinor)
        assertEquals(0L, statement.totalOutflowOrigMinor)
        assertEquals(50_000L, statement.closingBalanceOrigMinor)
        assertEquals(0L, statement.closingBalanceBaseMinor)

        // Verify reconciliation
        assertTrue("Original balance must reconcile", statement.reconciliation.isOrigReconciled)
        assertTrue("Base balance must reconcile", statement.reconciliation.isBaseReconciled)
        assertTrue("Statement must be fully reconciled", statement.reconciliation.isFullyReconciled)
        assertEquals(0L, statement.reconciliation.origDifferenceMinor)
        assertEquals(0L, statement.reconciliation.baseDifferenceMinor)
    }

    /**
     * Test 2: Future-Dated Entry Exclusion
     * Entries dated after endDateEpochDay must NOT appear in the register,
     * must NOT impact running balances, and must NOT affect closing balances.
     */
    @Test
    fun testFutureDatedEntryExclusion() = runBlocking {
        val fiscalYear = 2026
        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan10 = LocalDate.of(2026, 1, 10).toEpochDay()
        val jan15 = LocalDate.of(2026, 1, 15).toEpochDay() // Report End Date
        val jan25 = LocalDate.of(2026, 1, 25).toEpochDay() // Future-dated entry

        // 1. Initial Capital Receipt on Jan 1: 100,000 YER (10,000,000 minor)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 10_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "إيداع رأس مال أولي"
        )

        // 2. Customer Receipt on Jan 10: 25,000 YER (2,500,000 minor)
        val customer = PartyEntity(
            id = "CUST_FUTURE_TEST",
            name = "عميل سداد محلي",
            isCustomer = true
        )
        db.partyDao().insertParty(customer)

        ledgerWriter.postCustomerReceipt(
            partyId = customer.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan10,
            amountOrigMinor = 2_500_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "تحصيل اشتراك شهري"
        )

        // 3. Future-Dated Customer Receipt on Jan 25: 50,000 YER (5,000,000 minor)
        ledgerWriter.postCustomerReceipt(
            partyId = customer.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan25,
            amountOrigMinor = 5_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "حركة نقدية مستقبلية"
        )

        // 4. Generate Register as of Jan 15 (Jan 25 is in the future)
        val statement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_MAIN_YER",
            startDateEpochDay = jan1,
            endDateEpochDay = jan15
        )

        // Verify only Jan 1 and Jan 10 entries are included (2 entries total)
        assertEquals("Should only contain 2 entries up to Jan 15", 2, statement.lines.size)
        assertTrue("No entries should be dated after Jan 15", statement.lines.none { it.dateEpochDay > jan15 })

        // Closing balance on Jan 15 must be 10,000,000 + 2,500,000 = 12,500,000 YER minor (NOT 17,500,000)
        assertEquals("Closing balance must exclude future Jan 25 entry", 12_500_000L, statement.closingBalanceOrigMinor)
        assertEquals("Base closing balance must exclude future Jan 25 entry", 12_500_000L, statement.closingBalanceBaseMinor)

        // Reconciliation checks
        assertTrue("Original balance must reconcile as of Jan 15", statement.reconciliation.isOrigReconciled)
        assertTrue("Base balance must reconcile as of Jan 15", statement.reconciliation.isBaseReconciled)
        assertTrue(statement.reconciliation.isFullyReconciled)
        assertEquals(12_500_000L, statement.reconciliation.treasuryCashBalanceOrigMinor)
    }

    /**
     * Test 3: Exact Currency Matching & Treasury Isolation
     * Ensuring that only transactions matching both the selected treasuryId
     * AND designated currency are included.
     */
    @Test
    fun testExactCurrencyMatchingAndTreasuryIsolation() = runBlocking {
        val fiscalYear = 2026
        val testDate = LocalDate.of(2026, 2, 1).toEpochDay()

        // 1. Transaction in YER Treasury (TR_MAIN_YER): 300,000 YER (30,000,000 minor)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = testDate,
            amountOrigMinor = 30_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "تغذية صندوق الريال اليمني"
        )

        // 2. Transaction in USD Treasury (TR_USD_VAULT): $1,000 USD (100,000 minor)
        val rateUsd = ExchangeRate(
            fromCurrency = CurrencyCode.USD,
            toCurrency = CurrencyCode.FUNCTIONAL,
            rateMicros = 530_000_000L // 530 YER/USD
        )
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_USD_VAULT",
            fiscalYear = fiscalYear,
            dateEpochDay = testDate,
            amountOrigMinor = 100_000L, // $1,000.00
            currency = CurrencyCode.USD,
            exchangeRate = rateUsd,
            notes = "تغذية خزينة الدولار"
        )

        // 3. Generate Register specifically for TR_USD_VAULT
        val usdStatement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_USD_VAULT",
            startDateEpochDay = testDate,
            endDateEpochDay = testDate
        )

        // Verify only the USD transaction is present
        assertEquals("USD register must contain exactly 1 transaction", 1, usdStatement.lines.size)
        val usdLine = usdStatement.lines[0]
        assertEquals("Currency must be USD", "USD", usdStatement.currency)
        assertEquals("Line amount must be 100,000 USD minor", 100_000L, usdLine.inflowOrigMinor)
        assertEquals("Line base amount must be 53,000,000 YER minor", 53_000_000L, usdLine.inflowBaseMinor)
        assertEquals("Closing balance must be 100,000 USD minor", 100_000L, usdStatement.closingBalanceOrigMinor)
        assertEquals("Closing base balance must be 53,000,000 YER minor", 53_000_000L, usdStatement.closingBalanceBaseMinor)

        // Verify YER transactions are completely excluded from USD register
        assertTrue("USD register must not contain any YER lines", usdStatement.lines.none { it.inflowOrigMinor == 30_000_000L })

        // Verify TR_MAIN_YER register has only YER
        val yerStatement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_MAIN_YER",
            startDateEpochDay = testDate,
            endDateEpochDay = testDate
        )
        assertEquals("YER register must contain exactly 1 transaction", 1, yerStatement.lines.size)
        assertEquals("Currency must be YER", "YER", yerStatement.currency)
        assertEquals(30_000_000L, yerStatement.closingBalanceOrigMinor)
    }

    /**
     * Test 4: Daily Totals Toggle & Grouping
     * Grouping lines by date and computing exact day subtotals (Inflow, Outflow, Net, Day Ending Balance).
     */
    @Test
    fun testDailyTotalsToggleAndGrouping() = runBlocking {
        val fiscalYear = 2026
        val day1 = LocalDate.of(2026, 3, 1).toEpochDay()
        val day2 = LocalDate.of(2026, 3, 2).toEpochDay()

        val customer = PartyEntity(id = "CUST_DAILY_01", name = "عميل مشترك", isCustomer = true)
        db.partyDao().insertParty(customer)

        // Day 1: Capital receipt +50,000 YER (5,000,000 minor)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = day1,
            amountOrigMinor = 5_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "إيداع اليوم الأول"
        )

        // Day 2 Transaction 1: Customer Receipt +20,000 YER (2,000,000 minor)
        ledgerWriter.postCustomerReceipt(
            partyId = customer.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = day2,
            amountOrigMinor = 2_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "تحصيل اليوم الثاني"
        )

        // Day 2 Transaction 2: Direct Expense Payment -10,000 YER (1,000,000 minor)
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = AppDatabase.WALK_IN_CASH_PARTY_ID,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = day2,
            amountOrigMinor = 1_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.OPERATING_EXPENSE,
            customExpenseCode = AccountConstants.OPERATING_EXPENSES,
            notes = "شراء ديزل للمولد"
        )

        // Generate statement covering both days
        val statement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_MAIN_YER",
            startDateEpochDay = day1,
            endDateEpochDay = day2
        )

        assertEquals("Total lines must be 3", 3, statement.lines.size)
        assertEquals("Should have exactly 2 daily groups", 2, statement.dailyGroups.size)

        // Verify Day 1 Group
        val groupDay1 = statement.dailyGroups.first { it.dateEpochDay == day1 }
        assertEquals(5_000_000L, groupDay1.dayInflowOrigMinor)
        assertEquals(0L, groupDay1.dayOutflowOrigMinor)
        assertEquals(5_000_000L, groupDay1.dayNetOrigMinor)
        assertEquals(5_000_000L, groupDay1.dayEndingBalanceOrigMinor)

        // Verify Day 2 Group
        val groupDay2 = statement.dailyGroups.first { it.dateEpochDay == day2 }
        assertEquals(2_000_000L, groupDay2.dayInflowOrigMinor)
        assertEquals(1_000_000L, groupDay2.dayOutflowOrigMinor)
        assertEquals(1_000_000L, groupDay2.dayNetOrigMinor) // 2M - 1M = +1M
        assertEquals(6_000_000L, groupDay2.dayEndingBalanceOrigMinor) // 5M + 1M = 6M

        // Closing Balance
        assertEquals(6_000_000L, statement.closingBalanceOrigMinor)
        assertTrue(statement.reconciliation.isFullyReconciled)
    }

    /**
     * Test 5: Value Estimation Note with Required Disclosure
     * Ensures estimation note uses current exchange rate, computes market YER equivalent,
     * and verbatim includes: "تقدير بسعر اليوم، لا يدخل في أي قيد".
     */
    @Test
    fun testValueEstimationNoteDisclosure() = runBlocking {
        val fiscalYear = 2026
        val testDate = LocalDate.of(2026, 4, 1).toEpochDay()

        // 1. Post USD Receipt of $2,000 USD (200,000 minor) booked at 530 YER/USD
        val bookedRateUsd = ExchangeRate(
            fromCurrency = CurrencyCode.USD,
            toCurrency = CurrencyCode.FUNCTIONAL,
            rateMicros = 530_000_000L
        )
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_USD_VAULT",
            fiscalYear = fiscalYear,
            dateEpochDay = testDate,
            amountOrigMinor = 200_000L, // $2,000.00
            currency = CurrencyCode.USD,
            exchangeRate = bookedRateUsd,
            notes = "إيداع دولاري"
        )

        // 2. Set updated market exchange rate: 1 USD = 540 YER (540_000_000 micros)
        db.currencyRateDao().insertRate(
            com.example.data.local.entity.CurrencyRateEntity(
                id = "RATE_USD_TEST_LATEST",
                currency = "USD",
                zone = "SANAA",
                rateMicros = 540_000_000L,
                effectiveDateEpochDay = testDate,
                reason = "تحديث سعر الصرف اليومي"
            )
        )

        // 3. Generate Register
        val statement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_USD_VAULT",
            startDateEpochDay = testDate,
            endDateEpochDay = testDate
        )

        val note = statement.valueEstimation
        assertTrue("Must be foreign currency", note.isForeignCurrency)
        assertEquals("Currency must be USD", "USD", note.currency)
        assertEquals("Current exchange rate must be 540,000,000 micros", 540_000_000L, note.currentExchangeRateMicros)

        // Estimated YER: $2,000 * 540 = 1,080,000 YER (108,000,000 minor)
        val expectedEstimatedYer = (200_000L * 540_000_000L) / ExchangeRate.SCALE_MICROS
        assertEquals(108_000_000L, expectedEstimatedYer)
        assertEquals(expectedEstimatedYer, note.estimatedCurrentYerMinor)

        // Book value YER: $2,000 * 530 = 1,060,000 YER (106,000,000 minor)
        assertEquals(106_000_000L, note.bookValueBaseMinor)

        // Unrealized delta: +2,000,000 YER minor
        assertEquals(2_000_000L, note.unrealizedDeltaYerMinor)

        // VERIFY MANDATORY DISCLOSURE PHRASE
        assertTrue(
            "Note must strictly include: 'تقدير بسعر اليوم، لا يدخل في أي قيد'",
            note.noteTextAr.contains("تقدير بسعر اليوم، لا يدخل في أي قيد")
        )
    }

    /**
     * Test 6: Period Opening Balance Continuity
     * Running report for a second period should carry forward opening balance from previous transactions.
     */
    @Test
    fun testOpeningBalanceContinuity() = runBlocking {
        val fiscalYear = 2026
        val jan5 = LocalDate.of(2026, 1, 5).toEpochDay()
        val feb1 = LocalDate.of(2026, 2, 1).toEpochDay()
        val feb15 = LocalDate.of(2026, 2, 15).toEpochDay()

        // Transaction in January: 80,000 YER (8,000,000 minor)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan5,
            amountOrigMinor = 8_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "حركة شهر يناير"
        )

        // Generate register for February 1 to February 15
        val statement = generateDailyCashRegister(
            db = db,
            treasuryId = "TR_MAIN_YER",
            startDateEpochDay = feb1,
            endDateEpochDay = feb15
        )

        // Opening balance in February must be 8,000,000 YER minor
        assertEquals(8_000_000L, statement.openingBalanceOrigMinor)
        assertEquals(8_000_000L, statement.openingBalanceBaseMinor)

        // No transactions in Feb, so closing = opening
        assertEquals(0, statement.lines.size)
        assertEquals(8_000_000L, statement.closingBalanceOrigMinor)
        assertEquals(8_000_000L, statement.closingBalanceBaseMinor)
        assertTrue(statement.reconciliation.isFullyReconciled)
    }
}
