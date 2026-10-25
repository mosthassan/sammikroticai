package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.domain.report.ReconciliationCheck
import com.example.domain.report.ReconciliationSummary
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * Reusable reconciliation status badge for reports and financial statements.
 * Displays ✓ (green) if reconciled, or ✗ (red) if any reconciliation check failed.
 * Tapping the badge opens an interactive details dialog with individual check calculations.
 */
@Composable
fun ReconciliationStatusBadge(
    reconciliation: ReconciliationSummary,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val isReconciled = reconciliation.reconciled
    val statusColor = if (isReconciled) SemanticIncomeGreen else SemanticExpenseRed
    val statusBg = if (isReconciled) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticExpenseRed.copy(alpha = 0.15f)
    val statusText = if (isReconciled) {
        "مطابق محاسبياً ✓"
    } else {
        "عدم تطابق (${reconciliation.failedCount}) ✗"
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .background(statusBg)
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("reconciliation_status_badge"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDialog) {
        ReconciliationDetailsDialog(
            reconciliation = reconciliation,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Detailed breakdown dialog displaying individual reconciliation checks.
 * Formats all financial amounts using Money(...).format() without displaying raw minor units.
 */
@Composable
fun ReconciliationDetailsDialog(
    reconciliation: ReconciliationSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reconciliation_details_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reconciliation.reconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (reconciliation.reconciled) SemanticIncomeGreen else SemanticExpenseRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فحوصات السلامة المحاسبية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (reconciliation.reconciled) {
                        "جميع القيود والحسابات متطابقة مع الأستاذ العام بنسبة 100%."
                    } else {
                        "تم رصد فروقات غير مطابقة تتطلب المراجعة والتسوية."
                    },
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(reconciliation.checks) { index, check ->
                        ReconciliationCheckItemCard(check = check, index = index)
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reconciliation_dialog_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun ReconciliationCheckItemCard(
    check: ReconciliationCheck,
    index: Int
) {
    val cardColor = if (check.passed) {
        CyberDarkCardElevated
    } else {
        SemanticExpenseRed.copy(alpha = 0.08f)
    }
    val borderColor = if (check.passed) {
        SemanticIncomeGreen.copy(alpha = 0.3f)
    } else {
        SemanticExpenseRed.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reconciliation_check_item_$index")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = check.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (check.passed) SemanticIncomeGreen.copy(alpha = 0.2f) else SemanticExpenseRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (check.passed) "✓ مطابق" else "✗ فارق",
                        color = if (check.passed) SemanticIncomeGreen else SemanticExpenseRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المتوقع: ${Money(check.expectedMinor, CurrencyCode.FUNCTIONAL).format()}",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "الفعلي: ${Money(check.actualMinor, CurrencyCode.FUNCTIONAL).format()}",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!check.passed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "قيمة الفرق: ${Money(check.discrepancyMinor, CurrencyCode.FUNCTIONAL).format()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SemanticExpenseRed,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (check.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = check.note,
                    fontSize = 10.sp,
                    color = TextMutedDark
                )
            }
        }
    }
}
