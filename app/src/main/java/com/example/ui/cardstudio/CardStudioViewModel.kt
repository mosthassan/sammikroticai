package com.example.ui.cardstudio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.data.card.CardBatch
import com.example.data.card.CardInjectionStatus
import com.example.data.card.CardInjectionTarget
import com.example.data.card.CardPreviewMode
import com.example.data.card.CardStudioRepository
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.ElementLayout
import com.example.data.card.defaultElementLayout
import com.example.data.card.GeneratedCard
import com.example.data.card.CardCharacterSet
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.data.card.quotaMb
import com.example.data.card.uptimeHours
import com.example.data.card.profileName
import com.example.data.local.entity.CardPackageEntity
import com.example.util.CardPdfDocumentGenerator
import com.example.util.MikroTikCardProvisioner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.network.RouterProfilesRepository
import com.example.data.network.MikrotikApiClient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

enum class CardStudioTab(
    val titleAr: String,
    val shortTitle: String,
    val subtitle: String
) {
    GENERATOR(
        titleAr = "معايير وتوليد الدفعة ⚡",
        shortTitle = "توليد",
        subtitle = "إعداد الأكواد وربط الباقات وحقن RouterOS"
    ),
    TEMPLATES(
        titleAr = "القوالب و AI 🎨",
        shortTitle = "القوالب",
        subtitle = "تصفح القوالب والسمات السحابية الجاهزة"
    ),
    DESIGNER(
        titleAr = "عناصر ومقاس الكرت 🎛️",
        shortTitle = "التصميم",
        subtitle = "تعديل النصوص والشعار ورمز QR والألوان"
    ),
    BATCHES(
        titleAr = "ورقة A4 والطباعة 📄",
        shortTitle = "الطباعة",
        subtitle = "تخطيط 24 أو 42 كرت وخطوط القص وتصدير PDF"
    )
}

enum class CardSystemTarget(val displayNameAr: String) {
    HOTSPOT("نظام الهوتسبوت (Hotspot)"),
    USER_MANAGER("نظام اليوزرمانجر (UserManager)")
}

data class CardStudioUiState(
    val activeTab: CardStudioTab = CardStudioTab.GENERATOR,
    val selectedSystemTarget: CardSystemTarget = CardSystemTarget.HOTSPOT,
    val editingTemplate: CardTemplate = CardTemplate(),
    val previewMode: CardPreviewMode = CardPreviewMode.A4_FULL_SHEET,
    val isGenerating: Boolean = false,
    val selectedBatchForPreview: CardBatch? = null,
    val batchTitleInput: String = "كرت 500 - باقة 2700 ميجا",
    val mikrotikCommentPreview: String = "",
    val internalNoteInput: String = "",
    val batchCountInput: Int = 24,
    val batchProfileInput: String = "2700M_35H",
    val batchPrefixInput: String = "",
    val batchUserLengthInput: Int = 6,
    val batchCharacterSet: CardCharacterSet = CardCharacterSet.NUMBERS_ONLY,
    val batchPasswordPattern: PasswordPattern = PasswordPattern.SAME_AS_USERNAME,
    val selectedPrintFormat: PrintPageFormat = PrintPageFormat.A4_GRID_24,
    val generatedPdfFile: File? = null,
    val isGeneratingPdf: Boolean = false,
    val pdfProgress: Pair<Int, Int>? = null,
    val isInjectingToRouter: Boolean = false,
    val activeInjectionBatchId: String? = null,
    val injectProgress: Pair<Int, Int>? = null,
    val injectSuccessDialogCount: Int? = null,
    val missingProfileWarning: Pair<CardBatch, String>? = null
)

class CardStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardStudioRepository.getInstance(application)
    private val routerProfilesRepository = RouterProfilesRepository(application)

    val templates: StateFlow<List<CardTemplate>> = repository.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batches: StateFlow<List<CardBatch>> = repository.batches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePackages: StateFlow<List<com.example.data.local.entity.CardPackageEntity>> = com.example.data.local.AppDatabase.getInstance(application).cardPackageDao().getAllPackagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CardStudioUiState())
    val uiState: StateFlow<CardStudioUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()
    private val isCreatingBatch = AtomicBoolean(false)
    private val isCreatingPdf = AtomicBoolean(false)
    private val isAnyBatchInjecting = AtomicBoolean(false)
    private val activeInjectionIds = ConcurrentHashMap.newKeySet<String>()
    private var initialTemplateLoaded = false

    fun selectPackageForGeneration(pkg: CardPackageEntity) {
        val q = pkg.quotaMb
        val u = pkg.uptimeHours
        val p = pkg.profileName
        val priceText = "${pkg.retailPriceMinor / 100L} ر.ي"

        _uiState.update { current ->
            val updatedTemplate = current.editingTemplate.copy(
                targetPackageName = pkg.name,
                targetPackageProfile = p,
                priceText = priceText,
                quotaText = if (q > 0) "$q ميجا" else "سعة مفتوحة",
                validityText = if (u > 0) "$u ساعة" else "وقت مفتوح"
            )
            current.copy(
                activeTab = CardStudioTab.GENERATOR,
                batchProfileInput = p,
                editingTemplate = updatedTemplate
            ).withBatchIdentity(updatedTemplate, p, nextBatchNumber())
        }
    }

    init {
        viewModelScope.launch {
            repository.templates.collect { list ->
                if (list.isNotEmpty() && !initialTemplateLoaded) {
                    initialTemplateLoaded = true
                    val template = list.first()
                    _uiState.update {
                        it.copy(editingTemplate = template, batchProfileInput = template.targetPackageProfile)
                            .withBatchIdentity(template, template.targetPackageProfile, nextBatchNumber())
                    }
                }
            }
        }
    }

    fun selectTab(tab: CardStudioTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun selectSystemTarget(target: CardSystemTarget) {
        _uiState.update { it.copy(selectedSystemTarget = target) }
    }

    fun selectTemplate(template: CardTemplate) {
        _uiState.update { it.copy(editingTemplate = template, batchProfileInput = template.targetPackageProfile)
            .withBatchIdentity(template, template.targetPackageProfile, nextBatchNumber()) }
    }

    fun setEditingTemplate(template: CardTemplate) {
        _uiState.update {
            it.copy(editingTemplate = template, activeTab = CardStudioTab.DESIGNER, batchProfileInput = template.targetPackageProfile)
                .withBatchIdentity(template, template.targetPackageProfile, nextBatchNumber())
        }
    }

    fun updateEditingTemplate(updated: CardTemplate) {
        _uiState.update { current ->
            val next = current.copy(editingTemplate = updated)
            if (updated.targetPackageProfile != current.editingTemplate.targetPackageProfile ||
                updated.targetPackageName != current.editingTemplate.targetPackageName
            ) {
                next.copy(batchProfileInput = updated.targetPackageProfile)
                    .withBatchIdentity(updated, updated.targetPackageProfile, nextBatchNumber())
            } else next
        }
    }

    private fun nextBatchNumber(): Long = (batches.value.maxOfOrNull { it.batchNumber } ?: 0L) + 1L

    private fun CardStudioUiState.withBatchIdentity(
        template: CardTemplate,
        profile: String,
        sequence: Long,
        createdAt: Long = System.currentTimeMillis()
    ): CardStudioUiState {
        val date = SimpleDateFormat("dd/MM", Locale.US).format(Date(createdAt))
        val packageName = template.targetPackageName.ifBlank { template.name.ifBlank { profile } }
        val title = "$packageName (${template.quotaText}) - $date #$sequence"
        return copy(
            batchTitleInput = title,
            mikrotikCommentPreview = com.example.data.card.createMikrotikBatchComment(profile, createdAt, sequence)
        )
    }

    fun applyThemePreset(preset: CardThemePreset) {
        val current = _uiState.value.editingTemplate
        val updated = when (preset) {
            CardThemePreset.CYBER_NEON -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFF00F0FF,
                backgroundColorHex = 0xFF080318,
                gradientEndColorHex = 0xFF12062B,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFFF007F,
                badgeBgColorHex = 0xFF00F0FF,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.PILL_BADGE
            )
            CardThemePreset.ROYAL_GOLD -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFFEAB308,
                backgroundColorHex = 0xFF0C0A09,
                gradientEndColorHex = 0xFF1C1917,
                textColorHex = 0xFFFEF08A,
                accentColorHex = 0xFFCA8A04,
                badgeBgColorHex = 0xFFEAB308,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.MODERN_BOX
            )
            CardThemePreset.CORPORATE_BLUE -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFF38BDF8,
                backgroundColorHex = 0xFF071322,
                gradientEndColorHex = 0xFF0F2744,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFF0284C7,
                badgeBgColorHex = 0xFF0284C7,
                badgeTextColorHex = 0xFFFFFFFF,
                codeBoxStyle = CodeBoxStyle.TICKET_DASHED
            )
            CardThemePreset.CLEAN_WHITE -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFF0284C7,
                backgroundColorHex = 0xFFFFFFFF,
                gradientEndColorHex = 0xFFF8FAFC,
                textColorHex = 0xFF0F172A,
                accentColorHex = 0xFF64748B,
                badgeBgColorHex = 0xFF0F172A,
                badgeTextColorHex = 0xFFFFFFFF,
                codeBoxStyle = CodeBoxStyle.MODERN_BOX
            )
            CardThemePreset.SPORT_SPEED -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFFEA580C,
                backgroundColorHex = 0xFF141418,
                gradientEndColorHex = 0xFF202028,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFF59E0B,
                badgeBgColorHex = 0xFFEA580C,
                badgeTextColorHex = 0xFFFFFFFF,
                codeBoxStyle = CodeBoxStyle.MODERN_BOX
            )
            CardThemePreset.SCRATCH_CARD -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFFF59E0B,
                backgroundColorHex = 0xFF1E293B,
                gradientEndColorHex = 0xFF0F172A,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFD97706,
                badgeBgColorHex = 0xFFF59E0B,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.SCRATCH_FOIL
            )
        }
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
    }

    fun applyCodeBoxStyle(style: CodeBoxStyle) {
        val updated = _uiState.value.editingTemplate.copy(codeBoxStyle = style)
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
    }

    fun saveEditingTemplate() {
        viewModelScope.launch {
            repository.saveTemplate(_uiState.value.editingTemplate)
            _userMessage.emit("تم حفظ قالب الكروت بنجاح")
        }
    }

    fun resetTemplatesToDefault() {
        viewModelScope.launch {
            repository.resetToDefaultTemplates()
            _userMessage.emit("تمت استعادة القوالب الافتراضية بنجاح")
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _userMessage.emit("تم حذف القالب")
        }
    }

    fun setCustomBackgroundImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val dir = File(context.filesDir, "card_templates").apply { mkdirs() }
                val targetFile = File(dir, "template_${_uiState.value.editingTemplate.id}_bg.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val updated = _uiState.value.editingTemplate.copy(
                    customBackgroundImagePath = targetFile.absolutePath,
                    customBackgroundImageUri = uri.toString()
                )
                _uiState.value = _uiState.value.copy(editingTemplate = updated)
                repository.saveTemplate(updated)
                _userMessage.emit("تم بنجاح جلب صورة القالب من الاستديو واستخدامها كخلفية للكرت 🖼️✨")
            } catch (e: Exception) {
                _userMessage.emit("تعذر استيراد الصورة: ${e.message}")
            }
        }
    }

    fun removeCustomBackgroundImage() {
        viewModelScope.launch {
            val current = _uiState.value.editingTemplate
            if (!current.customBackgroundImagePath.isNullOrBlank()) {
                try { File(current.customBackgroundImagePath).delete() } catch (_: Exception) {}
            }
            val updated = current.copy(
                customBackgroundImagePath = null,
                customBackgroundImageUri = null
            )
            _uiState.value = _uiState.value.copy(editingTemplate = updated)
            repository.saveTemplate(updated)
            _userMessage.emit("تمت إزالة صورة القالب والرجوع للثيم الرقمي 🎨")
        }
    }

    fun togglePreviewMode(mode: CardPreviewMode) {
        _uiState.value = _uiState.value.copy(previewMode = mode)
    }

    fun toggleCutLines(enabled: Boolean) {
        val updated = _uiState.value.editingTemplate.copy(showCutLines = enabled)
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
    }

    fun updateBatchConfig(
        title: String? = null,
        count: Int? = null,
        profile: String? = null,
        prefix: String? = null,
        userLength: Int? = null,
        pattern: PasswordPattern? = null,
        characterSet: CardCharacterSet? = null,
        format: PrintPageFormat? = null,
        internalNote: String? = null
    ) {
        _uiState.update { current ->
            val updated = current.copy(
                batchCountInput = count ?: current.batchCountInput,
                batchProfileInput = profile ?: current.batchProfileInput,
                batchPrefixInput = prefix ?: current.batchPrefixInput,
                batchUserLengthInput = userLength ?: current.batchUserLengthInput,
                batchPasswordPattern = pattern ?: current.batchPasswordPattern,
                batchCharacterSet = characterSet ?: current.batchCharacterSet,
                selectedPrintFormat = format ?: current.selectedPrintFormat,
                internalNoteInput = internalNote ?: current.internalNoteInput
            )
            if (profile != null && profile != current.batchProfileInput) {
                updated.withBatchIdentity(current.editingTemplate, profile, nextBatchNumber())
            } else updated
        }
    }

    fun generateBatch() {
        generateBatchWithAction(andInjectToRouter = false)
    }

    fun generateBatchWithAction(andInjectToRouter: Boolean = false, onSuccess: ((CardBatch) -> Unit)? = null) {
        if (!isCreatingBatch.compareAndSet(false, true)) return
        val state = _uiState.value
        if (state.batchCountInput <= 0) {
            isCreatingBatch.set(false)
            viewModelScope.launch { _userMessage.emit("يرجى تحديد عدد كروت صالح") }
            return
        }

        _uiState.update { it.copy(isGenerating = true) }
        viewModelScope.launch {
            try {
                val batch = repository.generateBatch(
                    template = state.editingTemplate,
                    profileName = state.batchProfileInput,
                    count = state.batchCountInput,
                    prefix = state.batchPrefixInput,
                    usernameLength = state.batchUserLengthInput,
                    passwordPattern = state.batchPasswordPattern,
                    characterSet = state.batchCharacterSet,
                    internalNote = state.internalNoteInput,
                    injectionTarget = if (state.selectedSystemTarget == CardSystemTarget.USER_MANAGER) {
                        CardInjectionTarget.USER_MANAGER
                    } else {
                        CardInjectionTarget.HOTSPOT
                    }
                )
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    selectedBatchForPreview = batch,
                    activeTab = CardStudioTab.BATCHES,
                    batchTitleInput = batch.displayTitle,
                    mikrotikCommentPreview = batch.technicalMikrotikComment
                )
                _userMessage.emit("تم توليد ${batch.cardCount} كرت بنجاح مع رموز QR الذكية!")
                onSuccess?.invoke(batch)
                if (andInjectToRouter) {
                    injectBatchToRouter(batch)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
                _userMessage.emit("تعذر توليد الدفعة: ${e.message}")
            } finally {
                isCreatingBatch.set(false)
            }
        }
    }

    fun duplicateTemplate(template: CardTemplate) {
        viewModelScope.launch {
            val copy = template.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${template.name} (نسخة)",
                createdAt = System.currentTimeMillis()
            )
            repository.saveTemplate(copy)
            _uiState.value = _uiState.value.copy(editingTemplate = copy)
            _userMessage.emit("تم تكرار القالب '${template.name}' بنجاح 📋")
        }
    }

    fun updateTemplateMetadata(
        templateId: String,
        name: String,
        priceText: String,
        quotaText: String,
        validityText: String,
        networkName: String,
        supportPhone: String,
        profile: String
    ) {
        viewModelScope.launch {
            val target = templates.value.firstOrNull { it.id == templateId } ?: _uiState.value.editingTemplate
            val updated = target.copy(
                name = name.ifBlank { target.name },
                priceText = priceText.ifBlank { target.priceText },
                quotaText = quotaText.ifBlank { target.quotaText },
                validityText = validityText.ifBlank { target.validityText },
                networkName = networkName.ifBlank { target.networkName },
                supportPhone = supportPhone.ifBlank { target.supportPhone },
                targetPackageProfile = profile.ifBlank { target.targetPackageProfile }
            )
            repository.saveTemplate(updated)
            if (_uiState.value.editingTemplate.id == templateId) {
                _uiState.value = _uiState.value.copy(editingTemplate = updated)
            }
            _userMessage.emit("تم تحديث بيانات القالب '${updated.name}' بنجاح ✏️✨")
        }
    }

    fun selectBatchForPreview(batch: CardBatch?) {
        _uiState.value = _uiState.value.copy(selectedBatchForPreview = batch)
    }

    fun exportAndSharePdf(context: Context, batch: CardBatch, format: PrintPageFormat = _uiState.value.selectedPrintFormat) {
        if (!isCreatingPdf.compareAndSet(false, true)) return
        _uiState.update { it.copy(isGeneratingPdf = true, pdfProgress = null) }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    CardPdfDocumentGenerator.generateBatchPdf(context, batch, format) { page, total ->
                        _uiState.update { it.copy(pdfProgress = page to total) }
                    }
                }
                _uiState.value = _uiState.value.copy(generatedPdfFile = file, isGeneratingPdf = false, pdfProgress = null)
                CardPdfDocumentGenerator.sharePdfFile(context, file, "كروت الميكروتك - دفعة #${batch.batchNumber}")
                repository.markBatchPrinted(batch.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGeneratingPdf = false, pdfProgress = null)
                _userMessage.emit("خطأ أثناء إنشاء ملف PDF: ${e.message}")
            } finally {
                isCreatingPdf.set(false)
            }
        }
    }

    fun shareMikrotikScript(context: Context, batch: CardBatch) {
        viewModelScope.launch {
            try {
                MikroTikCardProvisioner.shareScriptFile(context, batch)
                repository.markBatchPrinted(batch.id)
            } catch (e: Exception) {
                _userMessage.emit("خطأ في تصدير السكربت: ${e.message}")
            }
        }
    }

    fun updateElementLayout(elementKey: String, layout: ElementLayout) {
        val current = _uiState.value.editingTemplate
        val updated = when (elementKey) {
            "price" -> current.copy(priceLayout = layout)
            "network" -> current.copy(networkLayout = layout)
            "code_box" -> current.copy(codeBoxLayout = layout)
            "qr" -> current.copy(qrCodeLayout = layout)
            "meta" -> current.copy(metaLayout = layout)
            "phone" -> current.copy(supportPhoneLayout = layout)
            "date" -> current.copy(creationDateLayout = layout)
            "serial" -> current.copy(serialLayout = layout)
            else -> current
        }
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
    }

    fun resetAllElementPositions() {
        val current = _uiState.value.editingTemplate
        val updated = current.copy(
            priceLayout = defaultElementLayout("price"),
            networkLayout = defaultElementLayout("network"),
            codeBoxLayout = defaultElementLayout("code_box"),
            qrCodeLayout = defaultElementLayout("qr"),
            metaLayout = defaultElementLayout("meta"),
            supportPhoneLayout = defaultElementLayout("phone"),
            creationDateLayout = defaultElementLayout("date"),
            serialLayout = defaultElementLayout("serial")
        )
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
        viewModelScope.launch {
            _userMessage.emit("تمت إعادة ضبط مواقع جميع العناصر للمواضع القياسية 🔄")
        }
    }

    fun toggleStudioCodeOnlyMode() {
        val current = _uiState.value.editingTemplate
        val isCurrentlyCodeOnly = !current.showNetworkName && !current.showPrice && !current.showValidity && !current.showCreationDate && !current.showSupportPhone && !current.showSerialNumber
        val updated = if (isCurrentlyCodeOnly) {
            current.copy(
                showNetworkName = true,
                showPrice = true,
                showValidity = true,
                showCreationDate = true,
                showSupportPhone = true,
                showSerialNumber = true
            )
        } else {
            current.copy(
                showNetworkName = false,
                showPrice = false,
                showValidity = false,
                showCreationDate = false,
                showSupportPhone = false,
                showSerialNumber = false
            )
        }
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
        viewModelScope.launch {
            if (isCurrentlyCodeOnly) {
                _userMessage.emit("تم إظهار كافة نصوص وبيانات الكرت 🔤")
            } else {
                _userMessage.emit("تم تفعيل نمط الاستديو: طباعة صندوق الكود فقط وإخفاء النصوص المكررة 🎯")
            }
        }
    }

    fun saveTemplateForPackage(
        name: String,
        targetPackageProfile: String,
        targetPackageName: String,
        priceText: String,
        quotaText: String
    ) {
        viewModelScope.launch {
            val current = _uiState.value.editingTemplate
            val updated = current.copy(
                name = name.ifBlank { current.name },
                targetPackageProfile = targetPackageProfile,
                targetPackageName = targetPackageName,
                priceText = priceText.ifBlank { current.priceText },
                quotaText = quotaText.ifBlank { current.quotaText }
            )
            _uiState.value = _uiState.value.copy(editingTemplate = updated)
            repository.saveTemplate(updated)
            _userMessage.emit("تم حفظ القالب '${updated.name}' وربطه بـ '${targetPackageName}' 💾✨")
        }
    }

    fun selectPrintFormat(format: PrintPageFormat) {
        _uiState.value = _uiState.value.copy(
            selectedPrintFormat = format,
            batchCountInput = format.cardsPerPage
        )
    }

    fun selectPackageProfile(profile: String, packageName: String, price: String, quota: String) {
        val matchingTemplate = templates.value.firstOrNull {
            it.targetPackageProfile == profile || it.name.contains(price.filter { c -> c.isDigit() })
        }
        if (matchingTemplate != null) {
            _uiState.update {
                it.copy(editingTemplate = matchingTemplate, batchProfileInput = profile)
                    .withBatchIdentity(matchingTemplate, profile, nextBatchNumber())
            }
            viewModelScope.launch {
                _userMessage.emit("تم تفعيل قالب '${matchingTemplate.name}' المربوط بباقة $packageName 🎨")
            }
        } else {
            val current = _uiState.value.editingTemplate
            val updated = current.copy(
                targetPackageProfile = profile,
                targetPackageName = packageName,
                priceText = "$price ر.ي",
                quotaText = quota
            )
            _uiState.value = _uiState.value.copy(
                editingTemplate = updated,
                batchProfileInput = profile
            )
            _uiState.update { it.withBatchIdentity(updated, profile, nextBatchNumber()) }
        }
    }

    fun previewAndSaveSamplePdf(context: Context) {
        if (!isCreatingPdf.compareAndSet(false, true)) return
        _uiState.update { it.copy(isGeneratingPdf = true, pdfProgress = null) }
        viewModelScope.launch {
            try {
                val template = _uiState.value.editingTemplate
                val format = _uiState.value.selectedPrintFormat
                val file = withContext(Dispatchers.IO) {
                    val grid = template.printGridSettings.normalized()
                    val sampleCount = if (grid.isCustom && format != PrintPageFormat.THERMAL_ROLL_80MM) {
                        grid.columns * grid.rows
                    } else {
                        format.cardsPerPage
                    }
                    val sampleCards = (1..sampleCount).map { i ->
                        val code = String.format(java.util.Locale.US, "%06d", 820000 + i)
                        GeneratedCard(
                            serialNumber = String.format(java.util.Locale.US, "%06d", i),
                            username = code,
                            password = code,
                            priceText = template.priceText,
                            quotaText = template.quotaText,
                            validityText = template.validityText,
                            networkName = template.networkName,
                            supportPhone = template.supportPhone,
                            loginUrlWithCredentials = "${template.hotspotLoginUrl}?username=$code&password=$code",
                            batchId = "preview_sheet"
                        )
                    }
                    val sampleBatch = CardBatch(
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
                    CardPdfDocumentGenerator.generateBatchPdf(context, sampleBatch, format) { page, total ->
                        _uiState.update { it.copy(pdfProgress = page to total) }
                    }
                }
                _uiState.value = _uiState.value.copy(generatedPdfFile = file, isGeneratingPdf = false, pdfProgress = null)
                CardPdfDocumentGenerator.openOrSharePdfFile(context, file, "معاينة كروت ${template.name} - ورقة ${format.cardsPerPage} كرت")
                _userMessage.emit("تم تجهيز ورقة الطباعة للمعاينة الفورية 📄✨")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGeneratingPdf = false, pdfProgress = null)
                _userMessage.emit("خطأ أثناء تجهيز ورقة المعاينة: ${e.message}")
            } finally {
                isCreatingPdf.set(false)
            }
        }
    }

    fun deleteBatch(id: String) {
        viewModelScope.launch {
            repository.deleteBatch(id)
            if (_uiState.value.selectedBatchForPreview?.id == id) {
                _uiState.value = _uiState.value.copy(selectedBatchForPreview = null)
            }
            _userMessage.emit("تم حذف الدفعة")
        }
    }

    fun copyBatchScript(context: Context, batch: CardBatch) {
        val success = MikroTikCardProvisioner.copyScriptToClipboard(context, batch)
        viewModelScope.launch {
            if (success) {
                repository.markBatchPrinted(batch.id)
                _userMessage.emit("تم نسخ سكربت RouterOS (.rsc) للحافظة بنجاح 📋 جاهز للصق في Terminal")
            } else {
                _userMessage.emit("تعذر نسخ السكربت للحافظة")
            }
        }
    }

    fun shareBatchScript(context: Context, batch: CardBatch) {
        runCatching {
            MikroTikCardProvisioner.shareScriptFile(context, batch)
            viewModelScope.launch { repository.markBatchPrinted(batch.id) }
        }.onFailure {
            viewModelScope.launch {
                _userMessage.emit("تعذر مشاركة ملف السكربت: ${it.message}")
            }
        }
    }

    fun injectBatchToRouter(
        batch: CardBatch,
        skipProfileCheck: Boolean = false,
        recheckOnly: Boolean = false
    ) {
        val active = routerProfilesRepository.activeProfile.value
        if (active == null) {
            viewModelScope.launch {
                _userMessage.emit("لا يوجد راوتر متصل حالياً! يرجى الاتصال بالراوتر أولاً من شاشة تسجيل الدخول ⚠️")
            }
            return
        }
        if (!isAnyBatchInjecting.compareAndSet(false, true)) return
        if (!activeInjectionIds.add(batch.id)) {
            isAnyBatchInjecting.set(false)
            return
        }

        val plan = batch.getEffectivePlan()

        viewModelScope.launch {
            var injectionStarted = false
            var routerInjectionSucceeded = false
            _uiState.update {
                it.copy(
                    isInjectingToRouter = true,
                    activeInjectionBatchId = batch.id,
                    injectProgress = null,
                    missingProfileWarning = null
                )
            }
            try {
                val client = MikrotikApiClient(
                    host = active.host,
                    apiPort = active.apiPort,
                    httpPort = active.webPort,
                    username = active.username,
                    password = active.password
                )
                val isUserManager = batch.injectionTarget == CardInjectionTarget.USER_MANAGER

                if (!skipProfileCheck && !isUserManager) {
                    val existsResult = client.checkHotspotProfileExists(plan.profileName)
                    if (existsResult.isSuccess && !existsResult.getOrDefault(true)) {
                        _uiState.update { it.copy(missingProfileWarning = Pair(batch, plan.profileName)) }
                        return@launch
                    }
                }

                val latestBatch = batches.value.firstOrNull { it.id == batch.id } ?: batch
                repository.markBatchInjectionStarted(batch.id, active.host)
                injectionStarted = true
                val pendingCards = if (recheckOnly) {
                    latestBatch.cards
                } else {
                    latestBatch.cards.filterNot { it.id in latestBatch.injectedCardIds }
                }
                val alreadyConfirmed = latestBatch.injectedCardIds.size.coerceAtMost(batch.cardCount)
                _uiState.update { it.copy(injectProgress = alreadyConfirmed to batch.cardCount) }

                val result = if (isUserManager) {
                    client.injectUserManagerBatch(
                        cards = pendingCards,
                        plan = plan,
                        batchComment = batch.technicalMikrotikComment
                    ) { injected, _ ->
                        pendingCards.getOrNull(injected - 1)?.let { card ->
                            repository.markBatchCardsInjected(batch.id, listOf(card.id))
                        }
                        _uiState.update {
                            it.copy(injectProgress = (alreadyConfirmed + injected).coerceAtMost(batch.cardCount) to batch.cardCount)
                        }
                    }
                } else {
                    client.injectHotspotBatch(
                        cards = pendingCards,
                        plan = plan,
                        batchComment = batch.technicalMikrotikComment
                    ) { injected, _ ->
                        pendingCards.getOrNull(injected - 1)?.let { card ->
                            repository.markBatchCardsInjected(batch.id, listOf(card.id))
                        }
                        _uiState.update {
                            it.copy(injectProgress = (alreadyConfirmed + injected).coerceAtMost(batch.cardCount) to batch.cardCount)
                        }
                    }
                }

                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "فشل غير معروف"
                    repository.finishBatchInjection(batch.id, active.host, failed = true)
                    val current = batches.value.firstOrNull { it.id == batch.id } ?: batch
                    val remaining = (current.cardCount - current.injectedCardIds.size).coerceAtLeast(0)
                    _userMessage.emit(
                        if (current.injectedCardIds.isNotEmpty()) {
                            "توقف الحقن بعد تأكيد ${current.injectedCardIds.size} كرت؛ يمكنك استئناف حقن المتبقي ($remaining). $error"
                        } else {
                            "فشل حقن الدفعة في الراوتر: $error"
                        }
                    )
                    return@launch
                }
                repository.finishBatchInjection(batch.id, active.host, failed = false)
                routerInjectionSucceeded = true
                val stock = repository.addBatchStockToInventory(batch.id)
                _uiState.update { it.copy(injectSuccessDialogCount = stock.quantity) }
                _userMessage.emit(
                    if (stock.alreadyAdded) {
                        "✅ تم التحقق من الدفعة؛ مخزون (${stock.quantity}) كرت لباقة (${stock.packageName}) مسجل مسبقاً دون تكرار"
                    } else {
                        "✅ تم حقن الدفعة في الميكروتك وإضافة (${stock.quantity}) كرت إلى مخزن باقة (${stock.packageName}) بنجاح"
                    }
                )
            } catch (error: Exception) {
                if (injectionStarted && !routerInjectionSucceeded) {
                    runCatching { repository.finishBatchInjection(batch.id, active.host, failed = true) }
                }
                _userMessage.emit("تعذر إكمال حقن الدفعة أو تحديث حالتها: ${error.message}")
            } finally {
                activeInjectionIds.remove(batch.id)
                isAnyBatchInjecting.set(false)
                _uiState.update {
                    it.copy(
                        isInjectingToRouter = false,
                        activeInjectionBatchId = null,
                        injectProgress = null
                    )
                }
            }
        }
    }

    fun dismissMissingProfileWarning() {
        _uiState.update { it.copy(missingProfileWarning = null) }
    }

    fun dismissInjectSuccessDialog() {
        _uiState.update { it.copy(injectSuccessDialogCount = null) }
    }
}
