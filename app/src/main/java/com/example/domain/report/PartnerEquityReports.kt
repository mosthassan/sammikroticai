package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.data.local.AppDatabase
import com.example.data.local.dao.EquityPeriodLineRow
import com.example.data.local.dao.PartnerMovementRow
import com.example.data.local.entity.PartyEntity
import java.math.BigInteger

/**
 * Represents the nature of movement on partner capital or current accounts.
 */
enum class PartnerMotionType(val titleAr: String) {
    CASH_CONTRIBUTION("مساهمة نقدية"),
    IN_KIND_CONTRIBUTION("مساهمة عينية"),
    WITHDRAWAL("مسحوبات الشريك"),
    PAID_ON_BEHALF("سداد بالنيابة عن المنشأة"),
    SETTLEMENT("تسوية وتوزيع أرباح"),
    OTHER("حركة أخرى")
}

/**
 * Line item in Partner Statement of Account.
 */
data class PartnerStatementLine(
    val entryId: String,
    val docId: String,
    val dateEpochDay: Long,
    val motionType: PartnerMotionType,
    val motionNameAr: String,
    val docType: String,
    val docNumber: Long,
    val description: String,
    val currency: String,
    val amountOrigMinor: Long,
    val exchangeRateMicros: Long,
    val accountCode: String,
    val debitMinor: Long,
    val creditMinor: Long,
    val capitalRunningBalanceMinor: Long,
    val currentRunningBalanceMinor: Long
)

/**
 * Complete Partner Statement of Account Report.
 */
data class PartnerStatementReport(
    val partnerPartyId: String,
    val partnerName: String,
    val equityPercentageBasisPoints: Int,
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val capitalOpeningBalanceMinor: Long,
    val capitalClosingBalanceMinor: Long,
    val currentOpeningBalanceMinor: Long,
    val currentClosingBalanceMinor: Long,
    val totalCapitalDebitMinor: Long,
    val totalCapitalCreditMinor: Long,
    val totalCurrentDebitMinor: Long,
    val totalCurrentCreditMinor: Long,
    val lines: List<PartnerStatementLine>,
    val reconciliation: ReconciliationSummary
) {
    val totalOpeningEquityMinor: Long
        get() = capitalOpeningBalanceMinor + currentOpeningBalanceMinor

    val totalClosingEquityMinor: Long
        get() = capitalClosingBalanceMinor + currentClosingBalanceMinor

    val netChangeEquityMinor: Long
        get() = totalClosingEquityMinor - totalOpeningEquityMinor
}

/**
 * Row type in the Statement of Changes in Equity.
 */
enum class EquityChangeRowType(val titleAr: String) {
    OPENING_BALANCE("رصيد أول الفترة"),
    CASH_CONTRIBUTIONS("مساهمات نقدية في رأس المال / الجاري"),
    IN_KIND_CONTRIBUTIONS("مساهمات عينية بأصول ومعدات"),
    PERIOD_PROFIT("صافي ربح / (خسارة) الفترة"),
    PROFIT_DISTRIBUTIONS("توزيعات الأرباح على الشركاء"),
    PARTNER_WITHDRAWALS("مسحوبات الشركاء النقدية"),
    PAYMENTS_ON_BEHALF("مدفوعات ومصاريف سددها الشركاء بالنيابة"),
    OTHER_MOVEMENTS("تسويات وقيود أخرى"),
    CLOSING_BALANCE("رصيد نهاية الفترة")
}

/**
 * Row in Statement of Changes in Equity matrix.
 */
data class EquityChangeRow(
    val rowType: EquityChangeRowType,
    val label: String,
    val capitalMinor: Long,
    val currentMinor: Long,
    val retainedEarningsMinor: Long,
    val periodProfitMinor: Long
) {
    val totalMinor: Long
        get() = capitalMinor + currentMinor + retainedEarningsMinor + periodProfitMinor
}

/**
 * Partner equity summary row within Statement of Changes in Equity.
 */
data class PartnerEquitySummaryRow(
    val partnerPartyId: String,
    val partnerName: String,
    val equityPercentageBasisPoints: Int,
    val capitalOpeningMinor: Long,
    val currentOpeningMinor: Long,
    val cashContributionsMinor: Long,
    val inKindContributionsMinor: Long,
    val paidOnBehalfMinor: Long,
    val withdrawalsMinor: Long,
    val dividendsReceivedMinor: Long,
    val estimatedPeriodProfitMinor: Long,
    val capitalClosingMinor: Long,
    val currentClosingMinor: Long
) {
    val totalPartnerEquityMinor: Long
        get() = capitalClosingMinor + currentClosingMinor + estimatedPeriodProfitMinor
}

/**
 * Complete Statement of Changes in Equity Report.
 */
data class StatementOfChangesInEquityReport(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val rows: List<EquityChangeRow>,
    val partnerSummaries: List<PartnerEquitySummaryRow>,
    val reconciliation: ReconciliationSummary
) {
    val closingRow: EquityChangeRow
        get() = rows.firstOrNull { it.rowType == EquityChangeRowType.CLOSING_BALANCE }
            ?: EquityChangeRow(EquityChangeRowType.CLOSING_BALANCE, "رصيد نهاية الفترة", 0L, 0L, 0L, 0L)

    val totalClosingEquityMinor: Long
        get() = closingRow.totalMinor
}

/**
 * Generates a Partner Statement Report with separate capital (3101) and current (3201) running balances.
 */
suspend fun generatePartnerStatementReport(
    db: AppDatabase,
    partnerPartyId: String,
    fromEpochDay: Long,
    toEpochDay: Long
): PartnerStatementReport {
    val partner = db.partyDao().getPartyById(partnerPartyId)
        ?: error("Partner with ID $partnerPartyId not found")

    // Opening balances prior to start of date range
    val priorDay = fromEpochDay - 1
    val capitalOpening = db.journalDao().getPartnerAccountCreditBalanceAsOf(
        partyId = partnerPartyId,
        accountCode = AccountConstants.CAPITAL,
        asOfDateEpochDay = priorDay
    )
    val currentOpening = db.journalDao().getPartnerAccountCreditBalanceAsOf(
        partyId = partnerPartyId,
        accountCode = AccountConstants.PARTNER_CURRENT,
        asOfDateEpochDay = priorDay
    )

    // Load movement rows in date range
    val rawLines = db.journalDao().getPartnerMovementLines(
        partyId = partnerPartyId,
        startDateEpochDay = fromEpochDay,
        endDateEpochDay = toEpochDay
    )

    var runningCapital = capitalOpening
    var runningCurrent = currentOpening
    var totalCapDebit = 0L
    var totalCapCredit = 0L
    var totalCurDebit = 0L
    var totalCurCredit = 0L

    val statementLines = ArrayList<PartnerStatementLine>(rawLines.size)

    for (raw in rawLines) {
        // Query linked assets if in-kind
        val assets = db.assetDao().getAssetsForDoc(raw.docId)
        val assetNames = assets.joinToString("، ") { it.name }

        // Classify motion type
        val motionType = classifyPartnerMotion(raw, assets.isNotEmpty(), assetNames)

        // Clean description including asset name if applicable
        val fullDescription = buildStatementDescription(raw, motionType, assetNames)

        if (raw.accountCode == AccountConstants.CAPITAL) {
            totalCapDebit += raw.baseDebitMinor
            totalCapCredit += raw.baseCreditMinor
            runningCapital += (raw.baseCreditMinor - raw.baseDebitMinor)
        } else if (raw.accountCode == AccountConstants.PARTNER_CURRENT) {
            totalCurDebit += raw.baseDebitMinor
            totalCurCredit += raw.baseCreditMinor
            runningCurrent += (raw.baseCreditMinor - raw.baseDebitMinor)
        }

        statementLines.add(
            PartnerStatementLine(
                entryId = raw.entryId,
                docId = raw.docId,
                dateEpochDay = raw.entryDateEpochDay,
                motionType = motionType,
                motionNameAr = motionType.titleAr,
                docType = raw.docType,
                docNumber = raw.docNumber,
                description = fullDescription,
                currency = raw.currency,
                amountOrigMinor = raw.origMinor,
                exchangeRateMicros = raw.exchangeRateMicros,
                accountCode = raw.accountCode,
                debitMinor = raw.baseDebitMinor,
                creditMinor = raw.baseCreditMinor,
                capitalRunningBalanceMinor = runningCapital,
                currentRunningBalanceMinor = runningCurrent
            )
        )
    }

    // Verify closing balances against ledger
    val ledgerCapClosing = db.journalDao().getPartnerAccountCreditBalanceAsOf(
        partyId = partnerPartyId,
        accountCode = AccountConstants.CAPITAL,
        asOfDateEpochDay = toEpochDay
    )
    val ledgerCurClosing = db.journalDao().getPartnerAccountCreditBalanceAsOf(
        partyId = partnerPartyId,
        accountCode = AccountConstants.PARTNER_CURRENT,
        asOfDateEpochDay = toEpochDay
    )

    val checks = listOf(
        ReconciliationCheck(
            name = "مطابقة رصيد رأس المال النهائي (3101) للشريك",
            expectedMinor = ledgerCapClosing,
            actualMinor = runningCapital,
            note = "مطابقة رصيد رأس المال التراكمي مع دفتر الأستاذ"
        ),
        ReconciliationCheck(
            name = "مطابقة رصيد الحساب الجاري النهائي (3201) للشريك",
            expectedMinor = ledgerCurClosing,
            actualMinor = runningCurrent,
            note = "مطابقة رصيد جاري الشريك التراكمي مع دفتر الأستاذ"
        )
    )

    return PartnerStatementReport(
        partnerPartyId = partnerPartyId,
        partnerName = partner.name,
        equityPercentageBasisPoints = partner.equityPercentageBasisPoints,
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        capitalOpeningBalanceMinor = capitalOpening,
        capitalClosingBalanceMinor = runningCapital,
        currentOpeningBalanceMinor = currentOpening,
        currentClosingBalanceMinor = runningCurrent,
        totalCapitalDebitMinor = totalCapDebit,
        totalCapitalCreditMinor = totalCapCredit,
        totalCurrentDebitMinor = totalCurDebit,
        totalCurrentCreditMinor = totalCurCredit,
        lines = statementLines,
        reconciliation = ReconciliationSummary(checks)
    )
}

/**
 * Generates the Statement of Changes in Equity Report.
 */
suspend fun generateStatementOfChangesInEquityReport(
    db: AppDatabase,
    fromEpochDay: Long,
    toEpochDay: Long
): StatementOfChangesInEquityReport {
    val priorDay = fromEpochDay - 1

    // 1. Opening Balances (prior to fromEpochDay)
    val capOpening = db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.CAPITAL, priorDay)
    val curOpening = db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.PARTNER_CURRENT, priorDay)
    val retOpening = db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.RETAINED_EARNINGS, priorDay)
    val profitOpening = db.journalDao().getUnclosedNominalBalance(priorDay)

    // 2. Movements in Period
    val lines = db.journalDao().getEquityPeriodLines(fromEpochDay, toEpochDay)

    var cashCapMinor = 0L
    var cashCurMinor = 0L
    var inKindCapMinor = 0L
    var inKindCurMinor = 0L
    var distRetMinor = 0L
    var distCurMinor = 0L
    var withCapMinor = 0L
    var withCurMinor = 0L
    var paidCurMinor = 0L
    var otherCapMinor = 0L
    var otherCurMinor = 0L
    var otherRetMinor = 0L

    for (line in lines) {
        val isCredit = line.baseCreditMinor > 0L
        val amount = if (isCredit) line.baseCreditMinor else line.baseDebitMinor
        val assets = db.assetDao().getAssetsForDoc(line.docId)
        val hasAssets = assets.isNotEmpty() || line.memo.contains("عيني") || line.docNotes.contains("عيني")

        when (line.accountCode) {
            AccountConstants.CAPITAL -> {
                if (isCredit) {
                    if (hasAssets) inKindCapMinor += amount else cashCapMinor += amount
                } else {
                    withCapMinor += amount
                }
            }
            AccountConstants.PARTNER_CURRENT -> {
                if (isCredit) {
                    if (line.docType == DocumentType.PAYMENT_VOUCHER.name) {
                        paidCurMinor += amount
                    } else if (line.docType == DocumentType.DIVIDEND_DISTRIBUTION.name || line.memo.contains("توزيع أرباح")) {
                        distCurMinor += amount
                    } else if (hasAssets) {
                        inKindCurMinor += amount
                    } else {
                        cashCurMinor += amount
                    }
                } else {
                    if (line.docType == DocumentType.PAYMENT_VOUCHER.name || line.memo.contains("مسحوبات")) {
                        withCurMinor += amount
                    } else {
                        otherCurMinor -= amount
                    }
                }
            }
            AccountConstants.RETAINED_EARNINGS -> {
                if (!isCredit) {
                    if (line.docType == DocumentType.DIVIDEND_DISTRIBUTION.name || line.memo.contains("توزيع أرباح")) {
                        distRetMinor += amount
                    } else {
                        otherRetMinor -= amount
                    }
                } else {
                    otherRetMinor += amount
                }
            }
        }
    }

    // 3. Period Net Profit from 4xxx & 5xxx accounts
    val nominalAccounts = db.journalDao().getNominalAccountBalancesForPeriod(fromEpochDay, toEpochDay)
    val totalRevenue = nominalAccounts.filter { it.accountCode.startsWith("4") }
        .sumOf { it.totalCreditMinor - it.totalDebitMinor }
    val totalExpense = nominalAccounts.filter { it.accountCode.startsWith("5") }
        .sumOf { it.totalDebitMinor - it.totalCreditMinor }
    val periodNetProfitMinor = totalRevenue - totalExpense

    // Build the 9 Matrix Rows
    val rowOpening = EquityChangeRow(
        rowType = EquityChangeRowType.OPENING_BALANCE,
        label = EquityChangeRowType.OPENING_BALANCE.titleAr,
        capitalMinor = capOpening,
        currentMinor = curOpening,
        retainedEarningsMinor = retOpening,
        periodProfitMinor = profitOpening
    )

    val rowCash = EquityChangeRow(
        rowType = EquityChangeRowType.CASH_CONTRIBUTIONS,
        label = EquityChangeRowType.CASH_CONTRIBUTIONS.titleAr,
        capitalMinor = cashCapMinor,
        currentMinor = cashCurMinor,
        retainedEarningsMinor = 0L,
        periodProfitMinor = 0L
    )

    val rowInKind = EquityChangeRow(
        rowType = EquityChangeRowType.IN_KIND_CONTRIBUTIONS,
        label = EquityChangeRowType.IN_KIND_CONTRIBUTIONS.titleAr,
        capitalMinor = inKindCapMinor,
        currentMinor = inKindCurMinor,
        retainedEarningsMinor = 0L,
        periodProfitMinor = 0L
    )

    val rowProfit = EquityChangeRow(
        rowType = EquityChangeRowType.PERIOD_PROFIT,
        label = EquityChangeRowType.PERIOD_PROFIT.titleAr,
        capitalMinor = 0L,
        currentMinor = 0L,
        retainedEarningsMinor = 0L,
        periodProfitMinor = periodNetProfitMinor
    )

    val rowDist = EquityChangeRow(
        rowType = EquityChangeRowType.PROFIT_DISTRIBUTIONS,
        label = EquityChangeRowType.PROFIT_DISTRIBUTIONS.titleAr,
        capitalMinor = 0L,
        currentMinor = distCurMinor,
        retainedEarningsMinor = -distRetMinor,
        periodProfitMinor = 0L
    )

    val rowWithdrawals = EquityChangeRow(
        rowType = EquityChangeRowType.PARTNER_WITHDRAWALS,
        label = EquityChangeRowType.PARTNER_WITHDRAWALS.titleAr,
        capitalMinor = -withCapMinor,
        currentMinor = -withCurMinor,
        retainedEarningsMinor = 0L,
        periodProfitMinor = 0L
    )

    val rowPaid = EquityChangeRow(
        rowType = EquityChangeRowType.PAYMENTS_ON_BEHALF,
        label = EquityChangeRowType.PAYMENTS_ON_BEHALF.titleAr,
        capitalMinor = 0L,
        currentMinor = paidCurMinor,
        retainedEarningsMinor = 0L,
        periodProfitMinor = 0L
    )

    val rowOther = EquityChangeRow(
        rowType = EquityChangeRowType.OTHER_MOVEMENTS,
        label = EquityChangeRowType.OTHER_MOVEMENTS.titleAr,
        capitalMinor = otherCapMinor,
        currentMinor = otherCurMinor,
        retainedEarningsMinor = otherRetMinor,
        periodProfitMinor = 0L
    )

    val closingCap = capOpening + cashCapMinor + inKindCapMinor - withCapMinor + otherCapMinor
    val closingCur = curOpening + cashCurMinor + inKindCurMinor + distCurMinor - withCurMinor + paidCurMinor + otherCurMinor
    val closingRet = retOpening - distRetMinor + otherRetMinor
    val closingProfit = profitOpening + periodNetProfitMinor

    val rowClosing = EquityChangeRow(
        rowType = EquityChangeRowType.CLOSING_BALANCE,
        label = EquityChangeRowType.CLOSING_BALANCE.titleAr,
        capitalMinor = closingCap,
        currentMinor = closingCur,
        retainedEarningsMinor = closingRet,
        periodProfitMinor = closingProfit
    )

    val matrixRows = listOf(
        rowOpening,
        rowCash,
        rowInKind,
        rowProfit,
        rowDist,
        rowWithdrawals,
        rowPaid,
        rowOther,
        rowClosing
    )

    // 4. Partner Summaries Breakdown
    val allPartners = db.partyDao().getPartnersSync()
    val partnerShares = calculatePartnerProfitShares(periodNetProfitMinor, allPartners)

    val partnerSummaries = mutableListOf<PartnerEquitySummaryRow>()
    for (partner in allPartners) {
        val pCapOpen = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.id, AccountConstants.CAPITAL, priorDay)
        val pCurOpen = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.id, AccountConstants.PARTNER_CURRENT, priorDay)

        val pLines = lines.filter { it.partyId == partner.id }
        var pCash = 0L
        var pInKind = 0L
        var pPaid = 0L
        var pWith = 0L
        var pDiv = 0L

        for (pLine in pLines) {
            val isCr = pLine.baseCreditMinor > 0L
            val amt = if (isCr) pLine.baseCreditMinor else pLine.baseDebitMinor
            val assets = db.assetDao().getAssetsForDoc(pLine.docId)
            val hasAst = assets.isNotEmpty() || pLine.memo.contains("عيني") || pLine.docNotes.contains("عيني")

            if (pLine.accountCode == AccountConstants.CAPITAL) {
                if (isCr) {
                    if (hasAst) pInKind += amt else pCash += amt
                } else {
                    pWith += amt
                }
            } else if (pLine.accountCode == AccountConstants.PARTNER_CURRENT) {
                if (isCr) {
                    if (pLine.docType == DocumentType.PAYMENT_VOUCHER.name) pPaid += amt
                    else if (pLine.docType == DocumentType.DIVIDEND_DISTRIBUTION.name || pLine.memo.contains("توزيع أرباح")) pDiv += amt
                    else if (hasAst) pInKind += amt
                    else pCash += amt
                } else {
                    pWith += amt
                }
            }
        }

        val pCapClose = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.id, AccountConstants.CAPITAL, toEpochDay)
        val pCurClose = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.id, AccountConstants.PARTNER_CURRENT, toEpochDay)
        val pEstimatedProfit = partnerShares.firstOrNull { it.partnerPartyId == partner.id }?.estimatedShareMinor ?: 0L

        partnerSummaries.add(
            PartnerEquitySummaryRow(
                partnerPartyId = partner.id,
                partnerName = partner.name,
                equityPercentageBasisPoints = partner.equityPercentageBasisPoints,
                capitalOpeningMinor = pCapOpen,
                currentOpeningMinor = pCurOpen,
                cashContributionsMinor = pCash,
                inKindContributionsMinor = pInKind,
                paidOnBehalfMinor = pPaid,
                withdrawalsMinor = pWith,
                dividendsReceivedMinor = pDiv,
                estimatedPeriodProfitMinor = pEstimatedProfit,
                capitalClosingMinor = pCapClose,
                currentClosingMinor = pCurClose
            )
        )
    }

    // 5. Dynamic Reconciliation Checks
    val ledgerTotalCap = db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.CAPITAL, toEpochDay)
    val ledgerTotalCur = db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.PARTNER_CURRENT, toEpochDay)
    val sumOfPartnerCap = partnerSummaries.sumOf { it.capitalClosingMinor }
    val sumOfPartnerCur = partnerSummaries.sumOf { it.currentClosingMinor }

    // Check Orphan Lines in 3101 & 3201
    val orphanBalance = db.journalDao().getOrphanPartnerLinesBalance(toEpochDay)

    // Balance Sheet Equity = Assets - Liabilities = (3101 + 3201 + 3301 + Unclosed Nominal)
    val balanceSheetEquity = ledgerTotalCap + ledgerTotalCur +
            db.journalDao().getAccountNetCreditBaseBalanceAsOf(AccountConstants.RETAINED_EARNINGS, toEpochDay) +
            db.journalDao().getUnclosedNominalBalance(toEpochDay)

    val checks = mutableListOf<ReconciliationCheck>()

    // Global Check 1: Sum of partner capitals = 3101 balance
    checks.add(
        ReconciliationCheck(
            name = "إجمالي رأس مال الشركاء = رصيد الحساب العام (3101)",
            expectedMinor = ledgerTotalCap,
            actualMinor = sumOfPartnerCap,
            note = "التحقق من تطابق مجموع رؤوس أموال الشركاء مع الأستاذ العام"
        )
    )

    // Global Check 2: Sum of partner currents = 3201 balance
    checks.add(
        ReconciliationCheck(
            name = "إجمالي الحسابات الجارية للشركاء = رصيد الحساب العام (3201)",
            expectedMinor = ledgerTotalCur,
            actualMinor = sumOfPartnerCur,
            note = "التحقق من تطابق مجموع جاري الشركاء مع الأستاذ العام"
        )
    )

    // Global Check 3: Total Closing Equity = Balance Sheet Equity at date
    checks.add(
        ReconciliationCheck(
            name = "إجمالي حقوق الملكية الختامية = حقوق الملكية بالميزانية العمومية",
            expectedMinor = balanceSheetEquity,
            actualMinor = rowClosing.totalMinor,
            note = "مطابقة رصيد حقوق الملكية الإجمالي مع مركز الميزانية العمومية"
        )
    )

    // Global Check 4: Orphan lines handling
    checks.add(
        ReconciliationCheck(
            name = "قيود حقوق ملكية معلقة (أيتام بدون شريك / Orphan Lines)",
            expectedMinor = 0L,
            actualMinor = orphanBalance,
            note = if (orphanBalance == 0L) "لا توجد قيود معلقة بدون شريك في حسابات الشركاء"
            else "تنبيه: توجد قيود معلقة في حسابات الشركاء بدون ربط بشريك معتمد بمبلغ ${Money(orphanBalance, CurrencyCode.FUNCTIONAL).format()}"
        )
    )

    // Per-Partner Balances Checks
    for (partner in partnerSummaries) {
        val ledgerCap = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.partnerPartyId, AccountConstants.CAPITAL, toEpochDay)
        val ledgerCur = db.journalDao().getPartnerAccountCreditBalanceAsOf(partner.partnerPartyId, AccountConstants.PARTNER_CURRENT, toEpochDay)

        checks.add(
            ReconciliationCheck(
                name = "مطابقة رصيد الشريك (${partner.partnerName}) في رأس المال والجاري",
                expectedMinor = ledgerCap + ledgerCur,
                actualMinor = partner.capitalClosingMinor + partner.currentClosingMinor,
                note = "مطابقة حركة الشريك المباشرة مع سجلات الأستاذ المالي"
            )
        )
    }

    return StatementOfChangesInEquityReport(
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        rows = matrixRows,
        partnerSummaries = partnerSummaries,
        reconciliation = ReconciliationSummary(checks)
    )
}

private fun classifyPartnerMotion(
    row: PartnerMovementRow,
    hasAssets: Boolean,
    assetNames: String
): PartnerMotionType {
    val isCredit = row.baseCreditMinor > 0L

    return if (isCredit) {
        if (hasAssets || row.memo.contains("عيني") || row.docNotes.contains("عيني")) {
            PartnerMotionType.IN_KIND_CONTRIBUTION
        } else if (row.docType == DocumentType.PAYMENT_VOUCHER.name) {
            PartnerMotionType.PAID_ON_BEHALF
        } else if (row.docType == DocumentType.DIVIDEND_DISTRIBUTION.name || row.memo.contains("توزيع أرباح")) {
            PartnerMotionType.SETTLEMENT
        } else {
            PartnerMotionType.CASH_CONTRIBUTION
        }
    } else {
        if (row.docType == DocumentType.PAYMENT_VOUCHER.name || row.memo.contains("مسحوبات")) {
            PartnerMotionType.WITHDRAWAL
        } else {
            PartnerMotionType.SETTLEMENT
        }
    }
}

private fun buildStatementDescription(
    row: PartnerMovementRow,
    motionType: PartnerMotionType,
    assetNames: String
): String {
    return when (motionType) {
        PartnerMotionType.IN_KIND_CONTRIBUTION -> {
            if (assetNames.isNotBlank()) "مساهمة عينية بأصل ثابت: $assetNames"
            else if (row.docNotes.isNotBlank()) row.docNotes
            else row.memo
        }
        PartnerMotionType.PAID_ON_BEHALF -> {
            if (row.docNotes.isNotBlank()) "سداد مصاريف/التزامات بالنيابة: ${row.docNotes}"
            else row.memo
        }
        PartnerMotionType.WITHDRAWAL -> {
            if (row.docNotes.isNotBlank()) "مسحوبات شخصية: ${row.docNotes}"
            else row.memo
        }
        PartnerMotionType.CASH_CONTRIBUTION -> {
            if (row.docNotes.isNotBlank()) row.docNotes else row.memo
        }
        PartnerMotionType.SETTLEMENT -> {
            if (row.docNotes.isNotBlank()) row.docNotes else row.memo
        }
        PartnerMotionType.OTHER -> row.memo
    }
}
