package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.AdminIssuedLicense
import com.example.util.GeneratedLicenseResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onOpenTeamAccessCodes: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val adminSigner = viewModel.adminLicenseSigner

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Generator, 1: Cloud Sync, 2: History, 3: Inspector

    // Generator inputs
    var deviceIdInput by remember { mutableStateOf("") }
    var customerNameInput by remember { mutableStateOf("") }
    var durationMonths by remember { mutableIntStateOf(12) }
    var durationExpanded by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<GeneratedLicenseResult?>(null) }
    var generatorError by remember { mutableStateOf<String?>(null) }

    // Cloud Subscription inputs
    var cloudTargetUid by remember { mutableStateOf("") }
    var cloudCustomerName by remember { mutableStateOf("") }
    var cloudDurationMonths by remember { mutableIntStateOf(12) }
    var isSubmittingCloud by remember { mutableStateOf(false) }
    var cloudActionMessage by remember { mutableStateOf<String?>(null) }
    var isCloudError by remember { mutableStateOf(false) }

    // History state
    var issuedHistory by remember { mutableStateOf(adminSigner.getIssuedLicenses()) }

    // Inspector inputs
    var inspectKeyInput by remember { mutableStateOf("") }
    var inspectionResult by remember { mutableStateOf<String?>(null) }
    var isInspectionValid by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberDarkCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة تحكم المسؤول (Admin Panel)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = currentUser?.email ?: "المسؤول الأعلى للنظام",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCyanPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (onOpenTeamAccessCodes != null) {
                        OutlinedButton(
                            onClick = onOpenTeamAccessCodes,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("admin_open_team_codes_btn"),
                            border = BorderStroke(1.dp, BrandCyanPrimary)
                        ) {
                            Text("إدارة الوكلاء والأكواد", color = BrandCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "SUPER ADMIN",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Tab Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("🔑 إصدار مفتاح", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD700),
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("☁️ تفعيل سحابي", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = {
                        issuedHistory = adminSigner.getIssuedLicenses()
                        selectedTab = 2
                    },
                    label = { Text("📋 السجل (${issuedHistory.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    label = { Text("🔍 فحص مفتاح", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: OFFLINE KEY GENERATOR
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "توليد وتوقيع ترخيص رقمي (RSA-2048)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = "يتم توقيع الترخيص بالمفتاح الخاص المعتمد ليعمل بدون إنترنت على جهاز العميل:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = deviceIdInput,
                                        onValueChange = { deviceIdInput = it.uppercase() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_device_id_input"),
                                        label = { Text("بصمة جهاز العميل (Hardware ID)") },
                                        placeholder = { Text("SAM-XXXX-XXXX-XXXX أو * للكل") },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip
                                                if (clip != null && clip.itemCount > 0) {
                                                    deviceIdInput = clip.getItemAt(0).text?.toString()?.uppercase() ?: ""
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "لصق",
                                                    tint = BrandCyanPrimary
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFD700),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = customerNameInput,
                                        onValueChange = { customerNameInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("اسم العميل / اسم الشبكة") },
                                        placeholder = { Text("مثال: شبكة الأفق نت") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandCyanPrimary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Duration Selector
                                    ExposedDropdownMenuBox(
                                        expanded = durationExpanded,
                                        onExpandedChange = { durationExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = when (durationMonths) {
                                                1 -> "شهر واحد (30 يوماً)"
                                                6 -> "6 أشهر (180 يوماً)"
                                                12 -> "سنة كاملة (365 يوماً - الموصى به)"
                                                0 -> "دائم مدى الحياة (Lifetime Enterprise)"
                                                else -> "$durationMonths شهراً"
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                            label = { Text("مدة صلاحية الترخيص") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandCyanPrimary,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        ExposedDropdownMenu(
                                            expanded = durationExpanded,
                                            onDismissRequest = { durationExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("شهر واحد (30 يوماً)") },
                                                onClick = { durationMonths = 1; durationExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("6 أشهر (180 يوماً)") },
                                                onClick = { durationMonths = 6; durationExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("سنة كاملة (365 يوماً)") },
                                                onClick = { durationMonths = 12; durationExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("دائم مدى الحياة (Lifetime)") },
                                                onClick = { durationMonths = 0; durationExpanded = false }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            generatorError = null
                                            val res = viewModel.generateAdminLicenseToken(
                                                deviceId = deviceIdInput,
                                                durationMonths = durationMonths,
                                                customerName = customerNameInput
                                            )
                                            if (res.isSuccess) {
                                                generatedResult = res.getOrNull()
                                                issuedHistory = adminSigner.getIssuedLicenses()
                                            } else {
                                                generatorError = res.exceptionOrNull()?.message ?: "خطأ أثناء التوليد"
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("admin_generate_key_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("توليد وتوقيع المفتاح الرقمي", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    generatorError?.let { err ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(text = err, color = SemanticExpenseRed, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Generated Token Output Card
                        generatedResult?.let { res ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16251E)),
                                    border = BorderStroke(1.dp, SemanticIncomeGreen)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SemanticIncomeGreen
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "تم إصدار المفتاح بنجاح!",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SemanticIncomeGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "العميل: ${res.payload.customerName} | الجهاز: ${res.payload.deviceId} | الصلاحية: ${res.expiryFormatted}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = res.token,
                                                color = Color(0xFF64FFDA),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("License Key", res.token)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "تم نسخ المفتاح للحافظة", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("نسخ المفتاح", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    val name = res.payload.customerName.ifBlank { "العميل الكريم" }
                                                    val msg = "مرحباً بك $name! 🎉\nإليك مفتاح تفعيل النسخة الاحترافية (PRO) لتطبيق SamMikrotik:\n\n${res.token}\n\nطريقة التفعيل: افتح التطبيق -> الإعدادات -> إدارة الاشتراك -> الصق المفتاح واضغط تفعيل."
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, msg)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة المفتاح مع العميل عبر:"))
                                                },
                                                modifier = Modifier.weight(1.3f),
                                                colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("إرسال للعميل (WhatsApp)", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: CLOUD ACTIVATION IN FIRESTORE
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "تفعيل الاشتراك السحابي (Firebase Cloud Sync)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyanPrimary
                                    )
                                    Text(
                                        text = "كتابة وثيقة الاشتراك مباشرة في Firestore في المسار users/{uid}/subscription/status ليتفعل حساب العميل تلقائياً بمجرد فتح التطبيق:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = cloudTargetUid,
                                        onValueChange = { cloudTargetUid = it.trim() },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("معرف المستخدم في Firebase (User UID أو البريد)") },
                                        placeholder = { Text("UID المسجل في Firebase Auth") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandCyanPrimary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = cloudCustomerName,
                                        onValueChange = { cloudCustomerName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("اسم العميل / الشبكة") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandCyanPrimary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            if (cloudTargetUid.isBlank()) {
                                                cloudActionMessage = "يرجى إدخال معرف المستخدم المستهدف (UID)."
                                                isCloudError = true
                                                return@Button
                                            }
                                            isSubmittingCloud = true
                                            viewModel.setCloudSubscriptionForUser(
                                                targetUid = cloudTargetUid,
                                                tier = "PRO",
                                                months = cloudDurationMonths,
                                                customerName = cloudCustomerName
                                            ) { success, msg ->
                                                isSubmittingCloud = false
                                                isCloudError = !success
                                                cloudActionMessage = msg
                                            }
                                        },
                                        enabled = !isSubmittingCloud,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isSubmittingCloud) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("تفعيل الاشتراك السحابي في Firestore", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    cloudActionMessage?.let { msg ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = msg,
                                            color = if (isCloudError) SemanticExpenseRed else SemanticIncomeGreen,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: ISSUED LICENSES HISTORY
                        if (issuedHistory.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                                ) {
                                    Text(
                                        text = "لا توجد مفاتيح صادرة مسجلة في هذا الجهاز حتى الآن.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        } else {
                            items(issuedHistory) { item ->
                                IssuedLicenseCard(
                                    license = item,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("License Key", item.token)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ المفتاح للعميل [${item.customerName}]", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        adminSigner.deleteIssuedLicense(item.id)
                                        issuedHistory = adminSigner.getIssuedLicenses()
                                    }
                                )
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: KEY INSPECTOR & VERIFIER
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "فحص والتحقق من صحة مفتاح تفعيل",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandCyanPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = inspectKeyInput,
                                        onValueChange = { inspectKeyInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("الصق رمز التفعيل هنا") },
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandCyanPrimary,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val res = viewModel.licenseManager.activateLicenseKey(inspectKeyInput)
                                            if (res.isSuccess) {
                                                val st = res.getOrThrow()
                                                isInspectionValid = true
                                                inspectionResult = "✅ المفتاح سليم وموقّع رقمياً!\nالجهاز: ${st.hardwareDeviceId}\nالخطة: ${st.tier.name}\nالعميل: ${st.customerName ?: "غير محدد"}"
                                            } else {
                                                isInspectionValid = false
                                                inspectionResult = "❌ غير صالح: ${res.exceptionOrNull()?.message}"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("فحص التوقيع وصلاحية المفتاح", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    inspectionResult?.let { resText ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = if (isInspectionValid) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticExpenseRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isInspectionValid) SemanticIncomeGreen else SemanticExpenseRed)
                                        ) {
                                            Text(
                                                text = resText,
                                                color = if (isInspectionValid) SemanticIncomeGreen else SemanticExpenseRed,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(12.dp)
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
    }
}

@Composable
private fun IssuedLicenseCard(
    license: AdminIssuedLicense,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, Color(0xFF2C3E50))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = license.customerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "الجهاز: ${license.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCyanPrimary
                    )
                }

                Surface(
                    color = if (license.expirationEpochMs == null) Color(0xFFFFD700).copy(alpha = 0.2f) else BrandCyanPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (license.expirationEpochMs == null) "Lifetime" else "${license.durationMonths} شهور",
                        color = if (license.expirationEpochMs == null) Color(0xFFFFD700) else BrandCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCopy) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", tint = BrandCyanPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = SemanticExpenseRed)
                }
            }
        }
    }
}
