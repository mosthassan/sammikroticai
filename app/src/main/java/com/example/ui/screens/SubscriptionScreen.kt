package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.subscription.SubscriptionLimits
import com.example.domain.subscription.SubscriptionTier
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.TransactionDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()
    val totalInvoices by viewModel.totalActiveInvoicesCount.collectAsState()
    val networkDevices by viewModel.networkRepository.devices.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val totalDevices = networkDevices.size

    val isPro = subscriptionStatus.isPro && !subscriptionStatus.isExpired
    val hardwareId = viewModel.licenseManager.hardwareDeviceId

    var activationKeyInput by remember { mutableStateOf("") }
    var isCheckingCloud by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberDarkCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة الاشتراك والتفعيل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPro) "الخطة الاحترافية (PRO) نشطة" else "الخطة المجانية (Free)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPro) Color(0xFFFFD700) else BrandCyanPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("subscription_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Super Admin Control Card (If Admin is logged in)
            if (isAdmin) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("super_admin_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E2113)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFFFD700))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "حساب المسؤول الأعلى (Super Admin)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = currentUser?.email ?: "mosthassan.ye@gmail.com",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "أنت مسجل الدخول بحساب المسؤول المعتمد. لديك وصول كامل وغير محدود لجميع الميزات، ويمكنك إدارة تراخيص المشتركين وإصدار مفاتيح التفعيل مباشرة من هاتفك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToAdmin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_admin_dashboard_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فتح لوحة تحكم المسؤول (إصدار وتفعيل التراخيص)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 1. Hero Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscription_hero_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPro) Color(0xFF16251E) else Color(0xFF131B24)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isPro) Color(0xFFFFD700) else BrandCyanPrimary.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isPro) Color(0xFFFFD700).copy(alpha = 0.2f) else BrandCyanPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPro) Icons.Default.WorkspacePremium else Icons.Default.Router,
                                        contentDescription = null,
                                        tint = if (isPro) Color(0xFFFFD700) else BrandCyanPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPro) "SamMikrotik PRO" else "SamMikrotik Free",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPro) Color(0xFFFFD700) else Color.White
                                )
                                Text(
                                    text = if (isPro) "ترخيص تجاري كامل الميزات" else "نسخة تجريبية مجانية ومحدودة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPro) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = if (isPro) "PRO ACTIVE" else "FREE TIER",
                                    color = if (isPro) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isPro) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (subscriptionStatus.isLifetime) "الصلاحية: دائم مدى الحياة (Lifetime)"
                                    else "ينتهي في: ${subscriptionStatus.expirationEpochMs?.let { TransactionDateUtils.formatDateTime(it) } ?: "غير محدد"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0E0E0)
                                )
                                subscriptionStatus.remainingDays?.let { days ->
                                    Text(
                                        text = "$days يوم متبقي",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (days < 10) SemanticExpenseRed else SemanticIncomeGreen
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "محدودية الخطة المجانية: 100 فاتورة فقط، 50 جهازاً، ومحلل الواي فاي مقفل.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFCC80)
                            )
                        }
                    }
                }
            }

            // WhatsApp Activation Request Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_activation_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2417)),
                    border = BorderStroke(1.5.dp, Color(0xFF25D366))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF25D366).copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "طلب تفعيل ترخيص SamMikrotik PRO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "للتفعيل راسلنا وتساب على الرقم 770446040",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF25D366)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لترقية حسابك وإصدار مفتاح التفعيل الرسمي، راسلنا عبر واتساب مباشرة وسنقوم بتفعيل حسابك فوراً:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hardware ID", hardwareId)
                                clipboard.setPrimaryClip(clip)

                                com.example.util.WhatsAppDispatcher.sendTextMessage(
                                    context = context,
                                    phoneNumber = SubscriptionLimits.ACTIVATION_WHATSAPP_NUMBER,
                                    message = SubscriptionLimits.buildActivationWhatsAppMessage(hardwareId)
                                )
                                Toast.makeText(context, "تم نسخ معرف جهازك وجاري فتح واتساب للتفعيل...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_request_activation_whatsapp"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "راسلنا وتساب على الرقم 770446040",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 2. Resource Consumption Tracker
            item {
                Text(
                    text = "مؤشرات استهلاك الموارد الحالية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyanPrimary
                )
            }

            item {
                val invoiceLimit = SubscriptionLimits.FREE_MAX_INVOICES
                val invoiceProgress = if (isPro) 0.3f else (totalInvoices.toFloat() / invoiceLimit).coerceIn(0f, 1f)
                val isInvoiceNearLimit = !isPro && totalInvoices >= 80

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إجمالي الفواتير (مبيعات + مشتريات)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isPro) "$totalInvoices / غير محدود" else "$totalInvoices / $invoiceLimit",
                                fontWeight = FontWeight.Bold,
                                color = if (isInvoiceNearLimit) SemanticExpenseRed else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (isPro) 0.15f else invoiceProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPro) Color(0xFFFFD700) else if (isInvoiceNearLimit) SemanticExpenseRed else BrandCyanPrimary,
                            trackColor = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                val deviceLimit = SubscriptionLimits.FREE_MAX_DEVICES
                val deviceProgress = if (isPro) 0.2f else (totalDevices.toFloat() / deviceLimit).coerceIn(0f, 1f)
                val isDeviceNearLimit = !isPro && totalDevices >= 40

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = BrandCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "أجهزة الشبكة والمايكروتك",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isPro) "$totalDevices / غير محدود" else "$totalDevices / $deviceLimit",
                                fontWeight = FontWeight.Bold,
                                color = if (isDeviceNearLimit) SemanticExpenseRed else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (isPro) 0.15f else deviceProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPro) Color(0xFFFFD700) else if (isDeviceNearLimit) SemanticExpenseRed else BrandCyanPrimary,
                            trackColor = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 3. Hardware Fingerprint (Device ID) Card
            item {
                Text(
                    text = "بصمة الجهاز للتفعيل (Hardware ID)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyanPrimary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, Color(0xFF2C3E50))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "هذا المعرف مخصص لجهازك فقط، ويُستخدم في لوحة الإدارة لإصدار مفتاح تفعيل رقمي مشفر غير متصل:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = hardwareId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE082),
                                modifier = Modifier.testTag("hardware_id_text")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Hardware ID", hardwareId)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ معرف الجهاز بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("copy_hardware_id_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نسخ", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Hardware ID", hardwareId)
                                        clipboard.setPrimaryClip(clip)

                                        com.example.util.WhatsAppDispatcher.sendTextMessage(
                                            context = context,
                                            phoneNumber = SubscriptionLimits.ACTIVATION_WHATSAPP_NUMBER,
                                            message = SubscriptionLimits.buildActivationWhatsAppMessage(hardwareId)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("whatsapp_hardware_id_btn")
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("واتساب", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Offline Activation Key Input
            item {
                Text(
                    text = "تفعيل المفتاح الرقمي (Offline License)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandCyanPrimary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "أدخل رمز التفعيل المشفر (الذي حصلت عليه من لوحة الإدارة):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = activationKeyInput,
                            onValueChange = { activationKeyInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_key_input"),
                            label = { Text("رمز التفعيل (License Key)") },
                            placeholder = { Text("Base64.Payload.Signature...") },
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            activationKeyInput = clip.getItemAt(0).text?.toString() ?: ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "لصق من الحافظة",
                                        tint = BrandCyanPrimary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                actionMessage = null
                                viewModel.activateLicenseKey(activationKeyInput) { result ->
                                    if (result.isSuccess) {
                                        isError = false
                                        actionMessage = "تم تفعيل الترخيص الاحترافي (PRO) بنجاح!"
                                        activationKeyInput = ""
                                    } else {
                                        isError = true
                                        actionMessage = result.exceptionOrNull()?.message ?: "فشل التحقق من رمز التفعيل"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("activate_license_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPro) Color(0xFF2E7D32) else BrandCyanPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفعيل الاشتراك الرقمي الآن",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        actionMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = if (isError) SemanticExpenseRed.copy(alpha = 0.2f) else SemanticIncomeGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isError) SemanticExpenseRed else SemanticIncomeGreen)
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isError) SemanticExpenseRed else SemanticIncomeGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Cloud Auto-Renewal Check
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = BrandCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "التحقق من التفعيل السحابي (Firebase)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "مزامنة التجديدات الصادرة من لوحة الإدارة السحابية تلقائياً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                isCheckingCloud = true
                                viewModel.syncSubscriptionFromCloud { success, msg ->
                                    isCheckingCloud = false
                                    isError = !success
                                    actionMessage = msg
                                }
                            },
                            enabled = !isCheckingCloud,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("check_cloud_subscription_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandCyanPrimary
                            ),
                            border = BorderStroke(1.dp, BrandCyanPrimary)
                        ) {
                            if (isCheckingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BrandCyanPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("مزامنة والتحقق من الاشتراك عبر السحاب")
                        }
                    }
                }
            }

            // 6. Deactivate option if currently Pro
            if (isPro) {
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.deactivateLicense()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deactivate_license_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SemanticExpenseRed
                        ),
                        border = BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.5f))
                    ) {
                        Text("إلغاء التفعيل والرجوع للخطة المجانية (Reset License)")
                    }
                }
            }

            // 7. Comparison Table Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "مقارنة ميزات الخطط",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FeatureComparisonRow(
                            feature = "عدد الفواتير (مبيعات + مشتريات)",
                            freeVal = "100 كحد أقصى",
                            proVal = "غير محدود"
                        )
                        FeatureComparisonRow(
                            feature = "أجهزة الشبكة والمايكروتك",
                            freeVal = "50 جهازاً",
                            proVal = "غير محدود"
                        )
                        FeatureComparisonRow(
                            feature = "محلل طيف الواي فاي وفحص القنوات",
                            freeVal = "مقفل 🔒",
                            proVal = "متاح بالكامل ✅"
                        )
                        FeatureComparisonRow(
                            feature = "مساعد سامي للذكاء الاصطناعي (AI ERP)",
                            freeVal = "أساسي",
                            proVal = "متقدم وغير محدود ✅"
                        )
                        FeatureComparisonRow(
                            feature = "النسخ الاحتياطي السحابي المشفر",
                            freeVal = "متاح",
                            proVal = "متاح مع التزامن الفوري"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FeatureComparisonRow(
    feature: String,
    freeVal: String,
    proVal: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = freeVal,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = proVal,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
