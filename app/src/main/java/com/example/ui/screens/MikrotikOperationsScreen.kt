package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.NetworkRepository
import com.example.ui.components.CardQuotaAndExpiredDetailsCard
import com.example.ui.components.LiveInternetTrafficCard
import com.example.ui.components.MkCardsLiveTelemetryQuadRow
import com.example.ui.components.RouterHealthTelemetryCard
import com.example.ui.theme.AiGeminiPurple
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel

data class ServiceGridItem(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val tag: String,
    val onClick: () -> Unit
)

/**
 * Dedicated MikroTik Telecom Operations Hub (Matching MkCards Screenshot 4).
 * Separates MikroTik field engineering and network operations from the accounting ledger.
 */
@Composable
fun MikrotikOperationsScreen(
    networkRepository: NetworkRepository,
    viewModel: AppViewModel,
    onNavigateToDeviceMonitor: () -> Unit,
    onNavigateToCardStudio: () -> Unit,
    onNavigateToWifiSpectrum: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToRouterLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telemetry by networkRepository.telemetry.collectAsState()
    val devices by networkRepository.devices.collectAsState()
    val activeRouter by viewModel.activeRouterProfile.collectAsState()
    val liveInterfaces by viewModel.liveInterfaces.collectAsState()
    val selectedIface by viewModel.selectedInterfaceTraffic.collectAsState()
    val cardQuota by viewModel.cardQuotaTelemetry.collectAsState()
    val isLiveConnected by viewModel.isRouterLiveConnected.collectAsState()

    val services = listOf(
        ServiceGridItem(
            title = "بوابة تسجيل ودخول الراوترات",
            icon = Icons.Default.VpnKey,
            iconColor = SemanticWarningAmber,
            iconBgColor = Color(0xFF451A03),
            tag = "service_router_login",
            onClick = onNavigateToRouterLogin
        ),
        ServiceGridItem(
            title = "مراقبة الأجهزة والـ ARP",
            icon = Icons.Default.Router,
            iconColor = MikroTikCyan,
            iconBgColor = Color(0xFF042F2E),
            tag = "service_device_monitor",
            onClick = onNavigateToDeviceMonitor
        ),
        ServiceGridItem(
            title = "تصاميم واستوديو الكروت",
            icon = Icons.Default.Style,
            iconColor = Color(0xFF38BDF8),
            iconBgColor = Color(0xFF0C243C),
            tag = "service_card_studio",
            onClick = onNavigateToCardStudio
        ),
        ServiceGridItem(
            title = "طيف وقنوات WiFi الميداني",
            icon = Icons.Default.WifiTethering,
            iconColor = Color(0xFF818CF8),
            iconBgColor = Color(0xFF1E1B4B),
            tag = "service_wifi_spectrum",
            onClick = onNavigateToWifiSpectrum
        ),
        ServiceGridItem(
            title = "مساعد سامي الذكي (AI)",
            icon = Icons.Default.AutoAwesome,
            iconColor = AiGeminiPurple,
            iconBgColor = Color(0xFF2E1065),
            tag = "service_ai_assistant",
            onClick = onNavigateToAiAssistant
        ),
        ServiceGridItem(
            title = "نظام الهوتسبوت (Hotspot)",
            icon = Icons.Default.Wifi,
            iconColor = Color(0xFF34D399),
            iconBgColor = Color(0xFF064E3B),
            tag = "service_hotspot",
            onClick = onNavigateToDeviceMonitor
        ),
        ServiceGridItem(
            title = "نظام اليوزرمجر (UserManager)",
            icon = Icons.Default.People,
            iconColor = Color(0xFF60A5FA),
            iconBgColor = Color(0xFF1E3A8A),
            tag = "service_user_manager",
            onClick = onNavigateToCardStudio
        ),
        ServiceGridItem(
            title = "الأكتف والمستخدمون النشطون",
            icon = Icons.Default.Lan,
            iconColor = SemanticWarningAmber,
            iconBgColor = Color(0xFF451A03),
            tag = "service_active_users",
            onClick = onNavigateToDeviceMonitor
        ),
        ServiceGridItem(
            title = "النسخ الاحتياطي السحابي",
            icon = Icons.Default.CloudUpload,
            iconColor = Color(0xFF2DD4BF),
            iconBgColor = Color(0xFF134E4A),
            tag = "service_backup",
            onClick = onNavigateToCloudSync
        ),
        ServiceGridItem(
            title = "سجل الأنشطة الحديثة",
            icon = Icons.Default.History,
            iconColor = Color(0xFFF472B6),
            iconBgColor = Color(0xFF500724),
            tag = "service_activity_log",
            onClick = onNavigateToDeviceMonitor
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkCanvas)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Router Quick Banner (Allows switching router profile anytime)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRouterLogin() }
                    .testTag("banner_active_router"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2B34)),
                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MikroTikCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = SemanticWarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (activeRouter != null) "الراوتر النشط: ${activeRouter?.name}" else "لم يتم ربط راوتر بعد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (isLiveConnected) {
                                    "${activeRouter?.host}:${activeRouter?.apiPort} • قراءة حية حقيقية (RouterOS API)"
                                } else if (activeRouter != null) {
                                    "${activeRouter?.host}:${activeRouter?.apiPort} • غير متصل (تأكد من الشبكة)"
                                } else {
                                    "انقر لتسجيل الدخول إلى الراوتر"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLiveConnected) StatusOnline else SemanticWarningAmber
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.refreshRealRouterData() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث القراءة من الراوتر",
                                tint = if (isLiveConnected) StatusOnline else SemanticWarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkSurface,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.clickable { onNavigateToRouterLogin() }
                        ) {
                            Text(
                                text = "تبديل الراوتر ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Live Internet Speedometer & Traffic Waveform Cards (Matching MkCards Screenshot 2)
        item {
            LiveInternetTrafficCard(
                interfaceTraffic = selectedIface,
                allInterfaces = liveInterfaces,
                onSelectInterface = { viewModel.selectTrafficInterface(it) },
                isLiveConnected = isLiveConnected,
                onRefreshFromRouter = { viewModel.refreshRealRouterData() }
            )
        }

        // 2. MkCards Live Telemetry Quad Row (المتصلين الآن, البروباند, يوزرات الهوتسبوت, والمانجر)
        item {
            MkCardsLiveTelemetryQuadRow(
                telemetry = cardQuota
            )
        }

        // 3. Card Quota Telemetry & Expired Cards Breakdown (الكروت المنتهية الصلاحية والميجا بدقة)
        item {
            CardQuotaAndExpiredDetailsCard(
                telemetry = cardQuota
            )
        }

        // 4. Live Hardware Telemetry Card (MkCards hardware card: Uptime, OS, CPU%, Temp with fan alert, RAM)
        item {
            RouterHealthTelemetryCard(
                telemetry = telemetry,
                onRefresh = { viewModel.refreshRealRouterData() }
            )
        }

        // 3. Live Announcement Banner
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📢🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استوديو الكروت الذكي ومساعد الذكاء الاصطناعي مفعّلان وجاهزان",
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        color = Color(0xFF92400E),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onNavigateToCardStudio() }
                    ) {
                        Text(
                            text = "عرض",
                            color = Color(0xFFFEF3C7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 4. Section Title: "الخدمات"
        item {
            Text(
                text = "الخدمات الميدانية والتشغيلية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        // 5. Services Grid (MkCards Services Grid with clean icons)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val chunked = services.chunked(3)
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            ServiceGridCard(
                                item = item,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperationStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, color = TextSecondaryDark, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = TextMutedDark)
        }
    }
}

@Composable
fun ServiceGridCard(
    item: ServiceGridItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag(item.tag)
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.iconBgColor)
                    .border(BorderStroke(1.dp, item.iconColor.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}
