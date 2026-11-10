package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.card.CardCharacterSet
import com.example.data.card.CardTemplate
import com.example.data.card.PasswordPattern
import com.example.data.card.profileName
import com.example.data.card.quotaMb
import com.example.data.card.uptimeHours
import com.example.data.local.entity.CardPackageEntity
import com.example.ui.cardstudio.CardStudioTab
import com.example.ui.cardstudio.CardStudioViewModel
import com.example.ui.cardstudio.CardSystemTarget
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber

/**
 * Dedicated Clutter-Free Card Generator Screen (توليد الكروت السريع ⚡)
 * Separated from visual card design and canvas layout to maximize speed, simplicity, and user focus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCardGeneratorScreen(
    viewModel: CardStudioViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCardStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val availablePackages by viewModel.availablePackages.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var showAdvancedDetails by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "توليد الكروت السريع ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "إنشاء كروت وحقنها فوراً في الراوتر أو تجهيزها للطباعة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_card_generator")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick shortcut to Card Studio designer
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onNavigateToCardStudio() }
                            .testTag("btn_open_card_studio")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(15.dp))
                            Text("استوديو التصاميم 🎨", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. نظام التشغيل المستهدف (هوتسبوت أو يوزرمانجر)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. نوع النظام المستهدف في المايكروتك:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option A: Hotspot
                        val isHotspot = uiState.selectedSystemTarget == CardSystemTarget.HOTSPOT
                        Surface(
                            color = if (isHotspot) Color(0xFF064E3B) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (isHotspot) SemanticIncomeGreen else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectSystemTarget(CardSystemTarget.HOTSPOT) }
                                .testTag("target_hotspot")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (isHotspot) SemanticIncomeGreen else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "الهوتسبوت (Hotspot)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHotspot) Color.White else Color.LightGray
                                    )
                                    Text(
                                        text = "/ip/hotspot/user",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isHotspot) Color(0xFFA7F3D0) else Color.Gray
                                    )
                                }
                            }
                        }

                        // Option B: UserManager
                        val isUserMgr = uiState.selectedSystemTarget == CardSystemTarget.USER_MANAGER
                        Surface(
                            color = if (isUserMgr) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (isUserMgr) BrandCyanPrimary else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectSystemTarget(CardSystemTarget.USER_MANAGER) }
                                .testTag("target_user_manager")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = if (isUserMgr) BrandCyanPrimary else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "اليوزرمانجر (UserMgr)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUserMgr) Color.White else Color.LightGray
                                    )
                                    Text(
                                        text = "/user-manager/user",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isUserMgr) Color(0xFFBAE6FD) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. اختيار الباقة (Package Selection)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. اختيار باقة الشبكة:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${availablePackages.size} باقة معرّفة",
                            fontSize = 11.sp,
                            color = BrandCyanPrimary
                        )
                    }

                    if (availablePackages.isEmpty()) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = SemanticWarningAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "لا توجد باقات معرّفة حالياً. سيتم استخدام باقة افتراضية (500 ريال)",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availablePackages, key = { it.id }) { pkg ->
                                val isSelected = uiState.batchProfileInput == pkg.profileName || uiState.editingTemplate.targetPackageName == pkg.name
                                Surface(
                                    color = if (isSelected) Color(0xFF0F2B48) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, if (isSelected) BrandCyanPrimary else Color(0xFF334155)),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .clickable { viewModel.selectPackageForGeneration(pkg) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pkg.name,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        val priceVal = pkg.retailPriceMinor / 100L
                                        Text(
                                            text = "$priceVal ر.ي",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SemanticIncomeGreen
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))
                                        val quotaVal = pkg.quotaMb
                                        val uptimeVal = pkg.uptimeHours
                                        Text(
                                            text = "${if (quotaVal > 0) "$quotaVal ميجا" else "مفتوح"} • ${if (uptimeVal > 0) "$uptimeVal س" else "صلاحية 30 يوم"}",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = "بروفايل: ${pkg.profileName}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. اختيار قالب الكرت (Template Selection)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. اختيار مظهر وقالب الكرت:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "القالب النشط: ${uiState.editingTemplate.name}",
                            fontSize = 11.sp,
                            color = BrandCyanPrimary,
                            maxLines = 1
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates, key = { it.id }) { tpl ->
                            val isSelected = uiState.editingTemplate.id == tpl.id
                            Surface(
                                color = if (isSelected) Color(0xFF0B243B) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, if (isSelected) BrandCyanPrimary else Color(0xFF334155)),
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { viewModel.selectTemplate(tpl) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tpl.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Mini Theme Preview Strip
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(tpl.backgroundColorHex), Color(tpl.gradientEndColorHex))
                                                )
                                            )
                                            .border(0.5.dp, Color(tpl.primaryColorHex), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "820001",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (!tpl.customBackgroundImagePath.isNullOrBlank()) "🖼️ قالب استديو مخصص" else "🎨 سمة ${tpl.themePreset.name}",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. عدد الكروت المطلوبة (Card Count)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "4. عدد الكروت المطلوبة للتوليد:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(24, 42, 50, 100, 200).forEach { cnt ->
                            FilterChip(
                                selected = uiState.batchCountInput == cnt,
                                onClick = { viewModel.updateBatchConfig(count = cnt) },
                                label = {
                                    Text(
                                        text = when (cnt) {
                                            24 -> "24 كرت (ورقة A4 كاملة 3×8) ⭐"
                                            42 -> "42 كرت (ورقة A4 شريط 3×14)"
                                            else -> "$cnt كرت"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Stepper + Custom Number Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val next = (uiState.batchCountInput - 10).coerceAtLeast(1)
                                    viewModel.updateBatchConfig(count = next)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "إنقاص 10", tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = uiState.batchCountInput.toString(),
                                onValueChange = { str ->
                                    val num = str.toIntOrNull() ?: 1
                                    viewModel.updateBatchConfig(count = num.coerceIn(1, 5000))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandCyanPrimary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.width(110.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val next = (uiState.batchCountInput + 10).coerceAtMost(5000)
                                    viewModel.updateBatchConfig(count = next)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة 10", tint = Color.White)
                            }
                        }

                        Text(
                            text = "الإجمالي: ${uiState.batchCountInput} كرت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SemanticIncomeGreen
                        )
                    }
                }
            }

            // ==========================================
            // 5. تفاصيل الكود والبادئة (Expandable Specs)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedDetails = !showAdvancedDetails },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "5. معايير الأكواد وكلمة المرور ⚙️",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "طول الكود (${uiState.batchUserLengthInput} أرقام) • بادئة (${uiState.batchPrefixInput.ifBlank { "بدون" }})",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                        Text(
                            text = if (showAdvancedDetails) "إخفاء ▴" else "تعديل ▾",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyanPrimary
                        )
                    }

                    if (showAdvancedDetails) {
                        HorizontalDividerClean()

                        // Title / Batch Comment
                        OutlinedTextField(
                            value = uiState.batchTitleInput,
                            onValueChange = { viewModel.updateBatchConfig(title = it) },
                            label = { Text("عنوان أو تعليق الدفعة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = BrandCyanPrimary,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Prefix & Length
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.batchPrefixInput,
                                onValueChange = { viewModel.updateBatchConfig(prefix = it) },
                                label = { Text("بادئة الكود (مثال: 82)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandCyanPrimary,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedLabelColor = BrandCyanPrimary,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )

                            // Length Stepper
                            Column(modifier = Modifier.weight(1f)) {
                                Text("طول الكود: ${uiState.batchUserLengthInput} خانات", fontSize = 11.sp, color = Color.LightGray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val len = (uiState.batchUserLengthInput - 1).coerceAtLeast(4)
                                            viewModel.updateBatchConfig(userLength = len)
                                        },
                                        modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "تقليص", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }

                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${uiState.batchUserLengthInput} أرقام",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val len = (uiState.batchUserLengthInput + 1).coerceAtMost(12)
                                            viewModel.updateBatchConfig(userLength = len)
                                        },
                                        modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Pattern Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.batchPasswordPattern == PasswordPattern.SAME_AS_USERNAME,
                                onClick = { viewModel.updateBatchConfig(pattern = PasswordPattern.SAME_AS_USERNAME) },
                                label = { Text("المستخدم = كلمة المرور", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandCyanPrimary, selectedLabelColor = Color.Black)
                            )
                            FilterChip(
                                selected = uiState.batchPasswordPattern == PasswordPattern.NUMBERS_ONLY,
                                onClick = { viewModel.updateBatchConfig(pattern = PasswordPattern.NUMBERS_ONLY) },
                                label = { Text("كلمة مرور منفصلة", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandCyanPrimary, selectedLabelColor = Color.Black)
                            )
                        }

                        // Character Set
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.batchCharacterSet == CardCharacterSet.NUMBERS_ONLY,
                                onClick = { viewModel.updateBatchConfig(characterSet = CardCharacterSet.NUMBERS_ONLY) },
                                label = { Text("أرقام فقط (123456)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandCyanPrimary, selectedLabelColor = Color.Black)
                            )
                            FilterChip(
                                selected = uiState.batchCharacterSet == CardCharacterSet.UPPERCASE_ALPHANUMERIC,
                                onClick = { viewModel.updateBatchConfig(characterSet = CardCharacterSet.UPPERCASE_ALPHANUMERIC) },
                                label = { Text("أرقام وحروف (A8K2F9)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandCyanPrimary, selectedLabelColor = Color.Black)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. Action Execution Buttons (أزرار التنفيذ)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Button 1: Direct Injection to RouterOS
                Button(
                    onClick = {
                        viewModel.generateBatchWithAction(andInjectToRouter = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_generate_and_inject")
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري توليد الكروت وحقن الراوتر...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val sysLabel = if (uiState.selectedSystemTarget == CardSystemTarget.USER_MANAGER) "اليوزرمانجر" else "الهوتسبوت"
                        Text(
                            text = "توليد وحقن مباشر في $sysLabel ⚡",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Button 2: Generate for Print & PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.generateBatchWithAction(andInjectToRouter = false) { batch ->
                                onNavigateToCardStudio()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_generate_for_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد للطباعة (PDF) 🖨️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.generateBatchWithAction(andInjectToRouter = false) { batch ->
                                viewModel.shareMikrotikScript(context, batch)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سكربت RSC 📜", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Router Direct Injection: In-Progress Dialog
    if (uiState.isInjectingToRouter) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = CyberDarkCardElevated,
            icon = {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("جاري حقن الكروت في المايكروتك ⚡", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val progress = uiState.injectProgress
                    if (progress != null && progress.second > 0) {
                        val fraction = progress.first.toFloat() / progress.second.toFloat()
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = BrandCyanPrimary,
                            trackColor = Color(0xFF1E293B)
                        )
                        Text(
                            text = "تم إرسال ${progress.first} من أصل ${progress.second} كرت عبر API إلى سيرفر الراوتر...",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = BrandCyanPrimary, modifier = Modifier.size(28.dp))
                            Text(
                                text = "جاري التحقق من بروفايل الباقة والاتصال بسيرفر الراوتر...",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        )
    }

    // Router Direct Injection: Missing Profile Warning Dialog
    val missingProfile = uiState.missingProfileWarning
    if (missingProfile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMissingProfileWarning() },
            containerColor = CyberDarkCardElevated,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SemanticExpenseRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("تنبيه بروفايل الباقة ⚠️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "بروفايل الباقة '${missingProfile.second}' غير معرّف حالياً على الراوتر.\n\nهل ترغب بالمتابعة وحقن الكروت على أي حال وتجاوز فحص البروفايل؟",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.injectBatchToRouter(missingProfile.first, skipProfileCheck = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                ) {
                    Text("متابعة الحقن ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMissingProfileWarning() }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Router Direct Injection: Success Confirmation Dialog
    val injectSuccessCount = uiState.injectSuccessDialogCount
    if (injectSuccessCount != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissInjectSuccessDialog() },
            containerColor = CyberDarkCardElevated,
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SemanticIncomeGreen, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("تم الحقن بنجاح ⚡🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                val sys = if (uiState.selectedSystemTarget == CardSystemTarget.USER_MANAGER) "اليوزرمانجر" else "الهوتسبوت"
                Text(
                    text = "تم بنجاح حقن $injectSuccessCount كرت في نظام $sys بنجاح وبأعلى سرعة! الكروت مفعلة ومخزنة في الراوتر وجاهزة لاستخدام العملاء فوراً.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissInjectSuccessDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen)
                ) {
                    Text("تم", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun HorizontalDividerClean() {
    androidx.compose.material3.HorizontalDivider(
        color = Color(0xFF334155),
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
