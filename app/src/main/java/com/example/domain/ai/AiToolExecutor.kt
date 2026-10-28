package com.example.domain.ai

import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.time.AppClock
import com.example.data.local.AppDatabase
import com.example.data.network.DeviceStatus
import com.example.data.network.NetworkRepository
import com.example.domain.report.generateCashFlowStatement
import com.example.domain.report.generateMonthlyProfitReport
import com.example.domain.usecase.StatementOfAccountUseCase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

/**
 * Executes read-only tool calls dispatched from the Gemini API model.
 * Strictly Read-Only operations. Never executes inserts, updates, or deletes.
 * Formats all monetary figures using [Money] to guarantee zero floating point loss.
 */
class AiToolExecutor(
    private val db: AppDatabase,
    private val networkRepository: NetworkRepository,
    private val statementOfAccountUseCase: StatementOfAccountUseCase
) {

    suspend fun execute(functionName: String, args: JSONObject): JSONObject {
        return when (functionName) {
            "get_monthly_profit_report" -> executeMonthlyProfitReport(args)
            "get_cash_flow_statement" -> executeCashFlowStatement(args)
            "get_party_ledger" -> executePartyLedger(args)
            "get_assets_summary" -> executeAssetsSummary(args)
            "get_network_devices_status" -> executeNetworkDevicesStatus(args)
            else -> JSONObject().apply {
                put("error", "Unknown function: $functionName")
            }
        }
    }

    private suspend fun executeMonthlyProfitReport(args: JSONObject): JSONObject {
        val today = AppClock.todayLocalDate()
        val requestedMonth = args.optInt("month", 0)
        val requestedYear = args.optInt("year", 0)

        val month = if (requestedMonth in 1..12) requestedMonth else today.monthValue
        val year = if (requestedYear > 2000) requestedYear else today.year

        val report = generateMonthlyProfitReport(db = db, fiscalYear = year)
        val monthItem = report.months.find { it.monthIndex == month }

        val result = JSONObject()
        result.put("fiscalYear", year)
        result.put("monthIndex", month)
        result.put("monthName", monthItem?.monthName ?: "الشهر $month")

        if (monthItem != null) {
            result.put("status", monthItem.status.status.title)
            result.put("netRevenue", Money(monthItem.netRevenueMinor, CurrencyCode.YER).format())
            result.put("cardRevenue", Money(monthItem.cardRevenueMinor, CurrencyCode.YER).format())
            result.put("serviceRevenue", Money(monthItem.serviceRevenueMinor, CurrencyCode.YER).format())
            result.put("salesReturns", Money(monthItem.salesReturnsMinor, CurrencyCode.YER).format())
            result.put("directIspCost", Money(monthItem.directIspCostMinor, CurrencyCode.YER).format())
            result.put("grossProfit", Money(monthItem.grossProfitMinor, CurrencyCode.YER).format())
            result.put(
                "grossMarginPercent",
                String.format(Locale.US, "%.2f%%", monthItem.grossMarginBasisPoints / 100.0)
            )
            result.put("operatingExpenses", Money(monthItem.totalOperatingExpensesMinor, CurrencyCode.YER).format())
            result.put("salariesExpenses", Money(monthItem.salariesExpensesMinor, CurrencyCode.YER).format())
            result.put("maintenanceExpenses", Money(monthItem.maintenanceExpensesMinor, CurrencyCode.YER).format())
            result.put("depreciationExpense", Money(monthItem.depreciationExpenseMinor, CurrencyCode.YER).format())
            result.put(
                "realizedFxNet",
                Money(monthItem.realizedFxGainMinor - monthItem.realizedFxLossMinor, CurrencyCode.YER).format()
            )
            result.put("netProfit", Money(monthItem.netProfitMinor, CurrencyCode.YER).format())
            result.put(
                "netMarginPercent",
                String.format(Locale.US, "%.2f%%", monthItem.netMarginBasisPoints / 100.0)
            )
            result.put("cumulativeProfit", Money(monthItem.cumulativeProfitMinor, CurrencyCode.YER).format())
        } else {
            result.put("netRevenue", "0.00 YER")
            result.put("grossProfit", "0.00 YER")
            result.put("netProfit", "0.00 YER")
            result.put("note", "لا توجد حركات مالية مسجلة لهذا الشهر")
        }

        result.put(
            "reconciliation",
            if (report.reconciliation.reconciled) "مطابق وموزون مع دفتر الأستاذ IFRS" else "يوجد فرق تدقيق"
        )
        return result
    }

    private suspend fun executeCashFlowStatement(args: JSONObject): JSONObject {
        val today = AppClock.todayLocalDate()
        val startDateStr = args.optString("start_date", "").trim()
        val endDateStr = args.optString("end_date", "").trim()

        val fromDate = runCatching { LocalDate.parse(startDateStr) }.getOrNull()
            ?: LocalDate.of(today.year, today.monthValue, 1)
        val toDate = runCatching { LocalDate.parse(endDateStr) }.getOrNull()
            ?: fromDate.plusMonths(1).minusDays(1)

        val fromEpoch = fromDate.toEpochDay()
        val toEpoch = toDate.toEpochDay()

        val statement = generateCashFlowStatement(db = db, fromEpochDay = fromEpoch, toEpochDay = toEpoch)

        val result = JSONObject()
        result.put("period", "$fromDate إلى $toDate")
        result.put("openingCash", Money(statement.openingCashMinor, CurrencyCode.YER).format())
        result.put("cashReceiptsOperating", Money(statement.operatingSection.totalInflowsMinor, CurrencyCode.YER).format())
        result.put("cashDisbursementsOperating", Money(statement.operatingSection.totalOutflowsMinor, CurrencyCode.YER).format())
        result.put("netOperatingCashFlow", Money(statement.netOperatingCashFlowMinor, CurrencyCode.YER).format())
        result.put("netInvestingCashFlow", Money(statement.netInvestingCashFlowMinor, CurrencyCode.YER).format())
        result.put("netFinancingCashFlow", Money(statement.netFinancingCashFlowMinor, CurrencyCode.YER).format())
        result.put("fxImpactOnCash", Money(statement.fxImpactOnCashMinor, CurrencyCode.YER).format())
        result.put("netChangeInCash", Money(statement.netChangeInCashMinor, CurrencyCode.YER).format())
        result.put("closingCashBalance", Money(statement.closingCashMinor, CurrencyCode.YER).format())
        result.put("vaultCashClosing", Money(statement.vaultCashClosingMinor, CurrencyCode.YER).format())
        result.put("bankCashClosing", Money(statement.bankCashClosingMinor, CurrencyCode.YER).format())
        result.put(
            "reconciliation",
            if (statement.reconciliation.reconciled) "مطابق وموزون مع أرصدة الخزينة والحسابات النقدية" else "يوجد فرق تدقيق"
        )

        return result
    }

    private suspend fun executePartyLedger(args: JSONObject): JSONObject {
        val partyId = args.optString("party_id", "").trim().takeIf { it.isNotBlank() }
        val status = args.optString("status", "").trim().lowercase()

        val result = JSONObject()

        if (partyId != null) {
            val party = db.partyDao().getPartyById(partyId)
            if (party == null) {
                result.put("error", "الجهة برقم $partyId غير موجودة في النظام")
                return result
            }

            val controlAccount = if (party.isCustomer) AccountConstants.ACCOUNTS_RECEIVABLE else AccountConstants.ACCOUNTS_PAYABLE
            val statement = statementOfAccountUseCase.generateStatement(partyId = partyId, controlAccountCode = controlAccount)

            result.put("partyId", party.id)
            result.put("partyName", party.name)
            result.put("partyType", if (party.isCustomer) "عميل / وكيل" else if (party.isVendor) "مورد" else "شريك / جهة")
            result.put("phone", party.phone)
            result.put("openingBalance", Money(statement.openingBalanceMinor, CurrencyCode.YER).format())
            result.put("totalDebit", Money(statement.totalDebitMinor, CurrencyCode.YER).format())
            result.put("totalCredit", Money(statement.totalCreditMinor, CurrencyCode.YER).format())
            result.put("closingBalance", Money(statement.closingBalanceMinor, CurrencyCode.YER).format())
            result.put("isDebitNormal", party.isCustomer)
            result.put("transactionsCount", statement.items.size)

            val recentItems = JSONArray()
            statement.items.takeLast(10).forEach { item ->
                recentItems.put(JSONObject().apply {
                    put("date", LocalDate.ofEpochDay(item.entryDateEpochDay).toString())
                    put("docType", item.docType)
                    put("docNumber", item.docNumber)
                    put("memo", item.memo)
                    put("debit", Money(item.baseDebitMinor, CurrencyCode.YER).format())
                    put("credit", Money(item.baseCreditMinor, CurrencyCode.YER).format())
                    put("balance", Money(item.runningBalanceMinor, CurrencyCode.YER).format())
                })
            }
            result.put("recentTransactions", recentItems)
        } else {
            // General debtors & creditors summary
            val receivables = db.journalDao().getPartyBalancesForControlAccount(AccountConstants.ACCOUNTS_RECEIVABLE)
            val payables = db.journalDao().getPartyBalancesForControlAccount(AccountConstants.ACCOUNTS_PAYABLE)

            val totalReceivablesMinor = receivables.sumOf { it.netBalanceMinor }
            val totalPayablesMinor = payables.sumOf { it.totalCreditMinor - it.totalDebitMinor }

            result.put("totalReceivables", Money(totalReceivablesMinor, CurrencyCode.YER).format())
            result.put("totalPayables", Money(totalPayablesMinor, CurrencyCode.YER).format())
            result.put("activeCustomersCount", receivables.count { it.netBalanceMinor != 0L })
            result.put("activeVendorsCount", payables.count { (it.totalCreditMinor - it.totalDebitMinor) != 0L })

            val breakdown = JSONArray()
            val includeReceivables = status.isEmpty() || status == "all" || status == "receivables" || status == "customers"
            val includePayables = status.isEmpty() || status == "all" || status == "payables" || status == "vendors"

            if (includeReceivables) {
                receivables.filter { it.netBalanceMinor != 0L }.sortedByDescending { it.netBalanceMinor }.take(15).forEach { row ->
                    breakdown.put(JSONObject().apply {
                        put("partyId", row.partyId)
                        put("partyName", row.partyName)
                        put("type", "مدين (عميل)")
                        put("balance", Money(row.netBalanceMinor, CurrencyCode.YER).format())
                    })
                }
            }

            if (includePayables) {
                payables.filter { (it.totalCreditMinor - it.totalDebitMinor) != 0L }
                    .sortedByDescending { it.totalCreditMinor - it.totalDebitMinor }.take(15).forEach { row ->
                        val payableNet = row.totalCreditMinor - row.totalDebitMinor
                        breakdown.put(JSONObject().apply {
                            put("partyId", row.partyId)
                            put("partyName", row.partyName)
                            put("type", "دائن (مورد)")
                            put("balance", Money(payableNet, CurrencyCode.YER).format())
                        })
                    }
            }

            result.put("balancesList", breakdown)
        }

        return result
    }

    private suspend fun executeAssetsSummary(@Suppress("UNUSED_PARAMETER") args: JSONObject): JSONObject {
        val assets = db.assetDao().getAllAssetsSync()
        val activeAssets = assets.filter { !it.isDisposed }

        val totalCostMinor = activeAssets.sumOf { it.purchaseCostMinor }
        val totalDeprecMinor = activeAssets.sumOf { it.accumulatedDepreciationMinor }
        val netBookValueMinor = totalCostMinor - totalDeprecMinor

        val result = JSONObject()
        result.put("totalAssetsCount", assets.size)
        result.put("activeAssetsCount", activeAssets.size)
        result.put("disposedAssetsCount", assets.count { it.isDisposed })
        result.put("totalPurchaseCost", Money(totalCostMinor, CurrencyCode.YER).format())
        result.put("totalAccumulatedDepreciation", Money(totalDeprecMinor, CurrencyCode.YER).format())
        result.put("totalNetBookValue", Money(netBookValueMinor, CurrencyCode.YER).format())

        val assetList = JSONArray()
        activeAssets.take(20).forEach { asset ->
            val bookValue = asset.purchaseCostMinor - asset.accumulatedDepreciationMinor
            assetList.put(JSONObject().apply {
                put("id", asset.id)
                put("name", asset.name)
                put("cost", Money(asset.purchaseCostMinor, CurrencyCode.YER).format())
                put("accumulatedDepreciation", Money(asset.accumulatedDepreciationMinor, CurrencyCode.YER).format())
                put("netBookValue", Money(bookValue, CurrencyCode.YER).format())
                put("usefulLifeMonths", asset.usefulLifeMonths)
                put("purchaseDate", LocalDate.ofEpochDay(asset.purchaseDateEpochDay).toString())
            })
        }
        result.put("activeAssets", assetList)

        return result
    }

    private suspend fun executeNetworkDevicesStatus(@Suppress("UNUSED_PARAMETER") args: JSONObject): JSONObject {
        val devices = networkRepository.getAllDevices()

        val onlineCount = devices.count { it.status == DeviceStatus.ONLINE }
        val warningCount = devices.count { it.status == DeviceStatus.WARNING }
        val offlineCount = devices.count { it.status == DeviceStatus.OFFLINE }

        val result = JSONObject()
        result.put("totalDevicesCount", devices.size)
        result.put("onlineCount", onlineCount)
        result.put("warningCount", warningCount)
        result.put("offlineCount", offlineCount)

        val devicesArray = JSONArray()
        devices.forEach { device ->
            devicesArray.put(JSONObject().apply {
                put("id", device.id)
                put("name", device.name)
                put("ip", device.ipAddress)
                put("type", device.deviceType.labelArabic)
                put("status", device.status.labelArabic)
                put("location", device.towerLocation)
                put("model", device.model)
                put("notes", device.notes)
            })
        }
        result.put("devicesList", devicesArray)

        // Offline / Warning attention list
        val attentionArray = JSONArray()
        devices.filter { it.status != DeviceStatus.ONLINE }.forEach { device ->
            attentionArray.put(JSONObject().apply {
                put("name", device.name)
                put("ip", device.ipAddress)
                put("status", device.status.labelArabic)
                put("location", device.towerLocation)
            })
        }
        result.put("offlineOrWarningAlerts", attentionArray)

        return result
    }
}
