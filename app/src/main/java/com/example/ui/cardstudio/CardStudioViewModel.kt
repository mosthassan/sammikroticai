package com.example.ui.cardstudio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.card.CardBatch
import com.example.data.card.CardStudioRepository
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
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

enum class CardStudioTab(val titleAr: String) {
    TEMPLATES("القوالب الجاهزة"),
    DESIGNER("المحرر المرئي"),
    GENERATOR("توليد دفعة جديدة"),
    BATCHES("الدفعات المولدة والطباعة")
}

data class CardStudioUiState(
    val activeTab: CardStudioTab = CardStudioTab.TEMPLATES,
    val editingTemplate: CardTemplate = CardTemplate(),
    val isGenerating: Boolean = false,
    val selectedBatchForPreview: CardBatch? = null,
    val batchTitleInput: String = "دفعة إنترنت فئة 500",
    val batchCountInput: Int = 50,
    val batchProfileInput: String = "2G_1W",
    val batchPrefixInput: String = "77",
    val batchUserLengthInput: Int = 6,
    val batchPasswordPattern: PasswordPattern = PasswordPattern.SAME_AS_USERNAME,
    val selectedPrintFormat: PrintPageFormat = PrintPageFormat.A4_GRID_10,
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
        // Initialize editing template with first template once available
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
                primaryColorHex = 0xFF00E5FF,
                backgroundColorHex = 0xFF0F172A,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFF00E5FF
            )
            CardThemePreset.GOLDEN_LUXURY -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFFFFD700,
                backgroundColorHex = 0xFF181824,
                textColorHex = 0xFFFFF8E7,
                accentColorHex = 0xFFFFB300
            )
            CardThemePreset.CLEAN_INK_SAVER -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFF0284C7,
                backgroundColorHex = 0xFFFFFFFF,
                textColorHex = 0xFF0F172A,
                accentColorHex = 0xFF0284C7
            )
            CardThemePreset.TECH_DARK -> current.copy(
                themePreset = preset,
                primaryColorHex = 0xFF10B981,
                backgroundColorHex = 0xFF050505,
                textColorHex = 0xFFE2E8F0,
                accentColorHex = 0xFF34D399
            )
        }
        _uiState.value = _uiState.value.copy(editingTemplate = updated)
    }

    fun saveEditingTemplate() {
        viewModelScope.launch {
            repository.saveTemplate(_uiState.value.editingTemplate)
            _userMessage.emit("تم حفظ قالب الكروت بنجاح")
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _userMessage.emit("تم حذف القالب")
        }
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
