package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.core.model.Money
import com.example.data.local.entity.TreasuryAccountEntity
import com.example.domain.report.DailyCashRegisterLine
import com.example.domain.report.DailyCashRegisterStatement
import com.example.domain.report.DailyGroupSummary
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
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel
import java.time.LocalDate

@Composable
fun DailyCashRegisterView(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val treasuries by viewModel.allTreasuries.collectAsState()
    val statement by viewModel.dailyCashRegister.collectAsState()

    var selectedTreasuryId by remember { mutableStateOf<String?>(null) }
    var periodMode by remember { mutableIntStateOf(3) } // 0: Today, 1: Yesterday, 2: Last 7 Days, 3: This Month, 4: This Year
    var showDailyTotals by remember { mutableStateOf(false) }

    val todayEpoch = remember { com.example.core.time.AppClock.todayEpochDay() }

    // Initialize selected treasury once available
    LaunchedEffect(treasuries) {
        if (selectedTreasuryId == null && treasuries.isNotEmpty()) {
            selectedTreasuryId = treasuries.firstOrNull { it.currency == "YER" }?.id ?: treasuries.first().id
        }
    }

    // Load register when selection changes
    LaunchedEffect(selectedTreasuryId, periodMode) {
        val targetId = selectedTreasuryId ?: return@LaunchedEffect
        val now = LocalDate.now()
        val (start, end) = when (periodMode) {
            0 -> Pair(todayEpoch, todayEpoch)
            1 -> Pair(todayEpoch - 1, todayEpoch - 1)
            2 -> Pair(todayEpoch - 6, todayEpoch)
            3 -> {
                val firstDay = LocalDate.of(now.year, now.monthValue, 1).toEpochDay()
                Pair(firstDay, todayEpoch)
            }
            4 -> {
                val firstDay = LocalDate.of(now.year, 1, 1).toEpochDay()
                Pair(firstDay, todayEpoch)
            }
            else -> Pair(todayEpoch, todayEpoch)
        }
        viewModel.loadDailyCashRegister(targetId, start, end)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_cash_register_screen")
    ) {
        // 1. Treasury / Cash Box Selector
        item {
            TreasurySelectionBar(
                treasuries = treasuries,
                selectedTreasuryId = selectedTreasuryId,
                onTreasurySelected = { selectedTreasuryId = it }
            )
        }

        // 2. Period Filter Chips & Controls Row
        item {
            PeriodAndToggleControlBar(
                periodMode = periodMode,
                onPeriodSelected = { periodMode = it },
                showDailyTotals = showDailyTotals,
                onToggleDailyTotals = { showDailyTotals = it },
                onRefresh = {
                    selectedTreasuryId?.let { id ->
                        val now = LocalDate.now()
                        val (start, end) = when (periodMode) {
                            0 -> Pair(todayEpoch, todayEpoch)
                            1 -> Pair(todayEpoch - 1, todayEpoch - 1)
                            2 -> Pair(todayEpoch - 6, todayEpoch)
                            3 -> Pair(LocalDate.of(now.year, now.monthValue, 1).toEpochDay(), todayEpoch)
                            4 -> Pair(LocalDate.of(now.year, 1, 1).toEpochDay(), todayEpoch)
                            else -> Pair(todayEpoch, todayEpoch)
                        }
                        viewModel.loadDailyCashRegister(id, start, end)
                    }
                }
            )
        }

        val currentStatement = statement
        if (currentStatement == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "جاري تحميل سجل الصندوق...",
                            color = TextSecondaryDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // 3. KPI Header Summary Cards
            item {
                RegisterSummaryKpis(statement = currentStatement)
            }

            // Export Buttons (PDF & CSV)
            item {
                val context = LocalContext.current
                ReportExportButtons(
                    onExportPdf = {
                        val pdf = ReportsPdfGenerator.generateDailyCashRegisterPdf(context, currentStatement)
                        ReportExportManager.sharePdf(context, pdf, "سجل الصندوق اليومي - ${currentStatement.treasuryName}")
                    },
                    onExportCsv = {
                        val csv = ReportsCsvGenerator.generateDailyCashRegisterCsv(context, currentStatement)
                        ReportExportManager.shareCsv(context, csv, "سجل الصندوق اليومي - ${currentStatement.treasuryName}")
                    },
                    tagPrefix = "daily_cash"
                )
            }

            // 4. Value Estimation Note Card
            item {
                ValueEstimationBanner(note = currentStatement.valueEstimation)
            }

            // 5. Reconciliation Status Card
            item {
                RegisterReconciliationCard(reconciliation = currentStatement.reconciliation)
            }

            // 6. Transaction Register (Continuous or Daily Grouped)
            if (currentStatement.lines.isEmpty()) {
                item {
                    EmptyRegisterCard(treasuryName = currentStatement.treasuryName)
                }
            } else if (showDailyTotals) {
                // Grouped by Day with Subtotals
                items(currentStatement.dailyGroups, key = { it.dateEpochDay }) { group ->
                    DailyGroupCard(group = group, currency = currentStatement.currency)
                }
            } else {
                // Chronological Ledger Table
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = MikroTikCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "حركات الصندوق التفصيلية (${currentStatement.lines.size} حركة)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Surface(
                                    color = MikroTikCyan.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currentStatement.currency,
                                        color = MikroTikCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = CyberBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            currentStatement.lines.forEachIndexed { idx, line ->
                                RegisterLineRow(line = line, currency = currentStatement.currency)
                                if (idx < currentStatement.lines.size - 1) {
                                    HorizontalDivider(
                                        color = CyberBorder.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreasurySelectionBar(
    treasuries: List<TreasuryAccountEntity>,
    selectedTreasuryId: String?,
    onTreasurySelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "اختر الصندوق / الخزينة:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            treasuries.forEach { tr ->
                val isSelected = (tr.id == selectedTreasuryId)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTreasurySelected(tr.id) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (tr.glAccountCode == "1102") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(text = tr.name, maxLines = 1)
                            Surface(
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MikroTikCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tr.currency,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MikroTikCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = CyberDarkCardElevated,
                        labelColor = TextPrimaryDark
                    ),
                    border = BorderStroke(1.dp, if (isSelected) MikroTikCyan else CyberBorder),
                    modifier = Modifier.testTag("treasury_chip_${tr.id}")
                )
            }
        }
    }
}

@Composable
private fun PeriodAndToggleControlBar(
    periodMode: Int,
    onPeriodSelected: (Int) -> Unit,
    showDailyTotals: Boolean,
    onToggleDailyTotals: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Periods
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    0 to "اليوم",
                    1 to "أمس",
                    2 to "7 أيام",
                    3 to "هذا الشهر",
                    4 to "هذا العام"
                ).forEach { (mode, label) ->
                    val isSelected = (periodMode == mode)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPeriodSelected(mode) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCyanPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = TextPrimaryDark
                        ),
                        border = BorderStroke(1.dp, if (isSelected) BrandCyanPrimary else CyberBorder),
                        modifier = Modifier.testTag("period_chip_$mode")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp).testTag("refresh_cash_register")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = MikroTikCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Toggle Daily Totals Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "عرض المجاميع اليومية (Daily Totals)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "تجميع الحركات حسب التاريخ مع إظهار صافي حركة ورصيد كل يوم",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = showDailyTotals,
                    onCheckedChange = onToggleDailyTotals,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = MikroTikCyan,
                        uncheckedThumbColor = TextMutedDark,
                        uncheckedTrackColor = CyberDarkCardElevated
                    ),
                    modifier = Modifier.testTag("daily_totals_switch")
                )
            }
        }
    }
}

@Composable
private fun RegisterSummaryKpis(statement: DailyCashRegisterStatement) {
    val currency = statement.currency
    val currencyCode = try { CurrencyCode.valueOf(currency) } catch (_: Exception) { CurrencyCode.YER }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Opening Balance
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "الرصيد الافتتاحي", color = TextSecondaryDark, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.openingBalanceOrigMinor, currencyCode),
                        semanticType = AmountSemanticType.NEUTRAL,
                        fontSize = 18,
                        fontWeight = FontWeight.Bold
                    )
                    if (currency != "YER") {
                        Text(
                            text = Money(statement.openingBalanceBaseMinor, CurrencyCode.YER).format(),
                            color = TextMutedDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Closing Balance
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, MikroTikCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "رصيد الإغلاق الحالي", color = MikroTikCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.closingBalanceOrigMinor, currencyCode),
                        semanticType = AmountSemanticType.INCOME,
                        fontSize = 18,
                        fontWeight = FontWeight.Bold
                    )
                    if (currency != "YER") {
                        Text(
                            text = Money(statement.closingBalanceBaseMinor, CurrencyCode.YER).format(),
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Inflows
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = SemanticIncomeGreen, modifier = Modifier.size(14.dp))
                        Text(text = "إجمالي الوارد (+)", color = SemanticIncomeGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.totalInflowOrigMinor, currencyCode),
                        semanticType = AmountSemanticType.INCOME,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Outflows
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = SemanticExpenseRed, modifier = Modifier.size(14.dp))
                        Text(text = "إجمالي المنصرف (-)", color = SemanticExpenseRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.totalOutflowOrigMinor, currencyCode),
                        semanticType = AmountSemanticType.EXPENSE,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Net Change
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "صافي الحركة", color = TextSecondaryDark, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.netChangeOrigMinor, currencyCode),
                        semanticType = if (statement.netChangeOrigMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueEstimationBanner(note: com.example.domain.report.ValueEstimationNote) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142436)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MikroTikCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MikroTikCyan,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ملاحظة التقدير بسعر اليوم (للمعلومات فقط)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MikroTikCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.noteTextAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryDark,
                    lineHeight = 18.sp
                )
                if (note.isForeignCurrency) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "القيمة الدفترية: ${Money(note.bookValueBaseMinor, CurrencyCode.YER).format()}",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        val deltaLabel = if (note.unrealizedDeltaYerMinor >= 0) "فارق موجب تقديري" else "فارق سالب تقديري"
                        Text(
                            text = "$deltaLabel: ${Money(Math.abs(note.unrealizedDeltaYerMinor), CurrencyCode.YER).format()}",
                            color = if (note.unrealizedDeltaYerMinor >= 0) SemanticIncomeGreen else SemanticWarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterReconciliationCard(
    reconciliation: com.example.domain.report.DailyCashRegisterReconciliation
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            if (reconciliation.isFullyReconciled) SemanticIncomeGreen.copy(alpha = 0.5f) else SemanticExpenseRed.copy(alpha = 0.5f),
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (reconciliation.isFullyReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reconciliation.isFullyReconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "فحوصات المطابقة والرقابة المحاسبية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = if (reconciliation.isFullyReconciled) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticExpenseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (reconciliation.isFullyReconciled) "مطابق 100%" else "غير مطابق",
                        color = if (reconciliation.isFullyReconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CyberBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Check 1: Original Currency Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (reconciliation.isOrigReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reconciliation.isOrigReconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "مطابقة رصيد الصندوق (العملة الأصلية):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
                Text(
                    text = if (reconciliation.isOrigReconciled) "مطابق (الفرق: 0)" else "فرق: ${reconciliation.origDifferenceMinor}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (reconciliation.isOrigReconciled) SemanticIncomeGreen else SemanticExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Check 2: Base Currency Book Value Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (reconciliation.isBaseReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reconciliation.isBaseReconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "مطابقة القيمة الدفترية مع الأستاذ العام (${reconciliation.glAccountCode}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
                Text(
                    text = if (reconciliation.isBaseReconciled) "مطابق (الفرق: 0 ر.ي)" else "فرق: ${reconciliation.baseDifferenceMinor} ر.ي",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (reconciliation.isBaseReconciled) SemanticIncomeGreen else SemanticExpenseRed
                )
            }
        }
    }
}

@Composable
private fun DailyGroupCard(
    group: DailyGroupSummary,
    currency: String
) {
    var expanded by remember { mutableStateOf(false) }
    val currencyCode = try { CurrencyCode.valueOf(currency) } catch (_: Exception) { CurrencyCode.YER }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = BrandCyanPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = group.dateFormatted,
                            color = BrandCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "(${group.lines.size} حركة)",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "رصيد نهاية اليوم: ",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                    AmountText(
                        money = Money(group.dayEndingBalanceOrigMinor, currencyCode),
                        semanticType = AmountSemanticType.NEUTRAL,
                        fontSize = 14,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtotals Bar for the Day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وارد: +${Money(group.dayInflowOrigMinor, currencyCode).format()}",
                    color = SemanticIncomeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "منصرف: -${Money(group.dayOutflowOrigMinor, currencyCode).format()}",
                    color = SemanticExpenseRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "صافي: ${Money(group.dayNetOrigMinor, currencyCode).format()}",
                    color = if (group.dayNetOrigMinor >= 0) SemanticIncomeGreen else SemanticExpenseRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Expandable details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    group.lines.forEachIndexed { idx, line ->
                        RegisterLineRow(line = line, currency = currency)
                        if (idx < group.lines.size - 1) {
                            HorizontalDivider(
                                color = CyberBorder.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterLineRow(
    line: DailyCashRegisterLine,
    currency: String
) {
    val currencyCode = try { CurrencyCode.valueOf(currency) } catch (_: Exception) { CurrencyCode.YER }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Badge and Date
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = when {
                        line.inflowOrigMinor > 0 -> SemanticIncomeGreen.copy(alpha = 0.12f)
                        line.outflowOrigMinor > 0 -> SemanticExpenseRed.copy(alpha = 0.12f)
                        else -> CyberBorder.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = line.docTypeAr,
                        color = when {
                            line.inflowOrigMinor > 0 -> SemanticIncomeGreen
                            line.outflowOrigMinor > 0 -> SemanticExpenseRed
                            else -> TextSecondaryDark
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "#${line.docNumber}",
                    color = TextMutedDark,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "• ${line.dateFormatted}",
                    color = TextMutedDark,
                    fontSize = 11.sp
                )
            }

            // Inflow / Outflow Amount
            if (line.inflowOrigMinor > 0) {
                Text(
                    text = "+${Money(line.inflowOrigMinor, currencyCode).format()}",
                    color = SemanticIncomeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            } else if (line.outflowOrigMinor > 0) {
                Text(
                    text = "-${Money(line.outflowOrigMinor, currencyCode).format()}",
                    color = SemanticExpenseRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "0.00 $currency",
                    color = TextMutedDark,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Party and Memo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                line.partyName?.let { party ->
                    Text(
                        text = "الطرف: $party",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = line.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            // Running Balance
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "الرصيد: ", color = TextMutedDark, fontSize = 10.sp)
                    Text(
                        text = Money(line.runningBalanceOrigMinor, currencyCode).format(),
                        color = MikroTikCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (currency != "YER") {
                    Text(
                        text = Money(line.runningBalanceBaseMinor, CurrencyCode.YER).format(),
                        color = TextMutedDark,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRegisterCard(treasuryName: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PointOfSale,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "لا توجد حركات نقدية مسجلة لـ $treasuryName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = "لم يتم تسجيل أي سندات قبض أو صرف أو فواتير خلال الفترة الزمنية المحددة.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            )
        }
    }
}
