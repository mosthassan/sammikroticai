package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.domain.report.CashFlowActivityType
import com.example.domain.report.CashFlowCategory
import com.example.domain.report.CashFlowCategorySummary
import com.example.domain.report.CashFlowLineItem
import com.example.domain.report.CashFlowSection
import com.example.domain.report.CashFlowStatement
import com.example.domain.report.NonCashDisclosureItem
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
fun CashFlowStatementView(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var periodMode by remember { mutableIntStateOf(1) } // 0: Today, 1: Month, 2: Year, 3: All time
    val todayEpoch = remember { com.example.core.time.AppClock.todayEpochDay() }
    val statement by viewModel.cashFlowStatement.collectAsState()

    var showDrilldown by remember { mutableStateOf(false) }

    // Auto-load statement on period change
    LaunchedEffect(periodMode) {
        val now = LocalDate.now()
        val (start, end) = when (periodMode) {
            0 -> Pair(todayEpoch, todayEpoch)
            1 -> {
                val firstDay = LocalDate.of(now.year, now.monthValue, 1).toEpochDay()
                Pair(firstDay, todayEpoch)
            }
            2 -> {
                val firstDay = LocalDate.of(now.year, 1, 1).toEpochDay()
                Pair(firstDay, todayEpoch)
            }
            else -> Pair(0L, todayEpoch)
        }
        viewModel.loadCashFlowStatement(start, end)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize().testTag("cash_flow_statement_container")
    ) {
        // --- 1. Period Selector Chips & Header ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "قائمة التدفقات النقدية (IAS 7)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "التصنيف المباشر لحركات الصندوق (1101) والبنوك (1102)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        statement?.let { rep ->
                            ReconciliationStatusBadge(reconciliation = rep.reconciliation)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = periodMode == 0,
                            onClick = { periodMode = 0 },
                            label = { Text("اليوم") }
                        )
                        FilterChip(
                            selected = periodMode == 1,
                            onClick = { periodMode = 1 },
                            label = { Text("هذا الشهر") }
                        )
                        FilterChip(
                            selected = periodMode == 2,
                            onClick = { periodMode = 2 },
                            label = { Text("هذا العام") }
                        )
                        FilterChip(
                            selected = periodMode == 3,
                            onClick = { periodMode = 3 },
                            label = { Text("كل الفترات") }
                        )
                    }
                }
            }
        }

        // --- 2. Main Cash Flow Cards ---
        if (statement == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "جاري تحميل قائمة التدفقات النقدية...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val rep = statement!!

            // Hero Summary Card: Opening, Net Change, Closing Cash
            item {
                CashFlowHeroCard(statement = rep)
            }

            // 1. Operating Activities Card
            item {
                ActivitySectionCard(
                    section = rep.operatingSection,
                    icon = Icons.Default.Payments,
                    accentColor = SemanticIncomeGreen,
                    testTagPrefix = "operating"
                )
            }

            // 2. Investing Activities Card
            item {
                ActivitySectionCard(
                    section = rep.investingSection,
                    icon = Icons.Default.Router,
                    accentColor = MikroTikCyan,
                    testTagPrefix = "investing"
                )
            }

            // 3. Financing Activities Card
            item {
                ActivitySectionCard(
                    section = rep.financingSection,
                    icon = Icons.Default.Handshake,
                    accentColor = AssetPurple,
                    testTagPrefix = "financing"
                )
            }

            // 4. Standalone FX Impact Card
            if (rep.fxImpactOnCashMinor != 0L || rep.fxSection.lines.isNotEmpty()) {
                item {
                    FxImpactSectionCard(section = rep.fxSection, fxImpactMinor = rep.fxImpactOnCashMinor)
                }
            }

            // 5. Dynamic Reconciliation Card
            item {
                ReconciliationChecksCard(reconciliation = rep.reconciliation)
            }

            // 6. Non-Cash Disclosures (Notes Section)
            if (rep.nonCashDisclosures.isNotEmpty()) {
                item {
                    NonCashDisclosuresCard(disclosures = rep.nonCashDisclosures)
                }
            }

            // 7. Drilldown Toggle Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showDrilldown = !showDrilldown }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showDrilldown) "إخفاء التفاصيل الفردية للقيود" else "عرض السجل التفصيلي للحركات النقدية (${rep.operatingSection.lines.size + rep.investingSection.lines.size + rep.financingSection.lines.size} حركة)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (showDrilldown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 8. Individual Movement Lines Drilldown
            if (showDrilldown) {
                val allFlowLines = (rep.operatingSection.lines + rep.investingSection.lines + rep.financingSection.lines)
                    .sortedByDescending { it.dateEpochDay }

                items(allFlowLines) { line ->
                    CashFlowLineItemRow(line = line)
                }
            }
        }
    }
}

@Composable
private fun CashFlowHeroCard(statement: CashFlowStatement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth().testTag("cash_flow_hero_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Three Pillars: Opening, Net Change, Closing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Opening Cash
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نقدية بداية الفترة",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.openingCashMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = AmountSemanticType.NEUTRAL,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Net Change
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "صافي التغير في النقدية",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.netChangeInCashMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = if (statement.netChangeInCashMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                        fontSize = 17,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Closing Cash
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "نقدية نهاية الفترة",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountText(
                        money = Money(statement.closingCashMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = AmountSemanticType.INCOME,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Account Equilibrium Breakdown (Vault 1101 + Banks 1102)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الصندوق (1101): ${Money(statement.vaultCashClosingMinor, CurrencyCode.FUNCTIONAL).format()}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AssetPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "البنوك والمحافظ (1102): ${Money(statement.bankCashClosingMinor, CurrencyCode.FUNCTIONAL).format()}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivitySectionCard(
    section: CashFlowSection,
    icon: ImageVector,
    accentColor: Color,
    testTagPrefix: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("${testTagPrefix}_section_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row (Clickable to collapse/expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = section.titleAr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "داخل: ${Money(section.totalInflowsMinor, CurrencyCode.FUNCTIONAL).format()} | خارج: ${Money(section.totalOutflowsMinor, CurrencyCode.FUNCTIONAL).format()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmountText(
                        money = Money(section.netFlowMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = if (section.netFlowMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                        fontSize = 15,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (section.categories.isEmpty()) {
                        Text(
                            text = "لا توجد تدفقات نقدية في هذا النشاط خلال الفترة.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        section.categories.forEach { cat ->
                            CategorySummaryRow(summary = cat)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(summary: CashFlowCategorySummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (summary.category.isOutflow) SemanticExpenseRed else SemanticIncomeGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = summary.category.titleAr,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " (${summary.count})",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AmountText(
            money = Money(summary.netFlowMinor, CurrencyCode.FUNCTIONAL),
            semanticType = if (summary.netFlowMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
            fontSize = 13,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FxImpactSectionCard(
    section: CashFlowSection,
    fxImpactMinor: Long
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("fx_impact_section_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SemanticWarningAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = SemanticWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "أثر تقلبات أسعار الصرف على النقدية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "فروق إعادة التقييم غير المحققة لحسابات النقدية الأجنبية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AmountText(
                money = Money(fxImpactMinor, CurrencyCode.FUNCTIONAL),
                semanticType = if (fxImpactMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                fontSize = 14,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReconciliationChecksCard(reconciliation: com.example.domain.report.ReconciliationSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reconciliation.reconciled) SemanticIncomeGreen.copy(alpha = 0.5f) else SemanticExpenseRed.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("cash_flow_reconciliation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reconciliation.reconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reconciliation.reconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مطابقة التوازن المالي للأستاذ (IAS 7 Reconciliation)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (reconciliation.reconciled) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticExpenseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (reconciliation.reconciled) "مطابق بنسبة 100%" else "غير متطابق",
                        color = if (reconciliation.reconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            reconciliation.checks.forEach { check ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (check.note.isNotBlank()) {
                            Text(
                                text = check.note,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!check.passed) {
                            Text(
                                text = "فارق: ${Money(check.discrepancyMinor, CurrencyCode.FUNCTIONAL).format()}",
                                fontSize = 11.sp,
                                color = SemanticExpenseRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        Icon(
                            imageVector = if (check.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (check.passed) SemanticIncomeGreen else SemanticExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NonCashDisclosuresCard(disclosures: List<NonCashDisclosureItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("non_cash_disclosures_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MikroTikCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إفصاحات الأنشطة الاستثمارية والتمويلية غير النقدية (إيضاحات)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            disclosures.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.typeAr} - ${item.partyName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.description} (سند #${item.docNumber} | الحساب: ${item.targetAccount})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AmountText(
                        money = Money(item.amountMinor, CurrencyCode.FUNCTIONAL),
                        semanticType = AmountSemanticType.NEUTRAL,
                        fontSize = 13,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowLineItemRow(line: CashFlowLineItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("cash_flow_line_${line.docNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when (line.category.activityType) {
                            CashFlowActivityType.OPERATING -> SemanticIncomeGreen.copy(alpha = 0.15f)
                            CashFlowActivityType.INVESTING -> MikroTikCyan.copy(alpha = 0.15f)
                            CashFlowActivityType.FINANCING -> AssetPurple.copy(alpha = 0.15f)
                            else -> SemanticWarningAmber.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = line.category.activityType.titleAr,
                            fontSize = 9.sp,
                            color = when (line.category.activityType) {
                                CashFlowActivityType.OPERATING -> SemanticIncomeGreen
                                CashFlowActivityType.INVESTING -> MikroTikCyan
                                CashFlowActivityType.FINANCING -> AssetPurple
                                else -> SemanticWarningAmber
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${line.docNumber} (${line.docType})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = line.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (line.partyName != null) {
                    Text(
                        text = "الطرف: ${line.partyName}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    money = Money(line.netCashFlowMinor, CurrencyCode.FUNCTIONAL),
                    semanticType = if (line.netCashFlowMinor >= 0) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                    fontSize = 13,
                    fontWeight = FontWeight.Bold
                )
                if (line.currency != CurrencyCode.FUNCTIONAL.name) {
                    Text(
                        text = "المبلغ الأصلي: ${Money(line.origMinor, CurrencyCode.fromString(line.currency)).format()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
