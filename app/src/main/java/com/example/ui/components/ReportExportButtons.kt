package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticIncomeGreen

/**
 * Modern cyber-fintech export button bar for financial reports.
 * Provides instant Arabic PDF generation and standard-compliant CSV export.
 */
@Composable
fun ReportExportButtons(
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier,
    pdfLabel: String = "تصدير تقرير PDF",
    csvLabel: String = "تصدير ملف CSV",
    tagPrefix: String = "report"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onExportPdf,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("${tagPrefix}_export_pdf_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
            border = BorderStroke(1.dp, MikroTikCyan)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = pdfLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
        }

        OutlinedButton(
            onClick = onExportCsv,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("${tagPrefix}_export_csv_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticIncomeGreen),
            border = BorderStroke(1.dp, SemanticIncomeGreen)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = csvLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
        }
    }
}
