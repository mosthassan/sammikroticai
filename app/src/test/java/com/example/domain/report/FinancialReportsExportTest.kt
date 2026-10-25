package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ShadowPdfDocument
import com.example.core.model.CurrencyCode
import com.example.util.ReportsCsvGenerator
import com.example.util.ReportsPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocument::class])
class FinancialReportsExportTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // =========================================================================
    // 1. DECIMAL FORMATTING & BOM TESTS
    // =========================================================================

    @Test
    fun testDecimalFormatting_neverUsesCommasAndHasPeriod() {
        assertEquals("1234.50", ReportsCsvGenerator.formatDecimal(123450L))
        assertEquals("0.00", ReportsCsvGenerator.formatDecimal(0L))
        assertEquals("-50.25", ReportsCsvGenerator.formatDecimal(-5025L))
        assertEquals("1000000.00", ReportsCsvGenerator.formatDecimal(100000000L))
        assertFalse(ReportsCsvGenerator.formatDecimal(100000000L).contains(","))
    }

    @Test
    fun testBasisPointsFormatting() {
        assertEquals("50.00%", ReportsCsvGenerator.formatBasisPoints(5000))
        assertEquals("100.00%", ReportsCsvGenerator.formatBasisPoints(10000))
        assertEquals("33.33%", ReportsCsvGenerator.formatBasisPoints(3333))
    }

    // =========================================================================
    // 2. REPORT 1: MONTHLY PROFIT EXPORT TESTS
    // =========================================================================

    @Test
    fun testMonthlyProfitExport_csvAndPdf() {
        val check1 = ReconciliationCheck("توازن مجمل الربح", 500000L, 500000L, true)
        val report = MonthlyProfitReport(
            fiscalYear = 2026,
            months = listOf(
                MonthlyProfitItem(
                    fiscalYear = 2026,
                    monthIndex = 1,
                    monthName = "يناير",
                    fromEpochDay = 20454L,
                    toEpochDay = 20484L,
                    cardRevenueMinor = 600000L,
                    serviceRevenueMinor = 100000L,
                    salesReturnsMinor = 0L,
                    otherRevenueMinor = 0L,
                    netRevenueMinor = 700000L,
                    directIspCostMinor = 200000L,
                    grossProfitMinor = 500000L,
                    grossMarginBasisPoints = 7142,
                    operatingExpensesMinor = 50000L,
                    maintenanceExpensesMinor = 20000L,
                    salariesExpensesMinor = 80000L,
                    miscExpensesMinor = 10000L,
                    otherExpensesMinor = 0L,
                    totalOperatingExpensesMinor = 160000L,
                    depreciationExpenseMinor = 0L,
                    realizedFxGainMinor = 0L,
                    realizedFxLossMinor = 0L,
                    realizedOperatingProfitMinor = 340000L,
                    unrealizedFxGainMinor = 0L,
                    unrealizedFxLossMinor = 0L,
                    netProfitMinor = 340000L,
                    netMarginBasisPoints = 4857,
                    cumulativeProfitMinor = 340000L,
                    status = MonthlyProfitStatus(MonthClosingStatus.FINAL, "تم الإقفال بنجاح")
                )
            ),
            yearTotalNetRevenueMinor = 700000L,
            yearTotalDirectIspCostMinor = 200000L,
            yearTotalGrossProfitMinor = 500000L,
            yearTotalOperatingExpensesMinor = 160000L,
            yearTotalDepreciationMinor = 0L,
            yearTotalRealizedFxNetMinor = 0L,
            yearTotalUnrealizedFxNetMinor = 0L,
            yearTotalNetProfitMinor = 340000L,
            partnerShares = listOf(
                PartnerProfitShare("p1", "سالم محمد", 6000, 204000L),
                PartnerProfitShare("p2", "أحمد علي", 4000, 136000L)
            ),
            reconciliation = ReconciliationSummary(listOf(check1))
        )

        // 1. CSV Export
        val csvFile = ReportsCsvGenerator.generateMonthlyProfitCsv(context, report)
        assertTrue(csvFile.exists())
        assertTrue(csvFile.length() > 0)

        // Verify UTF-8 BOM
        val bytes = ByteArray(3)
        FileInputStream(csvFile).use { it.read(bytes) }
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        val content = csvFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("تقرير الأرباح والخسائر الشهرية"))
        assertTrue(content.contains("7000.00")) // net revenue
        assertTrue(content.contains("3400.00")) // net profit
        assertTrue(content.contains("سالم محمد"))

        // 2. PDF Export
        val pdfFile = ReportsPdfGenerator.generateMonthlyProfitPdf(context, report)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.name.endsWith(".pdf"))
        val pdfHeader = ByteArray(5)
        FileInputStream(pdfFile).use { it.read(pdfHeader) }
        assertEquals("%PDF-", String(pdfHeader))
    }

    // =========================================================================
    // 3. REPORT 2: STATEMENT OF CHANGES IN EQUITY EXPORT TESTS
    // =========================================================================

    @Test
    fun testChangesInEquityExport_csvAndPdf() {
        val check = ReconciliationCheck("مطابقة رأس المال والجاري", 1000000L, 1000000L, true)
        val report = StatementOfChangesInEquityReport(
            fromEpochDay = 20454L,
            toEpochDay = 20818L,
            rows = listOf(
                EquityChangeRow(EquityChangeRowType.OPENING_BALANCE, "رصيد أول الفترة", 1000000L, 200000L, 0L, 0L),
                EquityChangeRow(EquityChangeRowType.CASH_CONTRIBUTIONS, "مساهمات نقدية", 500000L, 0L, 0L, 0L),
                EquityChangeRow(EquityChangeRowType.PERIOD_PROFIT, "أرباح الفترة", 0L, 0L, 0L, 340000L),
                EquityChangeRow(EquityChangeRowType.CLOSING_BALANCE, "رصيد نهاية الفترة", 1500000L, 200000L, 0L, 340000L)
            ),
            partnerSummaries = listOf(
                PartnerEquitySummaryRow("p1", "سالم", 6000, 600000L, 120000L, 300000L, 0L, 0L, 0L, 0L, 204000L, 900000L, 120000L)
            ),
            reconciliation = ReconciliationSummary(listOf(check))
        )

        // 1. CSV
        val csvFile = ReportsCsvGenerator.generateChangesInEquityCsv(context, report)
        assertTrue(csvFile.exists())
        val content = csvFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("قائمة التغيرات في حقوق الملكية"))
        assertTrue(content.contains("15000.00")) // closing capital

        // 2. PDF
        val pdfFile = ReportsPdfGenerator.generateChangesInEquityPdf(context, report)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.name.endsWith(".pdf"))
    }

    // =========================================================================
    // 4. REPORT 3: CASH FLOW STATEMENT EXPORT TESTS
    // =========================================================================

    @Test
    fun testCashFlowStatementExport_csvAndPdf() {
        val check = ReconciliationCheck("اتزان حركة النقدية", 500000L, 500000L, true)
        val opCat = CashFlowCategorySummary(
            category = CashFlowCategory.CUSTOMER_COLLECTIONS,
            totalInflowMinor = 700000L,
            totalOutflowMinor = 0L,
            netFlowMinor = 700000L,
            count = 5
        )
        val opSection = CashFlowSection(
            activityType = CashFlowActivityType.OPERATING,
            titleAr = "الأنشطة التشغيلية",
            totalInflowsMinor = 700000L,
            totalOutflowsMinor = 200000L,
            netFlowMinor = 500000L,
            categories = listOf(opCat),
            lines = emptyList()
        )
        val emptySection = CashFlowSection(
            activityType = CashFlowActivityType.INVESTING,
            titleAr = "الأنشطة الاستثمارية",
            totalInflowsMinor = 0L,
            totalOutflowsMinor = 0L,
            netFlowMinor = 0L,
            categories = emptyList(),
            lines = emptyList()
        )

        val report = CashFlowStatement(
            fromEpochDay = 20454L,
            toEpochDay = 20484L,
            openingCashMinor = 100000L,
            closingCashMinor = 600000L,
            netOperatingCashFlowMinor = 500000L,
            netInvestingCashFlowMinor = 0L,
            netFinancingCashFlowMinor = 0L,
            fxImpactOnCashMinor = 0L,
            netChangeInCashMinor = 500000L,
            operatingSection = opSection,
            investingSection = emptySection,
            financingSection = emptySection,
            fxSection = emptySection,
            nonCashDisclosures = listOf(
                NonCashDisclosureItem("d1", 101L, "JV", 20460L, "مساهمة عينية", "سالم", "راوتر", 150000L, "1201")
            ),
            vaultCashClosingMinor = 400000L,
            bankCashClosingMinor = 200000L,
            unclassifiedAmountMinor = 0L,
            reconciliation = ReconciliationSummary(listOf(check))
        )

        // 1. CSV
        val csvFile = ReportsCsvGenerator.generateCashFlowStatementCsv(context, report)
        assertTrue(csvFile.exists())
        val content = csvFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("قائمة التدفقات النقدية"))
        assertTrue(content.contains("تحصيلات من العملاء والوكلاء"))
        assertTrue(content.contains("5000.00")) // net operating
        assertTrue(content.contains("6000.00")) // closing cash

        // 2. PDF
        val pdfFile = ReportsPdfGenerator.generateCashFlowStatementPdf(context, report)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.name.endsWith(".pdf"))
    }

    // =========================================================================
    // 5. REPORT 4: DAILY CASH REGISTER EXPORT TESTS
    // =========================================================================

    @Test
    fun testDailyCashRegisterExport_csvAndPdf() {
        val line = DailyCashRegisterLine(
            lineId = "l1",
            entryId = "entry-001",
            docId = "doc-001",
            entryNumber = 1L,
            dateEpochDay = 20454L,
            dateFormatted = "2026-01-15",
            docType = "CRV",
            docTypeAr = "سند قبض",
            docNumber = 501L,
            partyId = "p1",
            partyName = "سالم محمد",
            description = "مبيعات كروت نقدية",
            inflowOrigMinor = 50000L,
            outflowOrigMinor = 0L,
            inflowBaseMinor = 50000L,
            outflowBaseMinor = 0L,
            exchangeRateMicros = 1_000_000L,
            runningBalanceOrigMinor = 150000L,
            runningBalanceBaseMinor = 150000L
        )

        val note = ValueEstimationNote(
            currency = "YER",
            isForeignCurrency = false,
            currentExchangeRateMicros = 1_000_000L,
            estimatedCurrentYerMinor = 150000L,
            bookValueBaseMinor = 150000L,
            unrealizedDeltaYerMinor = 0L,
            noteTextAr = "الصندوق بعملة الأساس (ريال يمني)، القيمة الدفترية مطابقة تماماً للقيمة السوقية. (تقدير بسعر اليوم، لا يدخل في أي قيد)"
        )

        val reconciliation = DailyCashRegisterReconciliation(
            closingBalanceOrigMinor = 150000L,
            treasuryCashBalanceOrigMinor = 150000L,
            origDifferenceMinor = 0L,
            isOrigReconciled = true,
            closingBalanceBaseMinor = 150000L,
            treasuryBaseBalanceMinor = 150000L,
            baseDifferenceMinor = 0L,
            isBaseReconciled = true,
            glAccountCode = "1101",
            glAccountName = "الصندوق الرئيسي",
            glAccountBaseBalanceMinor = 150000L,
            isFullyReconciled = true,
            statusMessageAr = "الصندوق مطابق تماماً مع رصيد الخزينة وسجل الأستاذ العام"
        )

        val report = DailyCashRegisterStatement(
            treasuryId = "t1",
            treasuryName = "صندوق المبيعات الرئيسي",
            currency = "YER",
            glAccountCode = "1101",
            glAccountName = "الصندوق الرئيسي",
            startDateEpochDay = 20454L,
            endDateEpochDay = 20454L,
            openingBalanceOrigMinor = 100000L,
            openingBalanceBaseMinor = 100000L,
            lines = listOf(line),
            dailyGroups = emptyList(),
            totalInflowOrigMinor = 50000L,
            totalOutflowOrigMinor = 0L,
            netChangeOrigMinor = 50000L,
            totalInflowBaseMinor = 50000L,
            totalOutflowBaseMinor = 0L,
            netChangeBaseMinor = 50000L,
            closingBalanceOrigMinor = 150000L,
            closingBalanceBaseMinor = 150000L,
            valueEstimation = note,
            reconciliation = reconciliation
        )

        // 1. CSV
        val csvFile = ReportsCsvGenerator.generateDailyCashRegisterCsv(context, report)
        assertTrue(csvFile.exists())
        val content = csvFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("سجل الصندوق اليومي"))
        assertTrue(content.contains("صندوق المبيعات الرئيسي"))
        assertTrue(content.contains("500.00")) // inflow 50000 minor = 500.00
        assertTrue(content.contains("1500.00")) // closing 150000 minor = 1500.00
        assertTrue(content.contains("تقدير بسعر اليوم، لا يدخل في أي قيد"))

        // 2. PDF
        val pdfFile = ReportsPdfGenerator.generateDailyCashRegisterPdf(context, report)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.name.endsWith(".pdf"))
    }
}
