package com.example.ui.cardstudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.card.CardPreviewMode
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
import java.io.File
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

    // Photo picker launcher (Android Photo Picker - compliant with zero broad permissions)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomBackgroundImage(uri, context)
        }
    }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "استوديو وتصميم الكروت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = BrandCyanPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "⚡ VIP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCyanPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "القالب النشط: ${uiState.editingTemplate.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    IconButton(
                        onClick = { viewModel.saveEditingTemplate() },
                        modifier = Modifier.testTag("btn_save_card_template_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "حفظ القالب",
                            tint = BrandCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        },
        containerColor = CyberDarkSurface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Interactive Live Preview Canvas Section (Persistent on top)
            LivePreviewCanvasSection(
                template = uiState.editingTemplate,
                previewMode = uiState.previewMode,
                onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                onToggleCutLines = { viewModel.toggleCutLines(it) },
                onExportPdf = {
                    val sampleBatch = createSampleBatch(uiState.editingTemplate)
                    viewModel.exportAndSharePdf(context, sampleBatch, uiState.selectedPrintFormat)
                },
                onCopyScript = {
                    val sampleBatch = createSampleBatch(uiState.editingTemplate)
                    val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                    // Emit clipboard message
                    android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // 2. Navigation Tabs (Matching sammikroticnew web project structure)
            val tabs = CardStudioTab.values()
            val selectedTabIndex = tabs.indexOf(uiState.activeTab)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CyberDarkCardElevated,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = BrandCyanPrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.titleAr,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) BrandCyanPrimary else Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            // 3. Tab Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.activeTab) {
                    CardStudioTab.GENERATOR -> GeneratorParametersTab(
                        uiState = uiState,
                        onUpdateConfig = { t, c, p, pr, ul, pat, f ->
                            viewModel.updateBatchConfig(t, c, p, pr, ul, pat, f)
                        },
                        onGenerate = { viewModel.generateBatch() }
                    )
                    CardStudioTab.TEMPLATES -> TemplatesGalleryTab(
                        currentTemplate = uiState.editingTemplate,
                        savedTemplates = templates,
                        onSelectTemplate = { viewModel.setEditingTemplate(it) },
                        onApplyPreset = { viewModel.applyThemePreset(it) },
                        onPickCustomImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveCustomImage = { viewModel.removeCustomBackgroundImage() },
                        onResetDefaults = { viewModel.resetTemplatesToDefault() }
                    )
                    CardStudioTab.DESIGNER -> ElementsAndLayoutTab(
                        template = uiState.editingTemplate,
                        onUpdateTemplate = { viewModel.updateEditingTemplate(it) },
                        onApplyCodeBoxStyle = { viewModel.applyCodeBoxStyle(it) },
                        onSave = { viewModel.saveEditingTemplate() },
                        onPickCustomImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveCustomImage = { viewModel.removeCustomBackgroundImage() }
                    )
                    CardStudioTab.BATCHES -> BatchesAndPrintTab(
                        batches = batches,
                        selectedFormat = uiState.selectedPrintFormat,
                        onSelectFormat = { viewModel.updateBatchConfig(format = it) },
                        onExportPdf = { batch, format ->
                            viewModel.exportAndSharePdf(context, batch, format)
                        },
                        onShareScript = { batch ->
                            viewModel.shareMikrotikScript(context, batch)
                        },
                        onDeleteBatch = { viewModel.deleteBatch(it) }
                    )
                }
            }
        }
    }
}

/**
 * Live Preview Canvas Section with toggle between Single Card & A4 Full Sheet.
 */
@Composable
private fun LivePreviewCanvasSection(
    template: CardTemplate,
    previewMode: CardPreviewMode,
    onTogglePreviewMode: (CardPreviewMode) -> Unit,
    onToggleCutLines: (Boolean) -> Unit,
    onExportPdf: () -> Unit,
    onCopyScript: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Controls Bar (Toggle Preview Mode + Cut Lines + Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = previewMode == CardPreviewMode.A4_FULL_SHEET,
                        onClick = { onTogglePreviewMode(CardPreviewMode.A4_FULL_SHEET) },
                        label = { Text("ورقة A4 كاملة 📄", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCyanPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = previewMode == CardPreviewMode.SINGLE_CARD,
                        onClick = { onTogglePreviewMode(CardPreviewMode.SINGLE_CARD) },
                        label = { Text("كرت مفرد 🎴", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCyanPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                // Quick Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cut Lines Toggle
                    IconButton(
                        onClick = { onToggleCutLines(!template.showCutLines) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "خطوط القص",
                            tint = if (template.showCutLines) BrandCyanPrimary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy Script
                    IconButton(
                        onClick = onCopyScript,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ سكربت",
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick PDF Export
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "طباعة PDF",
                            tint = SemanticIncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Content
            if (previewMode == CardPreviewMode.A4_FULL_SHEET) {
                A4SheetPreview(template = template)
            } else {
                SingleCardWYSIWYG(template = template)
            }
        }
    }
}

/**
 * Realistic A4 Sheet Preview matching the desktop web project (3x8 grid of cards with header and cut lines).
 */
@Composable
private fun A4SheetPreview(template: CardTemplate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // A4 Paper Header (matching sammikroticnew)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${template.networkName} • هاتف الدعم: ${template.supportPhone} • صفحة 1 من 1",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
            Text(
                text = if (template.showCutLines) "✂️ خطوط القص محددة" else "ورقة A4 (24 كرت)",
                fontSize = 8.sp,
                color = Color(0xFF64748B)
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(6.dp))

        // Mini Grid of Cards: 3 columns x 3 visible rows in preview (with indication of 24 cards)
        val sampleCards = listOf(
            Triple("821171", "500 ر.ي", "2700M"),
            Triple("495370", "500 ر.ي", "2700M"),
            Triple("125149", "500 ر.ي", "2700M"),
            Triple("950324", "500 ر.ي", "2700M"),
            Triple("580614", "500 ر.ي", "2700M"),
            Triple("732981", "500 ر.ي", "2700M")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 3) {
                        val cardIndex = (row * 3) + col
                        val cardData = sampleCards[cardIndex % sampleCards.size]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            MiniCardTile(
                                template = template,
                                code = cardData.first,
                                price = cardData.second,
                                quota = cardData.third
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "• • • شبكة كاملة (3 أعمدة × 8 صفوف = 24 كرت لكل ورقة A4 بمقاس 63×33 مم) • • •",
                fontSize = 8.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Individual Mini Card Tile rendered inside the A4 Sheet.
 */
@Composable
private fun MiniCardTile(
    template: CardTemplate,
    code: String,
    price: String,
    quota: String
) {
    // Check if custom background image exists
    val customBitmap = remember(template.customBackgroundImagePath) {
        if (!template.customBackgroundImagePath.isNullOrBlank()) {
            val file = File(template.customBackgroundImagePath)
            if (file.exists() && file.length() > 0) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null
    }

    val isCleanWhite = template.themePreset == CardThemePreset.CLEAN_WHITE
    val borderModifier = if (template.showCutLines) {
        Modifier.border(0.5.dp, Color(0xFF94A3B8), RoundedCornerShape(3.dp))
    } else {
        Modifier.border(0.5.dp, Color(template.primaryColorHex).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(borderModifier)
            .clip(RoundedCornerShape(3.dp))
            .then(
                if (customBitmap != null) {
                    Modifier
                } else if (isCleanWhite) {
                    Modifier.background(Color.White)
                } else {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                Color(template.backgroundColorHex),
                                Color(template.gradientEndColorHex)
                            )
                        )
                    )
                }
            )
    ) {
        // Draw Custom Background if present
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay scrim
            if (template.overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = template.overlayAlpha))
                )
            }
        }

        // Card Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Price pill on left + Network on center/right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(template.badgeBgColorHex),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = price,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(template.badgeTextColorHex),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                Text(
                    text = template.networkName,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customBitmap != null || !isCleanWhite) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Center: High-contrast Code Box (Matching sammikroticnew)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(template.primaryColorHex), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    color = Color(0xFF0F172A)
                )
            }

            // Footer: Quota + Time pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${template.quotaText} • ${template.validityText}",
                    fontSize = 5.5.sp,
                    color = if (customBitmap != null || !isCleanWhite) Color.LightGray else Color(0xFF475569)
                )
                Text(
                    text = "SN",
                    fontSize = 5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * High-Definition WYSIWYG Single Card Canvas Preview.
 */
@Composable
private fun SingleCardWYSIWYG(template: CardTemplate) {
    val context = LocalContext.current
    val customBitmap = remember(template.customBackgroundImagePath) {
        if (!template.customBackgroundImagePath.isNullOrBlank()) {
            val file = File(template.customBackgroundImagePath)
            if (file.exists() && file.length() > 0) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null
    }

    val isCleanWhite = template.themePreset == CardThemePreset.CLEAN_WHITE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (customBitmap != null) Color(template.primaryColorHex).copy(alpha = 0.8f)
            else Color(template.primaryColorHex)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (customBitmap != null) {
                        Modifier
                    } else if (isCleanWhite) {
                        Modifier.background(Color.White)
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(template.backgroundColorHex),
                                    Color(template.gradientEndColorHex)
                                )
                            )
                        )
                    }
                )
        ) {
            // 1. Draw custom background image if picked
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap,
                    contentDescription = "قالب مخصص من المعرض",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark overlay scrim
                if (template.overlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = template.overlayAlpha))
                    )
                }
            }

            // 2. Card Elements Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row: Price Badge (Left), Network Title with WiFi (Center), Date (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price Badge
                    if (template.showPrice) {
                        Surface(
                            color = Color(template.badgeBgColorHex),
                            shape = RoundedCornerShape(6.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = template.priceText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(template.badgeTextColorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Network Name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color(template.primaryColorHex),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = template.networkName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (customBitmap != null || !isCleanWhite) Color.White else Color(0xFF0F172A)
                        )
                    }

                    // Date
                    Text(
                        text = template.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customBitmap != null || !isCleanWhite) Color.LightGray else Color(0xFF64748B)
                    )
                }

                // Middle Row: QR Code (Left) + Code Box & Metadata (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // QR Code Box
                    if (template.showQrCode) {
                        val qrBitmap = remember(template.hotspotLoginUrl) {
                            try {
                                QrCodeGenerator.generateQrBitmap(
                                    content = "${template.hotspotLoginUrl}?username=821171&password=pin",
                                    sizePx = 80
                                ).asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(64.dp)
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "QR الدخول السريع",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    // Voucher Code Box
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Quota & Validity meta
                        if (template.showValidity) {
                            Text(
                                text = "${template.quotaText} • ${template.validityText}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(template.primaryColorHex),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Code display according to style
                        when (template.codeBoxStyle) {
                            CodeBoxStyle.PILL_BADGE -> {
                                Surface(
                                    color = Color(template.primaryColorHex).copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(template.primaryColorHex)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "821 171",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                            CodeBoxStyle.SCRATCH_FOIL -> {
                                Surface(
                                    color = Color(0xFFD97706),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "امسح برفق للكشف: 821171",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            CodeBoxStyle.SPLIT_DIGITS -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    "821171".forEach { digit ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(template.primaryColorHex)),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = digit.toString(),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                // MODERN_BOX or TICKET_DASHED
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(template.primaryColorHex)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "821171",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Row: Support Phone & Serial Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (template.showSupportPhone) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(template.primaryColorHex),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الدعم: ${template.supportPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (customBitmap != null || !isCleanWhite) Color.LightGray else Color(0xFF475569)
                            )
                        }
                    }

                    if (template.showSerialNumber) {
                        Text(
                            text = "SN: #000142",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (customBitmap != null || !isCleanWhite) Color.Gray else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 0: Generator Parameters & Instant MikroTik Batch Provisioning.
 */
@Composable
private fun GeneratorParametersTab(
    uiState: CardStudioUiState,
    onUpdateConfig: (String?, Int?, String?, String?, Int?, PasswordPattern?, PrintPageFormat?) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary & Total Box (matching sammikroticnew)
        val template = uiState.editingTemplate
        val priceNumeric = template.priceText.filter { it.isDigit() }.toIntOrNull() ?: 500
        val totalRevenue = priceNumeric * uiState.batchCountInput

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معايير وتوليد الدفعة ⚡",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = BrandCyanPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "الإجمالي: $totalRevenue ر.ي",
                            fontWeight = FontWeight.Bold,
                            color = BrandCyanPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "كروت عامة • الحجم: ${template.quotaText} • الوقت: ${template.validityText} • السعر: ${template.priceText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        // Quick Package Presets
        Text("باقة وسرعة الإنترنت المربوطة بالكرت:", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("كرت 500 (2.7GB)", "2700M_35H", 500),
                Triple("كرت 1000 (5GB)", "5G_15D", 1000),
                Triple("كرت 300 (1.5GB)", "1500M_3D", 300),
                Triple("كرت 200 (1GB)", "1G_1D", 200)
            ).forEach { (label, profile, price) ->
                FilterChip(
                    selected = uiState.batchProfileInput == profile,
                    onClick = {
                        onUpdateConfig(label, null, profile, null, null, null, null)
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Card Count Presets
        Text("عدد الكروت المطلوبة للدفعة:", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                24 to "24 كرت (ورقة A4)",
                42 to "42 كرت (A4 شريط)",
                48 to "48 كرت (ورقتين)",
                100 to "100 كرت"
            ).forEach { (count, label) ->
                FilterChip(
                    selected = uiState.batchCountInput == count,
                    onClick = { onUpdateConfig(null, count, null, null, null, null, null) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Password Pattern Selector
        Text("نمط توليد بيانات الدخول (Credentials):", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PasswordPattern.SAME_AS_USERNAME to "اسم فقط (دخول سهل)",
                PasswordPattern.NUMBERS_ONLY to "اسم + أرقام PIN"
            ).forEach { (pattern, label) ->
                FilterChip(
                    selected = uiState.batchPasswordPattern == pattern,
                    onClick = { onUpdateConfig(null, null, null, null, null, pattern, null) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Prefix and Length
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.batchPrefixInput,
                onValueChange = { onUpdateConfig(null, null, null, it, null, null, null) },
                label = { Text("بادئة الكروت (Prefix)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = uiState.batchUserLengthInput.toString(),
                onValueChange = { onUpdateConfig(null, null, null, null, it.toIntOrNull() ?: 6, null, null) },
                label = { Text("طول الرمز (خانات)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Big Action Button
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_execute_generate_batch"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isGenerating
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
            } else {
                Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توليد الدفعة وحقن الميكروتك (${uiState.batchCountInput} كرت) ⚡",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Tab 1: Template Presets + Custom Image Picking from Device Gallery.
 */
@Composable
private fun TemplatesGalleryTab(
    currentTemplate: CardTemplate,
    savedTemplates: List<CardTemplate>,
    onSelectTemplate: (CardTemplate) -> Unit,
    onApplyPreset: (CardThemePreset) -> Unit,
    onPickCustomImage: () -> Unit,
    onRemoveCustomImage: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ★ Section 1: Custom Background Image from Phone Gallery (Requested by User)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandCyanPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = BrandCyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "جلب صورة قالب جاهز من الاستديو 🖼️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "استيراد قالب مصمم في الفوتوشوب واستخدامه كخلفية لطباعة الكروت",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!currentTemplate.customBackgroundImagePath.isNullOrBlank()) {
                    // Show active imported image info
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SemanticIncomeGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("قالب مصمم نشط من المعرض 🖼️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onPickCustomImage,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("تغيير 🔄", fontSize = 11.sp, color = BrandCyanPrimary)
                                }
                                IconButton(
                                    onClick = onRemoveCustomImage,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = SemanticExpenseRed)
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onPickCustomImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_pick_custom_card_image"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اختيار صورة قالب كرت من المعرض / الاستديو 📸",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 2: Built-in High-Fidelity Presets (Matching Web Engine)
        Text(
            text = "الثيمات الرقمية الجاهزة (Built-in Themes) 🎨",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        val presets = CardThemePreset.values()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                val isSelected = currentTemplate.themePreset == preset && currentTemplate.customBackgroundImagePath.isNullOrBlank()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyPreset(preset) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else CyberDarkCardElevated
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrandCyanPrimary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = if (isSelected) BrandCyanPrimary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = preset.displayNameAr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.LightGray
                            )
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BrandCyanPrimary)
                        }
                    }
                }
            }
        }

        // Reset to Defaults Button
        OutlinedButton(
            onClick = onResetDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.LightGray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("استعادة قوالب الويب الافتراضية", color = Color.LightGray)
        }
    }
}

/**
 * Tab 2: Elements Visibility, Positioning & Direct Attributes.
 */
@Composable
private fun ElementsAndLayoutTab(
    template: CardTemplate,
    onUpdateTemplate: (CardTemplate) -> Unit,
    onApplyCodeBoxStyle: (CodeBoxStyle) -> Unit,
    onSave: () -> Unit,
    onPickCustomImage: () -> Unit,
    onRemoveCustomImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Card Image Bar
        Surface(
            color = CyberDarkCardElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("خلفية الكرت (الصورة / الثيم):", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Text(
                        text = if (!template.customBackgroundImagePath.isNullOrBlank()) "قالب مخصص من الاستديو 🖼️" else template.themePreset.displayNameAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCyanPrimary
                    )
                }
                Button(
                    onClick = onPickCustomImage,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary.copy(alpha = 0.2f))
                ) {
                    Text("استيراد صورة 📸", color = BrandCyanPrimary, fontSize = 12.sp)
                }
            }
        }

        // Overlay contrast scrim slider if custom image
        if (!template.customBackgroundImagePath.isNullOrBlank()) {
            Text("شفافية طبقة التباين (Contrast Scrim):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Slider(
                value = template.overlayAlpha,
                onValueChange = { onUpdateTemplate(template.copy(overlayAlpha = it)) },
                valueRange = 0f..0.6f,
                colors = SliderDefaults.colors(
                    thumbColor = BrandCyanPrimary,
                    activeTrackColor = BrandCyanPrimary
                )
            )
        }

        // Text Fields
        OutlinedTextField(
            value = template.networkName,
            onValueChange = { onUpdateTemplate(template.copy(networkName = it)) },
            label = { Text("اسم الشبكة (أعلى الكرت)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandCyanPrimary,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = template.priceText,
                onValueChange = { onUpdateTemplate(template.copy(priceText = it)) },
                label = { Text("سعر الكرت") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = template.quotaText,
                onValueChange = { onUpdateTemplate(template.copy(quotaText = it)) },
                label = { Text("الرصيد / الحجم") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = template.validityText,
                onValueChange = { onUpdateTemplate(template.copy(validityText = it)) },
                label = { Text("الصلاحية / الوقت") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = template.supportPhone,
                onValueChange = { onUpdateTemplate(template.copy(supportPhone = it)) },
                label = { Text("هاتف الدعم والتفعيل") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Code Box Styles (Matching Web Engine)
        Text("نمط وشكل صندوق الكود (Code Box):", style = MaterialTheme.typography.labelMedium, color = Color.White)
        val styles = CodeBoxStyle.values()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            styles.forEach { style ->
                val isSelected = template.codeBoxStyle == style
                Surface(
                    color = if (isSelected) Color(0xFF1E293B) else CyberDarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyCodeBoxStyle(style) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = style.displayNameAr, color = if (isSelected) BrandCyanPrimary else Color.LightGray, fontSize = 12.sp)
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Element Toggles
        Text("التحكم في إظهار وإخفاء العناصر:", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ElementToggleRow("إظهار رمز QR الذكي للدخول التلقائي", template.showQrCode) {
                    onUpdateTemplate(template.copy(showQrCode = it))
                }
                ElementToggleRow("إظهار شارة السعر (Price Badge)", template.showPrice) {
                    onUpdateTemplate(template.copy(showPrice = it))
                }
                ElementToggleRow("إظهار الرصيد والصلاحية", template.showValidity) {
                    onUpdateTemplate(template.copy(showValidity = it))
                }
                ElementToggleRow("إظهار هاتف الدعم الفني", template.showSupportPhone) {
                    onUpdateTemplate(template.copy(showSupportPhone = it))
                }
                ElementToggleRow("إظهار الرقم التسلسلي (Serial Number)", template.showSerialNumber) {
                    onUpdateTemplate(template.copy(showSerialNumber = it))
                }
                ElementToggleRow("إظهار خطوط القص المتقطعة ✂️", template.showCutLines) {
                    onUpdateTemplate(template.copy(showCutLines = it))
                }
            }
        }

        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_save_card_template"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ التعديلات على القالب 💾", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ElementToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandCyanPrimary,
                checkedTrackColor = BrandCyanPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * Tab 3: Generated Batches, PDF Exporting and RouterOS Scripting.
 */
@Composable
private fun BatchesAndPrintTab(
    batches: List<CardBatch>,
    selectedFormat: PrintPageFormat,
    onSelectFormat: (PrintPageFormat) -> Unit,
    onExportPdf: (CardBatch, PrintPageFormat) -> Unit,
    onShareScript: (CardBatch) -> Unit,
    onDeleteBatch: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Format Selector Bar
        Text("مقاس ورقة الطباعة المستهدفة:", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrintPageFormat.values().forEach { format ->
                FilterChip(
                    selected = selectedFormat == format,
                    onClick = { onSelectFormat(format) },
                    label = { Text(format.displayNameAr, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        if (batches.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد دفعات كروت مولدة حتى الآن",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray
                    )
                    Text(
                        text = "انتقل إلى تبويب 'معايير وتوليد الدفعة ⚡' لإنشاء أول دفعة كروت جاهزة للطباعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(batches, key = { it.id }) { batch ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
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
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(batch.createdAt))
                                    Text(
                                        text = "$dateStr • ${batch.cardCount} كرت • بروفايل: ${batch.profileName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBatch(batch.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الدفعة", tint = SemanticExpenseRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onExportPdf(batch, selectedFormat) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طباعة PDF 🖨️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onShareScript(batch) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("سكربت RSC ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to build a sample batch for instant preview and PDF export from the canvas.
 */
private fun createSampleBatch(template: CardTemplate): CardBatch {
    val sampleCards = (1..24).map { i ->
        val code = String.format(Locale.US, "%06d", 820000 + i)
        GeneratedCard(
            serialNumber = String.format(Locale.US, "%06d", i),
            username = code,
            password = code,
            priceText = template.priceText,
            quotaText = template.quotaText,
            validityText = template.validityText,
            networkName = template.networkName,
            supportPhone = template.supportPhone,
            loginUrlWithCredentials = "${template.hotspotLoginUrl}?username=$code&password=$code",
            batchId = "sample_preview"
        )
    }

    return CardBatch(
        batchNumber = 101,
        batchTitle = "معاينة ${template.name}",
        templateId = template.id,
        templateSnapshot = template,
        profileName = "default",
        cardCount = sampleCards.size,
        prefix = "82",
        usernameLength = 6,
        passwordPattern = PasswordPattern.SAME_AS_USERNAME,
        cards = sampleCards
    )
}
