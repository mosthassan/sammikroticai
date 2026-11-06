package com.example.ui.cardstudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.card.CardBatch
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.GeneratedCard
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.util.MikroTikCardProvisioner
import com.example.util.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.userMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "استوديو كروت الميكروتك (Card Studio)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "تصميم احترافي، ثيمات نيون وVIP، كروت كشط، وطباعة A4",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCyanPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_card_studio")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetTemplatesToDefault() }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "استعادة القوالب الافتراضية",
                            tint = BrandCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
                .padding(innerPadding)
        ) {
            // Sub-tabs navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardStudioTab.entries.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.22f) else CyberDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .clickable { viewModel.selectTab(tab) }
                            .testTag("tab_card_studio_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    CardStudioTab.TEMPLATES -> Icons.Default.Style
                                    CardStudioTab.DESIGNER -> Icons.Default.Palette
                                    CardStudioTab.GENERATOR -> Icons.Default.Add
                                    CardStudioTab.BATCHES -> Icons.Default.Print
                                },
                                contentDescription = null,
                                tint = if (isSelected) BrandCyanPrimary else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.titleAr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandCyanPrimary else Color.LightGray
                            )
                        }
                    }
                }
            }

            // Main Tab View Content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.activeTab) {
                    CardStudioTab.TEMPLATES -> TemplatesGalleryTab(
                        templates = templates,
                        currentTemplateId = uiState.editingTemplate.id,
                        onSelectTemplate = { viewModel.setEditingTemplate(it) },
                        onQuickGenerate = {
                            viewModel.setEditingTemplate(it)
                            viewModel.selectTab(CardStudioTab.GENERATOR)
                        },
                        onDelete = { viewModel.deleteTemplate(it.id) }
                    )
                    CardStudioTab.DESIGNER -> ProfessionalDesignerTab(
                        template = uiState.editingTemplate,
                        onUpdate = { viewModel.updateEditingTemplate(it) },
                        onApplyTheme = { viewModel.applyThemePreset(it) },
                        onApplyCodeStyle = { viewModel.applyCodeBoxStyle(it) },
                        onSave = { viewModel.saveEditingTemplate() }
                    )
                    CardStudioTab.GENERATOR -> FastGeneratorTab(
                        uiState = uiState,
                        template = uiState.editingTemplate,
                        onUpdateConfig = { t, c, p, pref, len, pat, fmt ->
                            viewModel.updateBatchConfig(t, c, p, pref, len, pat, fmt)
                        },
                        onGenerate = { viewModel.generateBatch() }
                    )
                    CardStudioTab.BATCHES -> ProfessionalBatchesTab(
                        batches = batches,
                        selectedBatch = uiState.selectedBatchForPreview,
                        onSelectBatch = { viewModel.selectBatchForPreview(it) },
                        onPrintPdf = { batch, format -> viewModel.exportAndSharePdf(context, batch, format) },
                        onShareScript = { batch -> viewModel.shareMikrotikScript(context, batch) },
                        onDeleteBatch = { viewModel.deleteBatch(it) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Templates Gallery (Matching SamMikrotik Web Engine Presets)
// -------------------------------------------------------------
@Composable
private fun TemplatesGalleryTab(
    templates: List<CardTemplate>,
    currentTemplateId: String,
    onSelectTemplate: (CardTemplate) -> Unit,
    onQuickGenerate: (CardTemplate) -> Unit,
    onDelete: (CardTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "قوالب كروت الإنترنت الاحترافية (${templates.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "اختر قالباً جاهزاً للتعديل السريع أو التوليد الفوري",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onSelectTemplate(CardTemplate(name = "قالب مخصص جديد")) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنشاء قالب", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        items(templates, key = { it.id }) { tpl ->
            val isCurrent = tpl.id == currentTemplateId
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isCurrent) 1.8.dp else 1.dp,
                    if (isCurrent) BrandCyanPrimary else Color(tpl.primaryColorHex).copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tpl.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BrandCyanPrimary.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BrandCyanPrimary)
                                    ) {
                                        Text(
                                            text = "محدد حالياً",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandCyanPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${tpl.networkName} • ${tpl.priceText} • ${tpl.quotaText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(tpl.primaryColorHex).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(tpl.primaryColorHex))
                        ) {
                            Text(
                                text = tpl.themePreset.displayNameAr.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(tpl.primaryColorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Authentic Live Preview
                    CardTicketLivePreview(
                        template = tpl,
                        sampleUsername = "770446",
                        samplePassword = "770446",
                        scale = 0.95f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (templates.size > 1) {
                            IconButton(onClick = { onDelete(tpl) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SemanticExpenseRed)
                            }
                        }

                        OutlinedButton(
                            onClick = { onSelectTemplate(tpl) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تخصيص في المحرر", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onQuickGenerate(tpl) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توليد كروت فوراً", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Professional Designer Tab (WYSIWYG Live Canvas)
// -------------------------------------------------------------
@Composable
private fun ProfessionalDesignerTab(
    template: CardTemplate,
    onUpdate: (CardTemplate) -> Unit,
    onApplyTheme: (CardThemePreset) -> Unit,
    onApplyCodeStyle: (CodeBoxStyle) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sticky Hero Live Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المعاينة الحية لبطاقة الكرت (Live Canvas):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${template.cardWidthMm} × ${template.cardHeightMm} مم",
                            fontSize = 11.sp,
                            color = BrandCyanPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CardTicketLivePreview(
                        template = template,
                        sampleUsername = "770446",
                        samplePassword = "770446",
                        scale = 1.0f
                    )
                }
            }
        }

        // Theme Preset Quick Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "اختر الثيم اللوني للكرت (Theme Preset):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardThemePreset.entries.forEach { preset ->
                            val isCurrent = template.themePreset == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) BrandCyanPrimary.copy(alpha = 0.25f) else CyberDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) BrandCyanPrimary else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.clickable { onApplyTheme(preset) }
                            ) {
                                Text(
                                    text = preset.displayNameAr,
                                    fontSize = 11.sp,
                                    color = if (isCurrent) BrandCyanPrimary else Color.LightGray,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Code Box Style Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ستايل صندوق الكود ورمز الدخول (Code Box Style):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodeBoxStyle.entries.forEach { style ->
                            val isCurrent = template.codeBoxStyle == style
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) SemanticIncomeGreen.copy(alpha = 0.25f) else CyberDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) SemanticIncomeGreen else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.clickable { onApplyCodeStyle(style) }
                            ) {
                                Text(
                                    text = style.displayNameAr,
                                    fontSize = 11.sp,
                                    color = if (isCurrent) SemanticIncomeGreen else Color.LightGray,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Data Fields
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بيانات وهوية الكرت:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)

                    OutlinedTextField(
                        value = template.name,
                        onValueChange = { onUpdate(template.copy(name = it)) },
                        label = { Text("اسم القالب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = template.networkName,
                            onValueChange = { onUpdate(template.copy(networkName = it)) },
                            label = { Text("اسم الشبكة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = template.priceText,
                            onValueChange = { onUpdate(template.copy(priceText = it)) },
                            label = { Text("السعر (مثال: 500 ر.ي)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = template.quotaText,
                            onValueChange = { onUpdate(template.copy(quotaText = it)) },
                            label = { Text("الرصيد (مثال: 2 جيجا)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = template.validityText,
                            onValueChange = { onUpdate(template.copy(validityText = it)) },
                            label = { Text("الصلاحية (مثال: 7 أيام)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = template.supportPhone,
                            onValueChange = { onUpdate(template.copy(supportPhone = it)) },
                            label = { Text("رقم خدمة العملاء / الدعم") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = template.hotspotLoginUrl,
                            onValueChange = { onUpdate(template.copy(hotspotLoginUrl = it)) },
                            label = { Text("رابط تسجيل دخول الهوتسبوت") },
                            placeholder = { Text("http://10.50.7.1/login") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = template.footerNote,
                        onValueChange = { onUpdate(template.copy(footerNote = it)) },
                        label = { Text("التعليمات والملاحظة السفلية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إظهار رمز QR للدخول الذكي", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = template.showQrCode,
                            onCheckedChange = { onUpdate(template.copy(showQrCode = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إظهار خطوط القص التوجيهية للتقطيع", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = template.showCutLines,
                            onCheckedChange = { onUpdate(template.copy(showCutLines = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandCyanPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_save_card_template")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ تعديلات القالب", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Fast Generator Tab (1-Tap Generation)
// -------------------------------------------------------------
@Composable
private fun FastGeneratorTab(
    uiState: CardStudioUiState,
    template: CardTemplate,
    onUpdateConfig: (String?, Int?, String?, String?, Int?, PasswordPattern?, PrintPageFormat?) -> Unit,
    onGenerate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = BrandCyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("القالب المعتمد للتوليد:", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${template.name} • ${template.priceText} - ${template.quotaText} (${template.themePreset.displayNameAr.split(" ").first()})",
                        color = BrandCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إعدادات الدفعة والأرقام السرية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)

                    OutlinedTextField(
                        value = uiState.batchTitleInput,
                        onValueChange = { onUpdateConfig(it, null, null, null, null, null, null) },
                        label = { Text("عنوان الدفعة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick count chips
                    Text("حدد عدد الكروت المطلوب توليدها:", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 48, 96, 200, 500).forEach { count ->
                            val isSelected = uiState.batchCountInput == count
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.3f) else CyberDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.clickable { onUpdateConfig(null, count, null, null, null, null, null) }
                            ) {
                                Text(
                                    text = if (count == 24) "24 كرت (صفحة A4)" else if (count == 48) "48 كرت (صفحتين)" else "$count كرت",
                                    color = if (isSelected) BrandCyanPrimary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.batchProfileInput,
                            onValueChange = { onUpdateConfig(null, null, it, null, null, null, null) },
                            label = { Text("بروفايل الميكروتك (Profile)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.batchPrefixInput,
                            onValueChange = { onUpdateConfig(null, null, null, it, null, null, null) },
                            label = { Text("بادئة الأرقام (Prefix)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Password pattern
                    Text("نمط كلمة المرور (Password Pattern):", color = Color.LightGray, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PasswordPattern.entries.forEach { pattern ->
                            val isSelected = uiState.batchPasswordPattern == pattern
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.2f) else CyberDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateConfig(null, null, null, null, null, pattern, null) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isSelected) BrandCyanPrimary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = pattern.displayNameAr,
                                        fontSize = 12.sp,
                                        color = if (isSelected) BrandCyanPrimary else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onGenerate,
                        enabled = !uiState.isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_execute_generate_batch")
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "توليد ${uiState.batchCountInput} كرت الآن مع رموز الـ QR ⚡",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Professional Batches & Printing Tab (A4 Grid & Thermal)
// -------------------------------------------------------------
@Composable
private fun ProfessionalBatchesTab(
    batches: List<CardBatch>,
    selectedBatch: CardBatch?,
    onSelectBatch: (CardBatch?) -> Unit,
    onPrintPdf: (CardBatch, PrintPageFormat) -> Unit,
    onShareScript: (CardBatch) -> Unit,
    onDeleteBatch: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(PrintPageFormat.A4_GRID_24) }
    var showScriptDialog by remember { mutableStateOf<CardBatch?>(null) }
    var showPrintDialog by remember { mutableStateOf<CardBatch?>(null) }

    // Print Format Chooser Dialog
    if (showPrintDialog != null) {
        val b = showPrintDialog!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPrintDialog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "خيارات طباعة دفعة #${b.batchNumber}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اختر قياس وتنسيق الصفحة المطلوب تصديره:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PrintPageFormat.entries.forEach { format ->
                        val isSel = selectedFormat == format
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) BrandCyanPrimary.copy(alpha = 0.2f) else CyberDarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedFormat = format }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSel) Icons.Default.Check else Icons.Default.Print,
                                    contentDescription = null,
                                    tint = if (isSel) BrandCyanPrimary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = format.displayNameAr,
                                    fontSize = 12.sp,
                                    color = if (isSel) BrandCyanPrimary else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onPrintPdf(b, selectedFormat)
                            showPrintDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء ومشاركة ملف الـ PDF 🖨️", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // MikroTik Script Dialog
    if (showScriptDialog != null) {
        val b = showScriptDialog!!
        val script = remember(b) { MikroTikCardProvisioner.generateRouterOsScript(b) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showScriptDialog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "سكربت ميكروتك دفعة #${b.batchNumber}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "انسخ السكربت والصقه في New Terminal، أو شارك ملف .rsc:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = script,
                            fontSize = 10.sp,
                            color = SemanticIncomeGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MikroTik Script", script)
                                clipboard.setPrimaryClip(clip)
                                showScriptDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ السكربت", color = Color.Black, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onShareScript(b)
                                showScriptDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة ملف .rsc", color = Color.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (batches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد دفعات كروت مولدة حتى الآن", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "انتقل إلى تبويب \"توليد دفعة جديدة\" لبدء إنشاء كروت الشبكة.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "سجل الدفعات المولدة والطباعة (${batches.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            items(batches, key = { it.id }) { batch ->
                val isSelected = selectedBatch?.id == batch.id
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(batch.createdAt))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) BrandCyanPrimary else Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "دفعة #${batch.batchNumber}: ${batch.batchTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$dateStr • ${batch.cardCount} كرت • بروفايل: ${batch.profileName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandCyanPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BrandCyanPrimary)
                            ) {
                                Text(
                                    text = "${batch.cardCount} كرت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCyanPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions Row: Print PDF, Export Script, View Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showPrintDialog = batch },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة PDF", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showScriptDialog = batch },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سكربت ميكروتك", color = BrandCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { onSelectBatch(if (isSelected) null else batch) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "عرض الكروت",
                                    tint = if (isSelected) BrandCyanPrimary else Color.LightGray
                                )
                            }

                            IconButton(
                                onClick = { onDeleteBatch(batch.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SemanticExpenseRed)
                            }
                        }

                        // Expanded Cards List Preview
                        AnimatedVisibility(visible = isSelected) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = "نماذج من كروت الدفعة (${batch.cards.size} كرت):",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                batch.cards.take(2).forEach { card ->
                                    CardTicketLivePreview(
                                        template = batch.templateSnapshot,
                                        sampleUsername = card.username,
                                        samplePassword = card.password,
                                        scale = 0.9f
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (batch.cards.size > 2) {
                                    Text(
                                        text = "+ ويوجد ${batch.cards.size - 2} كرت إضافي في ملف الـ PDF الجاهز للطباعة والتوزيع الشبكي",
                                        fontSize = 10.sp,
                                        color = BrandCyanPrimary
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

// -------------------------------------------------------------
// Interactive Live Card Ticket Preview (Component matching SamMikrotik Web Engine)
// -------------------------------------------------------------
@Composable
fun CardTicketLivePreview(
    template: CardTemplate,
    sampleUsername: String,
    samplePassword: String,
    scale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val qrUrl = "${template.hotspotLoginUrl.trimEnd('/')}?username=$sampleUsername&password=$samplePassword"
    val qrBitmap = remember(qrUrl) {
        try {
            QrCodeGenerator.generateQrBitmap(qrUrl, sizePx = 180)
        } catch (_: Exception) {
            null
        }
    }

    val primaryColor = Color(template.primaryColorHex)
    val bgColor = Color(template.backgroundColorHex)
    val gradEndColor = Color(template.gradientEndColorHex)
    val textColor = Color(template.textColorHex)
    val accentColor = Color(template.accentColorHex)
    val isCleanWhite = template.themePreset == CardThemePreset.CLEAN_WHITE

    val backgroundBrush = if (isCleanWhite) {
        Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFC)))
    } else {
        Brush.linearGradient(listOf(bgColor, gradEndColor))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_ticket_live_preview"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isCleanWhite) Color(0xFFCBD5E1) else primaryColor.copy(alpha = 0.8f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(10.dp)
        ) {
            Column {
                // Header Row: Network Name with WiFi Icon on Right & Price Pill on Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size((18 * scale).dp)
                                .clip(CircleShape)
                                .background(if (isCleanWhite) Color(0xFF0284C7) else primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isCleanWhite) Color.White else Color.Black,
                                modifier = Modifier.size((12 * scale).dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = template.networkName,
                            fontWeight = FontWeight.Bold,
                            fontSize = (12 * scale).sp,
                            color = textColor
                        )
                    }

                    if (template.showPrice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(template.badgeBgColorHex),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = template.priceText,
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color(template.badgeTextColorHex),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((6 * scale).dp))

                // Middle Row: QR Code on Left, Credentials & Quota on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code in high contrast container
                    if (template.showQrCode && qrBitmap != null) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33000000)),
                            modifier = Modifier.size((64 * scale).dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Column: Quota, Code Box according to style, and serial
                    Column(modifier = Modifier.weight(1f)) {
                        // Quota & Validity
                        Text(
                            text = "${template.quotaText} • ${template.validityText}",
                            color = if (isCleanWhite) Color(0xFF0284C7) else primaryColor,
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Render Code Box according to selected style
                        when (template.codeBoxStyle) {
                            CodeBoxStyle.PILL_BADGE -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isCleanWhite) Color(0xFFF1F5F9) else primaryColor.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, primaryColor)
                                ) {
                                    Text(
                                        text = sampleUsername,
                                        fontSize = (12 * scale).sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            CodeBoxStyle.SPLIT_DIGITS -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    sampleUsername.forEach { ch ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isCleanWhite) Color(0xFFF8FAFC) else Color(0xFF090E1F),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                                        ) {
                                            Text(
                                                text = ch.toString(),
                                                fontSize = (11 * scale).sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = textColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            CodeBoxStyle.SCRATCH_FOIL -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFD97706),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFDE68A))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sampleUsername,
                                            fontSize = (11 * scale).sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            CodeBoxStyle.TICKET_DASHED -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCleanWhite) Color.White else Color(0x22FFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                                ) {
                                    Text(
                                        text = sampleUsername,
                                        fontSize = (12 * scale).sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            else -> {
                                // MODERN_BOX
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCleanWhite) Color(0xFFF1F5F9) else Color(0x33000000),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = sampleUsername,
                                        fontSize = (12 * scale).sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (samplePassword != sampleUsername) {
                            Text(
                                text = "PIN: $samplePassword",
                                fontSize = (9 * scale).sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }

                        // Serial number
                        if (template.showSerialNumber) {
                            Text(
                                text = "SN: 000142",
                                fontSize = (7.5 * scale).sp,
                                color = textColor.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((5 * scale).dp))

                // Footer Row: Support phone on Right & Scan QR note on Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (template.showSupportPhone && template.supportPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (isCleanWhite) Color(0xFF0284C7) else primaryColor,
                                modifier = Modifier.size((10 * scale).dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = template.supportPhone,
                                fontSize = (8 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = template.footerNote,
                        fontSize = (7.5 * scale).sp,
                        color = textColor.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
