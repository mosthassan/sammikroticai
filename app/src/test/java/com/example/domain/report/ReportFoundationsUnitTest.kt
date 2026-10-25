package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.data.local.dao.IncomeStatementRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD Unit tests for Report Module Foundations and Base Rules:
 * 1. ReconciliationCheck & ReconciliationSummary (dynamic calculation, never hardcoded).
 * 2. Shared ProfitBreakdown calculation for ISP operations.
 * 3. Prevention of Double/Float and verification of basis points precision.
 */
class ReportFoundationsUnitTest {

    @Test
    fun testReconciliationCheckPassing() {
        val check = ReconciliationCheck(
            name = "تطابق الأصول مع الخصوم وحقوق الملكية",
            expectedMinor = 1_500_000_00L,
            actualMinor = 1_500_000_00L,
            passed = (1_500_000_00L == 1_500_000_00L),
            note = "الميزانية العمومية متوازنة"
        )

        assertTrue(check.passed)
        assertEquals(0L, check.discrepancyMinor)
    }

    @Test
    fun testReconciliationCheckFailingWhenDiscrepancyExists() {
        val check = ReconciliationCheck(
            name = "توازن ميزان المراجعة",
            expectedMinor = 10_000_000_00L,
            actualMinor = 9_990_000_00L,
            passed = (10_000_000_00L == 9_990_000_00L),
            note = "فرق مدين ودائن 10,000 ريال"
        )

        assertFalse(check.passed)
        assertEquals(-10_000_00L, check.discrepancyMinor)
    }

    @Test
    fun testReconciliationSummaryDynamicReconciledCalculation() {
        // Empty checks must NOT be reconciled
        val emptySummary = ReconciliationSummary(checks = emptyList())
        assertFalse("Empty reconciliation checks must not report reconciled = true", emptySummary.reconciled)

        // Multiple passing checks
        val check1 = ReconciliationCheck("Check 1", 100L, 100L, true)
        val check2 = ReconciliationCheck("Check 2", 200L, 200L, true)
        val passingSummary = ReconciliationSummary(checks = listOf(check1, check2))
        assertTrue(passingSummary.reconciled)
        assertEquals(2, passingSummary.passedCount)
        assertEquals(0, passingSummary.failedCount)

        // One failing check must invalidate whole summary
        val failingCheck = ReconciliationCheck("Check 3", 300L, 250L, false, "فرق غير مسوى")
        val mixedSummary = ReconciliationSummary(checks = listOf(check1, check2, failingCheck))
        assertFalse("Any failed check must make reconciled = false", mixedSummary.reconciled)
        assertEquals(2, mixedSummary.passedCount)
        assertEquals(1, mixedSummary.failedCount)
        assertEquals(-50L, mixedSummary.totalDiscrepancyMinor)
    }

    @Test
    fun testProfitBreakdownCalculation() {
        val fromEpoch = 20000L
        val toEpoch = 20030L

        val rows = listOf(
            IncomeStatementRow(
                accountCode = AccountConstants.CARD_SALES_REVENUE,
                accountName = "إيرادات مبيعات كروت الشبكة",
                totalDebitMinor = 0L,
                totalCreditMinor = 5_000_000_00L,
                isDebitNormal = false
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.DIRECT_SERVICE_REVENUE,
                accountName = "إيرادات اشتراكات وخدمات الشبكة",
                totalDebitMinor = 0L,
                totalCreditMinor = 2_000_000_00L,
                isDebitNormal = false
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.SALES_RETURNS,
                accountName = "مردودات ومسموحات المبيعات",
                totalDebitMinor = 200_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.DIRECT_ISP_SERVICE_COST,
                accountName = "تكلفة اشتراكات مزودي الخدمة (Starlink/ألياف)",
                totalDebitMinor = 2_500_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.OPERATING_EXPENSES,
                accountName = "مصاريف تشغيلية ومواقع ومحطات",
                totalDebitMinor = 600_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.MAINTENANCE_SPARES,
                accountName = "صيانة وقطع غيار أبراج",
                totalDebitMinor = 300_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.SALARIES_STAFF,
                accountName = "رواتب وأجور الفنيين",
                totalDebitMinor = 800_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.MISC_EXPENSES,
                accountName = "مصاريف إدارية وعمومية متنوعة",
                totalDebitMinor = 100_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.DEPRECIATION_EXPENSE,
                accountName = "إهلاك أصول ومعدات الشبكة",
                totalDebitMinor = 400_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.REALIZED_FX_GAIN,
                accountName = "أرباح فروق عملة محققة",
                totalDebitMinor = 0L,
                totalCreditMinor = 150_000_00L,
                isDebitNormal = false
            ),
            IncomeStatementRow(
                accountCode = AccountConstants.REALIZED_FX_LOSS,
                accountName = "خسائر فروق عملة محققة",
                totalDebitMinor = 50_000_00L,
                totalCreditMinor = 0L,
                isDebitNormal = true
            )
        )

        val breakdown = calculateProfitBreakdown(rows, fromEpoch, toEpoch)

        // Revenues
        assertEquals(5_000_000_00L, breakdown.cardRevenueMinor)
        assertEquals(2_000_000_00L, breakdown.serviceRevenueMinor)
        assertEquals(200_000_00L, breakdown.salesReturnsMinor)
        assertEquals(6_800_000_00L, breakdown.grossRevenueMinor)

        // Direct Costs & Gross Profit
        assertEquals(2_500_000_00L, breakdown.directIspCostMinor)
        val expectedGrossProfit = 6_800_000_00L - 2_500_000_00L // 4,300,000.00
        assertEquals(expectedGrossProfit, breakdown.grossProfitMinor)

        // Operating Expenses
        val expectedOpex = 600_000_00L + 300_000_00L + 800_000_00L + 100_000_00L // 1,800,000.00
        assertEquals(expectedOpex, breakdown.totalOperatingExpensesMinor)

        // Depreciation & Operating Profit
        assertEquals(400_000_00L, breakdown.depreciationExpenseMinor)
        val expectedOperatingProfit = expectedGrossProfit - expectedOpex - 400_000_00L // 2,100,000.00
        assertEquals(expectedOperatingProfit, breakdown.operatingProfitMinor)

        // FX Net
        val expectedFxNet = 150_000_00L - 50_000_00L // +100,000.00
        assertEquals(expectedFxNet, breakdown.netFxMinor)

        // Net Profit
        val expectedNetProfit = expectedOperatingProfit + expectedFxNet // 2,200,000.00
        assertEquals(expectedNetProfit, breakdown.netProfitMinor)

        // Basis Points Precision (gross margin: 4,300,000 / 6,800,000 = 63.235% -> 6323 bps)
        val expectedGrossBps = ((expectedGrossProfit * 10000L) / 6_800_000_00L).toInt()
        assertEquals(expectedGrossBps, breakdown.grossMarginBasisPoints)

        // Reconciliation
        assertTrue("Calculated profit breakdown must be internally reconciled", breakdown.reconciliation.reconciled)
        assertEquals(3, breakdown.reconciliation.checks.size)
    }
}
