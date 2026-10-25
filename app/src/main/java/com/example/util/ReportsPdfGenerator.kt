package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.domain.report.CashFlowStatement
import com.example.domain.report.DailyCashRegisterStatement
import com.example.domain.report.MonthlyProfitReport
import com.example.domain.report.StatementOfChangesInEquityReport
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Pixel-Perfect Arabic PDF Exporter for SamMikrotik Financial Reports.
 * Generates:
 * 1. Monthly Profit & Loss Statement (قائمة الأرباح والخسائر الشهرية - 12 شهراً)
 * 2. Statement of Changes in Equity (قائمة التغيرات في حقوق الملكية)
 * 3. Cash Flow Statement (قائمة التدفقات النقدية - IAS 7)
 * 4. Daily Cash Register (سجل الصندوق اليومي)
 */
object ReportsPdfGenerator {

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 36
    private const val MARGIN_RIGHT = 559
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 523

    // Brand Palette
    private const val COLOR_NAVY_DARK = 0xFF0B192C.toInt()
    private const val COLOR_NAVY_MEDIUM = 0xFF1E293B.toInt()
    private const val COLOR_CYAN_ACCENT = 0xFF00E5FF.toInt()
    private const val COLOR_BORDER = 0xFFCBD5E1.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FAFC.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFF0F172A.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_SUCCESS = 0xFF10B981.toInt()
    private const val COLOR_DANGER = 0xFFEF4444.toInt()
    private const val COLOR_WARNING = 0xFFF59E0B.toInt()

    private fun getDocsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "docs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // =========================================================================
    // 1. MONTHLY PROFIT REPORT PDF
    // =========================================================================
    fun generateMonthlyProfitPdf(
        context: Context,
        report: MonthlyProfitReport,
        organizationName: String = "شبكة طلقة نت - SamMikrotik",
        organizationContact: String = "هاتف: 777000000 - 733000000"
    ): File {
        val docsDir = getDocsDirectory(context)
        val file = File(docsDir, "monthly_profit_${report.fiscalYear}_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        drawHeaderBanner(canvas, organizationName, "تقرير الأرباح والخسائر الشهرية", "MONTHLY PROFIT & LOSS STATEMENT", organizationContact)

        var currentY = 94f

        // Details / KPI Card
        currentY = drawKpiBox(
            canvas = canvas,
            startY = currentY,
            title = "ملخص السنة المالية ${report.fiscalYear}",
            kpis = listOf(
                Pair("صافي الإيرادات", Money(report.yearTotalNetRevenueMinor, CurrencyCode.YER).format()),
                Pair("تكلفة الإنترنت المباشرة", Money(report.yearTotalDirectIspCostMinor, CurrencyCode.YER).format()),
                Pair("إجمالي الربح", Money(report.yearTotalGrossProfitMinor, CurrencyCode.YER).format()),
                Pair("المصاريف التشغيلية", Money(report.yearTotalOperatingExpensesMinor, CurrencyCode.YER).format()),
                Pair("صافي الربح السنوي", Money(report.yearTotalNetProfitMinor, CurrencyCode.YER).format())
            )
        )

        currentY += 12f

        // Table Header
        currentY = drawTableHeader(
            canvas = canvas,
            startY = currentY,
            columns = listOf(
                Pair("الشهر", 60f),
                Pair("صافي الإيراد", 75f),
                Pair("تكلفة النت", 70f),
                Pair("مجمل الربح", 75f),
                Pair("المصاريف", 70f),
                Pair("صافي الربح", 85f),
                Pair("الحالة", 88f)
            )
        )

        val rowH = 22f
        val maxY = 700f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        report.months.forEachIndexed { idx, m ->
            if (currentY + rowH > maxY) {
                drawPageFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderBanner(canvas, organizationName, "تقرير الأرباح والخسائر الشهرية (تابع)", "MONTHLY PROFIT (CONT.)", organizationContact)
                currentY = 94f
                currentY = drawTableHeader(
                    canvas = canvas,
                    startY = currentY,
                    columns = listOf(
                        Pair("الشهر", 60f),
                        Pair("صافي الإيراد", 75f),
                        Pair("تكلفة النت", 70f),
                        Pair("مجمل الربح", 75f),
                        Pair("المصاريف", 70f),
                        Pair("صافي الربح", 85f),
                        Pair("الحالة", 88f)
                    )
                )
            }

            // Draw row background
            if (idx % 2 == 1) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
            }
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)

            var colX = MARGIN_RIGHT.toFloat()
            // الشهر
            colX -= 60f
            drawText(canvas, m.monthName, colX, currentY + 4f, 56, 9.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER, true)
            // صافي الإيراد
            colX -= 75f
            drawText(canvas, Money(m.netRevenueMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 71, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER)
            // تكلفة النت
            colX -= 70f
            drawText(canvas, Money(m.directIspCostMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 66, 9f, COLOR_DANGER, Layout.Alignment.ALIGN_CENTER)
            // مجمل الربح
            colX -= 75f
            drawText(canvas, Money(m.grossProfitMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 71, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER)
            // المصاريف
            colX -= 70f
            drawText(canvas, Money(m.totalOperatingExpensesMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 66, 9f, COLOR_DANGER, Layout.Alignment.ALIGN_CENTER)
            // صافي الربح
            colX -= 85f
            val profitColor = if (m.netProfitMinor >= 0) COLOR_SUCCESS else COLOR_DANGER
            drawText(canvas, Money(m.netProfitMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 81, 9.5f, profitColor, Layout.Alignment.ALIGN_CENTER, true)
            // الحالة
            colX -= 88f
            drawText(canvas, m.status.status.title, colX, currentY + 4f, 84, 8f, COLOR_TEXT_MUTED, Layout.Alignment.ALIGN_CENTER)

            currentY += rowH
        }

        currentY += 14f

        // Partner Shares if present
        if (report.partnerShares.isNotEmpty() && currentY < 640f) {
            val partnersSummary = report.partnerShares.joinToString("  |  ") {
                "${it.partnerName}: ${Money(it.estimatedShareMinor, CurrencyCode.YER).format()}"
            }
            currentY = drawInfoCard(canvas, currentY, "الحصص التقديرية للشركاء من أرباح العام:", partnersSummary, COLOR_NAVY_MEDIUM)
            currentY += 10f
        }

        // Reconciliation Badge
        val isRec = report.reconciliation.reconciled
        val msg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        drawReconciliationBadge(canvas, currentY, isRec, msg)

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // =========================================================================
    // 2. STATEMENT OF CHANGES IN EQUITY PDF
    // =========================================================================
    fun generateChangesInEquityPdf(
        context: Context,
        report: StatementOfChangesInEquityReport,
        organizationName: String = "شبكة طلقة نت - SamMikrotik",
        organizationContact: String = "هاتف: 777000000 - 733000000"
    ): File {
        val docsDir = getDocsDirectory(context)
        val file = File(docsDir, "changes_in_equity_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        drawHeaderBanner(canvas, organizationName, "قائمة التغيرات في حقوق الملكية", "STATEMENT OF CHANGES IN EQUITY", organizationContact)

        var currentY = 94f

        // Header KPI box
        val fromDate = LocalDate.ofEpochDay(report.fromEpochDay).toString()
        val toDate = LocalDate.ofEpochDay(report.toEpochDay).toString()
        val closingRow = report.closingRow
        currentY = drawKpiBox(
            canvas = canvas,
            startY = currentY,
            title = "الفترة من $fromDate إلى $toDate",
            kpis = listOf(
                Pair("رأس المال (3101)", Money(closingRow.capitalMinor, CurrencyCode.YER).format()),
                Pair("جاري الشركاء (3201)", Money(closingRow.currentMinor, CurrencyCode.YER).format()),
                Pair("أرباح الفترة (3302)", Money(closingRow.periodProfitMinor, CurrencyCode.YER).format()),
                Pair("إجمالي حقوق الملكية", Money(closingRow.totalMinor, CurrencyCode.YER).format())
            )
        )

        currentY += 12f

        // Table Header
        currentY = drawTableHeader(
            canvas = canvas,
            startY = currentY,
            columns = listOf(
                Pair("بيان حركة حقوق الملكية", 153f),
                Pair("رأس المال", 90f),
                Pair("جاري الشركاء", 90f),
                Pair("أرباح الفترة", 90f),
                Pair("الإجمالي", 100f)
            )
        )

        val rowH = 24f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((idx, row) in report.rows.withIndex()) {
            if (idx % 2 == 1) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
            }
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)

            val isBoldRow = row.rowType == com.example.domain.report.EquityChangeRowType.OPENING_BALANCE ||
                    row.rowType == com.example.domain.report.EquityChangeRowType.CLOSING_BALANCE

            var colX = MARGIN_RIGHT.toFloat()
            // بيان الحركة
            colX -= 153f
            drawText(canvas, row.label, colX + 4f, currentY + 5f, 145, 9.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_NORMAL, isBoldRow)
            // رأس المال
            colX -= 90f
            drawText(canvas, Money(row.capitalMinor, CurrencyCode.YER).format(false), colX, currentY + 5f, 86, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER, isBoldRow)
            // جاري الشركاء
            colX -= 90f
            drawText(canvas, Money(row.currentMinor, CurrencyCode.YER).format(false), colX, currentY + 5f, 86, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER, isBoldRow)
            // أرباح الفترة
            colX -= 90f
            drawText(canvas, Money(row.periodProfitMinor, CurrencyCode.YER).format(false), colX, currentY + 5f, 86, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER, isBoldRow)
            // الإجمالي
            colX -= 100f
            drawText(canvas, Money(row.totalMinor, CurrencyCode.YER).format(true), colX, currentY + 5f, 96, 9.5f, COLOR_NAVY_DARK, Layout.Alignment.ALIGN_CENTER, true)

            currentY += rowH
        }

        currentY += 14f

        // Partner Summaries
        if (report.partnerSummaries.isNotEmpty()) {
            val partnersText = report.partnerSummaries.joinToString("\n") { p ->
                "• ${p.partnerName} (${p.equityPercentageBasisPoints / 100}%): إجمالي الحقوق = ${Money(p.totalPartnerEquityMinor, CurrencyCode.YER).format()}"
            }
            currentY = drawInfoCard(canvas, currentY, "ملخص حقوق الشركاء الفردية:", partnersText, COLOR_NAVY_DARK)
            currentY += 12f
        }

        // Reconciliation
        val isRec = report.reconciliation.reconciled
        val msg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        drawReconciliationBadge(canvas, currentY, isRec, msg)

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // =========================================================================
    // 3. CASH FLOW STATEMENT PDF (IAS 7)
    // =========================================================================
    fun generateCashFlowStatementPdf(
        context: Context,
        report: CashFlowStatement,
        organizationName: String = "شبكة طلقة نت - SamMikrotik",
        organizationContact: String = "هاتف: 777000000 - 733000000"
    ): File {
        val docsDir = getDocsDirectory(context)
        val file = File(docsDir, "cash_flow_statement_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        drawHeaderBanner(canvas, organizationName, "قائمة التدفقات النقدية (IAS 7)", "STATEMENT OF CASH FLOWS", organizationContact)

        var currentY = 94f

        val fromDate = LocalDate.ofEpochDay(report.fromEpochDay).toString()
        val toDate = LocalDate.ofEpochDay(report.toEpochDay).toString()

        currentY = drawKpiBox(
            canvas = canvas,
            startY = currentY,
            title = "حركة النقدية من $fromDate إلى $toDate (الطريقة المباشرة)",
            kpis = listOf(
                Pair("النقدية أول الفترة", Money(report.openingCashMinor, CurrencyCode.YER).format()),
                Pair("صافي التدفق التشغيلي", Money(report.netOperatingCashFlowMinor, CurrencyCode.YER).format()),
                Pair("صافي التدفق الاستثماري", Money(report.netInvestingCashFlowMinor, CurrencyCode.YER).format()),
                Pair("صافي التدفق التمويلي", Money(report.netFinancingCashFlowMinor, CurrencyCode.YER).format()),
                Pair("النقدية آخر الفترة", Money(report.closingCashMinor, CurrencyCode.YER).format())
            )
        )

        currentY += 12f

        // Table Header
        currentY = drawTableHeader(
            canvas = canvas,
            startY = currentY,
            columns = listOf(
                Pair("النشاط والتصنيف المحاسبي", 243f),
                Pair("التدفق الداخل", 90f),
                Pair("التدفق الخارج", 90f),
                Pair("صافي التدفق", 100f)
            )
        )

        val rowH = 20f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Helper to draw activity sections
        fun drawSection(title: String, netFlowMinor: Long, categories: List<com.example.domain.report.CashFlowCategorySummary>) {
            // Section header row
            paint.color = 0xFFE2E8F0.toInt()
            canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
            drawText(canvas, title, MARGIN_RIGHT - 240f, currentY + 4f, 230, 9.5f, COLOR_NAVY_DARK, Layout.Alignment.ALIGN_NORMAL, true)
            val netColor = if (netFlowMinor >= 0) COLOR_SUCCESS else COLOR_DANGER
            drawText(canvas, Money(netFlowMinor, CurrencyCode.YER).format(true), MARGIN_LEFT.toFloat() + 4f, currentY + 4f, 96, 9.5f, netColor, Layout.Alignment.ALIGN_CENTER, true)
            currentY += rowH

            for ((idx, cat) in categories.withIndex()) {
                if (idx % 2 == 1) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
                }
                paint.color = COLOR_BORDER
                paint.strokeWidth = 0.5f
                paint.style = Paint.Style.STROKE
                canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)

                var colX = MARGIN_RIGHT.toFloat()
                colX -= 243f
                drawText(canvas, cat.category.titleAr, colX + 12f, currentY + 4f, 220, 8.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_NORMAL)
                colX -= 90f
                drawText(canvas, if (cat.totalInflowMinor > 0) Money(cat.totalInflowMinor, CurrencyCode.YER).format(false) else "-", colX, currentY + 4f, 86, 8.5f, COLOR_SUCCESS, Layout.Alignment.ALIGN_CENTER)
                colX -= 90f
                drawText(canvas, if (cat.totalOutflowMinor > 0) Money(cat.totalOutflowMinor, CurrencyCode.YER).format(false) else "-", colX, currentY + 4f, 86, 8.5f, COLOR_DANGER, Layout.Alignment.ALIGN_CENTER)
                colX -= 100f
                val cColor = if (cat.netFlowMinor >= 0) COLOR_TEXT_PRIMARY else COLOR_DANGER
                drawText(canvas, Money(cat.netFlowMinor, CurrencyCode.YER).format(false), colX, currentY + 4f, 96, 8.5f, cColor, Layout.Alignment.ALIGN_CENTER)

                currentY += rowH
            }
        }

        drawSection("1. الأنشطة التشغيلية", report.netOperatingCashFlowMinor, report.operatingSection.categories)
        drawSection("2. الأنشطة الاستثمارية", report.netInvestingCashFlowMinor, report.investingSection.categories)
        drawSection("3. الأنشطة التمويلية", report.netFinancingCashFlowMinor, report.financingSection.categories)

        if (report.fxImpactOnCashMinor != 0L) {
            paint.color = COLOR_ROW_ALT
            canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
            drawText(canvas, "4. أثر تقلبات أسعار الصرف على النقدية", MARGIN_RIGHT - 240f, currentY + 4f, 230, 9f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_NORMAL, true)
            drawText(canvas, Money(report.fxImpactOnCashMinor, CurrencyCode.YER).format(true), MARGIN_LEFT.toFloat() + 4f, currentY + 4f, 96, 9f, COLOR_NAVY_DARK, Layout.Alignment.ALIGN_CENTER, true)
            currentY += rowH
        }

        currentY += 10f

        // Vault vs Bank breakdown
        val cashBreakdown = "الخزينة والصناديق (1101): ${Money(report.vaultCashClosingMinor, CurrencyCode.YER).format()}   |   الحسابات البنكية (1102): ${Money(report.bankCashClosingMinor, CurrencyCode.YER).format()}"
        currentY = drawInfoCard(canvas, currentY, "توزيع رصيد النقدية الختامي:", cashBreakdown, COLOR_NAVY_MEDIUM)

        currentY += 10f

        // Reconciliation
        val isRec = report.reconciliation.reconciled
        val msg = "${report.reconciliation.passedCount}/${report.reconciliation.checks.size} فحوصات معتمدة"
        drawReconciliationBadge(canvas, currentY, isRec, msg)

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // =========================================================================
    // 4. DAILY CASH REGISTER PDF
    // =========================================================================
    fun generateDailyCashRegisterPdf(
        context: Context,
        report: DailyCashRegisterStatement,
        organizationName: String = "شبكة طلقة نت - SamMikrotik",
        organizationContact: String = "هاتف: 777000000 - 733000000"
    ): File {
        val docsDir = getDocsDirectory(context)
        val file = File(docsDir, "daily_cash_register_${report.treasuryId}_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        drawHeaderBanner(canvas, organizationName, "سجل الصندوق اليومي", "DAILY CASH REGISTER", organizationContact)

        var currentY = 94f

        val fromDate = LocalDate.ofEpochDay(report.startDateEpochDay).toString()
        val toDate = LocalDate.ofEpochDay(report.endDateEpochDay).toString()
        val currCode = CurrencyCode.fromString(report.currency)

        // KPI Box
        currentY = drawKpiBox(
            canvas = canvas,
            startY = currentY,
            title = "${report.treasuryName} (${report.currency}) - الفترة: $fromDate إلى $toDate",
            kpis = listOf(
                Pair("رصيد أول (${report.currency})", Money(report.openingBalanceOrigMinor, currCode).format()),
                Pair("إجمالي الوارد", Money(report.totalInflowOrigMinor, currCode).format()),
                Pair("إجمالي المنصرف", Money(report.totalOutflowOrigMinor, currCode).format()),
                Pair("رصيد الإغلاق (${report.currency})", Money(report.closingBalanceOrigMinor, currCode).format()),
                Pair("الرصيد الدفتري (ر.ي)", Money(report.closingBalanceBaseMinor, CurrencyCode.YER).format())
            )
        )

        currentY += 12f

        // Table Header
        currentY = drawTableHeader(
            canvas = canvas,
            startY = currentY,
            columns = listOf(
                Pair("التاريخ", 55f),
                Pair("مستند", 45f),
                Pair("الطرف", 80f),
                Pair("البيان", 123f),
                Pair("الوارد", 70f),
                Pair("المنصرف", 70f),
                Pair("الرصيد", 80f)
            )
        )

        val rowH = 20f
        val maxY = 690f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        report.lines.forEachIndexed { idx, line ->
            if (currentY + rowH > maxY) {
                drawPageFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderBanner(canvas, organizationName, "سجل الصندوق اليومي (تابع)", "DAILY CASH REGISTER (CONT.)", organizationContact)
                currentY = 94f
                currentY = drawTableHeader(
                    canvas = canvas,
                    startY = currentY,
                    columns = listOf(
                        Pair("التاريخ", 55f),
                        Pair("مستند", 45f),
                        Pair("الطرف", 80f),
                        Pair("البيان", 123f),
                        Pair("الوارد", 70f),
                        Pair("المنصرف", 70f),
                        Pair("الرصيد", 80f)
                    )
                )
            }

            if (idx % 2 == 1) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)
            }
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(MARGIN_LEFT.toFloat(), currentY, MARGIN_RIGHT.toFloat(), currentY + rowH, paint)

            var colX = MARGIN_RIGHT.toFloat()
            // التاريخ
            colX -= 55f
            val dStr = line.dateFormatted.takeLast(5)
            drawText(canvas, dStr, colX, currentY + 4f, 51, 8.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER)
            // مستند
            colX -= 45f
            drawText(canvas, "#${line.docNumber}", colX, currentY + 4f, 41, 8.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_CENTER)
            // الطرف
            colX -= 80f
            drawText(canvas, (line.partyName ?: "-").take(14), colX, currentY + 4f, 76, 8.5f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_NORMAL)
            // البيان
            colX -= 123f
            drawText(canvas, line.description.take(24), colX, currentY + 4f, 119, 8.5f, COLOR_TEXT_MUTED, Layout.Alignment.ALIGN_NORMAL)
            // الوارد
            colX -= 70f
            drawText(canvas, if (line.inflowOrigMinor > 0) Money(line.inflowOrigMinor, currCode).format(false) else "-", colX, currentY + 4f, 66, 8.5f, COLOR_SUCCESS, Layout.Alignment.ALIGN_CENTER)
            // المنصرف
            colX -= 70f
            drawText(canvas, if (line.outflowOrigMinor > 0) Money(line.outflowOrigMinor, currCode).format(false) else "-", colX, currentY + 4f, 66, 8.5f, COLOR_DANGER, Layout.Alignment.ALIGN_CENTER)
            // الرصيد
            colX -= 80f
            drawText(canvas, Money(line.runningBalanceOrigMinor, currCode).format(false), colX, currentY + 4f, 76, 8.5f, COLOR_NAVY_DARK, Layout.Alignment.ALIGN_CENTER, true)

            currentY += rowH
        }

        currentY += 10f

        // Value Estimation Note
        currentY = drawInfoCard(canvas, currentY, "ملاحظة القيمة التقديرية بسعر السوق:", report.valueEstimation.noteTextAr, COLOR_NAVY_MEDIUM)
        currentY += 10f

        // Reconciliation
        val isFull = report.reconciliation.isFullyReconciled
        drawReconciliationBadge(canvas, currentY, isFull, report.reconciliation.statusMessageAr)

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    // =========================================================================
    // SHARED DRAWING PRIMITIVES
    // =========================================================================
    private fun drawHeaderBanner(
        canvas: Canvas,
        orgName: String,
        titleAr: String,
        titleEn: String,
        contactInfo: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_NAVY_DARK
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 80f, paint)

        paint.color = COLOR_CYAN_ACCENT
        canvas.drawRect(0f, 77f, PAGE_WIDTH.toFloat(), 80f, paint)

        drawText(canvas, orgName, MARGIN_LEFT.toFloat(), 12f, 260, 14f, Color.WHITE, Layout.Alignment.ALIGN_NORMAL, true)
        drawText(canvas, contactInfo, MARGIN_LEFT.toFloat(), 34f, 260, 8.5f, 0xFF94A3B8.toInt(), Layout.Alignment.ALIGN_NORMAL, false)

        val rightColX = PAGE_WIDTH - 250f - MARGIN_LEFT.toFloat()
        drawText(canvas, titleAr, rightColX, 10f, 250, 14f, COLOR_CYAN_ACCENT, Layout.Alignment.ALIGN_OPPOSITE, true)
        drawText(canvas, titleEn, rightColX, 32f, 250, 8.5f, Color.WHITE, Layout.Alignment.ALIGN_OPPOSITE, false)
    }

    private fun drawKpiBox(canvas: Canvas, startY: Float, title: String, kpis: List<Pair<String, String>>): Float {
        val h = 48f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_ROW_ALT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        drawText(canvas, title, MARGIN_LEFT + 8f, startY + 4f, CONTENT_WIDTH - 16, 8.5f, COLOR_TEXT_MUTED, Layout.Alignment.ALIGN_NORMAL, true)

        val colW = CONTENT_WIDTH / kpis.size.toFloat()
        var curX = MARGIN_RIGHT.toFloat()
        for ((label, value) in kpis) {
            curX -= colW
            drawText(canvas, label, curX + 2f, startY + 16f, (colW - 4).toInt(), 7.5f, COLOR_TEXT_MUTED, Layout.Alignment.ALIGN_CENTER)
            drawText(canvas, value, curX + 2f, startY + 28f, (colW - 4).toInt(), 9.5f, COLOR_NAVY_DARK, Layout.Alignment.ALIGN_CENTER, true)
        }

        return startY + h
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float, columns: List<Pair<String, Float>>): Float {
        val h = 24f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_NAVY_DARK
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h, paint)

        var curX = MARGIN_RIGHT.toFloat()
        for ((colName, width) in columns) {
            curX -= width
            drawText(canvas, colName, curX + 2f, startY + 5f, (width - 4).toInt(), 9f, Color.WHITE, Layout.Alignment.ALIGN_CENTER, true)
        }

        return startY + h
    }

    private fun drawInfoCard(canvas: Canvas, startY: Float, title: String, text: String, accentColor: Int): Float {
        val h = 38f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = 0xFFF1F5F9.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        drawText(canvas, title, MARGIN_LEFT + 8f, startY + 5f, CONTENT_WIDTH - 16, 8.5f, accentColor, Layout.Alignment.ALIGN_NORMAL, true)
        drawText(canvas, text, MARGIN_LEFT + 8f, startY + 18f, CONTENT_WIDTH - 16, 8f, COLOR_TEXT_PRIMARY, Layout.Alignment.ALIGN_NORMAL, false)

        return startY + h
    }

    private fun drawReconciliationBadge(canvas: Canvas, startY: Float, isReconciled: Boolean, message: String) {
        val h = 28f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val bgColor = if (isReconciled) 0xFFECFDF5.toInt() else 0xFFFEF2F2.toInt()
        val borderColor = if (isReconciled) COLOR_SUCCESS else COLOR_DANGER

        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        paint.color = borderColor
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(MARGIN_LEFT.toFloat(), startY, MARGIN_RIGHT.toFloat(), startY + h), 4f, 4f, paint)

        val statusText = if (isReconciled) "مطابقة محاسبية معتمدة (IFRS Verified)" else "تنبيه: عدم تطابق محاسبي"
        drawText(canvas, "$statusText: $message", MARGIN_LEFT + 8f, startY + 7f, CONTENT_WIDTH - 16, 9f, borderColor, Layout.Alignment.ALIGN_CENTER, true)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val y = PAGE_HEIGHT - 26f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_LEFT.toFloat(), y, MARGIN_RIGHT.toFloat(), y, paint)

        drawText(
            canvas = canvas,
            text = "تم الإصدار آلياً عبر نظام SamMikrotik المحاسبي | صفحة $pageNumber",
            x = MARGIN_LEFT.toFloat(),
            y = y + 6f,
            width = CONTENT_WIDTH,
            textSize = 8f,
            textColor = COLOR_TEXT_MUTED,
            align = Layout.Alignment.ALIGN_CENTER,
            isBold = false
        )
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        textSize: Float,
        textColor: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        isBold: Boolean = false
    ) {
        if (text.isBlank() || width <= 0) return

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.ANYRTL_LTR)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
