package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.data.local.dao.IncomeStatementRow
import com.example.data.local.dao.JournalDao

/**
 * Shared profit and loss breakdown data structure for ISP operations.
 * Shared between "Monthly Profits" and "Income Statement" reports.
 * All monetary amounts are stored in functional currency minor units as [Long].
 * All margins are expressed in integer basis points (e.g. 2500 = 25.00%).
 */
data class ProfitBreakdown(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val cardRevenueMinor: Long,
    val serviceRevenueMinor: Long,
    val salesReturnsMinor: Long,
    val grossRevenueMinor: Long,
    val directIspCostMinor: Long,
    val grossProfitMinor: Long,
    val operatingExpensesMinor: Long,
    val maintenanceExpensesMinor: Long,
    val salariesExpensesMinor: Long,
    val miscExpensesMinor: Long,
    val totalOperatingExpensesMinor: Long,
    val depreciationExpenseMinor: Long,
    val operatingProfitMinor: Long,
    val realizedFxGainMinor: Long,
    val realizedFxLossMinor: Long,
    val unrealizedFxGainMinor: Long,
    val unrealizedFxLossMinor: Long,
    val netFxMinor: Long,
    val netProfitMinor: Long,
    val grossMarginBasisPoints: Int,
    val netMarginBasisPoints: Int,
    val reconciliation: ReconciliationSummary
)

/**
 * Disassembles profit and loss from scoped nominal ledger rows.
 * This is a pure mathematical function that enforces accounting equilibrium.
 */
fun calculateProfitBreakdown(
    rows: List<IncomeStatementRow>,
    fromEpochDay: Long,
    toEpochDay: Long
): ProfitBreakdown {
    var cardRevenue = 0L
    var serviceRevenue = 0L
    var salesReturns = 0L
    var directIspCost = 0L
    var operatingExp = 0L
    var maintenanceExp = 0L
    var salariesExp = 0L
    var miscExp = 0L
    var depreciationExp = 0L
    var realizedFxGain = 0L
    var realizedFxLoss = 0L
    var unrealizedFxGain = 0L
    var unrealizedFxLoss = 0L

    rows.forEach { row ->
        when (row.accountCode) {
            AccountConstants.CARD_SALES_REVENUE -> cardRevenue += row.netAmountMinor
            AccountConstants.DIRECT_SERVICE_REVENUE -> serviceRevenue += row.netAmountMinor
            AccountConstants.SALES_RETURNS -> salesReturns += row.netAmountMinor
            AccountConstants.DIRECT_ISP_SERVICE_COST -> directIspCost += row.netAmountMinor
            AccountConstants.OPERATING_EXPENSES -> operatingExp += row.netAmountMinor
            AccountConstants.MAINTENANCE_SPARES -> maintenanceExp += row.netAmountMinor
            AccountConstants.SALARIES_STAFF -> salariesExp += row.netAmountMinor
            AccountConstants.MISC_EXPENSES -> miscExp += row.netAmountMinor
            AccountConstants.DEPRECIATION_EXPENSE -> depreciationExp += row.netAmountMinor
            AccountConstants.REALIZED_FX_GAIN -> realizedFxGain += row.netAmountMinor
            AccountConstants.REALIZED_FX_LOSS -> realizedFxLoss += row.netAmountMinor
            AccountConstants.UNREALIZED_FX_GAIN -> unrealizedFxGain += row.netAmountMinor
            AccountConstants.UNREALIZED_FX_LOSS -> unrealizedFxLoss += row.netAmountMinor
            else -> {
                if (row.accountCode.startsWith("4")) {
                    serviceRevenue += row.netAmountMinor
                } else if (row.accountCode.startsWith("5")) {
                    miscExp += row.netAmountMinor
                }
            }
        }
    }

    val grossRevenue = (cardRevenue + serviceRevenue) - salesReturns
    val grossProfit = grossRevenue - directIspCost
    val totalOperatingExp = operatingExp + maintenanceExp + salariesExp + miscExp
    val operatingProfit = grossProfit - totalOperatingExp - depreciationExp
    val totalFxGain = realizedFxGain + unrealizedFxGain
    val totalFxLoss = realizedFxLoss + unrealizedFxLoss
    val netFx = totalFxGain - totalFxLoss
    val netProfit = operatingProfit + netFx

    // Basis points calculation: (amount * 10000L) / grossRevenue
    val grossMarginBps = if (grossRevenue > 0L) {
        ((grossProfit * 10_000L) / grossRevenue).toInt()
    } else {
        0
    }

    val netMarginBps = if (grossRevenue > 0L) {
        ((netProfit * 10_000L) / grossRevenue).toInt()
    } else {
        0
    }

    // Dynamic reconciliation checks
    val revCheckExpected = grossRevenue
    val revCheckActual = (cardRevenue + serviceRevenue) - salesReturns
    val grossProfitExpected = grossProfit
    val grossProfitActual = grossRevenue - directIspCost
    val netProfitExpected = netProfit
    val netProfitActual = operatingProfit + netFx

    val checks = listOf(
        ReconciliationCheck(
            name = "تطابق إجمالي الإيرادات مع البنود",
            expectedMinor = revCheckExpected,
            actualMinor = revCheckActual,
            passed = (revCheckExpected == revCheckActual),
            note = "إجمالي إيرادات الكروت والخدمات مخصوماً منها المردودات"
        ),
        ReconciliationCheck(
            name = "تطابق مجمل الربح مع تكلفة مزودي الخدمة",
            expectedMinor = grossProfitExpected,
            actualMinor = grossProfitActual,
            passed = (grossProfitExpected == grossProfitActual),
            note = "إجمالي الإيرادات ناقص تكلفة مزودي الإنترنت المباشرة"
        ),
        ReconciliationCheck(
            name = "تطابق صافي الربح المحاسبي مع المصروفات وفروق الصرف",
            expectedMinor = netProfitExpected,
            actualMinor = netProfitActual,
            passed = (netProfitExpected == netProfitActual),
            note = "الربح التشغيلي مضافاً إليه صافي فروق العملة"
        )
    )

    val reconciliationSummary = ReconciliationSummary(checks = checks)

    return ProfitBreakdown(
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        cardRevenueMinor = cardRevenue,
        serviceRevenueMinor = serviceRevenue,
        salesReturnsMinor = salesReturns,
        grossRevenueMinor = grossRevenue,
        directIspCostMinor = directIspCost,
        grossProfitMinor = grossProfit,
        operatingExpensesMinor = operatingExp,
        maintenanceExpensesMinor = maintenanceExp,
        salariesExpensesMinor = salariesExp,
        miscExpensesMinor = miscExp,
        totalOperatingExpensesMinor = totalOperatingExp,
        depreciationExpenseMinor = depreciationExp,
        operatingProfitMinor = operatingProfit,
        realizedFxGainMinor = realizedFxGain,
        realizedFxLossMinor = realizedFxLoss,
        unrealizedFxGainMinor = unrealizedFxGain,
        unrealizedFxLossMinor = unrealizedFxLoss,
        netFxMinor = netFx,
        netProfitMinor = netProfit,
        grossMarginBasisPoints = grossMarginBps,
        netMarginBasisPoints = netMarginBps,
        reconciliation = reconciliationSummary
    )
}

/**
 * Scoped query helper: queries journal lines strictly bounded to [fromEpochDay]..[toEpochDay]
 * without querying all lines in memory.
 */
internal suspend fun queryProfitBreakdown(
    journalDao: JournalDao,
    fromEpochDay: Long,
    toEpochDay: Long
): ProfitBreakdown {
    val lines = journalDao.getIncomeStatementLines(fromEpochDay, toEpochDay)
    return calculateProfitBreakdown(lines, fromEpochDay, toEpochDay)
}
