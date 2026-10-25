package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentVoucherType
import com.example.data.ledger.SalesItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.dao.IncomeStatementRow
import com.example.data.local.entity.FiscalPeriodEntity
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
class MonthlyProfitBreakdownTest {

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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testMonthDateBoundariesAndLeapYear() {
        // Test leap year (2024: Feb has 29 days)
        val feb2024Start = LocalDate.of(2024, 2, 1).toEpochDay()
        val feb2024End = LocalDate.of(2024, 2, 29).toEpochDay()
        assertEquals(29L, feb2024End - feb2024Start + 1)

        // Test non-leap year (2025: Feb has 28 days)
        val feb2025Start = LocalDate.of(2025, 2, 1).toEpochDay()
        val feb2025End = LocalDate.of(2025, 2, 28).toEpochDay()
        assertEquals(28L, feb2025End - feb2025Start + 1)

        // Test January (31 days) and transition to Feb 1
        val janStart = LocalDate.of(2026, 1, 1).toEpochDay()
        val janEnd = LocalDate.of(2026, 1, 31).toEpochDay()
        val febStart = LocalDate.of(2026, 2, 1).toEpochDay()
        assertEquals(janEnd + 1, febStart)
    }

    @Test
    fun testLossesDisplayAsNegativeNoMaxZero() {
        // Direct ISP cost of 1,000,000 YER exceeds revenue of 300,000 YER -> Loss of -700,000 YER
        val rows = listOf(
            IncomeStatementRow(
                accountCode = AccountConstants.CARD_SALES_REVENUE,
                accountName = "إيرادات كروت",
                totalDebitMinor = 0L,
                totalCreditMinor = 300_000_00L,
                isDebitNormal = false
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.DIRECT_ISP_SERVICE_COST,
                accountName = "اشتراكات ستارلينك وألياف",
                totalDebitMinor = 1_000_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            )
        )

        val monthItem = calculateSingleMonthProfit(
            fiscalYear = 2026,
            monthIndex = 1,
            monthName = "يناير",
            fromEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            toEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
            rows = rows,
            cumulativeBeforeMinor = 0L,
            periodStatus = MonthlyProfitStatus(MonthClosingStatus.IN_PROGRESS, "الشهر قيد النشاط")
        )

        // Net profit MUST be negative (-700,000 YER) and NOT clamped to 0
        assertEquals(-700_000_00L, monthItem.grossProfitMinor)
        assertEquals(-700_000_00L, monthItem.netProfitMinor)
        assertEquals(-700_000_00L, monthItem.cumulativeProfitMinor)
        assertTrue("Net profit must be strictly negative", monthItem.netProfitMinor < 0L)
    }

    @Test
    fun testUnmappedAccountsCategorizedAsOtherAndMatchTotal() {
        val rows = listOf(
            IncomeStatementRow(
                accountCode = AccountConstants.CARD_SALES_REVENUE,
                accountName = "إيرادات كروت",
                totalDebitMinor = 0L,
                totalCreditMinor = 1_000_000_00L,
                isDebitNormal = false
            ),
            // Custom unlisted 4xxx account: e.g. 4888 إيرادات بيع أجهزة خردة
            IncomeStatementRow(
                accountCode = "4888",
                accountName = "إيرادات بيع أجهزة قديمة",
                totalDebitMinor = 0L,
                totalCreditMinor = 250_000_00L,
                isDebitNormal = false
            ),
            // Custom unlisted 5xxx account: e.g. 5888 إكراميات وتبرعات
            IncomeStatementRow(
                accountCode = "5888",
                accountName = "إكراميات وتبرعات حراسة",
                totalDebitMinor = 50_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            )
        )

        val monthItem = calculateSingleMonthProfit(
            fiscalYear = 2026,
            monthIndex = 1,
            monthName = "يناير",
            fromEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            toEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
            rows = rows,
            cumulativeBeforeMinor = 0L,
            periodStatus = MonthlyProfitStatus(MonthClosingStatus.IN_PROGRESS, "قيد النشاط")
        )

        assertEquals(250_000_00L, monthItem.otherRevenueMinor)
        assertEquals(50_000_00L, monthItem.otherExpensesMinor)
        assertEquals(1_250_000_00L, monthItem.netRevenueMinor)
        assertEquals(1_200_000_00L, monthItem.netProfitMinor)
    }

    @Test
    fun testPartnerShareEstimationLargestRemainderZeroShortfall() {
        val partners = listOf(
            PartyEntity(id = "p1", name = "شريك 1", isPartner = true, equityPercentageBasisPoints = 5000), // 50%
            PartyEntity(id = "p2", name = "شريك 2", isPartner = true, equityPercentageBasisPoints = 3000), // 30%
            PartyEntity(id = "p3", name = "شريك 3", isPartner = true, equityPercentageBasisPoints = 2000)  // 20%
        )

        // Odd number to test remainder distribution down to exact Rial
        val profitMinor = 1_000_001_00L // 1,000,001.00 YER
        val shares = calculatePartnerProfitShares(profitMinor, partners)

        assertEquals(3, shares.size)
        val sumOfShares = shares.sumOf { it.estimatedShareMinor }
        assertEquals("Sum of partner shares must equal total profit exactly", profitMinor, sumOfShares)

        // Test with negative profit (Loss distribution)
        val lossMinor = -500_003_00L
        val lossShares = calculatePartnerProfitShares(lossMinor, partners)
        assertEquals(3, lossShares.size)
        val sumOfLossShares = lossShares.sumOf { it.estimatedShareMinor }
        assertEquals("Sum of partner loss shares must equal total loss exactly", lossMinor, sumOfLossShares)
    }

    @Test
    fun testRealRoomDbLedgerWriterAndMonthlyReportGeneration() = runBlocking {
        val fiscalYear = 2026
        val customer = PartyEntity(id = "cust-1", name = "عميل تجريبي", isCustomer = true)
        val partner = PartyEntity(id = "partner-1", name = "الشريك المؤسس", isPartner = true, equityPercentageBasisPoints = 10000)
        val vendor = PartyEntity(id = "vend-1", name = "مزود ستارلينك", isVendor = true)
        db.partyDao().insertParty(customer)
        db.partyDao().insertParty(partner)
        db.partyDao().insertParty(vendor)

        db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)
        val rate = ExchangeRate.parity(CurrencyCode.YER)

        // 1. Post a Sales Invoice in Month 1 (Jan 15, 2026)
        val janEpochDay = LocalDate.of(2026, 1, 15).toEpochDay()
        ledgerWriter.postSalesInvoice(
            partyId = customer.id,
            cardItems = emptyList(),
            serviceItems = listOf(
                SalesItemSpec("اشتراك شهري", 1, 500_000_00L)
            ),
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            fiscalYear = fiscalYear,
            dateEpochDay = janEpochDay
        )

        // 2. Post Direct Upstream ISP payment in Month 1 (Jan 20, 2026)
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendor.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = janEpochDay + 5,
            amountOrigMinor = 200_000_00L,
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            paymentType = PaymentVoucherType.DIRECT_ISP_SERVICE,
            notes = "اشتراك سعات إنترنت"
        )

        // 3. Post a Sales Invoice in Month 2 (Feb 10, 2026)
        val febEpochDay = LocalDate.of(2026, 2, 10).toEpochDay()
        ledgerWriter.postSalesInvoice(
            partyId = customer.id,
            cardItems = emptyList(),
            serviceItems = listOf(
                SalesItemSpec("اشتراك شهر 2", 1, 300_000_00L)
            ),
            currency = CurrencyCode.YER,
            exchangeRate = rate,
            fiscalYear = fiscalYear,
            dateEpochDay = febEpochDay
        )

        // Mark Period 1 as Closed
        db.fiscalPeriodDao().insertPeriod(
            FiscalPeriodEntity(
                id = "FP_2026_01",
                year = 2026,
                month = 1,
                isClosed = true,
                closedAt = System.currentTimeMillis()
            )
        )

        // Generate the 12-month report from the real database
        val report = generateMonthlyProfitReport(db, fiscalYear, partners = listOf(partner))

        // Assert 12 months in matrix
        assertEquals(12, report.months.size)

        // Month 1: 500,000 revenue - 200,000 ISP cost = 300,000 net profit
        val m1 = report.months[0]
        assertEquals(1, m1.monthIndex)
        assertEquals(500_000_00L, m1.netRevenueMinor)
        assertEquals(200_000_00L, m1.directIspCostMinor)
        assertEquals(300_000_00L, m1.grossProfitMinor)
        assertEquals(300_000_00L, m1.netProfitMinor)
        assertEquals(300_000_00L, m1.cumulativeProfitMinor)
        assertEquals(MonthClosingStatus.FINAL, m1.status.status)

        // Month 2: 300,000 revenue - 0 cost = 300,000 profit; cumulative = 600,000
        val m2 = report.months[1]
        assertEquals(2, m2.monthIndex)
        assertEquals(300_000_00L, m2.netRevenueMinor)
        assertEquals(300_000_00L, m2.netProfitMinor)
        assertEquals(600_000_00L, m2.cumulativeProfitMinor)

        // Months 3..12 should have 0 profit, cumulative remains 600,000
        val m12 = report.months[11]
        assertEquals(12, m12.monthIndex)
        assertEquals(0L, m12.netProfitMinor)
        assertEquals(600_000_00L, m12.cumulativeProfitMinor)

        // Year Totals
        assertEquals(800_000_00L, report.yearTotalNetRevenueMinor)
        assertEquals(200_000_00L, report.yearTotalDirectIspCostMinor)
        assertEquals(600_000_00L, report.yearTotalNetProfitMinor)

        // Reconciliation checks
        assertTrue("Report must be 100% reconciled", report.reconciliation.reconciled)
        assertEquals(3, report.reconciliation.checks.size)

        // Partner shares: Partner has 100% equity -> gets exactly 600,000
        assertEquals(1, report.partnerShares.size)
        assertEquals(600_000_00L, report.partnerShares[0].estimatedShareMinor)
    }

    @Test
    fun testMonthlyComparisonCalculations() {
        val m1 = MonthlyProfitItem(
            fiscalYear = 2026,
            monthIndex = 1,
            monthName = "يناير",
            fromEpochDay = 100L,
            toEpochDay = 130L,
            cardRevenueMinor = 0L,
            serviceRevenueMinor = 500_000L,
            salesReturnsMinor = 0L,
            otherRevenueMinor = 0L,
            netRevenueMinor = 500_000L,
            directIspCostMinor = 200_000L,
            grossProfitMinor = 300_000L,
            grossMarginBasisPoints = 6000,
            operatingExpensesMinor = 0L,
            maintenanceExpensesMinor = 0L,
            salariesExpensesMinor = 0L,
            miscExpensesMinor = 0L,
            otherExpensesMinor = 0L,
            totalOperatingExpensesMinor = 0L,
            depreciationExpenseMinor = 0L,
            realizedFxGainMinor = 0L,
            realizedFxLossMinor = 0L,
            realizedOperatingProfitMinor = 300_000L,
            unrealizedFxGainMinor = 0L,
            unrealizedFxLossMinor = 0L,
            netProfitMinor = 300_000L,
            netMarginBasisPoints = 6000,
            cumulativeProfitMinor = 300_000L,
            status = MonthlyProfitStatus(MonthClosingStatus.FINAL, "مقفلة")
        )

        val m2 = m1.copy(
            monthIndex = 2,
            monthName = "فبراير",
            netProfitMinor = 450_000L,
            cumulativeProfitMinor = 750_000L
        )

        val lastYearM2 = m2.copy(
            fiscalYear = 2025,
            netProfitMinor = 250_000L
        )

        val comparison = calculateMonthlyComparison(
            selectedMonth = m2,
            previousMonth = m1,
            sameMonthLastYear = lastYearM2
        )

        assertEquals(150_000L, comparison.diffVsPreviousMinor) // 450,000 - 300,000
        assertEquals(200_000L, comparison.diffVsLastYearMinor) // 450,000 - 250,000
    }
}
