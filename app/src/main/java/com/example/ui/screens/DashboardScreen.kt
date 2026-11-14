package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.data.sync.SyncState
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.InvariantBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.AiGeminiPurple
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextPrimaryDark
import androidx.compose.runtime.remember
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PeriodFilter
import com.example.util.findActivity
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SmartToy

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onNavigateToQuickSale: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToReceipts: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToWifiSpectrum: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToCardStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val periodFilter by viewModel.selectedPeriodFilter.collectAsState()
    val invariantResult by viewModel.invariantResult.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Station & Financial Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_app_title_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CyberBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    CyberBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .background(Color(0xFF0B1426)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "شعار سام ميكروتك الرسمي",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "محطة شبكة طلقة نت",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusOnline)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "منظومة إدارة الميكروتك والمحاسبة المزدوجة IFRS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Operational Telemetry Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F1E36),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CCR2004 متصل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B291B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF165437)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnline,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دفتر IFRS متزن",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A1A2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32325A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = AiGeminiPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "10.10.0.0/16",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Invariant Status Banner
        item {
            InvariantBanner(
                isValid = invariantResult?.isValid ?: true,
                violationsCount = invariantResult?.violations?.size ?: 0,
                debitTotal = invariantResult?.trialBalanceDebit ?: 0L,
                creditTotal = invariantResult?.trialBalanceCredit ?: 0L,
                onRunCheck = { viewModel.runInvariantCheck() }
            )
        }

        // Google Account & Cloud Sync Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_cloud_sync_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CyberBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0C243C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مزامنة Firebase السحابية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val fbAuthUser = runCatching {
                                    if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser else null
                                }.getOrNull()
                                Text(
                                    text = if (fbAuthUser != null) (fbAuthUser.email ?: currentUser?.email ?: "حساب Google موثق")
                                           else if (currentUser != null) "${currentUser?.email} (بانتظار توثيق Google)"
                                           else "غير متصل بحساب Google",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (fbAuthUser != null) MikroTikCyan else if (currentUser != null) SemanticWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        val isSyncing = syncState is SyncState.InProgress
                        val fbAuthUser = runCatching {
                            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser else null
                        }.getOrNull()
                        val isOnline = fbAuthUser != null
                        val hasLocalEmail = currentUser != null
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSyncing) Color(0xFF0C243C)
                                    else if (isOnline) Color(0xFF0B291B)
                                    else if (hasLocalEmail) Color(0xFF332005)
                                    else Color(0xFF380D15),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSyncing) MikroTikCyan.copy(alpha = 0.5f)
                                else if (isOnline) StatusOnline.copy(alpha = 0.5f)
                                else if (hasLocalEmail) SemanticWarningAmber.copy(alpha = 0.5f)
                                else SemanticExpenseRed.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (isSyncing) "جاري الرفع..."
                                       else if (isOnline) "سحابة موثقة"
                                       else if (hasLocalEmail) "توثيق Google مطلوب"
                                       else "غير متصل",
                                color = if (isSyncing) MikroTikCyan
                                        else if (isOnline) StatusOnline
                                        else if (hasLocalEmail) SemanticWarningAmber
                                        else SemanticExpenseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val fbUser = runCatching {
                                    if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser else null
                                }.getOrNull()
                                if (fbUser == null) {
                                    viewModel.signInWithGoogle(activity ?: context)
                                } else {
                                    viewModel.syncPushToFirebase()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MikroTikPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_dashboard_sync_now")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مزامنة الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.signInWithGoogle(activity ?: context)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_dashboard_google_login")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دخول Google بنقرة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Quick Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToQuickSale,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MikroTikPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_quick_sale")
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بيع سريع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigateToReceipts,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SemanticIncomeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_receipt_voucher")
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سند قبض", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigateToPayments,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SemanticExpenseRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_payment_voucher")
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سند صرف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Network Tool Banner: WiFi Spectrum & Channel Inspector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWifiSpectrum() }
                    .testTag("dashboard_wifi_inspector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CyberBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C2A4D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = MikroTikPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "فاحص طيف وقنوات WiFi الميداني",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0C2A4D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "2.4G / 5G",
                                    fontSize = 10.sp,
                                    color = MikroTikPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تحليل الازدحام الراديوي، تقييم القنوات بالنجوم، وتوجيه الهوائيات اللحظي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MikroTikPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Hotspot Card Studio Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCardStudio() }
                    .testTag("dashboard_card_studio_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CyberBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF042F2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = MikroTikCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "استوديو كروت الميكروتك الذكية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF332405),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SemanticWarningAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "جديد ⚡",
                                    fontSize = 10.sp,
                                    color = SemanticWarningAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "محرر بصري متكامل، توليد بالدفعات، QR تلقائي، وحقن RouterOS فوري.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // AI Assistant (Copilot) Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAiAssistant() }
                    .testTag("dashboard_ai_assistant_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDarkCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CyberBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2E1065)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AiGeminiPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مساعد سامي الذكي (ERP AI Copilot)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2E1065),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AiGeminiPurple.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Gemini 3.8",
                                    fontSize = 10.sp,
                                    color = AiGeminiPurple,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تحليلات مالية دقيقة للقوائم والأرباح، ومساعد تشغيل أجهزة الشبكة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = AiGeminiPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Period Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = periodFilter == filter,
                        onClick = { viewModel.setPeriodFilter(filter) },
                        label = { Text(filter.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Financial KPIs Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "مبيعات الفترة",
                    amount = Money(summary.totalSalesPeriodMinor, CurrencyCode.FUNCTIONAL),
                    icon = Icons.Default.Receipt,
                    semanticType = AmountSemanticType.INCOME,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "مصروفات وتكاليف",
                    amount = Money(summary.totalExpensesPeriodMinor, CurrencyCode.FUNCTIONAL),
                    icon = Icons.Default.Payments,
                    semanticType = AmountSemanticType.EXPENSE,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "صافي ربح النشاط",
                    amount = Money(summary.netCashFlowMinor, CurrencyCode.FUNCTIONAL),
                    icon = Icons.Default.AccountBalance,
                    semanticType = AmountSemanticType.AUTO,
                    subtitle = "وفق معيار تكلفة الخدمة IFRS",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToReports() }
                )
                StatCard(
                    title = "ديون الوكلاء (الذمم)",
                    amount = Money(summary.totalReceivablesMinor, CurrencyCode.FUNCTIONAL),
                    icon = Icons.Default.Money,
                    semanticType = AmountSemanticType.AUTO,
                    subtitle = "إجمالي مستحقات الشبكة",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Multi-Currency Treasury Balances
        item {
            SectionHeader(title = "أرصدة الخزائن والصناديق")
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_treasury_balances_card")
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val yer = summary.treasuryBalancesByCurrency["YER"] ?: 0L
                    val usd = summary.treasuryBalancesByCurrency["USD"] ?: 0L
                    val sar = summary.treasuryBalancesByCurrency["SAR"] ?: 0L

                    TreasuryBalanceRow("صندوق الريال اليمني (YER)", Money(yer, CurrencyCode.YER), SemanticIncomeGreen, Modifier.testTag("row_vault_yer"))
                    TreasuryBalanceRow("خزينة الدولار الأمريكي (USD)", Money(usd, CurrencyCode.USD), MikroTikPrimary, Modifier.testTag("row_vault_usd"))
                    TreasuryBalanceRow("خزينة الريال السعودي (SAR)", Money(sar, CurrencyCode.SAR), SemanticWarningAmber, Modifier.testTag("row_vault_sar"))
                }
            }
        }

        // Financial Trend Flow (Executive Canvas Chart)
        item {
            SectionHeader(title = "مؤشر التدفق المالي اللحظي")
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "منحنى التدفق التشغيلي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F1E36),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B))
                        ) {
                            Text(
                                text = "مباشر من دفتر الأستاذ",
                                fontSize = 10.sp,
                                color = MikroTikCyan,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FinancialTrendCanvas(
                        sales = summary.totalSalesPeriodMinor,
                        expenses = summary.totalExpensesPeriodMinor,
                        profit = summary.netCashFlowMinor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المبيعات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المصروفات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("صافي الأرباح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreasuryBalanceRow(
    title: String,
    money: Money,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        AmountText(money = money, semanticType = AmountSemanticType.AUTO, fontSize = 14)
    }
}

@Composable
fun FinancialTrendCanvas(
    sales: Long,
    expenses: Long,
    profit: Long,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxVal = maxOf(sales, expenses, kotlin.math.abs(profit), 1000L).toFloat()
        val salesY = height - (sales.toFloat() / maxVal * (height * 0.8f))
        val expY = height - (expenses.toFloat() / maxVal * (height * 0.8f))
        val profitY = height - ((profit.toFloat() + maxVal) / (maxVal * 2f) * (height * 0.8f))

        // Draw baseline
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, height - 2f),
            end = Offset(width, height - 2f),
            strokeWidth = 2f
        )

        // Draw smooth bezier curve between points
        val path = Path().apply {
            moveTo(0f, height * 0.7f)
            cubicTo(
                width * 0.3f, salesY,
                width * 0.6f, expY,
                width, profitY
            )
        }

        drawPath(
            path = path,
            color = Color(0xFF06B6D4),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Draw indicator points
        drawCircle(color = Color(0xFF10B981), radius = 6f, center = Offset(width * 0.3f, salesY))
        drawCircle(color = Color(0xFFEF4444), radius = 6f, center = Offset(width * 0.6f, expY))
        drawCircle(color = Color(0xFF3B82F6), radius = 6f, center = Offset(width, profitY))
    }
}
