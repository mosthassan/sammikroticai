package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.CardAndQuotaTelemetry
import com.example.data.network.NetworkInterfaceTraffic
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.DecimalFormat

/**
 * Live Internet Speed, Interface Traffic Monitor & Real-Time Dual Waveform Charts
 * Matching MKCards Screenshot 2.
 */
@Composable
fun LiveInternetTrafficCard(
    interfaceTraffic: NetworkInterfaceTraffic,
    allInterfaces: List<NetworkInterfaceTraffic>,
    onSelectInterface: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isInterfaceDropdownOpen by remember { mutableStateOf(false) }

    // Pulsing green dot animation for live stream
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_internet_traffic_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2B)),
        border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: "سرعة الإنترنت" + Live Stream Badge + Interface Selector Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Stream Badge + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "سرعة الإنترنت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, StatusOnline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusOnline.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "بث مباشر",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOnline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interface Selector Dropdown
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isInterfaceDropdownOpen = true }
                            .testTag("btn_select_interface"),
                        color = Color(0xFF132A3E),
                        border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lan,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = interfaceTraffic.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(text = "▾", color = MikroTikCyan, fontSize = 12.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = isInterfaceDropdownOpen,
                        onDismissRequest = { isInterfaceDropdownOpen = false },
                        modifier = Modifier.background(CyberDarkCardElevated)
                    ) {
                        Text(
                            text = "واجهات شبكة الميكروتك (${allInterfaces.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MikroTikCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        allInterfaces.forEach { iface ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = iface.displayName,
                                            color = TextPrimaryDark,
                                            fontWeight = if (iface.id == interfaceTraffic.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (iface.id == interfaceTraffic.id) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MikroTikCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectInterface(iface.id)
                                    isInterfaceDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Big Live Metrics: Download (تحميل) & Upload (رفع)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Upload Column (رفع ⤒) - Amber
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "رفع", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Mbps ${interfaceTraffic.txMbps}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "GB ${interfaceTraffic.totalTxGb}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark
                    )
                }

                // Download Column (تحميل ⤓) - Emerald Cyan
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "تحميل", style = MaterialTheme.typography.bodyMedium, color = SemanticIncomeGreen, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = SemanticIncomeGreen, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Mbps ${interfaceTraffic.rxMbps}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SemanticIncomeGreen
                    )
                    Text(
                        text = "GB ${interfaceTraffic.totalRxGb}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark
                    )
                }
            }

            // 3. Dual Live Waveform Charts (Matching MkCards Screenshot 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Upload Waveform Box (رفع)
                WaveformBox(
                    title = "Mbps ${interfaceTraffic.txMbps}",
                    subtitle = "رفع",
                    isUpload = true,
                    maxScale = 41,
                    midScale = 20,
                    dataPoints = interfaceTraffic.txHistory,
                    waveColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )

                // Download Waveform Box (تحميل)
                WaveformBox(
                    title = "Mbps ${interfaceTraffic.rxMbps}",
                    subtitle = "تحميل",
                    isUpload = false,
                    maxScale = 4,
                    midScale = 2,
                    dataPoints = interfaceTraffic.rxHistory,
                    waveColor = SemanticIncomeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Reusable Waveform Chart Box with Canvas Grid and Cubic Curves
 */
@Composable
private fun WaveformBox(
    title: String,
    subtitle: String,
    isUpload: Boolean,
    maxScale: Int,
    midScale: Int,
    dataPoints: List<Float>,
    waveColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF091420),
        border = BorderStroke(1.dp, waveColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header: Value & Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = waveColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = waveColor
                    )
                    Icon(
                        imageVector = if (isUpload) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = waveColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Canvas Waveform with Grid and Area Gradient
            Box(modifier = Modifier.fillMaxSize()) {
                // Left scale labels
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "$maxScale", color = TextMutedDark, fontSize = 9.sp)
                    Text(text = "$midScale", color = TextMutedDark, fontSize = 9.sp)
                    Text(text = "0", color = TextMutedDark, fontSize = 9.sp)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 2.dp, top = 2.dp, bottom = 4.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw reference dashed horizontal grid lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, 0f),
                        end = Offset(w, 0f),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1f
                    )

                    if (dataPoints.size < 2) return@Canvas

                    val maxVal = maxScale.toFloat().coerceAtLeast(1f)
                    val stepX = w / (dataPoints.size - 1)

                    val strokePath = Path()
                    val fillPath = Path()

                    val firstY = h - ((dataPoints.first() / maxVal) * h).coerceIn(0f, h)
                    strokePath.moveTo(0f, firstY)
                    fillPath.moveTo(0f, h)
                    fillPath.lineTo(0f, firstY)

                    for (i in 0 until dataPoints.size - 1) {
                        val x0 = i * stepX
                        val y0 = h - ((dataPoints[i] / maxVal) * h).coerceIn(0f, h)
                        val x1 = (i + 1) * stepX
                        val y1 = h - ((dataPoints[i + 1] / maxVal) * h).coerceIn(0f, h)

                        val cx = (x0 + x1) / 2f
                        strokePath.cubicTo(cx, y0, cx, y1, x1, y1)
                        fillPath.cubicTo(cx, y0, cx, y1, x1, y1)
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Draw gradient fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                waveColor.copy(alpha = 0.28f),
                                waveColor.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw curve line
                    drawPath(
                        path = strokePath,
                        color = waveColor,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Card Telemetry & Accurate Megabytes Quota Card
 * Addresses: "باقي خدمة أضف هنا أيضا عدد الكروت المنتهية الصلاحية والميجا بدقة"
 */
@Composable
fun CardQuotaAndExpiredDetailsCard(
    telemetry: CardAndQuotaTelemetry,
    modifier: Modifier = Modifier
) {
    val df = remember { DecimalFormat("#,###") }
    val dfDec = remember { DecimalFormat("#,##0.0") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_quota_and_expired_details_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2B)),
        border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Quota Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DataUsage,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "رصيد الميجا والكروت المنتهية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0C243C)
                ) {
                    Text(
                        text = "إجمالي: ${df.format(telemetry.totalQuotaMb)} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MikroTikCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Accurate Megabytes & Gigabytes Consumption Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المستهلك بدقة: ${df.format(telemetry.consumedQuotaMb)} MB (${dfDec.format(telemetry.consumedGb)} GB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticWarningAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "المتبقي: ${df.format(telemetry.remainingQuotaMb)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusOnline,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (telemetry.consumedPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = SemanticWarningAmber,
                    trackColor = Color(0xFF1E293B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نسبة الاستهلاك: ${telemetry.consumedPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "سعة الرصيد المتاح: ${dfDec.format(telemetry.remainingGb)} GB",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusOnline
                    )
                }
            }

            // Four Cards Metrics Row (including Expired Cards & Remaining)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: الكروت المنتهية الصلاحية (Expired Cards) - Highlighted Red
                QuotaMiniCard(
                    title = "الكروت المنتهية",
                    value = "${telemetry.expiredCards}",
                    subtitle = "منتهية الصلاحية",
                    icon = Icons.Default.HourglassDisabled,
                    accentColor = SemanticExpenseRed,
                    modifier = Modifier.weight(1f)
                )

                // Card 2: الكروت المتبقية (Remaining Cards)
                QuotaMiniCard(
                    title = "الكروت المتبقية",
                    value = df.format(telemetry.remainingCards),
                    subtitle = "جاهزة للمستخدمين",
                    icon = Icons.Default.Style,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: الكروت المباعة النشطة
                QuotaMiniCard(
                    title = "الكروت المباعة",
                    value = df.format(telemetry.soldActiveCards),
                    subtitle = "كروت نشطة",
                    icon = Icons.Default.People,
                    accentColor = StatusOnline,
                    modifier = Modifier.weight(1f)
                )

                // Card 4: إجمالي الكروت المولدة
                QuotaMiniCard(
                    title = "إجمالي التوليد",
                    value = df.format(telemetry.totalGeneratedCards),
                    subtitle = "خلال 30 يوماً",
                    icon = Icons.Default.Bolt,
                    accentColor = MikroTikCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 4 MKCards Operations Live Stat Cards (Matching Screenshot 2 Under Speedometer)
 */
@Composable
fun MkCardsLiveTelemetryQuadRow(
    telemetry: CardAndQuotaTelemetry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: المتصلين الآن (53) & مستخدمي البروباند (0 / 0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // المتصلين الآن
            MkQuickStatCard(
                title = "المتصلين الآن",
                value = "${telemetry.onlineUsersNow}",
                icon = Icons.Default.Bolt,
                iconTint = Color(0xFFF59E0B),
                iconBg = Color(0xFF3D2506),
                modifier = Modifier.weight(1f)
            )

            // مستخدمي البروباند
            MkQuickStatCard(
                title = "مستخدمي البروباند",
                value = telemetry.broadbandUsers,
                icon = Icons.Default.People,
                iconTint = Color(0xFFA855F7),
                iconBg = Color(0xFF2E1065),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: يوزرات هوتسبوت (7691) & يوزرات المانجر (0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // يوزرات هوتسبوت
            MkQuickStatCard(
                title = "يوزرات هوتسبوت",
                value = "${telemetry.hotspotUsers}",
                icon = Icons.Default.Person,
                iconTint = Color(0xFF38BDF8),
                iconBg = Color(0xFF0C243C),
                modifier = Modifier.weight(1f)
            )

            // يوزرات المانجر
            MkQuickStatCard(
                title = "يوزرات المانجر",
                value = "${telemetry.userManagerUsers}",
                icon = Icons.Default.Security,
                iconTint = Color(0xFF60A5FA),
                iconBg = Color(0xFF1E3A8A),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuotaMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF091420),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextMutedDark, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MkQuickStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2B)),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action / Navigate Arrow
            Surface(
                shape = CircleShape,
                color = Color(0xFF172538),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right Content & Icon Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
