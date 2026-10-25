package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.local.AppDatabase
import java.time.LocalDate

/**
 * Individual chronological transaction line in the Daily Cash Register.
 */
data class DailyCashRegisterLine(
    val lineId: String,
    val entryId: String,
    val docId: String,
    val entryNumber: Long,
    val dateEpochDay: Long,
    val dateFormatted: String,
    val docType: String,
    val docTypeAr: String,
    val docNumber: Long,
    val partyId: String?,
    val partyName: String?,
    val description: String,
    val inflowOrigMinor: Long,
    val outflowOrigMinor: Long,
    val inflowBaseMinor: Long,
    val outflowBaseMinor: Long,
    val exchangeRateMicros: Long,
    val runningBalanceOrigMinor: Long,
    val runningBalanceBaseMinor: Long
)

/**
 * Daily summary grouping when daily totals toggle is activated.
 */
data class DailyGroupSummary(
    val dateEpochDay: Long,
    val dateFormatted: String,
    val lines: List<DailyCashRegisterLine>,
    val dayInflowOrigMinor: Long,
    val dayOutflowOrigMinor: Long,
    val dayNetOrigMinor: Long,
    val dayInflowBaseMinor: Long,
    val dayOutflowBaseMinor: Long,
    val dayNetBaseMinor: Long,
    val dayEndingBalanceOrigMinor: Long,
    val dayEndingBalanceBaseMinor: Long
)

/**
 * Non-ledger information-only note estimating base currency equivalent at current market rate.
 * Strictly includes: "تقدير بسعر اليوم، لا يدخل في أي قيد".
 */
data class ValueEstimationNote(
    val currency: String,
    val isForeignCurrency: Boolean,
    val currentExchangeRateMicros: Long,
    val estimatedCurrentYerMinor: Long,
    val bookValueBaseMinor: Long,
    val unrealizedDeltaYerMinor: Long,
    val noteTextAr: String
)

/**
 * Full reconciliation verification against ledger account balances and physical cash box records.
 */
data class DailyCashRegisterReconciliation(
    val closingBalanceOrigMinor: Long,
    val treasuryCashBalanceOrigMinor: Long,
    val origDifferenceMinor: Long,
    val isOrigReconciled: Boolean,

    val closingBalanceBaseMinor: Long,
    val treasuryBaseBalanceMinor: Long,
    val baseDifferenceMinor: Long,
    val isBaseReconciled: Boolean,

    val glAccountCode: String,
    val glAccountName: String,
    val glAccountBaseBalanceMinor: Long,

    val isFullyReconciled: Boolean,
    val statusMessageAr: String
)

/**
 * Complete Statement for Report 4: Daily Cash Register (سجل الصندوق اليومي).
 */
data class DailyCashRegisterStatement(
    val treasuryId: String,
    val treasuryName: String,
    val currency: String,
    val glAccountCode: String,
    val glAccountName: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val openingBalanceOrigMinor: Long,
    val openingBalanceBaseMinor: Long,
    val lines: List<DailyCashRegisterLine>,
    val dailyGroups: List<DailyGroupSummary>,
    val totalInflowOrigMinor: Long,
    val totalOutflowOrigMinor: Long,
    val netChangeOrigMinor: Long,
    val totalInflowBaseMinor: Long,
    val totalOutflowBaseMinor: Long,
    val netChangeBaseMinor: Long,
    val closingBalanceOrigMinor: Long,
    val closingBalanceBaseMinor: Long,
    val valueEstimation: ValueEstimationNote,
    val reconciliation: DailyCashRegisterReconciliation
)

/**
 * Generates the IFRS-compliant Daily Cash Register for an individual cash box / treasury and currency.
 */
suspend fun generateDailyCashRegister(
    db: AppDatabase,
    treasuryId: String,
    startDateEpochDay: Long,
    endDateEpochDay: Long
): DailyCashRegisterStatement {
    val treasury = db.treasuryDao().getTreasuryById(treasuryId)
        ?: error("Treasury account $treasuryId not found")

    val currency = treasury.currency
    val glCode = treasury.glAccountCode
    val glAccount = db.accountDao().getAccountByCode(glCode)
    val glAccountName = glAccount?.name ?: if (glCode == AccountConstants.CASH_VAULT) "النقدية بالصندوق" else "البنوك والمحافظ الإلكترونية"

    // 1. Opening Balances strictly prior to startDateEpochDay
    val openingBalOrig = db.journalDao().getTreasuryOpeningOrigBalance(treasuryId, currency, startDateEpochDay)
    val openingBalBase = db.journalDao().getTreasuryOpeningBaseBalance(treasuryId, currency, startDateEpochDay)

    // 2. Fetch all journal transaction lines for this treasury and currency in the date range
    val rawRows = db.journalDao().getDailyCashRegisterLines(
        treasuryId = treasuryId,
        currency = currency,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay
    )

    var currentRunningBalOrig = openingBalOrig
    var currentRunningBalBase = openingBalBase
    var totalInflowOrig = 0L
    var totalOutflowOrig = 0L
    var totalInflowBase = 0L
    var totalOutflowBase = 0L

    val lineItems = mutableListOf<DailyCashRegisterLine>()

    for (row in rawRows) {
        // Zero-base positive original lines handling:
        // When baseDebitMinor > 0: Inflow (Debit)
        // When baseCreditMinor > 0: Outflow (Credit)
        // When both baseDebitMinor == 0 && baseCreditMinor == 0:
        // if origMinor > 0, it is treated as Inflow (+origMinor) with 0 base.
        val isInflow = when {
            row.baseDebitMinor > 0L -> true
            row.baseCreditMinor > 0L -> false
            else -> row.origMinor >= 0L
        }

        val (inflowOrig, outflowOrig) = if (isInflow) {
            Pair(row.origMinor, 0L)
        } else {
            Pair(0L, row.origMinor)
        }

        val (inflowBase, outflowBase) = Pair(row.baseDebitMinor, row.baseCreditMinor)

        currentRunningBalOrig += (inflowOrig - outflowOrig)
        currentRunningBalBase += (inflowBase - outflowBase)

        totalInflowOrig += inflowOrig
        totalOutflowOrig += outflowOrig
        totalInflowBase += inflowBase
        totalOutflowBase += outflowBase

        val desc = when {
            row.lineMemo.isNotBlank() -> row.lineMemo
            row.docNotes.isNotBlank() -> row.docNotes
            else -> formatDocTypeArabic(row.docType)
        }

        lineItems.add(
            DailyCashRegisterLine(
                lineId = row.lineId,
                entryId = row.entryId,
                docId = row.docId,
                entryNumber = row.entryNumber,
                dateEpochDay = row.entryDateEpochDay,
                dateFormatted = formatEpochToIsoDate(row.entryDateEpochDay),
                docType = row.docType,
                docTypeAr = formatDocTypeArabic(row.docType),
                docNumber = row.docNumber,
                partyId = row.linePartyId ?: row.docPartyId,
                partyName = row.partyName,
                description = desc,
                inflowOrigMinor = inflowOrig,
                outflowOrigMinor = outflowOrig,
                inflowBaseMinor = inflowBase,
                outflowBaseMinor = outflowBase,
                exchangeRateMicros = row.exchangeRateMicros,
                runningBalanceOrigMinor = currentRunningBalOrig,
                runningBalanceBaseMinor = currentRunningBalBase
            )
        )
    }

    // 3. Compute optional daily totals groupings
    val dailyGroups = lineItems.groupBy { it.dateEpochDay }.map { (day, dayLines) ->
        val dayInflowOrig = dayLines.sumOf { it.inflowOrigMinor }
        val dayOutflowOrig = dayLines.sumOf { it.outflowOrigMinor }
        val dayInflowBase = dayLines.sumOf { it.inflowBaseMinor }
        val dayOutflowBase = dayLines.sumOf { it.outflowBaseMinor }
        val lastLine = dayLines.last()

        DailyGroupSummary(
            dateEpochDay = day,
            dateFormatted = formatEpochToIsoDate(day),
            lines = dayLines,
            dayInflowOrigMinor = dayInflowOrig,
            dayOutflowOrigMinor = dayOutflowOrig,
            dayNetOrigMinor = dayInflowOrig - dayOutflowOrig,
            dayInflowBaseMinor = dayInflowBase,
            dayOutflowBaseMinor = dayOutflowBase,
            dayNetBaseMinor = dayInflowBase - dayOutflowBase,
            dayEndingBalanceOrigMinor = lastLine.runningBalanceOrigMinor,
            dayEndingBalanceBaseMinor = lastLine.runningBalanceBaseMinor
        )
    }

    // 4. Value Estimation Note using current exchange rate
    val isForeign = (currency != CurrencyCode.FUNCTIONAL.name)
    val org = db.organizationDao().getOrganizationSync()
    val zone = org?.primaryRateZone ?: "SANAA"

    val rateEntity = if (isForeign) {
        db.currencyRateDao().getLatestRate(currency, zone, endDateEpochDay)
            ?: db.currencyRateDao().getLatestRateForZone(currency, zone)
    } else null

    val currentRateMicros = if (isForeign) {
        rateEntity?.rateMicros ?: ExchangeRate.SCALE_MICROS
    } else {
        ExchangeRate.SCALE_MICROS
    }

    val estimatedCurrentYerMinor = if (isForeign) {
        (currentRunningBalOrig * currentRateMicros) / ExchangeRate.SCALE_MICROS
    } else {
        currentRunningBalOrig
    }

    val unrealizedDeltaYerMinor = estimatedCurrentYerMinor - currentRunningBalBase

    val noteTextAr = if (isForeign) {
        val whole = currentRateMicros / ExchangeRate.SCALE_MICROS
        val rem = (currentRateMicros % ExchangeRate.SCALE_MICROS).let { if (it < 0) -it else it }
        val frac2 = rem / 10_000 // two decimal places
        val formattedRate = if (frac2 == 0L) {
            "$whole"
        } else {
            String.format(java.util.Locale.US, "%d.%02d", whole, frac2)
        }
        "القيمة المقدرة بالريال اليمني بسعر اليوم ($formattedRate ر.ي/$currency): ${Money(estimatedCurrentYerMinor, CurrencyCode.YER).format()} (تقدير بسعر اليوم، لا يدخل في أي قيد)"
    } else {
        "الصندوق بعملة الأساس (ريال يمني)، القيمة الدفترية مطابقة تماماً للقيمة السوقية. (تقدير بسعر اليوم، لا يدخل في أي قيد)"
    }

    val valueEstimation = ValueEstimationNote(
        currency = currency,
        isForeignCurrency = isForeign,
        currentExchangeRateMicros = currentRateMicros,
        estimatedCurrentYerMinor = estimatedCurrentYerMinor,
        bookValueBaseMinor = currentRunningBalBase,
        unrealizedDeltaYerMinor = unrealizedDeltaYerMinor,
        noteTextAr = noteTextAr
    )

    // 5. Reconciliation Checks
    // Check 1: Closing Balance (Original Currency) = Treasury Cash Balance
    val treasuryCashBalanceOrig = db.journalDao().getTreasuryClosingOrigBalance(
        treasuryId = treasuryId,
        currency = currency,
        asOfDateEpochDay = endDateEpochDay
    )
    val origDiff = currentRunningBalOrig - treasuryCashBalanceOrig
    val isOrigReconciled = (origDiff == 0L)

    // Check 2: Base Currency Balance = Ledger Account Balance for this treasury
    val treasuryBaseBalance = db.journalDao().getTreasuryClosingBaseBalance(
        treasuryId = treasuryId,
        currency = currency,
        asOfDateEpochDay = endDateEpochDay
    )
    val baseDiff = currentRunningBalBase - treasuryBaseBalance
    val isBaseReconciled = (baseDiff == 0L)

    // General ledger control account balance (1101 or 1102)
    val glAccountBaseBalance = db.journalDao().getAccountNetBaseBalanceAsOf(glCode, endDateEpochDay)

    val isFullyReconciled = isOrigReconciled && isBaseReconciled
    val statusMsg = if (isFullyReconciled) {
        "مطابق تماماً: الرصيد بالعملة الأصلية والقيمة الدفترية متطابقان مع دفتر الأستاذ وسجل الخزينة."
    } else {
        "فروقات مطابقة: فرق الأصلية ($origDiff $currency) - فرق الدفترية ($baseDiff ر.ي)"
    }

    val reconciliation = DailyCashRegisterReconciliation(
        closingBalanceOrigMinor = currentRunningBalOrig,
        treasuryCashBalanceOrigMinor = treasuryCashBalanceOrig,
        origDifferenceMinor = origDiff,
        isOrigReconciled = isOrigReconciled,
        closingBalanceBaseMinor = currentRunningBalBase,
        treasuryBaseBalanceMinor = treasuryBaseBalance,
        baseDifferenceMinor = baseDiff,
        isBaseReconciled = isBaseReconciled,
        glAccountCode = glCode,
        glAccountName = glAccountName,
        glAccountBaseBalanceMinor = glAccountBaseBalance,
        isFullyReconciled = isFullyReconciled,
        statusMessageAr = statusMsg
    )

    return DailyCashRegisterStatement(
        treasuryId = treasuryId,
        treasuryName = treasury.name,
        currency = currency,
        glAccountCode = glCode,
        glAccountName = glAccountName,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay,
        openingBalanceOrigMinor = openingBalOrig,
        openingBalanceBaseMinor = openingBalBase,
        lines = lineItems,
        dailyGroups = dailyGroups,
        totalInflowOrigMinor = totalInflowOrig,
        totalOutflowOrigMinor = totalOutflowOrig,
        netChangeOrigMinor = totalInflowOrig - totalOutflowOrig,
        totalInflowBaseMinor = totalInflowBase,
        totalOutflowBaseMinor = totalOutflowBase,
        netChangeBaseMinor = totalInflowBase - totalOutflowBase,
        closingBalanceOrigMinor = currentRunningBalOrig,
        closingBalanceBaseMinor = currentRunningBalBase,
        valueEstimation = valueEstimation,
        reconciliation = reconciliation
    )
}

private fun formatEpochToIsoDate(epochDay: Long): String {
    return try {
        val ld = LocalDate.ofEpochDay(epochDay)
        String.format(java.util.Locale.US, "%04d-%02d-%02d", ld.year, ld.monthValue, ld.dayOfMonth)
    } catch (_: Exception) {
        epochDay.toString()
    }
}

private fun formatDocTypeArabic(docType: String): String {
    return try {
        DocumentType.valueOf(docType).arabicName
    } catch (_: Exception) {
        docType
    }
}
