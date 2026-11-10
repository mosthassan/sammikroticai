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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.NetworkRepository
import com.example.ui.components.CardQuotaAndExpiredDetailsCard
import com.example.ui.components.LiveInternetTrafficCard
import com.example.ui.components.MkCardsLiveTelemetryQuadRow
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
    onNavigateToQuickCardGenerator: () -> Unit = onNavigateToCardStudio,
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
            title = "توليد الكروت ⚡",
            icon = Icons.Default.Bolt,
            iconColor = Color(0xFF00F0FF),
            iconBgColor = Color(0xFF0B2B3E),
            tag = "service_card_generator",
            onClick = onNavigateToQuickCardGenerator
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
        // Unified Executive Router Telemetry Card (Merging Screenshot 1 hardware card into the upper section of Screenshot 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_active_router_telemetry"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF062C24)),
                border = BorderStroke(1.dp, Color(0xFF0F5A47))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A3B30),
                                    Color(0xFF05221B)
                                )
                            )
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ROW 1: Router IP & Icon | Live Status / Devices Badge | Refresh ↻ | Switch Router Button (NO Cloud)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Router IP and Icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0E5343)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Router,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (activeRouter != null) activeRouter?.host ?: "10.0.0.1" else "10.0.0.1",
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (isLiveConnected) "قراءة حية (API 8728)" else "غير متصل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLiveConnected) StatusOnline else SemanticWarningAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Connected devices badge
                            Surface(
                                color = Color(0xFF0D4638),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF156955))
                            ) {
                                Text(
                                    text = "${telemetry.totalDiscoveredDevices} الأجهزة المتصلة",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Actions: Refresh + Switch Router (NO Cloud Icon)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.refreshRealRouterData() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحديث القياسات من الراوتر",
                                        tint = if (isLiveConnected) Color(0xFF6EE7B7) else SemanticWarningAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0D3E33),
                                    border = BorderStroke(1.dp, Color(0xFF16604E)),
                                    modifier = Modifier.clickable { onNavigateToRouterLogin() }
                                ) {
                                    Text(
                                        text = "تبديل ▾",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // ROW 2: Uptime (مدة التشغيل) & RouterOS Version (إصدار الراوتر)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مدة التشغيل ${telemetry.uptime}",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إصدار الراوتر ${telemetry.routerOsVersion}",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // ROW 3: CPU Load % & Temperature with Cooling Fan Warning
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المعالج ${telemetry.cpuLoadPercent}%",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(0.55f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الحرارة ${telemetry.temperatureCelsius}°",
                                            color = Color(0xFFF1F5F9),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (telemetry.needsCoolingFanAlert) {
                                        Surface(
                                            color = Color(0xFF114234),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFF1E6C56))
                                        ) {
                                            Text(
                                                text = "تفعيل مروحة التبريد",
                                                color = Color(0xFF6EE7B7),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ROW 4: Memory (RAM) Progress Bar & Active Users (المستخدمون النشطون - Pointed by red arrow in Screenshot 1!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Users Count (المستخدمون النشطون)
                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المستخدمون النشطون ${telemetry.activeUsersCount}",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // RAM usage progress bar with clean MB formatting
                            Surface(
                                color = Color(0xFF0B2D24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF165243)),
                                modifier = Modifier.weight(0.55f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الذاكرة", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        val formattedRam = String.format(java.util.Locale.US, "%.1f MB / 1 GB", telemetry.memoryUsedMb)
                                        Text(
                                            text = formattedRam,
                                            fontSize = 10.sp,
                                            color = Color(0xFFF1F5F9),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { (telemetry.memoryUsedMb / telemetry.memoryTotalMb).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF10B981),
                                        trackColor = Color(0xFF06221A),
                                    )
                                }
                            }
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
