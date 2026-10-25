package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.ReportExportButtons
import com.example.util.ReportExportManager
import com.example.util.ReportsCsvGenerator
import com.example.util.ReportsPdfGenerator
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.time.FiscalCalendar
import com.example.domain.report.MonthClosingStatus
import com.example.domain.report.MonthlyComparisonResult
import com.example.domain.report.MonthlyProfitItem
import com.example.domain.report.MonthlyProfitReport
import com.example.domain.report.MonthlyProfitStatus
import com.example.domain.report.calculateMonthlyComparison
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.ReconciliationStatusBadge
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel
import java.time.LocalDate

@Composable
fun MonthlyProfitReportView(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { LocalDate.now().year }
    var selectedFiscalYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonthIndex by remember { mutableIntStateOf(LocalDate.now().monthValue) }

    val reportState by viewModel.monthlyProfitReport.collectAsState()

    LaunchedEffect(selectedFiscalYear) {
        viewModel.loadMonthlyProfitReport(selectedFiscalYear)
    }

    val report = reportState

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("monthly_profit_report_view")
    ) {
        // Fiscal Year Selector & Top Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الأرباح الشهرية وتفكيك الدخل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        // Year Selector Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedFiscalYear-- },
                                modifier = Modifier.testTag("prev_year_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "السنة السابقة")
                            }
                            Text(
                                text = "$selectedFiscalYear",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { selectedFiscalYear++ },
                                modifier = Modifier.testTag("next_year_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "السنة التالية")
                            }
                        }
                    }

                    if (report != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حالة سلامة واتزان الأشهر:",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            ReconciliationStatusBadge(reconciliation = report.reconciliation)
                        }
                    }
                }
            }
        }

        if (report != null) {
            item {
                val context = LocalContext.current
                ReportExportButtons(
                    onExportPdf = {
                        val pdf = ReportsPdfGenerator.generateMonthlyProfitPdf(context, report)
                        ReportExportManager.sharePdf(context, pdf, "تقرير الأرباح والخسائر الشهرية ${report.fiscalYear}")
                    },
                    onExportCsv = {
                        val csv = ReportsCsvGenerator.generateMonthlyProfitCsv(context, report)
                        ReportExportManager.shareCsv(context, csv, "الأرباح الشهرية ${report.fiscalYear}")
                    },
                    tagPrefix = "monthly_profit"
                )
            }
        }

        if (report == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("جاري استخراج تفكيك الأرباح الشهرية للشبكة...", color = TextSecondaryDark)
                }
            }
        } else {
            // Full Year Summary Hero Card
            item {
                YearSummaryHeroCard(report = report)
            }

            // Month Comparison Selector Card
            val selectedItem = report.months.find { it.monthIndex == selectedMonthIndex } ?: report.months.first()
            val prevMonthItem = if (selectedItem.monthIndex > 1) {
                report.months.find { it.monthIndex == selectedItem.monthIndex - 1 }
            } else null

            item {
                MonthComparisonSection(
                    selectedMonth = selectedItem,
                    previousMonth = prevMonthItem,
                    allMonths = report.months,
                    onSelectMonth = { selectedMonthIndex = it }
                )
            }

            // 12-Month Matrix
            item {
                TwelveMonthMatrixSection(
                    months = report.months,
                    selectedMonthIndex = selectedMonthIndex,
                    onMonthClicked = { selectedMonthIndex = it }
                )
            }

            // Partner Share Estimation Card
            item {
                PartnerShareEstimationCard(
                    totalNetProfitMinor = report.yearTotalNetProfitMinor,
                    selectedMonthProfitMinor = selectedItem.netProfitMinor,
                    partnerShares = report.partnerShares,
                    selectedMonthName = selectedItem.monthName
                )
            }
        }
    }
}

@Composable
private fun YearSummaryHeroCard(report: MonthlyProfitReport) {
    val isProfit = report.yearTotalNetProfitMinor >= 0L
    val statusColor = if (isProfit) SemanticIncomeGreen else SemanticExpenseRed

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isProfit) "إجمالي صافي أرباح السنة المالية" else "إجمالي صافي خسائر السنة المالية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
                Icon(
                    imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AmountText(
                money = Money(report.yearTotalNetProfitMinor, CurrencyCode.FUNCTIONAL),
                semanticType = if (isProfit) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                fontSize = 26,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CyberBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي الإيرادات الصافية", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        Money(report.yearTotalNetRevenueMinor, CurrencyCode.FUNCTIONAL).format(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                }

                Column {
                    Text("تكلفة اشتراكات الإنترنت (5101)", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        Money(report.yearTotalDirectIspCostMinor, CurrencyCode.FUNCTIONAL).format(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SemanticExpenseRed
                    )
                }

                Column {
                    Text("مجمل الربح", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        Money(report.yearTotalGrossProfitMinor, CurrencyCode.FUNCTIONAL).format(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BrandCyanPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthComparisonSection(
    selectedMonth: MonthlyProfitItem,
    previousMonth: MonthlyProfitItem?,
    allMonths: List<MonthlyProfitItem>,
    onSelectMonth: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مقارنة أرباح شهر: ${selectedMonth.monthName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                }

                MonthStatusChip(status = selectedMonth.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Month Selector Chips
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allMonths.forEach { m ->
                    FilterChip(
                        selected = m.monthIndex == selectedMonth.monthIndex,
                        onClick = { onSelectMonth(m.monthIndex) },
                        label = { Text(m.monthName, fontSize = 11.sp) },
                        modifier = Modifier.testTag("month_chip_${m.monthIndex}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comparison Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Month Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ربح الشهر الحالي", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            Money(selectedMonth.netProfitMinor, CurrencyCode.FUNCTIONAL).format(),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedMonth.netProfitMinor >= 0) SemanticIncomeGreen else SemanticExpenseRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "الهامش: ${formatBps(selectedMonth.netMarginBasisPoints)}",
                            fontSize = 10.sp,
                            color = TextMutedDark
                        )
                    }
                }

                // Previous Month Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الشهر السابق", fontSize = 11.sp, color = TextSecondaryDark)
                        val prevAmount = previousMonth?.netProfitMinor
                        Text(
                            if (prevAmount != null) Money(prevAmount, CurrencyCode.FUNCTIONAL).format() else "لا يوجد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (previousMonth != null) {
                            val diff = selectedMonth.netProfitMinor - previousMonth.netProfitMinor
                            val isPos = diff >= 0
                            Text(
                                "الفارق: ${if (isPos) "+" else ""}${Money(diff, CurrencyCode.FUNCTIONAL).format()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPos) SemanticIncomeGreen else SemanticExpenseRed,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text("-", fontSize = 10.sp, color = TextMutedDark)
                        }
                    }
                }

                // Cumulative Up to Month Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("التراكمي حتى الشهر", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            Money(selectedMonth.cumulativeProfitMinor, CurrencyCode.FUNCTIONAL).format(),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedMonth.cumulativeProfitMinor >= 0) BrandCyanPrimary else SemanticExpenseRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("من بداية السنة", fontSize = 10.sp, color = TextMutedDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ملاحظة الحالة: ${selectedMonth.status.reason}",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun TwelveMonthMatrixSection(
    months: List<MonthlyProfitItem>,
    selectedMonthIndex: Int,
    onMonthClicked: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "مصفوفة الشهور الـ 12 والأرباح التراكمية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = "اضغط على أي شهر لعرض تحليله المقارن وتفاصيله",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            months.forEach { month ->
                MonthlyRowItem(
                    item = month,
                    isSelected = month.monthIndex == selectedMonthIndex,
                    onClick = { onMonthClicked(month.monthIndex) }
                )
                HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun MonthlyRowItem(
    item: MonthlyProfitItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isProfit = item.netProfitMinor >= 0L
    val bgColor = if (isSelected) MikroTikCyan.copy(alpha = 0.10f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp)
            .testTag("month_row_${item.monthIndex}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.monthName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) MikroTikCyan else TextPrimaryDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                MonthStatusBadgeMini(status = item.status.status)
            }
            Text(
                text = "إيراد: ${Money(item.netRevenueMinor, CurrencyCode.FUNCTIONAL).format()} • تكلفة: ${Money(item.directIspCostMinor, CurrencyCode.FUNCTIONAL).format()}",
                fontSize = 10.sp,
                color = TextSecondaryDark,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Text(
                text = Money(item.netProfitMinor, CurrencyCode.FUNCTIONAL).format(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isProfit) SemanticIncomeGreen else SemanticExpenseRed
            )
            Text(
                text = "تراكمي: ${Money(item.cumulativeProfitMinor, CurrencyCode.FUNCTIONAL).format()}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (item.cumulativeProfitMinor >= 0) TextSecondaryDark else SemanticExpenseRed
            )
        }
    }
}

@Composable
private fun PartnerShareEstimationCard(
    totalNetProfitMinor: Long,
    selectedMonthProfitMinor: Long,
    partnerShares: List<com.example.domain.report.PartnerProfitShare>,
    selectedMonthName: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth().testTag("partner_estimation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تقدير حصص الشركاء (معادلة أكبر البواقي)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyberDarkCardElevated
                ) {
                    Text(
                        text = "إحصائي فقط",
                        fontSize = 10.sp,
                        color = TextMutedDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "توزيع تقريبي لصافي أرباح السنة (${Money(totalNetProfitMinor, CurrencyCode.FUNCTIONAL).format()}) على الشركاء بدون أي باقي تدوير.",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (partnerShares.isEmpty()) {
                Text(
                    text = "لم يتم تسجيل أي شركاء أو حصص ملكية في النظام بعد.",
                    fontSize = 12.sp,
                    color = TextMutedDark,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                partnerShares.forEach { share ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(share.partnerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimaryDark)
                            Text("نسبة الحصة: ${formatBps(share.equityPercentageBasisPoints)}", fontSize = 10.sp, color = TextMutedDark)
                        }

                        Text(
                            Money(share.estimatedShareMinor, CurrencyCode.FUNCTIONAL).format(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (share.estimatedShareMinor >= 0) SemanticIncomeGreen else SemanticExpenseRed
                        )
                    }
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun MonthStatusChip(status: MonthlyProfitStatus) {
    val (text, color) = when (status.status) {
        MonthClosingStatus.FINAL -> "معتمد ✓" to SemanticIncomeGreen
        MonthClosingStatus.IN_PROGRESS -> "قيد النشاط ⚡" to BrandCyanPrimary
        MonthClosingStatus.DRAFT -> "مسودة / غير مقفل" to SemanticWarningAmber
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MonthStatusBadgeMini(status: MonthClosingStatus) {
    val (text, color) = when (status) {
        MonthClosingStatus.FINAL -> "✓" to SemanticIncomeGreen
        MonthClosingStatus.IN_PROGRESS -> "•" to BrandCyanPrimary
        MonthClosingStatus.DRAFT -> "!" to SemanticWarningAmber
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

private fun formatBps(bps: Int): String {
    val isNeg = bps < 0
    val absBps = if (isNeg) -bps else bps
    val whole = absBps / 100
    val frac = absBps % 100
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return "${if (isNeg) "-" else ""}$whole.$fracStr%"
}
