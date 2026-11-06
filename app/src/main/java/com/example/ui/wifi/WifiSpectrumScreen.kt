package com.example.ui.wifi

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import com.example.data.wifi.ScanState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.wifi.ChannelRating
import com.example.data.wifi.ScanRefreshInterval
import com.example.data.wifi.WifiAccessPoint
import com.example.data.wifi.WifiBand
import com.example.data.wifi.WifiSecurityType
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen

@Composable
fun WifiSpectrumScreen(
    viewModel: WifiSpectrumViewModel = viewModel(),
    onNavigateToSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val licenseManager = remember { com.example.util.LicenseManager.getInstance(context) }
    val subscriptionStatus by licenseManager.subscriptionStatus.collectAsState()
    val isPro = subscriptionStatus.isPro && !subscriptionStatus.isExpired

    if (!isPro) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CyberDarkCanvas)
                .padding(24.dp)
                .testTag("wifi_spectrum_locked_container"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "محلل طيف الواي فاي مقفل",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = com.example.domain.subscription.SubscriptionLimits.WIFI_SPECTRUM_LOCKED_MESSAGE_AR,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "✨ ميزات النسخة الاحترافية (PRO):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyanPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• تحليل فوري لترددات 2.4GHz و 5GHz", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("• فحص تداخل القنوات وحساب التشويش ونقاط التغطية", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("• فواتير وأجهزة مايكروتك غير محدودة", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateToSubscription,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("upgrade_pro_wifi_btn")
                    ) {
                        Text(
                            text = "ترقية الاشتراك وتفعيل Pro الآن",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val licMgr = com.example.util.LicenseManager.getInstance(context)
                            val hwId = licMgr.hardwareDeviceId
                            val msg = com.example.domain.subscription.SubscriptionLimits.buildActivationWhatsAppMessage(hwId)
                            com.example.util.WhatsAppDispatcher.sendTextMessage(
                                context = context,
                                phoneNumber = com.example.domain.subscription.SubscriptionLimits.ACTIVATION_WHATSAPP_NUMBER,
                                message = msg
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("whatsapp_activate_wifi_btn")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_whatsapp),
                            contentDescription = "واتساب",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "للتفعيل راسلنا وتساب على الرقم 770446040",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    var showIntervalMenu by remember { mutableStateOf(false) }

    // Runtime Permission Launcher for FINE_LOCATION and NEARBY_WIFI_DEVICES
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true
        } else {
            true
        }

        if (fineLocationGranted || nearbyGranted) {
            viewModel.onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionsAndScan()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkCanvas)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Permission Warning Banner if missing
        if (!uiState.hasPermissions || uiState.scanState is ScanState.NoPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wifi_permission_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A05)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "صلاحيات المسح اللاسلكي مطلوبة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = "يتطلب أندرويد إذن الموقع الدقيق أو الأجهزة القريبة لفحص ترددات وقنوات الـ WiFi المحيطة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_grant_wifi_permissions")
                        ) {
                            Text("منح الصلاحية", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 1.5. WiFi Disabled Banner
        if (uiState.scanState is ScanState.WifiDisabled || !uiState.isWifiEnabled) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wifi_disabled_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A05)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "محول Wi-Fi غير مفعل",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = "يرجى تشغيل Wi-Fi من إعدادات الجهاز لتمكين الماسح اللاسلكي من التقاط الإشارات الحقيقية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 1.6. Error Banner if ScanState.Error
        val errorState = uiState.scanState as? ScanState.Error
        if (errorState != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wifi_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = SemanticExpenseRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SemanticExpenseRed)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = SemanticExpenseRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "خطأ فحص الترددات: ${errorState.message}",
                            color = SemanticExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Control Bar (Band Toggles, Scan Button, Auto-refresh Interval)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 2.4 GHz vs 5.0 GHz Toggle Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.selectedBand == WifiBand.BAND_2_4_GHZ,
                                onClick = { viewModel.setSelectedBand(WifiBand.BAND_2_4_GHZ) },
                                label = { Text("نطاق 2.4 GHz", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCyanPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("chip_band_24ghz")
                            )

                            FilterChip(
                                selected = uiState.selectedBand == WifiBand.BAND_5_GHZ,
                                onClick = { viewModel.setSelectedBand(WifiBand.BAND_5_GHZ) },
                                label = { Text("نطاق 5.0 GHz", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCyanPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("chip_band_5ghz")
                            )
                        }

                        // Right side: Auto-refresh interval dropdown + Scan trigger
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                OutlinedButton(
                                    onClick = { showIntervalMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(uiState.refreshInterval.label, fontSize = 11.sp)
                                }

                                DropdownMenu(
                                    expanded = showIntervalMenu,
                                    onDismissRequest = { showIntervalMenu = false }
                                ) {
                                    ScanRefreshInterval.entries.forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval.label) },
                                            onClick = {
                                                viewModel.setRefreshInterval(interval)
                                                showIntervalMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.performScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !uiState.isScanning,
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("btn_trigger_wifi_scan")
                            ) {
                                if (uiState.isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Telemetry summary counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تم رصد ${uiState.filteredAccessPoints.size} شبكة في نطاق ${uiState.selectedBand.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "إجمالي الشبكات: ${uiState.allAccessPoints.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandCyanPrimary
                        )
                    }

                    // Sub-Tabs Navigation Row (matching WiFi Analyzer bottom bar: Channel Rating, Channel Graph, Live Meter, APs)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WifiSpectrumViewTab.entries.forEach { tab ->
                            val isTabSelected = uiState.selectedViewTab == tab
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTabSelected) BrandCyanPrimary.copy(alpha = 0.2f) else CyberDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isTabSelected) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectViewTab(tab) }
                                    .testTag("tab_wifi_${tab.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            WifiSpectrumViewTab.CHANNEL_RATING -> Icons.Default.Star
                                            WifiSpectrumViewTab.CHANNEL_GRAPH -> Icons.Default.Sensors
                                            WifiSpectrumViewTab.TIME_GRAPH -> Icons.Default.Speed
                                            WifiSpectrumViewTab.ACCESS_POINTS -> Icons.Default.WifiFind
                                        },
                                        contentDescription = tab.titleAr,
                                        tint = if (isTabSelected) BrandCyanPrimary else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.titleAr,
                                        fontSize = 10.sp,
                                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTabSelected) BrandCyanPrimary else Color.LightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // View 1: Channel Rating View (Matching WiFi Analyzer screenshot)
        if (uiState.selectedViewTab == WifiSpectrumViewTab.CHANNEL_RATING) {
            item {
                WifiChannelRatingView(
                    recommendation = uiState.recommendation,
                    currentConnection = uiState.currentConnection,
                    isScanThrottlingEnabled = uiState.isScanThrottlingEnabled,
                    selectedBand = uiState.selectedBand
                )
            }
        }

        // View 2: Channel Recommendation & RF Spectrum Graph (M3 Graph)
        if (uiState.selectedViewTab == WifiSpectrumViewTab.CHANNEL_GRAPH) {
            uiState.recommendation?.let { rec ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_channel_recommendation"),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            when (rec.rating) {
                                ChannelRating.EXCELLENT -> SemanticIncomeGreen
                                ChannelRating.GOOD -> BrandCyanPrimary
                                ChannelRating.MODERATE -> Color(0xFFFFB300)
                                ChannelRating.CONGESTED -> SemanticExpenseRed
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SemanticIncomeGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = SemanticIncomeGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "محرك التوصية الذكية بالقناة (Channel Recommendation)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "تحليل التداخل الكهرومغناطيسي وازدحام الترددات",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SemanticIncomeGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SemanticIncomeGreen)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("القناة الأفضل", fontSize = 9.sp, color = SemanticIncomeGreen)
                                        Text(
                                            text = "CH ${rec.recommendedChannel}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = SemanticIncomeGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = rec.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = BrandCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مخطط طيف الترددات (RF Spectrum Graph)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "dBm vs Channel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    WifiSpectrumGraph(
                        accessPoints = uiState.filteredAccessPoints,
                        band = uiState.selectedBand,
                        recommendation = uiState.recommendation,
                        selectedAp = uiState.selectedApForMeter,
                        modifier = Modifier.testTag("wifi_spectrum_canvas")
                    )
                }
            }
        }

        // View 3: Live Technician Signal Meter
        if (uiState.selectedViewTab == WifiSpectrumViewTab.TIME_GRAPH) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = BrandCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مقياس الإشارة المباشر لضبط الهوائيات (Live Alignment Meter)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    WifiLiveSignalMeter(
                        selectedAp = uiState.selectedApForMeter,
                        signalHistory = uiState.signalHistory,
                        minRssi = uiState.minRssiDbm,
                        maxRssi = uiState.maxRssiDbm,
                        avgRssi = uiState.avgRssiDbm,
                        modifier = Modifier.testTag("wifi_live_signal_meter")
                    )
                }
            }
        }

        // View 4 or General Search Bar & Detected APs List
        if (uiState.selectedViewTab == WifiSpectrumViewTab.ACCESS_POINTS || uiState.selectedViewTab == WifiSpectrumViewTab.CHANNEL_RATING) {

        // 6. Search Bar & AP Scan List
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiFind,
                            contentDescription = null,
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "قائمة نقاط البث المكتشفة (${uiState.filteredAccessPoints.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث باسم الشبكة (SSID) أو BSSID أو القناة...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandCyanPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyanPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = CyberDarkCardElevated,
                        unfocusedContainerColor = CyberDarkCardElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_wifi_aps")
                )
            }
        }

        // Items: Detected Access Points or Explicit Empty State
        if (uiState.filteredAccessPoints.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wifi_empty_state_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiFind,
                            contentDescription = null,
                            tint = BrandCyanPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotBlank())
                                "لا توجد شبكات مطابقة لعبارة البحث \"${uiState.searchQuery}\""
                            else
                                "لم يتم العثور على أي شبكات لاسلكية محيطة في نطاق ${uiState.selectedBand.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "لا يتم توليد أي شبكات وهمية أو مصطنعة. تظهر الشبكات فقط عند التقاطها فعلياً من عتاد الجهاز.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.performScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_empty_rescan")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة الفحص المباشر", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredAccessPoints,
                key = { it.bssid }
            ) { ap ->
                AccessPointCard(
                    ap = ap,
                    isSelected = uiState.selectedApForMeter?.bssid == ap.bssid,
                    onSelectForMeter = { viewModel.selectApForMeter(ap) }
                )
            }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccessPointCard(
    ap: WifiAccessPoint,
    isSelected: Boolean,
    onSelectForMeter: () -> Unit
) {
    val signalColor = when {
        ap.rssiDbm >= -55 -> SemanticIncomeGreen
        ap.rssiDbm >= -70 -> BrandCyanPrimary
        ap.rssiDbm >= -82 -> Color(0xFFFFB300)
        else -> SemanticExpenseRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectForMeter() }
            .testTag("card_ap_${ap.bssid.replace(":", "")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyberDarkCardElevated.copy(alpha = 0.9f) else CyberDarkCardElevated
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) signalColor else BrandCyanPrimary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(signalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = signalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ap.displaySsid,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (ap.securityType == WifiSecurityType.OPEN) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (ap.securityType == WifiSecurityType.OPEN) Color(0xFFFFB300) else BrandCyanPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = ap.bssid,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                // RSSI & Signal Quality
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${ap.rssiDbm} dBm",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = signalColor
                    )
                    Text(
                        text = "${ap.signalPercent}% جودة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row: Channel, Frequency, Width, Security
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApBadge(text = "القناة ${ap.channel}", color = BrandCyanPrimary)
                ApBadge(text = "${ap.frequencyMhz} MHz", color = Color(0xFF80D8FF))
                ApBadge(text = "${ap.channelWidthMhz} MHz", color = Color(0xFFB388FF))
                ApBadge(text = ap.securityType.displayName, color = Color(0xFFFFAB40))

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onSelectForMeter,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) signalColor else CyberDarkSurface
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("btn_select_meter_${ap.bssid.replace(":", "")}")
                ) {
                    Text(
                        text = if (isSelected) "الموجه نشط" else "توجيه الإشارة",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else BrandCyanPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ApBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
