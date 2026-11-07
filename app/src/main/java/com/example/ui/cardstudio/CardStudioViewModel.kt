package com.example.ui.cardstudio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.data.card.CardBatch
import com.example.data.card.CardPreviewMode
import com.example.data.card.CardStudioRepository
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.util.CardPdfDocumentGenerator
import com.example.util.MikroTikCardProvisioner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class CardStudioTab(val titleAr: String) {
    GENERATOR("معايير وتوليد الدفعة ⚡"),
    TEMPLATES("القوالب و AI 🎨"),
    DESIGNER("عناصر ومقاس الكرت 🎛️"),
    BATCHES("ورقة A4 والطباعة 📄")
}

data class CardStudioUiState(
    val activeTab: CardStudioTab = CardStudioTab.GENERATOR,
    val editingTemplate: CardTemplate = CardTemplate(),
    val previewMode: CardPreviewMode = CardPreviewMode.A4_FULL_SHEET,
    val isGenerating: Boolean = false,
    val selectedBatchForPreview: CardBatch? = null,
    val batchTitleInput: String = "كرت 500 - باقة 2700 ميجا",
    val batchCountInput: Int = 24,
    val batchProfileInput: String = "2700M_35H",
    val batchPrefixInput: String = "77",
    val batchUserLengthInput: Int = 6,
    val batchPasswordPattern: PasswordPattern = PasswordPattern.SAME_AS_USERNAME,
    val selectedPrintFormat: PrintPageFormat = PrintPageFormat.A4_GRID_24,
    val generatedPdfFile: File? = null
)

class CardStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardStudioRepository.getInstance(application)

    val templates: StateFlow<List<CardTemplate>> = repository.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batches: StateFlow<List<CardBatch>> = repository.batches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(CardStudioUiState())
    val uiState: StateFlow<CardStudioUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.templates.collect { list ->
                if (list.isNotEmpty() && _uiState.value.editingTemplate.id == CardTemplate().id) {
                    _uiState.value = _uiState.value.copy(editingTemplate = list.first())
                }
            }
        }
    }

    fun selectTab(tab: CardStudioTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setEditingTemplate(template: CardTemplate) {
        _uiState.value = _uiState.value.copy(
            editingTemplate = template,
            activeTab = CardStudioTab.DESIGNER
        )
    }

    fun updateEditingTemplate(updated: CardTemplate) {
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
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
        format: PrintPageFormat? = null
    ) {
        _uiState.value = _uiState.value.copy(
            batchTitleInput = title ?: _uiState.value.batchTitleInput,
            batchCountInput = count ?: _uiState.value.batchCountInput,
            batchProfileInput = profile ?: _uiState.value.batchProfileInput,
            batchPrefixInput = prefix ?: _uiState.value.batchPrefixInput,
            batchUserLengthInput = userLength ?: _uiState.value.batchUserLengthInput,
            batchPasswordPattern = pattern ?: _uiState.value.batchPasswordPattern,
            selectedPrintFormat = format ?: _uiState.value.selectedPrintFormat
        )
    }

    fun generateBatch() {
        val state = _uiState.value
        if (state.batchCountInput <= 0) {
            viewModelScope.launch { _userMessage.emit("يرجى تحديد عدد كروت صالح") }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            try {
                val batch = repository.generateBatch(
                    title = state.batchTitleInput,
                    template = state.editingTemplate,
                    profileName = state.batchProfileInput,
                    count = state.batchCountInput,
                    prefix = state.batchPrefixInput,
                    usernameLength = state.batchUserLengthInput,
                    passwordPattern = state.batchPasswordPattern
                )
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    selectedBatchForPreview = batch,
                    activeTab = CardStudioTab.BATCHES
                )
                _userMessage.emit("تم توليد ${batch.cardCount} كرت بنجاح مع رموز QR الذكية!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
                _userMessage.emit("تعذر توليد الدفعة: ${e.message}")
            }
        }
    }

    fun selectBatchForPreview(batch: CardBatch?) {
        _uiState.value = _uiState.value.copy(selectedBatchForPreview = batch)
    }

    fun exportAndSharePdf(context: Context, batch: CardBatch, format: PrintPageFormat = _uiState.value.selectedPrintFormat) {
        viewModelScope.launch {
            try {
                val file = CardPdfDocumentGenerator.generateBatchPdf(context, batch, format)
                _uiState.value = _uiState.value.copy(generatedPdfFile = file)
                CardPdfDocumentGenerator.sharePdfFile(context, file, "كروت الميكروتك - دفعة #${batch.batchNumber}")
            } catch (e: Exception) {
                _userMessage.emit("خطأ أثناء إنشاء ملف PDF: ${e.message}")
            }
        }
    }

    fun shareMikrotikScript(context: Context, batch: CardBatch) {
        try {
            MikroTikCardProvisioner.shareScriptFile(context, batch)
        } catch (e: Exception) {
            viewModelScope.launch { _userMessage.emit("خطأ في تصدير السكربت: ${e.message}") }
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
}
