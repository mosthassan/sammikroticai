package com.example.domain.report

import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.core.ledger.JournalEntryType
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CashJournalLineRow
import com.example.data.local.dao.EntryOffsetLineRow
import com.example.data.local.dao.NonCashDocRow
import com.example.data.local.entity.AllocationEntity
import com.example.data.local.entity.DocumentItemEntity
import com.example.data.local.entity.PartyEntity

/**
 * Top-level Cash Flow activity classification according to IAS 7 and IFRS.
 */
enum class CashFlowActivityType(val titleAr: String) {
    OPERATING("الأنشطة التشغيلية"),
    INVESTING("الأنشطة الاستثمارية"),
    FINANCING("الأنشطة التمويلية"),
    FX_IMPACT("أثر تقلبات أسعار الصرف على النقدية"),
    EXCLUDED_TRANSFER("تحويلات نقدية داخلية مستبعدة (أثر صفري)"),
    OPENING_BALANCE("رصيد افتتاحي (مستبعد من تدفق الفترة)")
}

/**
 * Specific cash flow category within each activity.
 */
enum class CashFlowCategory(
    val titleAr: String,
    val activityType: CashFlowActivityType,
    val isOutflow: Boolean
) {
    // --- Operating Activities (Inflows) ---
    CUSTOMER_COLLECTIONS("تحصيلات من العملاء والوكلاء", CashFlowActivityType.OPERATING, false),
    QUICK_SALES("مبيعات نقدية وفورية (كروت وخدمات)", CashFlowActivityType.OPERATING, false),
    OTHER_OPERATING_INFLOWS("إيرادات تشغيلية نقدية أخرى", CashFlowActivityType.OPERATING, false),

    // --- Operating Activities (Outflows) ---
    CUSTOMER_REFUNDS("استردادات ومردودات العملاء نقدياً", CashFlowActivityType.OPERATING, true),
    VENDOR_PAYMENTS_OPERATING("مدفوعات الموردين (المصاريف التشغيلية)", CashFlowActivityType.OPERATING, true),
    DIRECT_ISP_EXPENSES("اشتراكات الإنترنت الرئيسية (Starlink/Fiber)", CashFlowActivityType.OPERATING, true),
    OPERATING_EXPENSES("مصاريف تشغيلية وعمومية (ديزل/كهرباء/إيجار)", CashFlowActivityType.OPERATING, true),
    MAINTENANCE_EXPENSES("مصاريف صيانة وقطع غيار", CashFlowActivityType.OPERATING, true),
    SALARIES_STAFF("رواتب وأجور وفنيين", CashFlowActivityType.OPERATING, true),
    MISC_OPERATING_EXPENSES("مصاريف تشغيلية متنوعة", CashFlowActivityType.OPERATING, true),

    // --- Investing Activities (Inflows) ---
    ASSET_DISPOSAL_PROCEEDS("متحصلات بيع والتخلص من أصول ومعدات", CashFlowActivityType.INVESTING, false),

    // --- Investing Activities (Outflows) ---
    VENDOR_PAYMENTS_INVESTING("مدفوعات الموردين لأجهزة ومعدات الشبكة (رأسمالي)", CashFlowActivityType.INVESTING, true),
    DIRECT_CAPITAL_EXPENDITURES("شراء أصول ومعدات شبكة نقدياً", CashFlowActivityType.INVESTING, true),

    // --- Financing Activities (Inflows) ---
    PARTNER_CAPITAL_CONTRIBUTIONS("مساهمات الشركاء النقدية في رأس المال", CashFlowActivityType.FINANCING, false),
    PARTNER_CURRENT_CONTRIBUTIONS("إيداعات نقدية في جاري الشركاء", CashFlowActivityType.FINANCING, false),

    // --- Financing Activities (Outflows) ---
    PARTNER_DRAWINGS("مسحوبات الشركاء النقدية", CashFlowActivityType.FINANCING, true),
    DIVIDENDS_PAID("أرباح نقدية موزعة على الشركاء", CashFlowActivityType.FINANCING, true),

    // --- Standalone Reconciling Line ---
    FX_REVALUATION_CASH("أثر فروق تقييم أسعار الصرف على النقدية", CashFlowActivityType.FX_IMPACT, false),

    // --- Excluded Non-Flow Items ---
    CASH_TRANSFERS("تحويلات بين الصناديق والبنوك (صافي صفر)", CashFlowActivityType.EXCLUDED_TRANSFER, false),
    CURRENCY_EXCHANGE("مصارفة عملات نقدية (صافي صفر)", CashFlowActivityType.EXCLUDED_TRANSFER, false),
    OPENING_BALANCE_CASH("رصيد افتتاحي للنقدية", CashFlowActivityType.OPENING_BALANCE, false)
}

/**
 * Individual classified line item contributing to the Cash Flow Statement.
 */
data class CashFlowLineItem(
    val entryId: String,
    val docId: String,
    val docNumber: Long,
    val docType: String,
    val dateEpochDay: Long,
    val category: CashFlowCategory,
    val partyId: String?,
    val partyName: String?,
    val description: String,
    val currency: String,
    val origMinor: Long,
    val inflowMinor: Long,
    val outflowMinor: Long,
    val netCashFlowMinor: Long
)

/**
 * Summary for a specific category within an activity.
 */
data class CashFlowCategorySummary(
    val category: CashFlowCategory,
    val totalInflowMinor: Long,
    val totalOutflowMinor: Long,
    val netFlowMinor: Long,
    val count: Int
)

/**
 * Complete section for an activity (Operating, Investing, Financing, FX Impact).
 */
data class CashFlowSection(
    val activityType: CashFlowActivityType,
    val titleAr: String,
    val totalInflowsMinor: Long,
    val totalOutflowsMinor: Long,
    val netFlowMinor: Long,
    val categories: List<CashFlowCategorySummary>,
    val lines: List<CashFlowLineItem>
)

/**
 * Non-cash investing and financing transactions disclosed in the notes.
 */
data class NonCashDisclosureItem(
    val docId: String,
    val docNumber: Long,
    val docType: String,
    val dateEpochDay: Long,
    val typeAr: String,
    val partyName: String,
    val description: String,
    val amountMinor: Long,
    val targetAccount: String
)

/**
 * Complete IFRS-compliant Cash Flow Statement Report (قائمة التدفقات النقدية).
 */
data class CashFlowStatement(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val openingCashMinor: Long,
    val closingCashMinor: Long,
    val netOperatingCashFlowMinor: Long,
    val netInvestingCashFlowMinor: Long,
    val netFinancingCashFlowMinor: Long,
    val fxImpactOnCashMinor: Long,
    val netChangeInCashMinor: Long,
    val operatingSection: CashFlowSection,
    val investingSection: CashFlowSection,
    val financingSection: CashFlowSection,
    val fxSection: CashFlowSection,
    val nonCashDisclosures: List<NonCashDisclosureItem>,
    val vaultCashClosingMinor: Long,
    val bankCashClosingMinor: Long,
    val unclassifiedAmountMinor: Long,
    val reconciliation: ReconciliationSummary
)

/**
 * Hare-Niemeyer (Largest Remainder) integer allocation algorithm.
 * Guarantees zero floating-point usage, zero rounding loss, and exact integer balance.
 */
object HareNiemeyerSplitter {
    /**
     * Splits a payment amount [totalPaymentMinor] proportionally between Operating (expenses)
     * and Investing (capital assets) strictly based on invoice item costs.
     *
     * @return Pair(operatingMinor, investingMinor) where operatingMinor + investingMinor == totalPaymentMinor.
     */
    fun splitPaymentProportionally(
        totalPaymentMinor: Long,
        invoiceItems: List<DocumentItemEntity>
    ): Pair<Long, Long> {
        if (totalPaymentMinor <= 0L || invoiceItems.isEmpty()) {
            return Pair(totalPaymentMinor, 0L)
        }

        val totalInvoiceCostMinor = invoiceItems.sumOf { it.totalMinor }
        if (totalInvoiceCostMinor <= 0L) {
            return Pair(totalPaymentMinor, 0L)
        }

        val assetCostMinor = invoiceItems
            .filter { it.isAsset || it.accountCode.startsWith("15") }
            .sumOf { it.totalMinor }

        val expenseCostMinor = invoiceItems
            .filter { !it.isAsset && !it.accountCode.startsWith("15") }
            .sumOf { it.totalMinor }

        // Short-circuit pure cases
        if (assetCostMinor == 0L) return Pair(totalPaymentMinor, 0L)
        if (expenseCostMinor == 0L) return Pair(0L, totalPaymentMinor)

        // Exact integer quotients
        val qOp = (totalPaymentMinor * expenseCostMinor) / totalInvoiceCostMinor
        val rOp = (totalPaymentMinor * expenseCostMinor) % totalInvoiceCostMinor

        val qInv = (totalPaymentMinor * assetCostMinor) / totalInvoiceCostMinor
        val rInv = (totalPaymentMinor * assetCostMinor) % totalInvoiceCostMinor

        var allocatedOp = qOp
        var allocatedInv = qInv
        val remainder = totalPaymentMinor - (qOp + qInv)

        if (remainder > 0L) {
            // Allocate remaining minor units to the category with the larger fractional remainder
            if (rInv > rOp) {
                allocatedInv += remainder
            } else {
                allocatedOp += remainder
            }
        }

        // Mathematical invariant: exact equality must hold down to single minor unit
        check(allocatedOp + allocatedInv == totalPaymentMinor) {
            "Hare-Niemeyer split invariant failed: $allocatedOp + $allocatedInv != $totalPaymentMinor"
        }

        return Pair(allocatedOp, allocatedInv)
    }
}

/**
 * Primary Generator for Report 3: Cash Flow Statement (قائمة التدفقات النقدية).
 * Executes batch queries for O(1) query count stability regardless of ledger size.
 */
suspend fun generateCashFlowStatement(
    db: AppDatabase,
    fromEpochDay: Long,
    toEpochDay: Long
): CashFlowStatement {
    // 1. Batch Query Cash Journal Lines for the period (Single query)
    val cashLines = db.journalDao().getCashMovementLines(fromEpochDay, toEpochDay)

    // 2. Batch Query All Offsetting Journal Lines for these entries (Single query)
    val offsetLines = db.journalDao().getOffsetLinesForCashEntries(fromEpochDay, toEpochDay)
    val offsetMap = offsetLines.groupBy { it.entryId }

    // 3. Batch Query Active Allocations for Period Cash Payments (Single query)
    val periodAllocations = db.allocationDao().getActiveAllocationsForPeriodCashPayments(fromEpochDay, toEpochDay)
    val allocationsByPaymentDoc = periodAllocations.groupBy { it.paymentDocId }

    // 4. Batch Query Invoice Items for all allocated purchase invoices (Single query)
    val invoiceItems = db.documentDao().getDocumentItemsForPeriodCashPaymentInvoices(fromEpochDay, toEpochDay)
    val itemsByInvoiceDoc = invoiceItems.groupBy { it.docId }

    // 5. Batch Query Parties for name resolution (Single query)
    val partiesMap = db.partyDao().getAllPartiesSync().associateBy { it.id }

    // 6. Batch Query Opening, Closing, and Account balances (Constant queries)
    val openingCash = db.journalDao().getCashOpeningBalance(fromEpochDay, toEpochDay)
    val closingCash = db.journalDao().getCashClosingBalance(toEpochDay)
    val periodNetMovementFromLedger = db.journalDao().getCashNetPeriodMovement(fromEpochDay, toEpochDay)

    val accountBalances = db.journalDao().getCashAccountBalancesAsOf(toEpochDay)
        .associate { it.accountCode to it.balanceMinor }
    val vaultClose = accountBalances[AccountConstants.CASH_VAULT] ?: 0L
    val bankClose = accountBalances[AccountConstants.BANKS_WALLETS] ?: 0L

    // 7. Batch Query Non-Cash Disclosures (Single query)
    val nonCashRows = db.journalDao().getNonCashDisclosureLines(fromEpochDay, toEpochDay)

    // 8. In-Memory Direct Classification Engine
    val classifiedItems = mutableListOf<CashFlowLineItem>()
    var unclassifiedAmountMinor = 0L

    // Group cash lines by entryId to properly detect internal transfers and multi-line disbursements
    val cashLinesByEntry = cashLines.groupBy { it.entryId }

    for ((entryId, entryCashLines) in cashLinesByEntry) {
        val entryOffsets = offsetMap[entryId]?.filter { line ->
            entryCashLines.none { it.lineId == line.lineId }
        } ?: emptyList()

        val sampleCashLine = entryCashLines.first()
        val docType = sampleCashLine.docType
        val entryType = sampleCashLine.entryType
        val docParty = sampleCashLine.docPartyId?.let { partiesMap[it] }

        // --- Case 1: Opening Balance ---
        if (docType == DocumentType.OPENING_BALANCE.name || entryType == "OPENING") {
            for (line in entryCashLines) {
                val net = line.baseDebitMinor - line.baseCreditMinor
                classifiedItems.add(
                    CashFlowLineItem(
                        entryId = entryId,
                        docId = line.docId,
                        docNumber = line.docNumber,
                        docType = line.docType,
                        dateEpochDay = line.entryDateEpochDay,
                        category = CashFlowCategory.OPENING_BALANCE_CASH,
                        partyId = line.linePartyId ?: line.docPartyId,
                        partyName = docParty?.name,
                        description = "رصيد افتتاحي للنقدية",
                        currency = line.currency,
                        origMinor = line.origMinor,
                        inflowMinor = line.baseDebitMinor,
                        outflowMinor = line.baseCreditMinor,
                        netCashFlowMinor = net
                    )
                )
            }
            continue
        }

        // --- Case 2: Cash Transfers & Currency Exchanges (Excluded from net flows, Net Zero) ---
        val isInternalTransfer = docType in setOf(
            DocumentType.TREASURY_TRANSFER.name,
            DocumentType.CURRENCY_EXCHANGE.name
        ) || (entryCashLines.size >= 2 && entryCashLines.any { it.baseDebitMinor > 0 } && entryCashLines.any { it.baseCreditMinor > 0 }) ||
                entryOffsets.any { it.accountCode in setOf(AccountConstants.CASH_VAULT, AccountConstants.BANKS_WALLETS) }

        if (isInternalTransfer) {
            val transferCategory = if (docType == DocumentType.CURRENCY_EXCHANGE.name) {
                CashFlowCategory.CURRENCY_EXCHANGE
            } else {
                CashFlowCategory.CASH_TRANSFERS
            }

            for (line in entryCashLines) {
                val net = line.baseDebitMinor - line.baseCreditMinor
                classifiedItems.add(
                    CashFlowLineItem(
                        entryId = entryId,
                        docId = line.docId,
                        docNumber = line.docNumber,
                        docType = line.docType,
                        dateEpochDay = line.entryDateEpochDay,
                        category = transferCategory,
                        partyId = line.linePartyId ?: line.docPartyId,
                        partyName = docParty?.name,
                        description = if (line.memo.isNotBlank()) line.memo else "تحويل نقدية بين الصناديق/البنوك",
                        currency = line.currency,
                        origMinor = line.origMinor,
                        inflowMinor = line.baseDebitMinor,
                        outflowMinor = line.baseCreditMinor,
                        netCashFlowMinor = net
                    )
                )
            }
            continue
        }

        // --- Case 3: FX Revaluations and Reversals on Cash (Standalone Reconciling Section) ---
        val isFxRevaluation = docType == DocumentType.PERIODIC_REVALUATION.name ||
                entryType == JournalEntryType.REVERSAL.name && entryOffsets.any {
                    it.accountCode in setOf(
                        AccountConstants.UNREALIZED_FX_GAIN,
                        AccountConstants.UNREALIZED_FX_LOSS,
                        AccountConstants.REALIZED_FX_GAIN,
                        AccountConstants.REALIZED_FX_LOSS
                    )
                } ||
                entryOffsets.any {
                    it.accountCode in setOf(
                        AccountConstants.UNREALIZED_FX_GAIN,
                        AccountConstants.UNREALIZED_FX_LOSS
                    )
                }

        if (isFxRevaluation) {
            for (line in entryCashLines) {
                val net = line.baseDebitMinor - line.baseCreditMinor
                classifiedItems.add(
                    CashFlowLineItem(
                        entryId = entryId,
                        docId = line.docId,
                        docNumber = line.docNumber,
                        docType = line.docType,
                        dateEpochDay = line.entryDateEpochDay,
                        category = CashFlowCategory.FX_REVALUATION_CASH,
                        partyId = line.linePartyId ?: line.docPartyId,
                        partyName = docParty?.name,
                        description = if (line.memo.isNotBlank()) line.memo else "أثر إعادة تقييم فروق أسعار الصرف على النقدية",
                        currency = line.currency,
                        origMinor = line.origMinor,
                        inflowMinor = line.baseDebitMinor,
                        outflowMinor = line.baseCreditMinor,
                        netCashFlowMinor = net
                    )
                )
            }
            continue
        }

        // --- Standard Entries: Evaluate Each Cash Line ---
        for (line in entryCashLines) {
            val isDebit = line.baseDebitMinor > 0L
            val isCredit = line.baseCreditMinor > 0L

            if (isDebit) {
                // Inflows to Cash
                val category: CashFlowCategory = when {
                    // Financing: Capital Contribution (3101) or Partner Current Deposit (3201)
                    entryOffsets.any { it.accountCode == AccountConstants.CAPITAL } ||
                            line.memo.contains("مساهمة رأسمالية") ||
                            (docParty?.isPartner == true && entryOffsets.any { it.accountCode == AccountConstants.PARTNER_CURRENT }) -> {
                        if (entryOffsets.any { it.accountCode == AccountConstants.CAPITAL } || line.memo.contains("مساهمة رأسمالية")) {
                            CashFlowCategory.PARTNER_CAPITAL_CONTRIBUTIONS
                        } else {
                            CashFlowCategory.PARTNER_CURRENT_CONTRIBUTIONS
                        }
                    }

                    // Investing: Fixed Asset Disposal / Sale Proceeds (1501, 1599)
                    entryOffsets.any { it.accountCode == AccountConstants.FIXED_ASSETS_NETWORK || it.accountCode == AccountConstants.ACCUMULATED_DEPRECIATION } ||
                            line.memo.contains("بيع أصل") || line.memo.contains("استبعاد أصل") -> {
                        CashFlowCategory.ASSET_DISPOSAL_PROCEEDS
                    }

                    // Operating: Quick Sales / Cash Revenue (4101, 4201, 4xxx)
                    docType == DocumentType.SALES_INVOICE.name ||
                            entryOffsets.any { it.accountCode in setOf(AccountConstants.CARD_SALES_REVENUE, AccountConstants.DIRECT_SERVICE_REVENUE) } ||
                            line.memo.contains("بيع فوري") || line.memo.contains("بيع سريع") -> {
                        CashFlowCategory.QUICK_SALES
                    }

                    // Operating: Customer Collections (1201 or Receipt Voucher with Customer)
                    entryOffsets.any { it.accountCode == AccountConstants.ACCOUNTS_RECEIVABLE } ||
                            docType == DocumentType.RECEIPT_VOUCHER.name ||
                            (docParty?.isCustomer == true) -> {
                        CashFlowCategory.CUSTOMER_COLLECTIONS
                    }

                    // Operating: Other operating revenues
                    entryOffsets.any { it.accountCode.startsWith("4") } -> {
                        CashFlowCategory.OTHER_OPERATING_INFLOWS
                    }

                    else -> {
                        // Unclassified Debit
                        unclassifiedAmountMinor += line.baseDebitMinor
                        CashFlowCategory.OTHER_OPERATING_INFLOWS
                    }
                }

                classifiedItems.add(
                    CashFlowLineItem(
                        entryId = entryId,
                        docId = line.docId,
                        docNumber = line.docNumber,
                        docType = line.docType,
                        dateEpochDay = line.entryDateEpochDay,
                        category = category,
                        partyId = line.linePartyId ?: line.docPartyId,
                        partyName = docParty?.name,
                        description = if (line.memo.isNotBlank()) line.memo else "${category.titleAr} #${line.docNumber}",
                        currency = line.currency,
                        origMinor = line.origMinor,
                        inflowMinor = line.baseDebitMinor,
                        outflowMinor = 0L,
                        netCashFlowMinor = line.baseDebitMinor
                    )
                )
            } else if (isCredit) {
                // Outflows from Cash
                val totalOutflowMinor = line.baseCreditMinor

                when {
                    // Financing: Partner Drawings / Withdrawals (3201 / PARTNER_DRAWINGS)
                    entryOffsets.any { it.accountCode == AccountConstants.PARTNER_CURRENT } ||
                            line.memo.contains("مسحوبات") ||
                            (docParty?.isPartner == true && docType == DocumentType.PAYMENT_VOUCHER.name && entryOffsets.none { it.accountCode.startsWith("5") }) -> {
                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = CashFlowCategory.PARTNER_DRAWINGS,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "مسحوبات نقدية للشريك #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }

                    // Financing: Cash Dividends Paid (3301 / DIVIDEND_DISTRIBUTION)
                    entryOffsets.any { it.accountCode == AccountConstants.RETAINED_EARNINGS } ||
                            docType == DocumentType.DIVIDEND_DISTRIBUTION.name -> {
                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = CashFlowCategory.DIVIDENDS_PAID,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "توزيعات أرباح نقدية #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }

                    // Operating: Customer Refunds / Returns (1201 Credit / 4102 / CREDIT_NOTE)
                    entryOffsets.any { it.accountCode == AccountConstants.SALES_RETURNS } ||
                            (docParty?.isCustomer == true && docType == DocumentType.PAYMENT_VOUCHER.name) ||
                            (docType == DocumentType.CREDIT_NOTE.name) -> {
                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = CashFlowCategory.CUSTOMER_REFUNDS,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "استرداد نقدي لعميل #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }

                    // Investing: Direct Fixed Asset Purchase (1501 Debit)
                    entryOffsets.any { it.accountCode == AccountConstants.FIXED_ASSETS_NETWORK } -> {
                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = CashFlowCategory.DIRECT_CAPITAL_EXPENDITURES,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "شراء أصول ومعدات شبكة نقدياً #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }

                    // Operating: Direct Expenses (5xxx)
                    entryOffsets.any { it.accountCode.startsWith("5") } -> {
                        val expenseCategory = when {
                            entryOffsets.any { it.accountCode == AccountConstants.DIRECT_ISP_SERVICE_COST } -> CashFlowCategory.DIRECT_ISP_EXPENSES
                            entryOffsets.any { it.accountCode == AccountConstants.OPERATING_EXPENSES } -> CashFlowCategory.OPERATING_EXPENSES
                            entryOffsets.any { it.accountCode == AccountConstants.MAINTENANCE_SPARES } -> CashFlowCategory.MAINTENANCE_EXPENSES
                            entryOffsets.any { it.accountCode == AccountConstants.SALARIES_STAFF } -> CashFlowCategory.SALARIES_STAFF
                            else -> CashFlowCategory.MISC_OPERATING_EXPENSES
                        }

                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = expenseCategory,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "${expenseCategory.titleAr} #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }

                    // Vendor Payment (2101 Payables / Vendor Party): Proportional Split via Largest Remainder
                    entryOffsets.any { it.accountCode == AccountConstants.ACCOUNTS_PAYABLE } ||
                            docParty?.isVendor == true ||
                            docType == DocumentType.PAYMENT_VOUCHER.name -> {

                        val paymentAllocations = allocationsByPaymentDoc[line.docId] ?: emptyList()

                        if (paymentAllocations.isNotEmpty()) {
                            // Sum allocated amounts and split proportionally per invoice
                            var totalOperatingFromPayment = 0L
                            var totalInvestingFromPayment = 0L

                            for (alloc in paymentAllocations) {
                                val invoiceDocItems = itemsByInvoiceDoc[alloc.invoiceDocId] ?: emptyList()
                                val (allocOp, allocInv) = HareNiemeyerSplitter.splitPaymentProportionally(
                                    totalPaymentMinor = alloc.allocatedBaseMinor,
                                    invoiceItems = invoiceDocItems
                                )
                                totalOperatingFromPayment += allocOp
                                totalInvestingFromPayment += allocInv
                            }

                            // If total allocated base differs from total cash paid (partial unallocated),
                            // assign remainder to Operating
                            val totalAllocatedBase = totalOperatingFromPayment + totalInvestingFromPayment
                            val unallocatedCash = totalOutflowMinor - totalAllocatedBase
                            if (unallocatedCash > 0L) {
                                totalOperatingFromPayment += unallocatedCash
                            } else if (unallocatedCash < 0L) {
                                // Clamp to actual outflow
                                val scale = totalOutflowMinor
                                val (scaledOp, scaledInv) = HareNiemeyerSplitter.splitPaymentProportionally(
                                    totalPaymentMinor = scale,
                                    invoiceItems = paymentAllocations.flatMap { itemsByInvoiceDoc[it.invoiceDocId] ?: emptyList() }
                                )
                                totalOperatingFromPayment = scaledOp
                                totalInvestingFromPayment = scaledInv
                            }

                            // Add Operating portion if > 0
                            if (totalOperatingFromPayment > 0L) {
                                classifiedItems.add(
                                    CashFlowLineItem(
                                        entryId = entryId,
                                        docId = line.docId,
                                        docNumber = line.docNumber,
                                        docType = line.docType,
                                        dateEpochDay = line.entryDateEpochDay,
                                        category = CashFlowCategory.VENDOR_PAYMENTS_OPERATING,
                                        partyId = line.linePartyId ?: line.docPartyId,
                                        partyName = docParty?.name,
                                        description = if (line.memo.isNotBlank()) line.memo else "سداد مورد (حصة مصاريف تشغيلية) #${line.docNumber}",
                                        currency = line.currency,
                                        origMinor = line.origMinor,
                                        inflowMinor = 0L,
                                        outflowMinor = totalOperatingFromPayment,
                                        netCashFlowMinor = -totalOperatingFromPayment
                                    )
                                )
                            }

                            // Add Investing portion if > 0
                            if (totalInvestingFromPayment > 0L) {
                                classifiedItems.add(
                                    CashFlowLineItem(
                                        entryId = entryId,
                                        docId = line.docId,
                                        docNumber = line.docNumber,
                                        docType = line.docType,
                                        dateEpochDay = line.entryDateEpochDay,
                                        category = CashFlowCategory.VENDOR_PAYMENTS_INVESTING,
                                        partyId = line.linePartyId ?: line.docPartyId,
                                        partyName = docParty?.name,
                                        description = if (line.memo.isNotBlank()) line.memo else "سداد مورد (حصة أصول ومعدات شبكة) #${line.docNumber}",
                                        currency = line.currency,
                                        origMinor = line.origMinor,
                                        inflowMinor = 0L,
                                        outflowMinor = totalInvestingFromPayment,
                                        netCashFlowMinor = -totalInvestingFromPayment
                                    )
                                )
                            }
                        } else {
                            // Unallocated vendor payment: default 100% to Operating
                            classifiedItems.add(
                                CashFlowLineItem(
                                    entryId = entryId,
                                    docId = line.docId,
                                    docNumber = line.docNumber,
                                    docType = line.docType,
                                    dateEpochDay = line.entryDateEpochDay,
                                    category = CashFlowCategory.VENDOR_PAYMENTS_OPERATING,
                                    partyId = line.linePartyId ?: line.docPartyId,
                                    partyName = docParty?.name,
                                    description = if (line.memo.isNotBlank()) line.memo else "سداد مورد #${line.docNumber}",
                                    currency = line.currency,
                                    origMinor = line.origMinor,
                                    inflowMinor = 0L,
                                    outflowMinor = totalOutflowMinor,
                                    netCashFlowMinor = -totalOutflowMinor
                                )
                            )
                        }
                    }

                    else -> {
                        // Unclassified Credit: Track and flag
                        unclassifiedAmountMinor += totalOutflowMinor
                        classifiedItems.add(
                            CashFlowLineItem(
                                entryId = entryId,
                                docId = line.docId,
                                docNumber = line.docNumber,
                                docType = line.docType,
                                dateEpochDay = line.entryDateEpochDay,
                                category = CashFlowCategory.MISC_OPERATING_EXPENSES,
                                partyId = line.linePartyId ?: line.docPartyId,
                                partyName = docParty?.name,
                                description = if (line.memo.isNotBlank()) line.memo else "مدفوعات أخرى غير مصنفة #${line.docNumber}",
                                currency = line.currency,
                                origMinor = line.origMinor,
                                inflowMinor = 0L,
                                outflowMinor = totalOutflowMinor,
                                netCashFlowMinor = -totalOutflowMinor
                            )
                        )
                    }
                }
            }
        }
    }

    // 9. Build Sections
    val operatingLines = classifiedItems.filter { it.category.activityType == CashFlowActivityType.OPERATING }
    val investingLines = classifiedItems.filter { it.category.activityType == CashFlowActivityType.INVESTING }
    val financingLines = classifiedItems.filter { it.category.activityType == CashFlowActivityType.FINANCING }
    val fxLines = classifiedItems.filter { it.category.activityType == CashFlowActivityType.FX_IMPACT }

    val operatingSection = buildSection(CashFlowActivityType.OPERATING, operatingLines)
    val investingSection = buildSection(CashFlowActivityType.INVESTING, investingLines)
    val financingSection = buildSection(CashFlowActivityType.FINANCING, financingLines)
    val fxSection = buildSection(CashFlowActivityType.FX_IMPACT, fxLines)

    val netOperating = operatingSection.netFlowMinor
    val netInvesting = investingSection.netFlowMinor
    val netFinancing = financingSection.netFlowMinor
    val fxImpact = fxSection.netFlowMinor

    val netChangeInCash = netOperating + netInvesting + netFinancing + fxImpact

    // 10. Process Non-Cash Disclosures
    val nonCashDisclosures = nonCashRows.map { row ->
        val typeAr = when {
            row.accountCode == AccountConstants.FIXED_ASSETS_NETWORK -> "مساهمة عينية بأصول ومعدات"
            row.accountCode == AccountConstants.PARTNER_CURRENT -> "سداد مصاريف/موردين بالنيابة عن المنشأة"
            else -> "معاملة استثمارية/تمويلية غير نقدية"
        }
        val targetAccount = when (row.accountCode) {
            AccountConstants.FIXED_ASSETS_NETWORK -> "1501 - أجهزة ومعدات الشبكة"
            AccountConstants.PARTNER_CURRENT -> "3201 - جاري الشركاء"
            else -> row.accountCode
        }

        NonCashDisclosureItem(
            docId = row.docId,
            docNumber = row.docNumber,
            docType = row.docType,
            dateEpochDay = row.dateEpochDay,
            typeAr = typeAr,
            partyName = row.partyName ?: "شريك",
            description = if (row.notes.isNotBlank()) row.notes else row.lineMemo,
            amountMinor = if (row.baseDebitMinor > 0) row.baseDebitMinor else row.baseCreditMinor,
            targetAccount = targetAccount
        )
    }

    // 11. Dynamic Reconciliation Checks (Never hardcoded)
    val checks = mutableListOf<ReconciliationCheck>()

    // Check 1: Unclassified Amount must equal 0
    checks.add(
        ReconciliationCheck(
            name = "المبالغ النقدية غير المصنفة (Unclassified Amount == 0)",
            expectedMinor = 0L,
            actualMinor = unclassifiedAmountMinor,
            note = if (unclassifiedAmountMinor == 0L) {
                "تم تصنيف جميع الحركات النقدية بنسبة 100% وفق معايير IAS 7"
            } else {
                "تنبيه: توجد حركات نقدية غير مصنفة بمبلغ ${Money(unclassifiedAmountMinor, CurrencyCode.FUNCTIONAL).format()}"
            }
        )
    )

    // Check 2: Dynamic Cash Flow Identity: Opening Cash + Net Change == Closing Cash
    val calculatedClosingCash = openingCash + netChangeInCash
    checks.add(
        ReconciliationCheck(
            name = "معادلة التدفق النقدي (نقدية البداية + صافي التدفق = نقدية النهاية)",
            expectedMinor = closingCash,
            actualMinor = calculatedClosingCash,
            note = "التحقق من تطابق رصيد النقدية المحسوب مع رصيد الإقفال الفعلي"
        )
    )

    // Check 3: Closing Cash == Sum of Vault (1101) + Banks (1102) from General Ledger
    val ledgerTotalCashClosing = vaultClose + bankClose
    checks.add(
        ReconciliationCheck(
            name = "مطابقة نقدية النهاية مع أرصدة الأستاذ العام (1101 الصندوق + 1102 البنوك)",
            expectedMinor = ledgerTotalCashClosing,
            actualMinor = closingCash,
            note = "مطابقة رصيد نهاية الفترة مع ميزان المراجعة والميزانية العمومية"
        )
    )

    // Check 4: Net Change == Ledger Net Cash Movement for the period
    checks.add(
        ReconciliationCheck(
            name = "مطابقة صافي التغير مع الحركة الدفترية للأستاذ العام خلال الفترة",
            expectedMinor = periodNetMovementFromLedger,
            actualMinor = netChangeInCash,
            note = "التحقق من صحة صافي حركة الصناديق والبنوك المستخرجة من قيود اليومية"
        )
    )

    val reconciliationSummary = ReconciliationSummary(checks)

    return CashFlowStatement(
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        openingCashMinor = openingCash,
        closingCashMinor = closingCash,
        netOperatingCashFlowMinor = netOperating,
        netInvestingCashFlowMinor = netInvesting,
        netFinancingCashFlowMinor = netFinancing,
        fxImpactOnCashMinor = fxImpact,
        netChangeInCashMinor = netChangeInCash,
        operatingSection = operatingSection,
        investingSection = investingSection,
        financingSection = financingSection,
        fxSection = fxSection,
        nonCashDisclosures = nonCashDisclosures,
        vaultCashClosingMinor = vaultClose,
        bankCashClosingMinor = bankClose,
        unclassifiedAmountMinor = unclassifiedAmountMinor,
        reconciliation = reconciliationSummary
    )
}

private fun buildSection(
    activityType: CashFlowActivityType,
    lines: List<CashFlowLineItem>
): CashFlowSection {
    val totalInflows = lines.sumOf { it.inflowMinor }
    val totalOutflows = lines.sumOf { it.outflowMinor }
    val netFlow = totalInflows - totalOutflows

    val categoriesSummary = lines.groupBy { it.category }.map { (category, catLines) ->
        val catInflows = catLines.sumOf { it.inflowMinor }
        val catOutflows = catLines.sumOf { it.outflowMinor }
        CashFlowCategorySummary(
            category = category,
            totalInflowMinor = catInflows,
            totalOutflowMinor = catOutflows,
            netFlowMinor = catInflows - catOutflows,
            count = catLines.size
        )
    }.sortedByDescending { kotlin.math.abs(it.netFlowMinor) }

    return CashFlowSection(
        activityType = activityType,
        titleAr = activityType.titleAr,
        totalInflowsMinor = totalInflows,
        totalOutflowsMinor = totalOutflows,
        netFlowMinor = netFlow,
        categories = categoriesSummary,
        lines = lines
    )
}
