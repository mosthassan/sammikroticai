package com.example.util

import android.content.Context
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.domain.report.CashFlowStatement
import com.example.domain.report.DailyCashRegisterStatement
import com.example.domain.report.MonthlyProfitReport
import com.example.domain.report.StatementOfChangesInEquityReport
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale

/**
 * Standard-compliant CSV Exporter for SamMikrotik Financial Reports.
 * Follows strict financial data export specifications:
 * 1. UTF-8 with Byte Order Mark (BOM: \uFEFF) to guarantee Arabic text renders correctly in Microsoft Excel & Google Sheets.
 * 2. Standard decimal numbers (period separator '.', strictly NO thousands commas) using integer arithmetic (zero Double/Float).
 * 3. Proper RFC 4180 CSV escaping for fields containing commas, quotes, or newlines.
 */
object ReportsCsvGenerator {

    private const val UTF8_BOM = "\uFEFF"

    private fun getDocsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "docs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Converts minor units (cents / 100) into a standard decimal string (e.g. "1234.50" or "-50.00").
     * Strictly avoids Double and Float.
     */
    fun formatDecimal(minor: Long): String {
        val isNeg = minor < 0L
        val absMinor = kotlin.math.abs(minor)
        val wholePart = absMinor / 100L
        val fractionPart = absMinor % 100L
        val sign = if (isNeg) "-" else ""
        return "$sign$wholePart.${String.format(Locale.US, "%02d", fractionPart)}"
    }

    /**
     * Converts basis points (100 bp = 1%) into a decimal percent string (e.g. 5000 bp -> "50.00%").
     */
    fun formatBasisPoints(basisPoints: Int): String {
        val whole = basisPoints / 100
        val frac = kotlin.math.abs(basisPoints % 100)
        return "$whole.${String.format(Locale.US, "%02d", frac)}%"
    }

    /**
     * RFC 4180 CSV value escaper.
     */
    fun escape(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    private fun writeCsvFile(context: Context, filenamePrefix: String, content: String): File {
        val docsDir = getDocsDirectory(context)
        val file = File(docsDir, "${filenamePrefix}_${System.currentTimeMillis()}.csv")
        FileOutputStream(file).use { out ->
            out.write(UTF8_BOM.toByteArray(Charsets.UTF_8))
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        }
        return file
    }

    // =========================================================================
    // 1. MONTHLY PROFIT REPORT CSV
    // =========================================================================
    fun generateMonthlyProfitCsv(context: Context, report: MonthlyProfitReport): File {
        val sb = StringBuilder()

        // Header metadata
        sb.appendLine("${escape("تقرير الأرباح والخسائر الشهرية (12 شهراً)")},${escape("السنة المالية: ${report.fiscalYear}")}")
        sb.appendLine()

        // Column headers
        sb.append(escape("الشهر")).append(",")
        sb.append(escape("من تاريخ")).append(",")
        sb.append(escape("إلى تاريخ")).append(",")
        sb.append(escape("إيرادات الكروت")).append(",")
        sb.append(escape("إيرادات الخدمات")).append(",")
        sb.append(escape("مردودات المبيعات")).append(",")
        sb.append(escape("إيرادات أخرى")).append(",")
        sb.append(escape("صافي الإيرادات")).append(",")
        sb.append(escape("تكلفة الإنترنت المباشرة")).append(",")
        sb.append(escape("إجمالي الربح")).append(",")
        sb.append(escape("هامش الربح الإجمالي")).append(",")
        sb.append(escape("المصاريف التشغيلية")).append(",")
        sb.append(escape("مصاريف الصيانة")).append(",")
        sb.append(escape("الرواتب والأجور")).append(",")
        sb.append(escape("مصاريف متنوعة")).append(",")
        sb.append(escape("مصاريف أخرى")).append(",")
        sb.append(escape("إجمالي المصاريف")).append(",")
        sb.append(escape("إهلاك الأصول")).append(",")
        sb.append(escape("أرباح صرف محققة")).append(",")
        sb.append(escape("خسائر صرف محققة")).append(",")
        sb.append(escape("أرباح تشغيلية محققة")).append(",")
        sb.append(escape("أرباح صرف غير محققة")).append(",")
        sb.append(escape("خسائر صرف غير محققة")).append(",")
        sb.append(escape("صافي الربح النهائي")).append(",")
        sb.append(escape("هامش الربح الصافي")).append(",")
        sb.append(escape("الربح التراكمي")).append(",")
        sb.appendLine(escape("حالة الشهر"))

        // Months data
        for (m in report.months) {
            sb.append(escape(m.monthName)).append(",")
            sb.append(escape(LocalDate.ofEpochDay(m.fromEpochDay).toString())).append(",")
            sb.append(escape(LocalDate.ofEpochDay(m.toEpochDay).toString())).append(",")
            sb.append(formatDecimal(m.cardRevenueMinor)).append(",")
            sb.append(formatDecimal(m.serviceRevenueMinor)).append(",")
            sb.append(formatDecimal(m.salesReturnsMinor)).append(",")
            sb.append(formatDecimal(m.otherRevenueMinor)).append(",")
            sb.append(formatDecimal(m.netRevenueMinor)).append(",")
            sb.append(formatDecimal(m.directIspCostMinor)).append(",")
            sb.append(formatDecimal(m.grossProfitMinor)).append(",")
            sb.append(escape(formatBasisPoints(m.grossMarginBasisPoints))).append(",")
            sb.append(formatDecimal(m.operatingExpensesMinor)).append(",")
            sb.append(formatDecimal(m.maintenanceExpensesMinor)).append(",")
            sb.append(formatDecimal(m.salariesExpensesMinor)).append(",")
            sb.append(formatDecimal(m.miscExpensesMinor)).append(",")
            sb.append(formatDecimal(m.otherExpensesMinor)).append(",")
            sb.append(formatDecimal(m.totalOperatingExpensesMinor)).append(",")
            sb.append(formatDecimal(m.depreciationExpenseMinor)).append(",")
            sb.append(formatDecimal(m.realizedFxGainMinor)).append(",")
            sb.append(formatDecimal(m.realizedFxLossMinor)).append(",")
            sb.append(formatDecimal(m.realizedOperatingProfitMinor)).append(",")
            sb.append(formatDecimal(m.unrealizedFxGainMinor)).append(",")
            sb.append(formatDecimal(m.unrealizedFxLossMinor)).append(",")
            sb.append(formatDecimal(m.netProfitMinor)).append(",")
            sb.append(escape(formatBasisPoints(m.netMarginBasisPoints))).append(",")
            sb.append(formatDecimal(m.cumulativeProfitMinor)).append(",")
            sb.appendLine(escape(m.status.status.title))
        }

        // Year Totals Row
        sb.append(escape("الإجمالي السنوي")).append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append(formatDecimal(report.yearTotalNetRevenueMinor)).append(",")
        sb.append(formatDecimal(report.yearTotalDirectIspCostMinor)).append(",")
        sb.append(formatDecimal(report.yearTotalGrossProfitMinor)).append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append(formatDecimal(report.yearTotalOperatingExpensesMinor)).append(",")
        sb.append(formatDecimal(report.yearTotalDepreciationMinor)).append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append(formatDecimal(report.yearTotalNetProfitMinor)).append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.appendLine("")

        // Partner Profit Shares section
        if (report.partnerShares.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(escape("--- الحصص التقديرية للشركاء من صافي الأرباح السنوية ---"))
            sb.appendLine("${escape("اسم الشريك")},${escape("نسبة الملكية")},${escape("الحصة التقديرية (ريال يمني)")}")
            for (ps in report.partnerShares) {
                sb.appendLine("${escape(ps.partnerName)},${escape(formatBasisPoints(ps.equityPercentageBasisPoints))},${formatDecimal(ps.estimatedShareMinor)}")
            }
        }

        // Reconciliation row
        sb.appendLine()
        val recStatus = if (report.reconciliation.reconciled) "مطابق وموزون" else "غير مطابق"
        val recMsg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        sb.appendLine("${escape("المطابقة المحاسبية")},${escape(recStatus)},${escape(recMsg)}")

        return writeCsvFile(context, "monthly_profit_${report.fiscalYear}", sb.toString())
    }

    // =========================================================================
    // 2. STATEMENT OF CHANGES IN EQUITY CSV
    // =========================================================================
    fun generateChangesInEquityCsv(context: Context, report: StatementOfChangesInEquityReport): File {
        val sb = StringBuilder()

        val fromStr = LocalDate.ofEpochDay(report.fromEpochDay).toString()
        val toStr = LocalDate.ofEpochDay(report.toEpochDay).toString()
        sb.appendLine("${escape("قائمة التغيرات في حقوق الملكية")},${escape("من: $fromStr")},${escape("إلى: $toStr")}")
        sb.appendLine()

        // 1. Equity Movement Matrix
        sb.appendLine(escape("--- مصفوفة حركة حقوق الملكية ---"))
        sb.append(escape("بيان الحركة")).append(",")
        sb.append(escape("رأس المال (3101)")).append(",")
        sb.append(escape("جاري الشركاء (3201)")).append(",")
        sb.append(escape("الأرباح المبقاة (3301)")).append(",")
        sb.append(escape("أرباح الفترة (3302)")).append(",")
        sb.appendLine(escape("إجمالي حقوق الملكية"))

        for (row in report.rows) {
            sb.append(escape(row.label)).append(",")
            sb.append(formatDecimal(row.capitalMinor)).append(",")
            sb.append(formatDecimal(row.currentMinor)).append(",")
            sb.append(formatDecimal(row.retainedEarningsMinor)).append(",")
            sb.append(formatDecimal(row.periodProfitMinor)).append(",")
            sb.appendLine(formatDecimal(row.totalMinor))
        }

        // 2. Partner Summary Breakdown
        if (report.partnerSummaries.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(escape("--- تفصيل حقوق كل شريك ---"))
            sb.append(escape("معرف الشريك")).append(",")
            sb.append(escape("اسم الشريك")).append(",")
            sb.append(escape("نسبة الملكية")).append(",")
            sb.append(escape("رصيد أول رأس المال")).append(",")
            sb.append(escape("رصيد أول جاري")).append(",")
            sb.append(escape("مساهمات نقدية")).append(",")
            sb.append(escape("مساهمات عينية")).append(",")
            sb.append(escape("مدفوع بالنيابة")).append(",")
            sb.append(escape("مسحوبات الشريك")).append(",")
            sb.append(escape("أرباح مستلمة")).append(",")
            sb.append(escape("أرباح الفترة المقدرة")).append(",")
            sb.append(escape("رصيد نهاية رأس المال")).append(",")
            sb.append(escape("رصيد نهاية جاري")).append(",")
            sb.appendLine(escape("إجمالي حقوق الشريك"))

            for (p in report.partnerSummaries) {
                sb.append(escape(p.partnerPartyId)).append(",")
                sb.append(escape(p.partnerName)).append(",")
                sb.append(escape(formatBasisPoints(p.equityPercentageBasisPoints))).append(",")
                sb.append(formatDecimal(p.capitalOpeningMinor)).append(",")
                sb.append(formatDecimal(p.currentOpeningMinor)).append(",")
                sb.append(formatDecimal(p.cashContributionsMinor)).append(",")
                sb.append(formatDecimal(p.inKindContributionsMinor)).append(",")
                sb.append(formatDecimal(p.paidOnBehalfMinor)).append(",")
                sb.append(formatDecimal(p.withdrawalsMinor)).append(",")
                sb.append(formatDecimal(p.dividendsReceivedMinor)).append(",")
                sb.append(formatDecimal(p.estimatedPeriodProfitMinor)).append(",")
                sb.append(formatDecimal(p.capitalClosingMinor)).append(",")
                sb.append(formatDecimal(p.currentClosingMinor)).append(",")
                sb.appendLine(formatDecimal(p.totalPartnerEquityMinor))
            }
        }

        // Reconciliation
        sb.appendLine()
        val recStatus = if (report.reconciliation.reconciled) "مطابق وموزون" else "غير مطابق"
        val recMsg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        sb.appendLine("${escape("المطابقة مع ميزان المراجعة والأستاذ")},${escape(recStatus)},${escape(recMsg)}")

        return writeCsvFile(context, "changes_in_equity", sb.toString())
    }

    // =========================================================================
    // 3. CASH FLOW STATEMENT CSV
    // =========================================================================
    fun generateCashFlowStatementCsv(context: Context, report: CashFlowStatement): File {
        val sb = StringBuilder()

        val fromStr = LocalDate.ofEpochDay(report.fromEpochDay).toString()
        val toStr = LocalDate.ofEpochDay(report.toEpochDay).toString()
        sb.appendLine("${escape("قائمة التدفقات النقدية (المعيار المحاسبي الدولي IAS 7 - الطريقة المباشرة)")},${escape("من: $fromStr")},${escape("إلى: $toStr")}")
        sb.appendLine()

        sb.append(escape("النشاط")).append(",")
        sb.append(escape("الفئة / البيان")).append(",")
        sb.append(escape("التدفق الداخل (وارد)")).append(",")
        sb.append(escape("التدفق الخارج (منصرف)")).append(",")
        sb.appendLine(escape("صافي التدفق"))

        // Operating Section
        for (cat in report.operatingSection.categories) {
            sb.append(escape("الأنشطة التشغيلية")).append(",")
            sb.append(escape(cat.category.titleAr)).append(",")
            sb.append(formatDecimal(cat.totalInflowMinor)).append(",")
            sb.append(formatDecimal(cat.totalOutflowMinor)).append(",")
            sb.appendLine(formatDecimal(cat.netFlowMinor))
        }
        sb.appendLine("${escape("صافي التدفقات النقدية من الأنشطة التشغيلية")},,,,${formatDecimal(report.netOperatingCashFlowMinor)}")
        sb.appendLine()

        // Investing Section
        for (cat in report.investingSection.categories) {
            sb.append(escape("الأنشطة الاستثمارية")).append(",")
            sb.append(escape(cat.category.titleAr)).append(",")
            sb.append(formatDecimal(cat.totalInflowMinor)).append(",")
            sb.append(formatDecimal(cat.totalOutflowMinor)).append(",")
            sb.appendLine(formatDecimal(cat.netFlowMinor))
        }
        sb.appendLine("${escape("صافي التدفقات النقدية من الأنشطة الاستثمارية")},,,,${formatDecimal(report.netInvestingCashFlowMinor)}")
        sb.appendLine()

        // Financing Section
        for (cat in report.financingSection.categories) {
            sb.append(escape("الأنشطة التمويلية")).append(",")
            sb.append(escape(cat.category.titleAr)).append(",")
            sb.append(formatDecimal(cat.totalInflowMinor)).append(",")
            sb.append(formatDecimal(cat.totalOutflowMinor)).append(",")
            sb.appendLine(formatDecimal(cat.netFlowMinor))
        }
        sb.appendLine("${escape("صافي التدفقات النقدية من الأنشطة التمويلية")},,,,${formatDecimal(report.netFinancingCashFlowMinor)}")
        sb.appendLine()

        // FX Impact
        sb.appendLine("${escape("أثر تغيرات أسعار الصرف على النقدية")},,,,${formatDecimal(report.fxImpactOnCashMinor)}")
        sb.appendLine()

        // Summary & Reconciliation
        sb.appendLine(escape("--- خلاصة حركة ومطابقة النقدية ---"))
        sb.appendLine("${escape("صافي التغير في النقدية خلال الفترة")},,,,${formatDecimal(report.netChangeInCashMinor)}")
        sb.appendLine("${escape("النقدية وما في حكمها في بداية الفترة")},,,,${formatDecimal(report.openingCashMinor)}")
        sb.appendLine("${escape("النقدية وما في حكمها في نهاية الفترة")},,,,${formatDecimal(report.closingCashMinor)}")
        sb.appendLine("${escape("رصيد الصناديق والخزائن (حساب 1101)")},,,,${formatDecimal(report.vaultCashClosingMinor)}")
        sb.appendLine("${escape("رصيد الحسابات البنكية (حساب 1102)")},,,,${formatDecimal(report.bankCashClosingMinor)}")

        val recStatus = if (report.reconciliation.reconciled) "مطابق وموزون تماماً" else "غير مطابق"
        val recMsg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        sb.appendLine("${escape("حالة مطابقة النقدية مع الأستاذ العام")},${escape(recStatus)},${escape(recMsg)}")

        // Non-Cash Disclosures
        if (report.nonCashDisclosures.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(escape("--- إفصاح المعاملات الاستثمارية والتمويلية غير النقدية ---"))
            sb.append(escape("التاريخ")).append(",")
            sb.append(escape("رقم المستند")).append(",")
            sb.append(escape("النوع")).append(",")
            sb.append(escape("الطرف")).append(",")
            sb.append(escape("البيان")).append(",")
            sb.append(escape("الحساب المستهدف")).append(",")
            sb.appendLine(escape("المبلغ"))

            for (nc in report.nonCashDisclosures) {
                sb.append(escape(LocalDate.ofEpochDay(nc.dateEpochDay).toString())).append(",")
                sb.append(escape(nc.docNumber)).append(",")
                sb.append(escape(nc.typeAr)).append(",")
                sb.append(escape(nc.partyName)).append(",")
                sb.append(escape(nc.description)).append(",")
                sb.append(escape(nc.targetAccount)).append(",")
                sb.appendLine(formatDecimal(nc.amountMinor))
            }
        }

        return writeCsvFile(context, "cash_flow_statement", sb.toString())
    }

    // =========================================================================
    // 4. DAILY CASH REGISTER CSV
    // =========================================================================
    fun generateDailyCashRegisterCsv(context: Context, report: DailyCashRegisterStatement): File {
        val sb = StringBuilder()

        val fromStr = LocalDate.ofEpochDay(report.startDateEpochDay).toString()
        val toStr = LocalDate.ofEpochDay(report.endDateEpochDay).toString()

        sb.appendLine("${escape("سجل الصندوق اليومي")},${escape(report.treasuryName)},${escape("العملة: ${report.currency}")}")
        sb.appendLine("${escape("الحساب الدفتري")},${escape("${report.glAccountCode} - ${report.glAccountName}")},${escape("من: $fromStr")},${escape("إلى: $toStr")}")
        sb.appendLine("${escape("الرصيد الافتتاحي (عملة الأصل)")},${formatDecimal(report.openingBalanceOrigMinor)},${escape("الرصيد الافتتاحي (ريال يمني)")},${formatDecimal(report.openingBalanceBaseMinor)}")
        sb.appendLine()

        // Table Header
        sb.append(escape("التاريخ")).append(",")
        sb.append(escape("رقم القيد")).append(",")
        sb.append(escape("رقم المستند")).append(",")
        sb.append(escape("نوع المستند")).append(",")
        sb.append(escape("الطرف / العميل")).append(",")
        sb.append(escape("البيان")).append(",")
        sb.append(escape("الوارد (${report.currency})")).append(",")
        sb.append(escape("المنصرف (${report.currency})")).append(",")
        sb.append(escape("الرصيد الجاري (${report.currency})")).append(",")
        sb.append(escape("سعر الصرف")).append(",")
        sb.append(escape("الوارد (ر.ي)")).append(",")
        sb.append(escape("المنصرف (ر.ي)")).append(",")
        sb.appendLine(escape("الرصيد الجاري (ر.ي)"))

        // Lines
        for (line in report.lines) {
            val dateStr = line.dateFormatted
            val rateStr = if (line.exchangeRateMicros != com.example.core.model.ExchangeRate.SCALE_MICROS) {
                val whole = line.exchangeRateMicros / com.example.core.model.ExchangeRate.SCALE_MICROS
                val frac = (line.exchangeRateMicros % com.example.core.model.ExchangeRate.SCALE_MICROS) / 10_000L
                if (frac == 0L) "$whole" else String.format(Locale.US, "%d.%02d", whole, frac)
            } else {
                "1.00"
            }
            sb.append(escape(dateStr)).append(",")
            sb.append(escape(line.entryId.take(8))).append(",")
            sb.append(escape(line.docNumber)).append(",")
            sb.append(escape(line.docTypeAr)).append(",")
            sb.append(escape(line.partyName ?: "")).append(",")
            sb.append(escape(line.description)).append(",")
            sb.append(formatDecimal(line.inflowOrigMinor)).append(",")
            sb.append(formatDecimal(line.outflowOrigMinor)).append(",")
            sb.append(formatDecimal(line.runningBalanceOrigMinor)).append(",")
            sb.append(escape(rateStr)).append(",")
            sb.append(formatDecimal(line.inflowBaseMinor)).append(",")
            sb.append(formatDecimal(line.outflowBaseMinor)).append(",")
            sb.appendLine(formatDecimal(line.runningBalanceBaseMinor))
        }

        // Period Totals
        sb.appendLine()
        sb.append(escape("إجمالي حركات الفترة")).append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append("").append(",")
        sb.append(formatDecimal(report.totalInflowOrigMinor)).append(",")
        sb.append(formatDecimal(report.totalOutflowOrigMinor)).append(",")
        sb.append(formatDecimal(report.netChangeOrigMinor)).append(",")
        sb.append("").append(",")
        sb.append(formatDecimal(report.totalInflowBaseMinor)).append(",")
        sb.append(formatDecimal(report.totalOutflowBaseMinor)).append(",")
        sb.appendLine(formatDecimal(report.netChangeBaseMinor))

        // Closing Balances
        sb.appendLine("${escape("الرصيد الختامي (عملة الأصل)")},,,,,,${formatDecimal(report.closingBalanceOrigMinor)}")
        sb.appendLine("${escape("الرصيد الختامي الدفتري (ريال يمني)")},,,,,,,,,,,${formatDecimal(report.closingBalanceBaseMinor)}")

        // Value Estimation Note
        sb.appendLine()
        sb.appendLine("${escape("ملاحظة القيمة التقديرية (غير مقيدة)")},${escape(report.valueEstimation.noteTextAr)}")

        // Reconciliation
        sb.appendLine()
        val rec = report.reconciliation
        val origCheckStr = if (rec.isOrigReconciled) "مطابق لرصيد الخزينة" else "غير مطابق (فارق: ${formatDecimal(rec.origDifferenceMinor)})"
        val baseCheckStr = if (rec.isBaseReconciled) "مطابق لحساب الأستاذ العام" else "غير مطابق (فارق: ${formatDecimal(rec.baseDifferenceMinor)})"
        val fullCheckStr = if (rec.isFullyReconciled) "مطابق وموثق بالكامل" else "يوجد فروقات محاسبية"

        sb.appendLine("${escape("مطابقة عملة الأصل")},${escape(origCheckStr)}")
        sb.appendLine("${escape("مطابقة عملة الأساس")},${escape(baseCheckStr)}")
        sb.appendLine("${escape("الرقابة والمطابقة النهائية")},${escape(fullCheckStr)},${escape(rec.statusMessageAr)}")

        return writeCsvFile(context, "daily_cash_register_${report.treasuryId}", sb.toString())
    }
}
