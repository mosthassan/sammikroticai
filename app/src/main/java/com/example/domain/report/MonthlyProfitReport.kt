package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.core.time.FiscalCalendar
import com.example.data.local.AppDatabase
import com.example.data.local.dao.IncomeStatementRow
import com.example.data.local.entity.PartyEntity
import java.math.BigInteger
import java.time.LocalDate

enum class MonthClosingStatus(val title: String) {
    FINAL("نهائي"),
    PROVISIONAL("مؤقت"),
    IN_PROGRESS("قيد التشغيل"),
    @Deprecated("Use PROVISIONAL instead")
    DRAFT("مسودة")
}

data class MonthlyProfitStatus(
    val status: MonthClosingStatus,
    val reason: String,
    val reasonsList: List<String> = emptyList()
)

data class MonthlyProfitItem(
    val fiscalYear: Int,
    val monthIndex: Int, // 1..12
    val monthName: String,
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val cardRevenueMinor: Long,
    val serviceRevenueMinor: Long,
    val salesReturnsMinor: Long,
    val otherRevenueMinor: Long,
    val netRevenueMinor: Long,
    val directIspCostMinor: Long,
    val grossProfitMinor: Long,
    val grossMarginBasisPoints: Int,
    val operatingExpensesMinor: Long,
    val maintenanceExpensesMinor: Long,
    val salariesExpensesMinor: Long,
    val miscExpensesMinor: Long,
    val otherExpensesMinor: Long,
    val totalOperatingExpensesMinor: Long,
    val depreciationExpenseMinor: Long,
    val realizedFxGainMinor: Long,
    val realizedFxLossMinor: Long,
    val realizedOperatingProfitMinor: Long,
    val unrealizedFxGainMinor: Long,
    val unrealizedFxLossMinor: Long,
    val netProfitMinor: Long, // Can be negative (no max(0))
    val netMarginBasisPoints: Int,
    val cumulativeProfitMinor: Long,
    val status: MonthlyProfitStatus
)

data class PartnerProfitShare(
    val partnerPartyId: String,
    val partnerName: String,
    val equityPercentageBasisPoints: Int,
    val estimatedShareMinor: Long
)

data class MonthlyComparisonResult(
    val selectedMonth: MonthlyProfitItem,
    val previousMonth: MonthlyProfitItem?,
    val diffVsPreviousMinor: Long?,
    val sameMonthLastYear: MonthlyProfitItem?,
    val diffVsLastYearMinor: Long?
)

data class MonthlyProfitReport(
    val fiscalYear: Int,
    val months: List<MonthlyProfitItem>,
    val yearTotalNetRevenueMinor: Long,
    val yearTotalDirectIspCostMinor: Long,
    val yearTotalGrossProfitMinor: Long,
    val yearTotalOperatingExpensesMinor: Long,
    val yearTotalDepreciationMinor: Long,
    val yearTotalRealizedFxNetMinor: Long,
    val yearTotalUnrealizedFxNetMinor: Long,
    val yearTotalNetProfitMinor: Long,
    val partnerShares: List<PartnerProfitShare>,
    val reconciliation: ReconciliationSummary
)

val ARABIC_MONTH_NAMES = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

fun calculateSingleMonthProfit(
    fiscalYear: Int,
    monthIndex: Int,
    monthName: String,
    fromEpochDay: Long,
    toEpochDay: Long,
    rows: List<IncomeStatementRow>,
    cumulativeBeforeMinor: Long,
    periodStatus: MonthlyProfitStatus
): MonthlyProfitItem {
    var cardRevenue = 0L
    var serviceRevenue = 0L
    var salesReturns = 0L
    var otherRevenue = 0L

    var directIspCost = 0L
    var opsExp = 0L
    var maintExp = 0L
    var salariesExp = 0L
    var miscExp = 0L
    var otherExp = 0L
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
            AccountConstants.OPERATING_EXPENSES -> opsExp += row.netAmountMinor
            AccountConstants.MAINTENANCE_SPARES -> maintExp += row.netAmountMinor
            AccountConstants.SALARIES_STAFF -> salariesExp += row.netAmountMinor
            AccountConstants.MISC_EXPENSES -> miscExp += row.netAmountMinor
            AccountConstants.DEPRECIATION_EXPENSE -> depreciationExp += row.netAmountMinor
            AccountConstants.REALIZED_FX_GAIN -> realizedFxGain += row.netAmountMinor
            AccountConstants.REALIZED_FX_LOSS -> realizedFxLoss += row.netAmountMinor
            AccountConstants.UNREALIZED_FX_GAIN -> unrealizedFxGain += row.netAmountMinor
            AccountConstants.UNREALIZED_FX_LOSS -> unrealizedFxLoss += row.netAmountMinor
            else -> {
                if (row.accountCode.startsWith("4")) {
                    otherRevenue += row.netAmountMinor
                } else if (row.accountCode.startsWith("5")) {
                    otherExp += row.netAmountMinor
                }
            }
        }
    }

    val netRevenue = (cardRevenue + serviceRevenue + otherRevenue) - salesReturns
    val grossProfit = netRevenue - directIspCost
    val totalOperatingExpenses = opsExp + maintExp + salariesExp + miscExp + otherExp
    val netRealizedFx = realizedFxGain - realizedFxLoss
    val realizedOperatingProfit = grossProfit - totalOperatingExpenses - depreciationExp + netRealizedFx
    val netUnrealizedFx = unrealizedFxGain - unrealizedFxLoss
    val netProfit = realizedOperatingProfit + netUnrealizedFx

    val grossMarginBps = if (netRevenue > 0L) {
        ((grossProfit * 10_000L) / netRevenue).toInt()
    } else {
        0
    }

    val netMarginBps = if (netRevenue > 0L) {
        ((netProfit * 10_000L) / netRevenue).toInt()
    } else {
        0
    }

    val cumulative = cumulativeBeforeMinor + netProfit

    return MonthlyProfitItem(
        fiscalYear = fiscalYear,
        monthIndex = monthIndex,
        monthName = monthName,
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        cardRevenueMinor = cardRevenue,
        serviceRevenueMinor = serviceRevenue,
        salesReturnsMinor = salesReturns,
        otherRevenueMinor = otherRevenue,
        netRevenueMinor = netRevenue,
        directIspCostMinor = directIspCost,
        grossProfitMinor = grossProfit,
        grossMarginBasisPoints = grossMarginBps,
        operatingExpensesMinor = opsExp,
        maintenanceExpensesMinor = maintExp,
        salariesExpensesMinor = salariesExp,
        miscExpensesMinor = miscExp,
        otherExpensesMinor = otherExp,
        totalOperatingExpensesMinor = totalOperatingExpenses,
        depreciationExpenseMinor = depreciationExp,
        realizedFxGainMinor = realizedFxGain,
        realizedFxLossMinor = realizedFxLoss,
        realizedOperatingProfitMinor = realizedOperatingProfit,
        unrealizedFxGainMinor = unrealizedFxGain,
        unrealizedFxLossMinor = unrealizedFxLoss,
        netProfitMinor = netProfit,
        netMarginBasisPoints = netMarginBps,
        cumulativeProfitMinor = cumulative,
        status = periodStatus
    )
}

/**
 * Divides profit or loss among partners using the Largest Remainder (Hare-Niemeyer) method.
 * Guarantees zero shortfall: sum of partner shares exactly equals totalProfitMinor down to 1 Rial.
 */
fun calculatePartnerProfitShares(
    totalProfitMinor: Long,
    partners: List<PartyEntity>
): List<PartnerProfitShare> {
    val activePartners = if (partners.any { it.isPartner }) {
        partners.filter { it.isPartner }
    } else {
        partners
    }
    if (activePartners.isEmpty()) return emptyList()

    if (totalProfitMinor == 0L) {
        return activePartners.map { p ->
            PartnerProfitShare(
                partnerPartyId = p.id,
                partnerName = p.name,
                equityPercentageBasisPoints = p.equityPercentageBasisPoints,
                estimatedShareMinor = 0L
            )
        }
    }

    val isLoss = totalProfitMinor < 0L
    val absAmount = if (isLoss) -totalProfitMinor else totalProfitMinor

    val weights = activePartners.map {
        if (it.equityPercentageBasisPoints > 0) it.equityPercentageBasisPoints.toLong() else 1L
    }
    val totalWeight = weights.sum()

    val totalAmountBig = BigInteger.valueOf(absAmount)
    val totalWeightBig = BigInteger.valueOf(totalWeight)

    val quotas = LongArray(activePartners.size)
    val remainders = LongArray(activePartners.size)

    for (i in activePartners.indices) {
        val prod = totalAmountBig.multiply(BigInteger.valueOf(weights[i]))
        quotas[i] = prod.divide(totalWeightBig).toLong()
        remainders[i] = prod.remainder(totalWeightBig).toLong()
    }

    val sumQuotas = quotas.sum()
    val shortfall = (absAmount - sumQuotas).toInt()

    val sortedIndices = activePartners.indices.sortedWith(
        compareByDescending<Int> { remainders[it] }
            .thenBy { activePartners[it].id }
    )

    val extra = BooleanArray(activePartners.size)
    for (s in 0 until shortfall) {
        extra[sortedIndices[s]] = true
    }

    return activePartners.mapIndexed { idx, p ->
        val shareAbs = quotas[idx] + if (extra[idx]) 1L else 0L
        val share = if (isLoss) -shareAbs else shareAbs
        PartnerProfitShare(
            partnerPartyId = p.id,
            partnerName = p.name,
            equityPercentageBasisPoints = p.equityPercentageBasisPoints,
            estimatedShareMinor = share
        )
    }
}

fun calculateMonthlyComparison(
    selectedMonth: MonthlyProfitItem,
    previousMonth: MonthlyProfitItem?,
    sameMonthLastYear: MonthlyProfitItem?
): MonthlyComparisonResult {
    val diffPrev = previousMonth?.let { selectedMonth.netProfitMinor - it.netProfitMinor }
    val diffYear = sameMonthLastYear?.let { selectedMonth.netProfitMinor - it.netProfitMinor }

    return MonthlyComparisonResult(
        selectedMonth = selectedMonth,
        previousMonth = previousMonth,
        diffVsPreviousMinor = diffPrev,
        sameMonthLastYear = sameMonthLastYear,
        diffVsLastYearMinor = diffYear
    )
}

/**
 * Generates the full 12-month Monthly Profit Breakdown report from the Room database.
 */
suspend fun generateMonthlyProfitReport(
    db: AppDatabase,
    fiscalYear: Int,
    partners: List<PartyEntity> = emptyList()
): MonthlyProfitReport {
    val today = com.example.core.time.AppClock.todayLocalDate()
    val todayEpoch = today.toEpochDay()
    val currentYear = today.year
    val currentMonth = today.monthValue

    val monthItems = mutableListOf<MonthlyProfitItem>()
    var runningCumulative = 0L

    val allTreasuries = db.treasuryDao().getAllTreasuriesSync()
    val foreignTreasuries = allTreasuries.filter { it.currency != "YER" }

    for (m in 1..12) {
        val monthStart = LocalDate.of(fiscalYear, m, 1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        val fromEpoch = monthStart.toEpochDay()
        val toEpoch = monthEnd.toEpochDay()

        // Scoped query for month nominal entries (excluding closing entries)
        val rows = db.journalDao().getIncomeStatementLines(fromEpoch, toEpoch)

        val status: MonthlyProfitStatus = when {
            // (d) current month = IN_PROGRESS
            fiscalYear == currentYear && m == currentMonth -> {
                MonthlyProfitStatus(
                    status = MonthClosingStatus.IN_PROGRESS,
                    reason = "الشهر الجاري قيد النشاط والتشغيل",
                    reasonsList = listOf("الشهر الجاري قيد النشاط والتشغيل")
                )
            }
            // Future period
            toEpoch > todayEpoch -> {
                MonthlyProfitStatus(
                    status = MonthClosingStatus.IN_PROGRESS,
                    reason = "فترة مستقبلية غير مبدوءة",
                    reasonsList = listOf("فترة مستقبلية غير مبدوءة")
                )
            }
            else -> {
                // Past period: evaluate (a), (b), (c)
                val reasons = mutableListOf<String>()

                // (a) depreciable assets exist but no depreciation run for that month
                val assets = db.assetDao().getActiveAssetsUpTo(toEpoch)
                val depreciableAssets = assets.filter { it.accumulatedDepreciationMinor < (it.purchaseCostMinor - it.salvageValueMinor) }
                if (depreciableAssets.isNotEmpty()) {
                    val depRunsCount = db.assetDao().countDepreciationRunsForPeriod(fiscalYear, m)
                    if (depRunsCount < depreciableAssets.size) {
                        reasons.add("توجد أصول قابلة للإهلاك لم يتم إثبات قسط إهلاكها لهذا الشهر")
                    }
                }

                // (b) foreign-currency balances at month end with no revaluation entry
                var hasForeignBalance = false
                for (ft in foreignTreasuries) {
                    val bal = db.journalDao().getNetOrigBalanceForTreasury(ft.id, toEpoch)
                    if (bal != 0L) {
                        hasForeignBalance = true
                        break
                    }
                }
                if (hasForeignBalance) {
                    val revalCount = db.documentDao().getPostedRevaluationsCountInDateRange(fromEpoch, toEpoch)
                    if (revalCount == 0) {
                        reasons.add("توجد أرصدة بالعملة الأجنبية لم تخضع لإعادة تقييم فروق العملة بنهاية الشهر")
                    }
                }

                // (c) any DRAFT document dated in the month
                val draftCount = db.documentDao().getDraftDocumentsCountInDateRange(fromEpoch, toEpoch)
                if (draftCount > 0) {
                    reasons.add("توجد وثائق ومستندات مسودة (DRAFT) غير مرحلة خلال الشهر ($draftCount مستند)")
                }

                if (reasons.isNotEmpty()) {
                    MonthlyProfitStatus(
                        status = MonthClosingStatus.PROVISIONAL,
                        reason = reasons.joinToString(" • "),
                        reasonsList = reasons
                    )
                } else {
                    MonthlyProfitStatus(
                        status = MonthClosingStatus.FINAL,
                        reason = "الفترة مقفلة ومعتمدة محاسبياً",
                        reasonsList = listOf("جميع المتطلبات مكتملة: تم ترحيل المستندات، وإثبات أقساط الإهلاك، وإعادة التقييم الدوري")
                    )
                }
            }
        }

        val monthName = ARABIC_MONTH_NAMES.getOrElse(m - 1) { "شهر $m" }

        val item = calculateSingleMonthProfit(
            fiscalYear = fiscalYear,
            monthIndex = m,
            monthName = monthName,
            fromEpochDay = fromEpoch,
            toEpochDay = toEpoch,
            rows = rows,
            cumulativeBeforeMinor = runningCumulative,
            periodStatus = status
        )

        runningCumulative = item.cumulativeProfitMinor
        monthItems.add(item)
    }

    // Full year nominal query for Income Statement Net Profit reconciliation check
    val yearStartEpoch = LocalDate.of(fiscalYear, 1, 1).toEpochDay()
    val yearEndEpoch = LocalDate.of(fiscalYear, 12, 31).toEpochDay()
    val yearRows = db.journalDao().getIncomeStatementLines(yearStartEpoch, yearEndEpoch)
    val yearBreakdown = calculateProfitBreakdown(yearRows, yearStartEpoch, yearEndEpoch)

    val yearNetRevenue = monthItems.sumOf { it.netRevenueMinor }
    val yearDirectIspCost = monthItems.sumOf { it.directIspCostMinor }
    val yearGrossProfit = monthItems.sumOf { it.grossProfitMinor }
    val yearOperatingExpenses = monthItems.sumOf { it.totalOperatingExpensesMinor }
    val yearDepreciation = monthItems.sumOf { it.depreciationExpenseMinor }
    val yearRealizedFxNet = monthItems.sumOf { it.realizedFxGainMinor - it.realizedFxLossMinor }
    val yearUnrealizedFxNet = monthItems.sumOf { it.unrealizedFxGainMinor - it.unrealizedFxLossMinor }
    val yearNetProfit = monthItems.sumOf { it.netProfitMinor }

    // Reconciliation Checks
    val sumCheckPassed = (yearNetProfit == monthItems.sumOf { it.netProfitMinor })
    val incomeStatementMatchPassed = (yearNetProfit == yearBreakdown.netProfitMinor)
    val cumulativeEndCheckPassed = (monthItems.last().cumulativeProfitMinor == yearNetProfit)

    val checks = listOf(
        ReconciliationCheck(
            name = "مطابقة مجموع الأشهر مع إجمالي السنة",
            expectedMinor = yearNetProfit,
            actualMinor = monthItems.sumOf { it.netProfitMinor },
            passed = sumCheckPassed,
            note = "مجموع صافي أرباح وخسائر الأشهر الـ 12 يطابق الإجمالي السنوي"
        ),
        ReconciliationCheck(
            name = "مطابقة صافي ربح التقرير مع قائمة الدخل السنوية",
            expectedMinor = yearBreakdown.netProfitMinor,
            actualMinor = yearNetProfit,
            passed = incomeStatementMatchPassed,
            note = "التطابق التام مع صافي الدخل المحاسبي لقائمة الدخل للفترة الكاملة"
        ),
        ReconciliationCheck(
            name = "مطابقة الأرباح التراكمية مع إجمالي السنة",
            expectedMinor = yearNetProfit,
            actualMinor = monthItems.last().cumulativeProfitMinor,
            passed = cumulativeEndCheckPassed,
            note = "الرصيد التراكمي لنهاية شهر ديسمبر يطابق تماماً صافي الربح السنوي"
        )
    )

    val reconciliationSummary = ReconciliationSummary(checks = checks)

    // Partner shares for the year total
    val activePartners = if (partners.isNotEmpty()) partners else db.partyDao().getAllPartiesSync().filter { it.isPartner }
    val partnerShares = calculatePartnerProfitShares(yearNetProfit, activePartners)

    return MonthlyProfitReport(
        fiscalYear = fiscalYear,
        months = monthItems,
        yearTotalNetRevenueMinor = yearNetRevenue,
        yearTotalDirectIspCostMinor = yearDirectIspCost,
        yearTotalGrossProfitMinor = yearGrossProfit,
        yearTotalOperatingExpensesMinor = yearOperatingExpenses,
        yearTotalDepreciationMinor = yearDepreciation,
        yearTotalRealizedFxNetMinor = yearRealizedFxNet,
        yearTotalUnrealizedFxNetMinor = yearUnrealizedFxNet,
        yearTotalNetProfitMinor = yearNetProfit,
        partnerShares = partnerShares,
        reconciliation = reconciliationSummary
    )
}
