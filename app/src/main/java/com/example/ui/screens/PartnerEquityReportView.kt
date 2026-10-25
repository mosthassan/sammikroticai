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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.local.entity.PartyEntity
import com.example.domain.report.EquityChangeRow
import com.example.domain.report.EquityChangeRowType
import com.example.domain.report.PartnerEquitySummaryRow
import com.example.domain.report.PartnerMotionType
import com.example.domain.report.PartnerStatementLine
import com.example.domain.report.PartnerStatementReport
import com.example.domain.report.StatementOfChangesInEquityReport
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.ReconciliationStatusBadge
import com.example.ui.theme.AssetPurple
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
fun PartnerEquityReportView(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Partner Statement, 1: Statement of Changes in Equity
    val partners by viewModel.allPartners.collectAsState(initial = emptyList())
    val activePartners = remember(partners) { partners.filter { it.isPartner && it.isActive } }
    var selectedPartnerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activePartners) {
        if (selectedPartnerId == null && activePartners.isNotEmpty()) {
            selectedPartnerId = activePartners.first().id
        }
    }

    val todayEpoch = remember { com.example.core.time.AppClock.todayEpochDay() }
    val yearStartEpoch = remember {
        val now = LocalDate.now()
        LocalDate.of(now.year, 1, 1).toEpochDay()
    }

    val partnerStatement by viewModel.partnerStatement.collectAsState()
    val changesInEquity by viewModel.changesInEquity.collectAsState()

    LaunchedEffect(selectedPartnerId, subTab) {
        if (subTab == 0 && selectedPartnerId != null) {
            viewModel.loadPartnerStatement(selectedPartnerId!!, yearStartEpoch, todayEpoch)
        } else if (subTab == 1) {
            viewModel.loadChangesInEquity(yearStartEpoch, todayEpoch)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
        // Sub-tabs: Partner Statement vs Changes in Equity
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MikroTikCyan,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("كشف حركة الشريك", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_partner_statement")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("قائمة التغيّر في حقوق الملكية", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_changes_in_equity")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (subTab == 0) {
            PartnerStatementSubView(
                partners = activePartners,
                selectedPartnerId = selectedPartnerId,
                onSelectPartner = { partnerId ->
                    selectedPartnerId = partnerId
                    viewModel.loadPartnerStatement(partnerId, yearStartEpoch, todayEpoch)
                },
                statement = partnerStatement
            )
        } else {
            ChangesInEquitySubView(
                report = changesInEquity,
                onRefresh = {
                    viewModel.loadChangesInEquity(yearStartEpoch, todayEpoch)
                }
            )
        }
    }
}

@Composable
private fun PartnerStatementSubView(
    partners: List<PartyEntity>,
    selectedPartnerId: String?,
    onSelectPartner: (String) -> Unit,
    statement: PartnerStatementReport?
) {
    if (partners.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "لا يوجد شركاء مسجلون في النظام حتى الآن.\nيمكنك إضافة شركاء من شاشة إدارة الشركاء.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Partner Selection Chips
        item {
            Column {
                Text(
                    text = "اختر الشريك:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(partners) { p ->
                        val isSelected = p.id == selectedPartnerId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPartner(p.id) },
                            label = {
                                Text(
                                    text = "${p.name} (${(p.equityPercentageBasisPoints / 100.0)}%)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MikroTikCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MikroTikCyan
                            ),
                            modifier = Modifier.testTag("chip_partner_${p.id}")
                        )
                    }
                }
            }
        }

        if (statement == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("جاري استخراج كشف حركة الشريك من دفتر الأستاذ...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // Partner Summary Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MikroTikCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = statement.partnerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "نسبة الملكية المقيدة: ${(statement.equityPercentageBasisPoints / 100.0)}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ReconciliationStatusBadge(reconciliation = statement.reconciliation)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Running Balances Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Capital Account (3101)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("رأس المال (3101):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                AmountText(
                                    money = Money(statement.capitalClosingBalanceMinor, CurrencyCode.FUNCTIONAL),
                                    semanticType = AmountSemanticType.INCOME,
                                    fontSize = 16,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "افتتاحي: ${Money(statement.capitalOpeningBalanceMinor, CurrencyCode.FUNCTIONAL).format()}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Current Account (3201)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("جاري الشريك (3201):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                AmountText(
                                    money = Money(statement.currentClosingBalanceMinor, CurrencyCode.FUNCTIONAL),
                                    semanticType = if (statement.currentClosingBalanceMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                                    fontSize = 16,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "افتتاحي: ${Money(statement.currentOpeningBalanceMinor, CurrencyCode.FUNCTIONAL).format()}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Total Equity
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("إجمالي حقوق الشريك:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                AmountText(
                                    money = Money(statement.totalClosingEquityMinor, CurrencyCode.FUNCTIONAL),
                                    semanticType = AmountSemanticType.NEUTRAL,
                                    fontSize = 16,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "صافي التغير: ${Money(statement.netChangeEquityMinor, CurrencyCode.FUNCTIONAL).format()}",
                                    fontSize = 10.sp,
                                    color = if (statement.netChangeEquityMinor >= 0) SemanticIncomeGreen else SemanticExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Movements Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل حركات الشريك (${statement.lines.size} حركة)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (statement.lines.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "لا توجد حركات مسجلة على حساب رأس المال أو الجاري للشريك خلال هذه الفترة.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(statement.lines) { line ->
                    PartnerStatementLineCard(line = line)
                }
            }
        }
    }
}

@Composable
private fun PartnerStatementLineCard(line: PartnerStatementLine) {
    val motionColor = when (line.motionType) {
        PartnerMotionType.CASH_CONTRIBUTION -> SemanticIncomeGreen
        PartnerMotionType.IN_KIND_CONTRIBUTION -> BrandCyanPrimary
        PartnerMotionType.WITHDRAWAL -> SemanticExpenseRed
        PartnerMotionType.PAID_ON_BEHALF -> MikroTikCyan
        PartnerMotionType.SETTLEMENT -> AssetPurple
        PartnerMotionType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val motionIcon = when (line.motionType) {
        PartnerMotionType.CASH_CONTRIBUTION -> Icons.Default.MonetizationOn
        PartnerMotionType.IN_KIND_CONTRIBUTION -> Icons.Default.Build
        PartnerMotionType.WITHDRAWAL -> Icons.AutoMirrored.Filled.TrendingDown
        PartnerMotionType.PAID_ON_BEHALF -> Icons.Default.Payments
        PartnerMotionType.SETTLEMENT -> Icons.Default.AccountBalance
        PartnerMotionType.OTHER -> Icons.Default.Info
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Date, Motion Badge, Doc#
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(motionColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(motionIcon, contentDescription = null, tint = motionColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = motionColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = line.motionNameAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = motionColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حساب ${line.accountCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${LocalDate.ofEpochDay(line.dateEpochDay)} • سند #${line.docNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description (includes asset name if in-kind)
            Text(
                text = line.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Foreign currency details if non-YER
            if (line.currency != "YER") {
                Spacer(modifier = Modifier.height(3.dp))
                val whole = line.exchangeRateMicros / ExchangeRate.SCALE_MICROS
                val frac = (line.exchangeRateMicros % ExchangeRate.SCALE_MICROS) / 1000L
                val rateStr = if (frac == 0L) "$whole" else "$whole.${frac.toString().padStart(3, '0')}"
                Text(
                    text = "المبلغ الأصلي: ${Money(line.amountOrigMinor, CurrencyCode.fromString(line.currency)).format()} @ $rateStr ريال/${line.currency}",
                    fontSize = 11.sp,
                    color = MikroTikCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Financial impact & Running balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Movement Amount (Debit or Credit)
                if (line.creditMinor > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("دائن (+): ", fontSize = 11.sp, color = SemanticIncomeGreen)
                        AmountText(
                            money = Money(line.creditMinor, CurrencyCode.FUNCTIONAL),
                            semanticType = AmountSemanticType.INCOME,
                            fontSize = 13,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("مدين (-): ", fontSize = 11.sp, color = SemanticExpenseRed)
                        AmountText(
                            money = Money(line.debitMinor, CurrencyCode.FUNCTIONAL),
                            semanticType = AmountSemanticType.EXPENSE,
                            fontSize = 13,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Running balances after this movement
                Text(
                    text = if (line.accountCode == "3101")
                        "رصيد رأس المال: ${Money(line.capitalRunningBalanceMinor, CurrencyCode.FUNCTIONAL).format()}"
                    else
                        "رصيد الجاري: ${Money(line.currentRunningBalanceMinor, CurrencyCode.FUNCTIONAL).format()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChangesInEquitySubView(
    report: StatementOfChangesInEquityReport?,
    onRefresh: () -> Unit
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("جاري إنشاء قائمة التغيّر في حقوق الملكية وفحص اتزان الأستاذ...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Reconciliation Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("قائمة التغيّر في حقوق الملكية (IFRS)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "مطابقة شاملة لحسابات رأس المال (3101)، الجاري (3201)، الأرباح المبقاة (3301)، وأرباح الفترة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ReconciliationStatusBadge(reconciliation = report.reconciliation)
                }
            }
        }

        // Export Buttons (PDF & CSV)
        item {
            val context = LocalContext.current
            ReportExportButtons(
                onExportPdf = {
                    val pdf = ReportsPdfGenerator.generateChangesInEquityPdf(context, report)
                    ReportExportManager.sharePdf(context, pdf, "قائمة التغيرات في حقوق الملكية")
                },
                onExportCsv = {
                    val csv = ReportsCsvGenerator.generateChangesInEquityCsv(context, report)
                    ReportExportManager.shareCsv(context, csv, "قائمة التغيرات في حقوق الملكية")
                },
                tagPrefix = "equity"
            )
        }

        // 5-Column Equity Matrix
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "مصفوفة تطور حقوق الملكية (Equity Movement Matrix):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MikroTikCyan,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Scrollable Horizontal Table
                    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Column {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("البند / الحركة", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(170.dp))
                                Text("رأس المال (3101)", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                                Text("جاري الشركاء (3201)", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                                Text("أرباح مبقاة (3301)", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                                Text("أرباح الفترة", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                                Text("إجمالي حقوق الملكية", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(125.dp))
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Rows
                            report.rows.forEach { row ->
                                val isClosing = row.rowType == EquityChangeRowType.CLOSING_BALANCE
                                val isOpening = row.rowType == EquityChangeRowType.OPENING_BALANCE
                                val rowBg = if (isClosing) MikroTikCyan.copy(alpha = 0.12f)
                                else if (isOpening) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else Color.Transparent

                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = row.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isClosing || isOpening) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.width(170.dp)
                                    )
                                    MatrixAmountCell(row.capitalMinor, isClosing, 110)
                                    MatrixAmountCell(row.currentMinor, isClosing, 110)
                                    MatrixAmountCell(row.retainedEarningsMinor, isClosing, 110)
                                    MatrixAmountCell(row.periodProfitMinor, isClosing, 110)
                                    MatrixAmountCell(row.totalMinor, isClosing, 125, highlight = true)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }

        // Per-Partner Breakdown Section
        item {
            Text(
                text = "تفصيل حقوق كل شريك وتوزيع الأرباح التقديرية:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(report.partnerSummaries) { partner ->
            PartnerEquitySummaryCard(partner = partner)
        }
    }
}

@Composable
private fun MatrixAmountCell(
    amountMinor: Long,
    isClosing: Boolean,
    widthDp: Int,
    highlight: Boolean = false
) {
    val color = when {
        amountMinor < 0 -> SemanticExpenseRed
        amountMinor > 0 && highlight -> MikroTikCyan
        amountMinor > 0 -> SemanticIncomeGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Text(
        text = if (amountMinor == 0L) "-" else Money(amountMinor, CurrencyCode.FUNCTIONAL).format(),
        fontSize = 11.sp,
        fontWeight = if (isClosing || highlight) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.End,
        color = color,
        modifier = Modifier.width(widthDp.dp)
    )
}

@Composable
private fun PartnerEquitySummaryCard(partner: PartnerEquitySummaryRow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Partner Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(partner.partnerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("حصة الشريك: ${(partner.equityPercentageBasisPoints / 100.0)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي نصيب الشريك:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(
                        money = Money(partner.totalPartnerEquityMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = AmountSemanticType.INCOME,
                        fontSize = 15,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Breakdown Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryColumn("رأس المال النهائي", partner.capitalClosingMinor)
                SummaryColumn("الجاري النهائي", partner.currentClosingMinor)
                SummaryColumn("الربح المقدر (Largest Remainder)", partner.estimatedPeriodProfitMinor)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryDetailText("مساهمات نقدية:", partner.cashContributionsMinor)
                SummaryDetailText("مساهمات عينية:", partner.inKindContributionsMinor)
                SummaryDetailText("مسحوبات:", partner.withdrawalsMinor)
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, amountMinor: Long) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = Money(amountMinor, CurrencyCode.FUNCTIONAL).format(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (amountMinor >= 0) SemanticIncomeGreen else SemanticExpenseRed
        )
    }
}

@Composable
private fun SummaryDetailText(label: String, amountMinor: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = Money(amountMinor, CurrencyCode.FUNCTIONAL).format(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
