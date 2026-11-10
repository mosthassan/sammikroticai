package com.example.ui.cardstudio

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.card.CardBatch
import com.example.data.card.CardCharacterSet
import com.example.data.card.CardPreviewMode
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.ElementLayout
import com.example.data.card.GeneratedCard
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.data.card.quotaMb
import com.example.data.card.profileName
import com.example.data.local.entity.CardPackageEntity
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
    val availablePackages by viewModel.availablePackages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSavePackageDialog by remember { mutableStateOf(false) }

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
                    // Top Action: Preview Sheet as Professional PDF
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .clickable { viewModel.previewAndSaveSamplePdf(context) }
                            .testTag("btn_export_pdf_top")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "معاينة ورقة الطباعة PDF",
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "معاينة PDF 📄",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showSavePackageDialog = true },
                        modifier = Modifier.testTag("btn_save_card_template_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "حفظ القالب لباقة معينة",
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
        var isPreviewExpanded by rememberSaveable { mutableStateOf(false) }
        var designerSubMode by rememberSaveable { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Responsive 4-Segmented Control at the TOP (Immediate navigation, 48dp+ touch target, zero horizontal scroll)
            val tabs = CardStudioTab.values()

            Surface(
                color = CyberDarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = uiState.activeTab == tab
                        val icon = when (tab) {
                            CardStudioTab.GENERATOR -> Icons.Default.Bolt
                            CardStudioTab.TEMPLATES -> Icons.Default.Palette
                            CardStudioTab.DESIGNER -> Icons.Default.Tune
                            CardStudioTab.BATCHES -> Icons.Default.Print
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandCyanPrimary else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectTab(tab) }
                                .testTag("tab_${tab.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.titleAr,
                                    tint = if (isSelected) Color.Black else BrandCyanPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.shortTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 2. Contextual Section Banner (Clear guidance with full title & subtitle)
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, BrandCyanPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(BrandCyanPrimary, CircleShape)
                        )
                        Text(
                            text = uiState.activeTab.titleAr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandCyanPrimary
                        )
                    }
                    Text(
                        text = uiState.activeTab.subtitle,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Mode-Aware Body: Collapsible preview in Generator & Templates, full WYSIWYG in Designer, A4 Sheet in Batches
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.activeTab) {
                    CardStudioTab.GENERATOR -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isPreviewExpanded) {
                                LivePreviewCanvasSection(
                                    template = uiState.editingTemplate,
                                    previewMode = uiState.previewMode,
                                    selectedPrintFormat = uiState.selectedPrintFormat,
                                    showNudgeControls = false,
                                    isCollapsible = true,
                                    onCollapse = { isPreviewExpanded = false },
                                    onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                                    onToggleCutLines = { viewModel.toggleCutLines(it) },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onUpdateElementLayout = { key, layout -> viewModel.updateElementLayout(key, layout) },
                                    onResetElementPositions = { viewModel.resetAllElementPositions() }
                                )
                            } else {
                                CompactLivePreviewCard(
                                    template = uiState.editingTemplate,
                                    onExpand = { isPreviewExpanded = true },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                GeneratorParametersTab(
                                    uiState = uiState,
                                    savedTemplates = templates,
                                    availablePackages = availablePackages,
                                    onSelectPackage = { viewModel.selectPackageForGeneration(it) },
                                    onSelectSavedTemplate = { viewModel.setEditingTemplate(it) },
                                    onSelectPackageProfile = { profile, name, price, quota ->
                                        viewModel.selectPackageProfile(profile, name, price, quota)
                                    },
                                    onSelectPrintFormat = { viewModel.selectPrintFormat(it) },
                                    onUpdateConfig = { t, c, p, pr, ul, pat, cs, f ->
                                        viewModel.updateBatchConfig(t, c, p, pr, ul, pat, cs, f)
                                    },
                                    onGenerate = { viewModel.generateBatch() }
                                )
                            }
                        }
                    }
                    CardStudioTab.TEMPLATES -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isPreviewExpanded) {
                                LivePreviewCanvasSection(
                                    template = uiState.editingTemplate,
                                    previewMode = uiState.previewMode,
                                    selectedPrintFormat = uiState.selectedPrintFormat,
                                    showNudgeControls = false,
                                    isCollapsible = true,
                                    onCollapse = { isPreviewExpanded = false },
                                    onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                                    onToggleCutLines = { viewModel.toggleCutLines(it) },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onUpdateElementLayout = { key, layout -> viewModel.updateElementLayout(key, layout) },
                                    onResetElementPositions = { viewModel.resetAllElementPositions() }
                                )
                            } else {
                                CompactLivePreviewCard(
                                    template = uiState.editingTemplate,
                                    onExpand = { isPreviewExpanded = true },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                TemplatesGalleryTab(
                                    currentTemplate = uiState.editingTemplate,
                                    savedTemplates = templates,
                                    onSelectTemplate = { viewModel.setEditingTemplate(it) },
                                    onEditTemplate = { tmpl ->
                                        viewModel.setEditingTemplate(tmpl)
                                        viewModel.selectTab(CardStudioTab.DESIGNER)
                                    },
                                    onDuplicateTemplate = { viewModel.duplicateTemplate(it) },
                                    onDeleteTemplate = { viewModel.deleteTemplate(it.id) },
                                    onUpdateTemplateDetails = { id, name, price, quota, validity, net, phone, prof ->
                                        viewModel.updateTemplateMetadata(id, name, price, quota, validity, net, phone, prof)
                                    },
                                    onSaveCurrentAsNew = { showSavePackageDialog = true },
                                    onApplyPreset = { viewModel.applyThemePreset(it) },
                                    onPickCustomImage = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRemoveCustomImage = { viewModel.removeCustomBackgroundImage() },
                                    onResetDefaults = { viewModel.resetTemplatesToDefault() }
                                )
                            }
                        }
                    }
                    CardStudioTab.DESIGNER -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sub-mode Segmented Switch inside Designer: Canvas vs Properties
                            Surface(
                                color = CyberDarkCardElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isCanvas = designerSubMode == 0
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isCanvas) BrandCyanPrimary else Color.Transparent,
                                        border = if (isCanvas) BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { designerSubMode = 0 }
                                            .testTag("designer_subtab_canvas")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = if (isCanvas) Color.Black else BrandCyanPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "لوحة التحريك باللمس 🎴",
                                                fontSize = 12.sp,
                                                fontWeight = if (isCanvas) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCanvas) Color.Black else Color.White
                                            )
                                        }
                                    }

                                    val isProps = designerSubMode == 1
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isProps) BrandCyanPrimary else Color.Transparent,
                                        border = if (isProps) BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { designerSubMode = 1 }
                                            .testTag("designer_subtab_properties")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = if (isProps) Color.Black else BrandCyanPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تعديل النصوص والخصائص ✏️",
                                                fontSize = 12.sp,
                                                fontWeight = if (isProps) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isProps) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            if (designerSubMode == 0) {
                                // Mode 0: Interactive Touch Drag Canvas + Precision Nudge Tools (Full screen comfort)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    LivePreviewCanvasSection(
                                        template = uiState.editingTemplate,
                                        previewMode = CardPreviewMode.SINGLE_CARD,
                                        selectedPrintFormat = uiState.selectedPrintFormat,
                                        showNudgeControls = true,
                                        isCollapsible = false,
                                        onCollapse = null,
                                        onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                                        onToggleCutLines = { viewModel.toggleCutLines(it) },
                                        onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                        onCopyScript = {
                                            val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                            val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                            android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onUpdateElementLayout = { key, layout -> viewModel.updateElementLayout(key, layout) },
                                        onResetElementPositions = { viewModel.resetAllElementPositions() }
                                    )

                                    // Quick switch banner to edit texts & properties
                                    Surface(
                                        color = Color(0xFF0F2636),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .clickable { designerSubMode = 1 }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(18.dp))
                                                Column {
                                                    Text("تعديل نصوص وخصائص الكرت ✏️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text("اسم الشبكة، السعر، الرصيد، هاتف الدعم، أنماط الكود، وحفظ القالب", fontSize = 10.sp, color = Color.LightGray)
                                                }
                                            }
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                // Mode 1: Texts, Styles, Background Image & Properties (Full screen comfort)
                                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    if (isPreviewExpanded) {
                                        LivePreviewCanvasSection(
                                            template = uiState.editingTemplate,
                                            previewMode = CardPreviewMode.SINGLE_CARD,
                                            selectedPrintFormat = uiState.selectedPrintFormat,
                                            showNudgeControls = false,
                                            isCollapsible = true,
                                            onCollapse = { isPreviewExpanded = false },
                                            onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                                            onToggleCutLines = { viewModel.toggleCutLines(it) },
                                            onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                            onCopyScript = {
                                                val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                                val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                                android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onUpdateElementLayout = { key, layout -> viewModel.updateElementLayout(key, layout) },
                                            onResetElementPositions = { viewModel.resetAllElementPositions() }
                                        )
                                    } else {
                                        CompactLivePreviewCard(
                                            template = uiState.editingTemplate,
                                            onExpand = { isPreviewExpanded = true },
                                            onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                            onCopyScript = {
                                                val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                                val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                                android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        ElementsAndLayoutTab(
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
                                    }
                                }
                            }
                        }
                    }
                    CardStudioTab.BATCHES -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isPreviewExpanded) {
                                LivePreviewCanvasSection(
                                    template = uiState.editingTemplate,
                                    previewMode = uiState.previewMode,
                                    selectedPrintFormat = uiState.selectedPrintFormat,
                                    showNudgeControls = false,
                                    isCollapsible = true,
                                    onCollapse = { isPreviewExpanded = false },
                                    onTogglePreviewMode = { viewModel.togglePreviewMode(it) },
                                    onToggleCutLines = { viewModel.toggleCutLines(it) },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onUpdateElementLayout = { key, layout -> viewModel.updateElementLayout(key, layout) },
                                    onResetElementPositions = { viewModel.resetAllElementPositions() }
                                )
                            } else {
                                CompactLivePreviewCard(
                                    template = uiState.editingTemplate,
                                    onExpand = { isPreviewExpanded = true },
                                    onExportPdf = { viewModel.previewAndSaveSamplePdf(context) },
                                    onCopyScript = {
                                        val sampleBatch = createSampleBatch(uiState.editingTemplate, uiState.selectedPrintFormat.cardsPerPage)
                                        val script = MikroTikCardProvisioner.generateRouterOsScript(sampleBatch)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MikroTik Script", script))
                                        android.widget.Toast.makeText(context, "تم نسخ سكربت RouterOS للحافظة ⚡", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                BatchesAndPrintTab(
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
        }
    }

    if (showSavePackageDialog) {
        SaveTemplateForPackageDialog(
            currentTemplate = uiState.editingTemplate,
            onDismiss = { showSavePackageDialog = false },
            onConfirmSave = { name, profile, pkgName, price, quota ->
                viewModel.saveTemplateForPackage(name, profile, pkgName, price, quota)
                showSavePackageDialog = false
                android.widget.Toast.makeText(context, "تم حفظ $name بنجاح وربطه بالباقة 💾", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (uiState.isGeneratingPdf) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = CyberDarkCardElevated,
            title = {
                Text("معاينة ورقة PDF للطباعة 📄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = BrandCyanPrimary, modifier = Modifier.size(32.dp))
                    Text(
                        text = "جاري تجهيز ورقة ${uiState.selectedPrintFormat.cardsPerPage} كرت للمعاينة الفورية وفق التصميم...",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        )
    }
}

/**
 * Modern Compact Live Preview Banner for Generator and Templates tabs.
 * Gives 85%+ screen height to inputs, forms, and template browsing while
 * keeping real-time card preview accessible with one tap.
 */
@Composable
private fun CompactLivePreviewCard(
    template: CardTemplate,
    onExpand: () -> Unit,
    onExportPdf: () -> Unit,
    onCopyScript: () -> Unit
) {
    Surface(
        color = CyberDarkCardElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onExpand() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Mini preview box with actual template background & badge
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(template.backgroundColorHex),
                                    Color(template.gradientEndColorHex)
                                )
                            )
                        )
                        .border(0.5.dp, Color(template.primaryColorHex), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.priceText.take(6),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = template.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = BrandCyanPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = template.priceText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandCyanPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${template.networkName} • ${template.quotaText} • كود: 821171",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onCopyScript,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ سكربت",
                        tint = BrandCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "معاينة PDF",
                        tint = SemanticIncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    color = BrandCyanPrimary,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { onExpand() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "تكبير المعاينة",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "معاينة 👁️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
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
    selectedPrintFormat: PrintPageFormat,
    showNudgeControls: Boolean = false,
    isCollapsible: Boolean = false,
    onCollapse: (() -> Unit)? = null,
    onTogglePreviewMode: (CardPreviewMode) -> Unit,
    onToggleCutLines: (Boolean) -> Unit,
    onExportPdf: () -> Unit,
    onCopyScript: () -> Unit,
    onUpdateElementLayout: (String, ElementLayout) -> Unit,
    onResetElementPositions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
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
                        label = { Text("كرت مفرد (تحريك باللمس) 🎴", fontSize = 11.sp) },
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
                            contentDescription = "حفظ ومعاينة PDF",
                            tint = SemanticIncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Optional Collapse Button for Generator and Templates tabs
                    if (isCollapsible && onCollapse != null) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, BrandCyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onCollapse() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "تصغير المعاينة",
                                    tint = BrandCyanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "تصغير 🔼",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Content
            if (previewMode == CardPreviewMode.A4_FULL_SHEET) {
                A4SheetPreview(template = template, format = selectedPrintFormat)
            } else {
                SingleCardWYSIWYG(
                    template = template,
                    showNudgeControls = showNudgeControls,
                    onUpdateLayout = onUpdateElementLayout,
                    onResetPositions = onResetElementPositions
                )
            }
        }
    }
}

/**
 * Realistic Dynamic A4 Sheet Preview reacting to selected format and card count.
 */
@Composable
private fun A4SheetPreview(
    template: CardTemplate,
    format: PrintPageFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // A4 Paper Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${template.networkName} • هاتف الدعم: ${template.supportPhone} • ${format.displayNameAr}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
            Text(
                text = if (template.showCutLines) "✂️ خطوط القص محددة" else "ورقة A4 (${format.cardsPerPage} كرت)",
                fontSize = 8.sp,
                color = Color(0xFF64748B)
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(6.dp))

        val cols = format.cols
        val rows = format.rows
        val totalCards = format.cardsPerPage
        val cardHeightDp = when (format) {
            PrintPageFormat.A4_GRID_42 -> 34.dp // Compact strip cards
            PrintPageFormat.A4_GRID_24 -> 48.dp
            PrintPageFormat.A4_GRID_18 -> 60.dp
            PrintPageFormat.A4_GRID_10 -> 72.dp
            PrintPageFormat.THERMAL_ROLL_80MM -> 96.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until cols) {
                            val cardIndex = (row * cols) + col
                            val sampleCode = (821101 + cardIndex).toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cardHeightDp)
                            ) {
                                MiniCardTile(
                                    template = template,
                                    code = sampleCode,
                                    price = template.priceText,
                                    quota = template.quotaText,
                                    isCompactStrip = format == PrintPageFormat.A4_GRID_42
                                )
                            }
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
                text = "• • • ورقة كاملة (${format.cols} أعمدة × ${format.rows} صف = ${totalCards} كرت ظاهر بالكامل قابلة للتمرير 📄) • • •",
                fontSize = 8.5.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
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
    quota: String,
    isCompactStrip: Boolean = false
) {
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
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
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
                .padding(if (isCompactStrip) 1.5.dp else 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Price pill on left + Network on center/right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (template.showPrice && template.priceLayout.isVisible) {
                    Surface(
                        color = Color(template.badgeBgColorHex),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = price,
                            fontSize = if (isCompactStrip) 5.sp else 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(template.badgeTextColorHex),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
                if (template.showNetworkName && template.networkLayout.isVisible) {
                    Text(
                        text = template.networkName,
                        fontSize = if (isCompactStrip) 5.5.sp else 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customBitmap != null || !isCleanWhite) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Center: High-contrast Code Box
            if (template.codeBoxLayout.isVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactStrip) 14.dp else 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(template.primaryColorHex), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isCompactStrip) 7.5.sp else 9.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Footer: Quota + Time pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (template.metaLayout.isVisible) {
                    Text(
                        text = "${template.quotaText} • ${template.validityText}",
                        fontSize = if (isCompactStrip) 4.5.sp else 5.5.sp,
                        color = if (customBitmap != null || !isCleanWhite) Color.LightGray else Color(0xFF475569)
                    )
                }
                if (template.serialLayout.isVisible) {
                    val dateOrSn = if (template.showCreationDate) {
                        try {
                            SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
                        } catch (_: Exception) {
                            "SN"
                        }
                    } else "SN"
                    Text(
                        text = dateOrSn,
                        fontSize = 5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * High-Definition WYSIWYG Single Card Canvas Preview with Touch Dragging & Element Controls.
 */
@Composable
private fun SingleCardWYSIWYG(
    template: CardTemplate,
    showNudgeControls: Boolean = false,
    onUpdateLayout: (String, ElementLayout) -> Unit,
    onResetPositions: () -> Unit
) {
    val context = LocalContext.current
    var selectedElementKey by remember { mutableStateOf("code_box") }

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

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
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
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Header Row: Price Badge (Left), Network Title with WiFi (Center), Date (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Price Badge (Touch-draggable)
                        if (template.showPrice && template.priceLayout.isVisible) {
                            val priceLayout = template.priceLayout
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(priceLayout.offsetX.dp.roundToPx(), priceLayout.offsetY.dp.roundToPx()) }
                                    .pointerInput("price") {
                                        detectDragGestures(
                                            onDragStart = { selectedElementKey = "price" },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (priceLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                val newY = (priceLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                onUpdateLayout("price", priceLayout.copy(offsetX = newX, offsetY = newY))
                                            }
                                        )
                                    }
                                    .clickable { selectedElementKey = "price" }
                                    .then(
                                        if (selectedElementKey == "price") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(6.dp)).padding(2.dp)
                                        else Modifier.padding(2.dp)
                                    )
                            ) {
                                Surface(
                                    color = Color(template.badgeBgColorHex),
                                    shape = RoundedCornerShape(6.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = template.priceText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = (12 * priceLayout.scale).sp,
                                        color = Color(template.badgeTextColorHex),
                                        modifier = Modifier.padding(
                                            horizontal = (8 * priceLayout.scale * priceLayout.widthScale.coerceIn(0.4f, 2.5f)).dp,
                                            vertical = (4 * priceLayout.scale).dp
                                        )
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Network Name (Touch-draggable)
                        if (template.showNetworkName && template.networkLayout.isVisible) {
                            val netLayout = template.networkLayout
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(netLayout.offsetX.dp.roundToPx(), netLayout.offsetY.dp.roundToPx()) }
                                    .pointerInput("network") {
                                        detectDragGestures(
                                            onDragStart = { selectedElementKey = "network" },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (netLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                val newY = (netLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                onUpdateLayout("network", netLayout.copy(offsetX = newX, offsetY = newY))
                                            }
                                        )
                                    }
                                    .clickable { selectedElementKey = "network" }
                                    .then(
                                        if (selectedElementKey == "network") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(6.dp)).padding(2.dp)
                                        else Modifier.padding(2.dp)
                                    )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color(template.primaryColorHex),
                                        modifier = Modifier.size((16 * netLayout.scale).dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = template.networkName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = (15 * netLayout.scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (customBitmap != null || !isCleanWhite) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        // Creation Date (Touch-draggable)
                        if (template.showCreationDate && template.serialLayout.isVisible) {
                            val serialLayout = template.serialLayout
                            val formattedDate = remember(template.createdAt, template.dateFormatPattern) {
                                try {
                                    SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
                                } catch (_: Exception) {
                                    SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(template.createdAt))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(serialLayout.offsetX.dp.roundToPx(), serialLayout.offsetY.dp.roundToPx()) }
                                    .pointerInput("serial") {
                                        detectDragGestures(
                                            onDragStart = { selectedElementKey = "serial" },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (serialLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                val newY = (serialLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                onUpdateLayout("serial", serialLayout.copy(offsetX = newX, offsetY = newY))
                                            }
                                        )
                                    }
                                    .clickable { selectedElementKey = "serial" }
                                    .then(
                                        if (selectedElementKey == "serial") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(4.dp)).padding(2.dp)
                                        else Modifier.padding(2.dp)
                                    )
                            ) {
                                Surface(
                                    color = if (customBitmap != null || isCleanWhite) Color(0xFF0F172A).copy(alpha = 0.88f) else Color(0xFF0F1E2E).copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(5.dp),
                                    border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.7f)),
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = (6 * serialLayout.scale * serialLayout.widthScale.coerceIn(0.5f, 2.0f)).dp,
                                            vertical = (3 * serialLayout.scale).dp
                                        )
                                    ) {
                                        Text("📅", fontSize = (9 * serialLayout.scale).sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = (10 * serialLayout.scale).sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Middle Row: QR Code (Left) + Code Box & Metadata (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // QR Code Box (Touch-draggable)
                        if (template.showQrCode && template.qrCodeLayout.isVisible) {
                            val qrLayout = template.qrCodeLayout
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

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(qrLayout.offsetX.dp.roundToPx(), qrLayout.offsetY.dp.roundToPx()) }
                                    .pointerInput("qr") {
                                        detectDragGestures(
                                            onDragStart = { selectedElementKey = "qr" },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (qrLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                val newY = (qrLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                onUpdateLayout("qr", qrLayout.copy(offsetX = newX, offsetY = newY))
                                            }
                                        )
                                    }
                                    .clickable { selectedElementKey = "qr" }
                                    .then(
                                        if (selectedElementKey == "qr") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(8.dp)).padding(2.dp)
                                        else Modifier.padding(2.dp)
                                    )
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size((60 * qrLayout.scale).dp)
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
                        }

                        // Voucher Code Box & Quota Meta
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Quota & Validity meta (Touch-draggable)
                            if (template.showValidity && template.metaLayout.isVisible) {
                                val metaLayout = template.metaLayout
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(metaLayout.offsetX.dp.roundToPx(), metaLayout.offsetY.dp.roundToPx()) }
                                        .pointerInput("meta") {
                                            detectDragGestures(
                                                onDragStart = { selectedElementKey = "meta" },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val newX = (metaLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                    val newY = (metaLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                    onUpdateLayout("meta", metaLayout.copy(offsetX = newX, offsetY = newY))
                                                }
                                            )
                                        }
                                        .clickable { selectedElementKey = "meta" }
                                        .then(
                                            if (selectedElementKey == "meta") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(4.dp)).padding(2.dp)
                                            else Modifier.padding(2.dp)
                                        )
                                ) {
                                    Text(
                                        text = "${template.quotaText} • ${template.validityText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = (11 * metaLayout.scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(template.primaryColorHex),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }

                            // Voucher Code Box (Touch-draggable)
                            if (template.codeBoxLayout.isVisible) {
                                val codeLayout = template.codeBoxLayout
                                val boxWidthFraction = codeLayout.widthScale.coerceIn(0.35f, 1.0f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(codeLayout.offsetX.dp.roundToPx(), codeLayout.offsetY.dp.roundToPx()) }
                                        .pointerInput("code_box") {
                                            detectDragGestures(
                                                onDragStart = { selectedElementKey = "code_box" },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val newX = (codeLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                    val newY = (codeLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                    onUpdateLayout("code_box", codeLayout.copy(offsetX = newX, offsetY = newY))
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(boxWidthFraction)
                                            .clickable { selectedElementKey = "code_box" }
                                            .then(
                                                if (selectedElementKey == "code_box") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(8.dp)).padding(3.dp)
                                                else Modifier.padding(3.dp)
                                            )
                                    ) {
                                        when (template.codeBoxStyle) {
                                            CodeBoxStyle.PILL_BADGE -> {
                                                Surface(
                                                    color = Color(template.primaryColorHex).copy(alpha = 0.2f),
                                                    shape = CircleShape,
                                                    border = BorderStroke(1.5.dp, Color(template.primaryColorHex)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "821 171",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = (17 * codeLayout.scale).sp,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = (6 * codeLayout.scale).dp)
                                                    )
                                                }
                                            }
                                            CodeBoxStyle.SCRATCH_FOIL -> {
                                                Surface(
                                                    color = Color(0xFFD97706),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = (6 * codeLayout.scale).dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "امسح برفق: 821171",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = (12 * codeLayout.scale).sp,
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
                                                            border = BorderStroke(1.dp, Color(template.primaryColorHex)),
                                                            modifier = Modifier.size((24 * codeLayout.scale).dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = digit.toString(),
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = (13 * codeLayout.scale).sp,
                                                                    color = Color(0xFF0F172A)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.5.dp, Color(template.primaryColorHex)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "821171",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = (18 * codeLayout.scale).sp,
                                                        color = Color(0xFF0F172A),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = (5 * codeLayout.scale).dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer Row: Support Phone & Serial Number (Touch-draggable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (template.showSupportPhone && template.supportPhoneLayout.isVisible) {
                            val phoneLayout = template.supportPhoneLayout
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(phoneLayout.offsetX.dp.roundToPx(), phoneLayout.offsetY.dp.roundToPx()) }
                                    .pointerInput("phone") {
                                        detectDragGestures(
                                            onDragStart = { selectedElementKey = "phone" },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (phoneLayout.offsetX + dragAmount.x / density).coerceIn(-130f, 130f)
                                                val newY = (phoneLayout.offsetY + dragAmount.y / density).coerceIn(-65f, 65f)
                                                onUpdateLayout("phone", phoneLayout.copy(offsetX = newX, offsetY = newY))
                                            }
                                        )
                                    }
                                    .clickable { selectedElementKey = "phone" }
                                    .then(
                                        if (selectedElementKey == "phone") Modifier.border(1.5.dp, BrandCyanPrimary, RoundedCornerShape(4.dp)).padding(2.dp)
                                        else Modifier.padding(2.dp)
                                    )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(template.primaryColorHex),
                                        modifier = Modifier.size((12 * phoneLayout.scale).dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الدعم: ${template.supportPhone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = (10 * phoneLayout.scale).sp,
                                        color = if (customBitmap != null || !isCleanWhite) Color.LightGray else Color(0xFF475569)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (template.showSerialNumber && template.serialLayout.isVisible) {
                            Text(
                                text = "SN: #000142",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = (10 * template.serialLayout.scale).sp,
                                color = if (customBitmap != null || !isCleanWhite) Color.Gray else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // 3. Touch Controller & Precision Nudge Tool (Shown only in Designer tab)
        if (showNudgeControls) {
            ElementTouchControlBar(
                selectedKey = selectedElementKey,
                onSelectKey = { selectedElementKey = it },
                template = template,
                onUpdateLayout = onUpdateLayout,
                onResetPositions = onResetPositions
            )
        }
    }
}

/**
 * Interactive Touch Drag & Scale Controller Toolbar.
 * Allows quick scaling, fine-tuning nudge in 4 directions, and position reset.
 */
@Composable
private fun ElementTouchControlBar(
    selectedKey: String,
    onSelectKey: (String) -> Unit,
    template: CardTemplate,
    onUpdateLayout: (String, ElementLayout) -> Unit,
    onResetPositions: () -> Unit
) {
    val currentLayout = when (selectedKey) {
        "price" -> template.priceLayout
        "network" -> template.networkLayout
        "code_box" -> template.codeBoxLayout
        "qr" -> template.qrCodeLayout
        "meta" -> template.metaLayout
        "phone" -> template.supportPhoneLayout
        "serial" -> template.serialLayout
        else -> template.codeBoxLayout
    }

    val elementLabels = listOf(
        "code_box" to "صندوق الكود 🔢",
        "price" to "السعر 🏷️",
        "network" to "الشبكة 📶",
        "qr" to "رمز QR 📱",
        "meta" to "الرصيد/الوقت 📊",
        "phone" to "الدعم 📞",
        "serial" to "تاريخ الكرت 📅"
    )

    Surface(
        color = Color(0xFF0F1E2E),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row of element selection chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "تعديل باللمس:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                elementLabels.forEach { (key, label) ->
                    val isSel = selectedKey == key
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) BrandCyanPrimary else Color(0xFF1E293B),
                        border = BorderStroke(0.5.dp, if (isSel) Color.White else Color.Gray),
                        modifier = Modifier.clickable { onSelectKey(key) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Controls for active element: Scale, Width/Length & Directional Nudge Pad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Size controls (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("الحجم:", fontSize = 10.sp, color = Color.LightGray)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                val newScale = (currentLayout.scale - 0.1f).coerceIn(0.6f, 1.8f)
                                onUpdateLayout(selectedKey, currentLayout.copy(scale = newScale))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(
                        text = "${(currentLayout.scale * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                val newScale = (currentLayout.scale + 0.1f).coerceIn(0.6f, 1.8f)
                                onUpdateLayout(selectedKey, currentLayout.copy(scale = newScale))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Width / Length controls (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("العرض/الطول:", fontSize = 10.sp, color = Color.LightGray)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                val newWidth = (currentLayout.widthScale - 0.1f).coerceIn(0.4f, 2.0f)
                                onUpdateLayout(selectedKey, currentLayout.copy(widthScale = newWidth))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(
                        text = "${(currentLayout.widthScale * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAB308)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                val newWidth = (currentLayout.widthScale + 0.1f).coerceIn(0.4f, 2.0f)
                                onUpdateLayout(selectedKey, currentLayout.copy(widthScale = newWidth))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Precision Nudge pad (Move in 4 directions)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("تحريك:", fontSize = 10.sp, color = Color.LightGray)
                    IconButton(
                        onClick = {
                            val newX = (currentLayout.offsetX - 4f).coerceIn(-130f, 130f)
                            onUpdateLayout(selectedKey, currentLayout.copy(offsetX = newX))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("◀", fontSize = 10.sp, color = BrandCyanPrimary)
                    }
                    IconButton(
                        onClick = {
                            val newY = (currentLayout.offsetY - 4f).coerceIn(-70f, 70f)
                            onUpdateLayout(selectedKey, currentLayout.copy(offsetY = newY))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("▲", fontSize = 10.sp, color = BrandCyanPrimary)
                    }
                    IconButton(
                        onClick = {
                            val newY = (currentLayout.offsetY + 4f).coerceIn(-70f, 70f)
                            onUpdateLayout(selectedKey, currentLayout.copy(offsetY = newY))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("▼", fontSize = 10.sp, color = BrandCyanPrimary)
                    }
                    IconButton(
                        onClick = {
                            val newX = (currentLayout.offsetX + 4f).coerceIn(-130f, 130f)
                            onUpdateLayout(selectedKey, currentLayout.copy(offsetX = newX))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("▶", fontSize = 10.sp, color = BrandCyanPrimary)
                    }
                }

                // Reset position button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        onUpdateLayout(selectedKey, ElementLayout())
                    }
                ) {
                    Text(
                        text = "إعادة ضبط",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private data class QuickPackagePreset(
    val label: String,
    val profile: String,
    val price: String,
    val quota: String
)

/**
 * Dialog to save template linked to a specific package profile and price.
 */
@Composable
private fun SaveTemplateForPackageDialog(
    currentTemplate: CardTemplate,
    onDismiss: () -> Unit,
    onConfirmSave: (templateName: String, packageProfile: String, packageName: String, priceText: String, quotaText: String) -> Unit
) {
    var templateName by remember {
        mutableStateOf(if (currentTemplate.name.contains("قالب كرت")) currentTemplate.name else "قالب ${currentTemplate.priceText}")
    }
    var packageName by remember {
        mutableStateOf(if (currentTemplate.targetPackageName.isNotBlank()) currentTemplate.targetPackageName else "باقة ${currentTemplate.priceText}")
    }
    var packageProfile by remember {
        mutableStateOf(if (currentTemplate.targetPackageProfile.isNotBlank()) currentTemplate.targetPackageProfile else "profile_${currentTemplate.priceText.filter { it.isDigit() }}")
    }
    var priceText by remember { mutableStateOf(currentTemplate.priceText) }
    var quotaText by remember { mutableStateOf(currentTemplate.quotaText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDarkCardElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = BrandCyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ القالب وربطه بباقة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "سيتم حفظ تصميم هذا الكرت بكل أبعاده ومواقع عناصره وتنسيقاته وربطه تلقائياً بهذه الباقة لاسترجاعه بلمسة واحدة أثناء التوليد والطباعة.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                // Quick presets for fast fill
                Text("تعبئة سريعة لباقة شائعة:", fontSize = 11.sp, color = BrandCyanPrimary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        QuickPackagePreset("قالب كرت 200", "1G_1D", "200 ر.ي", "1 جيجا"),
                        QuickPackagePreset("قالب كرت 300", "1500M_3D", "300 ر.ي", "1.5 جيجا"),
                        QuickPackagePreset("قالب كرت 500", "2700M_35H", "500 ر.ي", "2700 ميجا"),
                        QuickPackagePreset("قالب كرت 1000", "5G_15D", "1000 ر.ي", "5 جيجا")
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.5.dp, BrandCyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                templateName = preset.label
                                packageName = "باقة ${preset.price}"
                                packageProfile = preset.profile
                                priceText = preset.price
                                quotaText = preset.quota
                            }
                        ) {
                            Text(
                                text = preset.label,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("اسم القالب (مثلاً: قالب كرت 200)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyanPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("اسم الباقة (مثلاً: باقة 200 ريال)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyanPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageProfile,
                    onValueChange = { packageProfile = it },
                    label = { Text("بروفايل الميكروتك (User Profile)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyanPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("السعر") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = quotaText,
                        onValueChange = { quotaText = it },
                        label = { Text("الرصيد / الميجا") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onConfirmSave(templateName, packageProfile, packageName, priceText, quotaText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
            ) {
                Text("حفظ وربط الباقة 💾", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.LightGray)
            }
        }
    )
}

/**
 * Tab 0: Generator Parameters & Instant MikroTik Batch Provisioning.
 */
@Composable
private fun GeneratorParametersTab(
    uiState: CardStudioUiState,
    savedTemplates: List<CardTemplate>,
    availablePackages: List<CardPackageEntity> = emptyList(),
    onSelectPackage: ((CardPackageEntity) -> Unit)? = null,
    onSelectSavedTemplate: (CardTemplate) -> Unit,
    onSelectPackageProfile: (profile: String, packageName: String, price: String, quota: String) -> Unit,
    onSelectPrintFormat: (PrintPageFormat) -> Unit,
    onUpdateConfig: (String?, Int?, String?, String?, Int?, PasswordPattern?, CardCharacterSet?, PrintPageFormat?) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary & Total Box
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
                            text = "الإجمالي: %,d ر.ي".format(totalRevenue),
                            fontWeight = FontWeight.Bold,
                            color = BrandCyanPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "القالب النشط: ${template.name} • الحجم: ${template.quotaText} • الوقت: ${template.validityText} • السعر: ${template.priceText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        // Section 0: Live System Packages (ربط مباشر مع باقات الكروت المعرفة)
        if (availablePackages.isNotEmpty()) {
            Text(
                text = "اختر باقة للربط والتوليد التلقائي 📦:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BrandCyanPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availablePackages.forEach { pkg ->
                    val isSelected = template.targetPackageName == pkg.name || template.targetPackageProfile == pkg.profileName
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandCyanPrimary else Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable { onSelectPackage?.invoke(pkg) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = pkg.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BrandCyanPrimary else Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${pkg.retailPriceMinor / 100L} ر.ي • ${if (pkg.quotaMb > 0) "${pkg.quotaMb}M" else "مفتوحة"}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Saved Templates linked to packages
        if (savedTemplates.isNotEmpty()) {
            Text(
                text = "قوالب الكروت المحفوظة للباقات (ربط سحابي وتلقائي):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BrandCyanPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedTemplates.forEach { tmpl ->
                    val isSelected = tmpl.id == template.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandCyanPrimary else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable { onSelectSavedTemplate(tmpl) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Style,
                                    contentDescription = null,
                                    tint = if (isSelected) BrandCyanPrimary else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tmpl.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                            Text(
                                text = "${tmpl.priceText} • ${tmpl.quotaText}",
                                fontSize = 9.sp,
                                color = BrandCyanPrimary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Quick Package Presets
        Text("باقة وسرعة الإنترنت المربوطة بالكرت:", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                QuickPackagePreset("كرت 200 (1GB • يوم)", "1G_1D", "200 ر.ي", "1 جيجا"),
                QuickPackagePreset("كرت 300 (1.5GB • 3 أيام)", "1500M_3D", "300 ر.ي", "1.5 جيجا"),
                QuickPackagePreset("كرت 500 (2.7GB • 35 ساعة)", "2700M_35H", "500 ر.ي", "2700 ميجا"),
                QuickPackagePreset("كرت 1000 (5GB • 15 يوم)", "5G_15D", "1000 ر.ي", "5 جيجا")
            ).forEach { (label, profile, price, quota) ->
                FilterChip(
                    selected = uiState.batchProfileInput == profile,
                    onClick = {
                        onSelectPackageProfile(profile, label, price, quota)
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Section 3: Paper Sheet Setup & Card Grid Format
        Text("إعداد الورقة ومقاس الطباعة (تحديث فوري للمعاينة 📄):", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PrintPageFormat.A4_GRID_24 to "24 كرت (ورقة A4 كلاسيك)",
                PrintPageFormat.A4_GRID_42 to "42 كرت (A4 شريط اقتصادي)",
                PrintPageFormat.A4_GRID_18 to "18 كرت (A4 عريض)",
                PrintPageFormat.A4_GRID_10 to "10 كروت (A4 كبير)",
                PrintPageFormat.THERMAL_ROLL_80MM to "رول فواتير حراري (80 مم)"
            ).forEach { (format, label) ->
                val isSelected = uiState.selectedPrintFormat == format
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPrintFormat(format) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandCyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // ★ Section 4: Interactive Card Count Box (Matching User's Attached Screenshot)
        val step = uiState.selectedPrintFormat.cardsPerPage
        var showDirectCountDialog by remember { mutableStateOf(false) }

        if (showDirectCountDialog) {
            var tempCountStr by remember { mutableStateOf(uiState.batchCountInput.toString()) }
            AlertDialog(
                onDismissRequest = { showDirectCountDialog = false },
                title = { Text("اكتب عدد الكروت المطلوب مباشرة ✏️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("يمكنك إدخال أي رقم من 1 كرت حتى 10,000 كرت:", color = Color.LightGray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = tempCountStr,
                            onValueChange = { tempCountStr = it.filter { c -> c.isDigit() } },
                            label = { Text("عدد الكروت") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = tempCountStr.toIntOrNull()?.coerceIn(1, 10000) ?: uiState.batchCountInput
                            onUpdateConfig(null, count, null, null, null, null, null, null)
                            showDirectCountDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                    ) {
                        Text("تأكيد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDirectCountDialog = false }) {
                        Text("إلغاء", color = Color.LightGray)
                    }
                },
                containerColor = CyberDarkCardElevated
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071224)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, BrandCyanPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sheetsCount = (uiState.batchCountInput + step - 1) / step
                    Surface(
                        color = Color(0xFF0E3042),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BrandCyanPrimary)
                    ) {
                        Text(
                            text = "${uiState.batchCountInput} كرت  •  $sheetsCount ورقة A4",
                            color = Color(0xFF67E8F9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "عدد الكروت المطلوب توليدها:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "✏️ اكتب العدد المطلوب مباشرة (مثال: 500، مرونة من 1 حتى 10,000 كرت):",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                // Direct Display Box & Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Steppers on the left
                    listOf(
                        -step to "-$step",
                        step to "+$step",
                        100 to "+100",
                        500 to "+500"
                    ).forEach { (diff, label) ->
                        Surface(
                            color = Color(0xFF13253B),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    val newCount = (uiState.batchCountInput + diff).coerceIn(1, 10000)
                                    onUpdateConfig(null, newCount, null, null, null, null, null, null)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (diff > 0) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Prominent Direct Display Box on Right (Click to edit directly)
                    Surface(
                        color = Color(0xFF0F2538),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, BrandCyanPrimary),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .clickable { showDirectCountDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.batchCountInput}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "كرت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Quick Buttons by sheet capacity (Row 1: multiples of sheet; Row 2: 240, 500, 1000, 2500, 5000)
                Text(
                    text = "أزرار سريعة للأعداد الشائعة بحسب سعة الورقة:",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                // Row 1: Sheet Multipliers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4, 5).forEach { mult ->
                        val countVal = step * mult
                        val isSel = uiState.batchCountInput == countVal
                        Surface(
                            color = if (isSel) BrandCyanPrimary.copy(alpha = 0.25f) else Color(0xFF132030),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) BrandCyanPrimary else Color(0xFF1E293B)),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clickable {
                                    onUpdateConfig(null, countVal, null, null, null, null, null, null)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$countVal",
                                    color = if (isSel) BrandCyanPrimary else Color.White,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Row 2: Standard Quantities
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(240, 500, 1000, 2500, 5000).forEach { countVal ->
                        val isSel = uiState.batchCountInput == countVal
                        val isSpecial = countVal in listOf(1000, 2500, 5000)
                        Surface(
                            color = if (isSel) BrandCyanPrimary.copy(alpha = 0.25f) else Color(0xFF132030),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) BrandCyanPrimary else if (isSpecial) Color(0xFFD97706).copy(alpha = 0.6f) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clickable {
                                    onUpdateConfig(null, countVal, null, null, null, null, null, null)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val formatted = if (countVal >= 1000) "%,d".format(countVal) else "$countVal"
                                Text(
                                    text = formatted,
                                    color = if (isSel) BrandCyanPrimary else if (isSpecial) Color(0xFFFBBF24) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Slider 1 to 5000
                Slider(
                    value = uiState.batchCountInput.toFloat().coerceIn(1f, 5000f),
                    onValueChange = {
                        onUpdateConfig(null, it.toInt().coerceAtLeast(1), null, null, null, null, null, null)
                    },
                    valueRange = 1f..5000f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandCyanPrimary,
                        activeTrackColor = BrandCyanPrimary,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 كرت", fontSize = 10.sp, color = Color.Gray)
                    Text("2,500 كرت", fontSize = 10.sp, color = Color.Gray)
                    Text("5,000 كرت", fontSize = 10.sp, color = Color.Gray)
                }

                // Financial Estimation Banner
                val retailTotal = priceNumeric * uiState.batchCountInput
                val wholesaleTotal = (priceNumeric * 0.75).toInt() * uiState.batchCountInput
                Surface(
                    color = Color(0xFF0F1E2E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تقدير مالي للدفعة:", fontSize = 11.5.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تجزئة : ", fontSize = 11.5.sp, color = Color.LightGray)
                        Text("%,d ر.ي".format(retailTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        Text("  •  ", color = Color.Gray)
                        Text("جملة : ", fontSize = 11.5.sp, color = Color.LightGray)
                        Text("%,d ر.ي".format(wholesaleTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                    }
                }
            }
        }

        // ★ Section 5: Character Set (Matching Screenshot)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandCyanPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (uiState.batchCharacterSet == CardCharacterSet.NUMBERS_ONLY) "أرقام 0-9" else "أرقام وحروف",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "نوع ونمط محارف الكود (Character Set)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: NUMBERS_ONLY
                val isNum = uiState.batchCharacterSet == CardCharacterSet.NUMBERS_ONLY
                Surface(
                    color = if (isNum) Color(0xFF0F2C3F) else Color(0xFF0B1422),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isNum) BrandCyanPrimary else Color(0xFF1E293B)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdateConfig(null, null, null, null, null, null, CardCharacterSet.NUMBERS_ONLY, null) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أرقام فقط (0 - 9)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNum) Color.White else Color.LightGray
                            )
                            Text(
                                text = "الأسهل في الإدخال بالهاتف",
                                fontSize = 10.sp,
                                color = if (isNum) BrandCyanPrimary else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isNum) BrandCyanPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isNum) BrandCyanPrimary else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Option 2: UPPERCASE_ALPHANUMERIC
                val isAlpha = uiState.batchCharacterSet == CardCharacterSet.UPPERCASE_ALPHANUMERIC
                Surface(
                    color = if (isAlpha) Color(0xFF0F2C3F) else Color(0xFF0B1422),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isAlpha) BrandCyanPrimary else Color(0xFF1E293B)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdateConfig(null, null, null, null, null, null, CardCharacterSet.UPPERCASE_ALPHANUMERIC, null) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أرقام وحروف كبيرة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAlpha) Color.White else Color.LightGray
                            )
                            Text(
                                text = "0-9 + A-Z (أمان أعلى)",
                                fontSize = 10.sp,
                                color = if (isAlpha) BrandCyanPrimary else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAlpha) BrandCyanPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "T",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAlpha) BrandCyanPrimary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // ★ Section 6: Prefix
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("بادئة الرمز (Prefix)", style = MaterialTheme.typography.labelMedium, color = Color.White)
            OutlinedTextField(
                value = uiState.batchPrefixInput,
                onValueChange = { onUpdateConfig(null, null, null, it, null, null, null, null) },
                placeholder = { Text("اتركه فارغاً لأرقام صافية 100%", fontSize = 13.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCyanPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF091220),
                    unfocusedContainerColor = Color(0xFF091220)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.batchPrefixInput.isBlank()) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SemanticIncomeGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("أرقام صافية 100% بدون أي حروف (مطابق لليوزر مانجر)", fontSize = 11.sp, color = SemanticIncomeGreen)
                } else {
                    Text("بادئة مخصصة: '${uiState.batchPrefixInput}' ستبدأ بها جميع الأكواد", fontSize = 11.sp, color = BrandCyanPrimary)
                }
            }
        }

        // ★ Section 7: Code Length (عدد خانات/أرقام الكود - قابل للتعديل بحرية)
        var lengthInputStr by remember(uiState.batchUserLengthInput) {
            mutableStateOf(uiState.batchUserLengthInput.toString())
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("طول خانات الكود (عدد الأرقام في الكرت):", style = MaterialTheme.typography.labelMedium, color = Color.White)
                Surface(
                    color = BrandCyanPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${uiState.batchUserLengthInput} خانات",
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Selection Chips for Common Lengths (4, 5, 6, 7, 8, 10, 12)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(4, 5, 6, 7, 8, 10, 12).forEach { len ->
                    val isSelected = uiState.batchUserLengthInput == len
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandCyanPrimary else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (isSelected) BrandCyanPrimary else Color(0xFF1E293B)),
                        modifier = Modifier
                            .clickable {
                                lengthInputStr = len.toString()
                                onUpdateConfig(null, null, null, null, len, null, null, null)
                            }
                            .testTag("chip_length_$len")
                    ) {
                        Text(
                            text = if (len == 6) "6 خانات (الأمثل)" else "$len خانات",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Interactive Stepper + Direct Editable Text Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement Button (-)
                Button(
                    onClick = {
                        val newLen = (uiState.batchUserLengthInput - 1).coerceAtLeast(3)
                        lengthInputStr = newLen.toString()
                        onUpdateConfig(null, null, null, null, newLen, null, null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandCyanPrimary)
                }

                // Direct Text Field (Accepts free editing, Arabic/English digits, clearing and typing)
                OutlinedTextField(
                    value = lengthInputStr,
                    onValueChange = { raw ->
                        val normalized = raw.trim()
                            .replace("،", "")
                            .replace(",", "")
                            .replace(" ", "")
                            .map { c ->
                                when (c) {
                                    '٠', '۰' -> '0'
                                    '١', '۱' -> '1'
                                    '٢', '۲' -> '2'
                                    '٣', '۳' -> '3'
                                    '٤', '۴' -> '4'
                                    '٥', '۵' -> '5'
                                    '٦', '۶' -> '6'
                                    '٧', '۷' -> '7'
                                    '٨', '۸' -> '8'
                                    '٩', '۹' -> '9'
                                    else -> c
                                }
                            }.joinToString("")
                            .filter { it.isDigit() }

                        lengthInputStr = normalized

                        val parsed = normalized.toIntOrNull()
                        if (parsed != null) {
                            val clamped = parsed.coerceIn(3, 16)
                            onUpdateConfig(null, null, null, null, clamped, null, null, null)
                        }
                    },
                    placeholder = { Text("مثال: 6", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_code_length"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCyanPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF091220),
                        unfocusedContainerColor = Color(0xFF091220)
                    )
                )

                // Increment Button (+)
                Button(
                    onClick = {
                        val newLen = (uiState.batchUserLengthInput + 1).coerceAtMost(16)
                        lengthInputStr = newLen.toString()
                        onUpdateConfig(null, null, null, null, newLen, null, null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandCyanPrimary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("توليد مشفر غير قابل للتخمين (Anti-Guessing CSPRNG) • متاح من 3 إلى 16 خانة", fontSize = 11.sp, color = BrandCyanPrimary)
            }
        }

        // ★ Section 8: Password Pattern Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("نوع كلمة المرور (Password Type)", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(PasswordPattern.SAME_AS_USERNAME, "نفس المستخدم", "User = Password"),
                    Triple(PasswordPattern.NUMBERS_ONLY, "PIN منفصل", "كلمة سر مخصصة"),
                    Triple(PasswordPattern.NO_PASSWORD, "بدون كلمة سر", "اسم مستخدم فقط")
                ).forEach { (pattern, title, sub) ->
                    val isSelected = uiState.batchPasswordPattern == pattern
                    Surface(
                        color = if (isSelected) Color(0xFF0F2C3F) else Color(0xFF0B1422),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, if (isSelected) BrandCyanPrimary else Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateConfig(null, null, null, null, null, pattern, null, null) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.LightGray,
                                maxLines = 1
                            )
                            Text(
                                text = sub,
                                fontSize = 9.sp,
                                color = if (isSelected) BrandCyanPrimary else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
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
    onEditTemplate: (CardTemplate) -> Unit,
    onDuplicateTemplate: (CardTemplate) -> Unit,
    onDeleteTemplate: (CardTemplate) -> Unit,
    onUpdateTemplateDetails: (id: String, name: String, price: String, quota: String, validity: String, net: String, phone: String, profile: String) -> Unit,
    onSaveCurrentAsNew: () -> Unit,
    onApplyPreset: (CardThemePreset) -> Unit,
    onPickCustomImage: () -> Unit,
    onRemoveCustomImage: () -> Unit,
    onResetDefaults: () -> Unit
) {
    var templateToEditDetails by remember { mutableStateOf<CardTemplate?>(null) }
    var templateToDelete by remember { mutableStateOf<CardTemplate?>(null) }

    // Dialog 1: Edit Template Metadata (Name, Price, Quota, etc.)
    templateToEditDetails?.let { tmpl ->
        var editName by remember(tmpl) { mutableStateOf(tmpl.name) }
        var editPrice by remember(tmpl) { mutableStateOf(tmpl.priceText) }
        var editQuota by remember(tmpl) { mutableStateOf(tmpl.quotaText) }
        var editValidity by remember(tmpl) { mutableStateOf(tmpl.validityText) }
        var editNetwork by remember(tmpl) { mutableStateOf(tmpl.networkName) }
        var editPhone by remember(tmpl) { mutableStateOf(tmpl.supportPhone) }
        var editProfile by remember(tmpl) { mutableStateOf(tmpl.targetPackageProfile) }

        AlertDialog(
            onDismissRequest = { templateToEditDetails = null },
            containerColor = CyberDarkCardElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = BrandCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل بيانات القالب ✏️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم القالب") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("السعر (مثال: 500 ر.ي)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editQuota,
                            onValueChange = { editQuota = it },
                            label = { Text("الرصيد (2700 ميجا)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editValidity,
                            onValueChange = { editValidity = it },
                            label = { Text("الصلاحية (35 ساعة)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editProfile,
                            onValueChange = { editProfile = it },
                            label = { Text("باقة الميكروتك") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editNetwork,
                        onValueChange = { editNetwork = it },
                        label = { Text("اسم الشبكة على الكرت") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("هاتف الدعم الفني") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTemplateDetails(
                            tmpl.id,
                            editName,
                            editPrice,
                            editQuota,
                            editValidity,
                            editNetwork,
                            editPhone,
                            editProfile
                        )
                        templateToEditDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                ) {
                    Text("حفظ التعديلات 💾", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToEditDetails = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            }
        )
    }

    // Dialog 2: Delete Confirmation Dialog
    templateToDelete?.let { tmpl ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            containerColor = CyberDarkCardElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SemanticExpenseRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف قالب الكرت 🗑️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف قالب '${tmpl.name}' نهائياً؟\nسيتم حذف القالب من القائمة المحفوظة مع إمكانية استعادة القوالب الافتراضية لاحقاً.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(tmpl)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) {
                    Text("تأكيد الحذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ★ Section 0: Saved Templates Management (Requested: Full flexibility to edit and delete)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, BrandCyanPrimary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "قوالب الكروت المحفوظة 💾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = BrandCyanPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${savedTemplates.size} قوالب",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "مرونة كاملة لإدارة قوالب الكروت: يمكنك تعديل التصميم، تعديل الأسعار، تكرار القالب، أو حذفه بسهولة.",
                    fontSize = 11.5.sp,
                    color = Color.LightGray
                )

                Button(
                    onClick = onSaveCurrentAsNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3A47)),
                    border = BorderStroke(1.dp, BrandCyanPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التصميم المفتوح حالياً كقالب جديد 💾", color = Color(0xFF67E8F9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // List of saved templates
                if (savedTemplates.isEmpty()) {
                    Surface(
                        color = Color(0xFF0B1422),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لا توجد قوالب مخصصة محفوظة بعد. يمكنك تصميم أي كرت وحفظه هنا.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    savedTemplates.forEach { tmpl ->
                        val isCurrent = tmpl.id == currentTemplate.id
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF0F2636) else Color(0xFF0B1422)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) BrandCyanPrimary else Color(0xFF1E293B)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Title & Price Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tmpl.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = SemanticIncomeGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "النشط حالياً ✓",
                                                    color = SemanticIncomeGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${tmpl.priceText} • ${tmpl.quotaText}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Details line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "الشبكة: ${tmpl.networkName} • الصلاحية: ${tmpl.validityText}",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = if (!tmpl.customBackgroundImagePath.isNullOrBlank()) "خلفية من الاستديو 🖼️" else tmpl.themePreset.displayNameAr,
                                        fontSize = 10.sp,
                                        color = BrandCyanPrimary
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)

                                // Actions row: Edit Design, Edit Details, Duplicate, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Edit Design in Designer Tab
                                    Surface(
                                        color = Color(0xFF0F3A47),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(34.dp)
                                            .clickable { onEditTemplate(tmpl) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.FitScreen, contentDescription = null, tint = BrandCyanPrimary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تعديل التصميم 🎨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    // 2. Edit Metadata
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(34.dp)
                                            .clickable { templateToEditDetails = tmpl }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تعديل البيانات ✏️", fontSize = 11.sp, color = Color.LightGray)
                                        }
                                    }

                                    // 3. Duplicate
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { onDuplicateTemplate(tmpl) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "تكرار القالب", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // 4. Delete
                                    Surface(
                                        color = Color(0xFF261214),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.8.dp, SemanticExpenseRed.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { templateToDelete = tmpl }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف القالب", tint = SemanticExpenseRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Custom Background Image from Phone Gallery
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandCyanPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Section 2: Built-in High-Fidelity Presets
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
                ElementToggleRow("إظهار اسم الشبكة في أعلى الكرت", template.showNetworkName) {
                    onUpdateTemplate(template.copy(showNetworkName = it))
                }
                ElementToggleRow("إظهار تاريخ إنشاء الكرت وصيغته", template.showCreationDate) {
                    onUpdateTemplate(template.copy(showCreationDate = it))
                }
                if (template.showCreationDate) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF0F1E2E), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("صيغة تاريخ الإنشاء (Date Format):", fontSize = 11.sp, color = BrandCyanPrimary, fontWeight = FontWeight.Bold)
                        val formats = listOf("yyyy/MM/dd", "dd/MM/yyyy", "yyyy-MM-dd", "yy/MM/dd", "dd-MM-yyyy")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formats.forEach { fmt ->
                                val isFmtSel = template.dateFormatPattern == fmt
                                FilterChip(
                                    selected = isFmtSel,
                                    onClick = { onUpdateTemplate(template.copy(dateFormatPattern = fmt)) },
                                    label = { Text(fmt, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandCyanPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                        val sampleDate = try {
                            SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
                        } catch (_: Exception) {
                            template.dateFormatPattern
                        }
                        Text(
                            text = "معاينة التاريخ الحالي بالصيغة المختارة: $sampleDate",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }
                }
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
private fun createSampleBatch(template: CardTemplate, count: Int = 24): CardBatch {
    val sampleCards = (1..count.coerceAtLeast(1)).map { i ->
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
        profileName = template.targetPackageProfile.ifBlank { "default" },
        cardCount = sampleCards.size,
        prefix = "82",
        usernameLength = 6,
        passwordPattern = PasswordPattern.SAME_AS_USERNAME,
        cards = sampleCards
    )
}
