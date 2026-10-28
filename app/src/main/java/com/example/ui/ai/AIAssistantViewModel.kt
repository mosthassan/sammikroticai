package com.example.ui.ai

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.network.NetworkRepository
import com.example.domain.ai.AiChatTurn
import com.example.domain.ai.AiProvider
import com.example.domain.ai.AiSettings
import com.example.domain.ai.AiToolExecutor
import com.example.domain.ai.GeminiApiClient
import com.example.domain.usecase.StatementOfAccountUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT
}

enum class MessageStatus {
    SENT, ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val toolExecutionNote: String? = null
)

sealed interface ConnectionTestState {
    object Idle : ConnectionTestState
    object Loading : ConnectionTestState
    data class Success(val message: String) : ConnectionTestState
    data class Error(val errorMessage: String) : ConnectionTestState
}

class AIAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val networkRepository = NetworkRepository(application)
    private val statementOfAccountUseCase = StatementOfAccountUseCase(db)

    private val toolExecutor = AiToolExecutor(
        db = db,
        networkRepository = networkRepository,
        statementOfAccountUseCase = statementOfAccountUseCase
    )

    private val geminiApiClient = GeminiApiClient(toolExecutor = toolExecutor)

    private val prefs = application.getSharedPreferences("ai_assistant_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AiSettings> = _settings.asStateFlow()

    private val _apiKey = MutableStateFlow(loadSettings().apiKey)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(getInitialWelcomeMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeToolStatus = MutableStateFlow<String?>(null)
    val activeToolStatus: StateFlow<String?> = _activeToolStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastUserPrompt: String? = null

    private fun loadApiKey(): String {
        val saved = prefs.getString("custom_gemini_api_key", null)
            ?: prefs.getString("ai_api_key", null)
        if (!saved.isNullOrBlank()) return saved

        return runCatching {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = (field.get(null) as? String).orEmpty()
            if (key == "YOUR_GEMINI_API_KEY" || key.startsWith("MY_NEW_API_KEY")) "" else key
        }.getOrDefault("")
    }

    private fun loadSettings(): AiSettings {
        val providerId = prefs.getString("ai_provider", AiProvider.GEMINI.id)
        val provider = AiProvider.fromId(providerId)
        val key = loadApiKey()
        val rawModel = prefs.getString("ai_model", null)?.ifBlank { null }
        val rawFallback = prefs.getString("ai_fallback_model", null)?.ifBlank { null }
        var model = AiSettings.sanitizeModel(rawModel).ifBlank { provider.defaultModel }
        var fallbackModel = AiSettings.sanitizeModel(rawFallback).ifBlank { provider.defaultFallbackModel }

        // Sanitize broken/unsupported legacy identifiers if present
        if (provider == AiProvider.GEMINI) {
            if (model == "gemini-3.8-flash" || model == "gemini-2.0-flash") {
                model = provider.defaultModel
            }
            if (fallbackModel == "gemini-3.8-flash" || fallbackModel == "gemini-2.0-flash") {
                fallbackModel = provider.defaultFallbackModel
            }
        }

        val customBaseUrl = prefs.getString("ai_custom_base_url", "") ?: ""

        return AiSettings(
            provider = provider,
            apiKey = key.trim(),
            model = model,
            fallbackModel = fallbackModel,
            customBaseUrl = customBaseUrl.trim()
        )
    }

    fun saveSettings(newSettings: AiSettings) {
        val cleanModel = newSettings.cleanModel
        val cleanFallback = newSettings.cleanFallbackModel
        val cleanKey = newSettings.cleanApiKey
        val cleanBaseUrl = newSettings.customBaseUrl.trim()

        val sanitized = newSettings.copy(
            apiKey = cleanKey,
            model = cleanModel,
            fallbackModel = cleanFallback,
            customBaseUrl = cleanBaseUrl
        )

        prefs.edit()
            .putString("ai_provider", sanitized.provider.id)
            .putString("custom_gemini_api_key", cleanKey)
            .putString("ai_api_key", cleanKey)
            .putString("ai_model", cleanModel)
            .putString("ai_fallback_model", cleanFallback)
            .putString("ai_custom_base_url", cleanBaseUrl)
            .apply()

        _settings.value = sanitized
        _apiKey.value = cleanKey
        _errorMessage.value = null
    }

    fun resetToDefaults() {
        val currentKey = _settings.value.apiKey.ifBlank { loadApiKey() }
        val defaultSettings = AiSettings(
            provider = AiProvider.GEMINI,
            apiKey = currentKey,
            model = AiProvider.GEMINI.defaultModel,
            fallbackModel = AiProvider.GEMINI.defaultFallbackModel,
            customBaseUrl = ""
        )

        prefs.edit()
            .remove("ai_provider")
            .remove("ai_model")
            .remove("ai_fallback_model")
            .remove("ai_custom_base_url")
            .apply()

        _settings.value = defaultSettings
        _errorMessage.value = null
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun saveApiKey(newKey: String) {
        saveSettings(_settings.value.copy(apiKey = newKey.trim()))
    }

    fun testConnection(settingsToTest: AiSettings) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Loading
            val res = geminiApiClient.testConnection(settingsToTest)
            res.onSuccess { msg ->
                _connectionTestState.value = ConnectionTestState.Success(msg)
            }.onFailure { err ->
                _connectionTestState.value = ConnectionTestState.Error(err.localizedMessage ?: "فشل الاتصال بالخادم")
            }
        }
    }

    fun resetConnectionTestState() {
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun clearChat() {
        _messages.value = getInitialWelcomeMessages()
        _errorMessage.value = null
        _activeToolStatus.value = null
        lastUserPrompt = null
    }

    fun sendMessage(promptText: String) {
        val prompt = promptText.trim()
        if (prompt.isBlank() || _isLoading.value) return

        lastUserPrompt = prompt
        _errorMessage.value = null

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        val currentSettings = _settings.value.let {
            if (it.apiKey.isBlank()) it.copy(apiKey = loadApiKey()) else it
        }

        if (currentSettings.apiKey.isBlank()) {
            _isLoading.value = false
            _messages.value = _messages.value + ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "⚠️ **مفتاح API غير مضبوط.**\n\nيرجى إدخال مفتاحك عبر النقر على أيقونة الإعدادات (⚙️) في الأعلى، أو إضافته في لوحة الأسرار (Secrets Panel) تحت اسم `GEMINI_API_KEY`.",
                status = MessageStatus.ERROR
            )
            return
        }

        viewModelScope.launch {
            try {
                // Prepare conversation history for the model
                val history = _messages.value
                    .filter { it.status == MessageStatus.SENT && it.id != userMessage.id }
                    .map { msg ->
                        when (msg.role) {
                            MessageRole.USER -> AiChatTurn.User(msg.content)
                            MessageRole.ASSISTANT -> AiChatTurn.Assistant(msg.content)
                        }
                    }

                var executedToolName: String? = null

                val result = geminiApiClient.sendChatMessage(
                    apiKey = currentSettings.apiKey,
                    history = history,
                    latestUserMessage = prompt,
                    settings = currentSettings,
                    onToolExecuting = { toolName ->
                        executedToolName = toolName
                        _activeToolStatus.value = formatToolNameForUser(toolName)
                    }
                )

                _isLoading.value = false
                _activeToolStatus.value = null

                result.onSuccess { responseText ->
                    val assistantMessage = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = responseText,
                        toolExecutionNote = executedToolName?.let { formatToolExecutionNote(it) }
                    )
                    _messages.value = _messages.value + assistantMessage
                }.onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "حدث خطأ غير متوقع"
                    val assistantErrorMessage = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "❌ **عذراً، تعذر إكمال الطلب:**\n\n${err.localizedMessage ?: "حدث خطأ غير معروف"}",
                        status = MessageStatus.ERROR
                    )
                    _messages.value = _messages.value + assistantErrorMessage
                }
            } catch (e: CancellationException) {
                _isLoading.value = false
                _activeToolStatus.value = null
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _activeToolStatus.value = null
                _errorMessage.value = e.localizedMessage
                _messages.value = _messages.value + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "❌ **حدث خطأ تقني:**\n\n${e.localizedMessage}",
                    status = MessageStatus.ERROR
                )
            }
        }
    }

    fun retryLastMessage() {
        val prompt = lastUserPrompt ?: return
        if (_messages.value.isNotEmpty() && _messages.value.last().status == MessageStatus.ERROR) {
            _messages.value = _messages.value.dropLast(1)
        }
        if (_messages.value.isNotEmpty() && _messages.value.last().role == MessageRole.USER) {
            _messages.value = _messages.value.dropLast(1)
        }
        sendMessage(prompt)
    }

    private fun formatToolNameForUser(toolName: String): String {
        return when (toolName) {
            "get_monthly_profit_report" -> "جاري استخراج تقرير الأرباح والخسائر من دفتر الأستاذ..."
            "get_cash_flow_statement" -> "جاري تحليل قائمة التدفقات النقدية للصندوق..."
            "get_party_ledger" -> "جاري جرد كشوفات وأرصدة العملاء والموردين..."
            "get_assets_summary" -> "جاري استعلام وحصر الأصول الثابتة والإهلاك..."
            "get_network_devices_status" -> "جاري فحص حالة أجهزة وشبكة الميكروتك..."
            else -> if (toolName.startsWith("التحويل")) toolName else "جاري استعلام البيانات بدقة..."
        }
    }

    private fun formatToolExecutionNote(toolName: String): String {
        return when (toolName) {
            "get_monthly_profit_report" -> "تم استخراج البيانات مباشرة من تقرير الأرباح والخسائر IFRS"
            "get_cash_flow_statement" -> "تم استخراج البيانات مباشرة من قائمة التدفقات النقدية الفعلية"
            "get_party_ledger" -> "تم جرد ومطابقة أرصدة الحسابات الآجلة من دفتر الأستاذ"
            "get_assets_summary" -> "تم حصر الأصول الثابتة ومجمعات الإهلاك المحاسبية"
            "get_network_devices_status" -> "تم فحص حالة أجهزة الميكروتك والمحطات اللاسلكية"
            else -> "تم تنفيذ استعلام محاسبي دقيق"
        }
    }

    private fun getInitialWelcomeMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                role = MessageRole.ASSISTANT,
                content = """
                    أهلاً بك! أنا **مساعد سامي الذكي (Sammi ERP Assistant)**.
                    
                    أنا مهيأ للارتباط المباشر بالنظام المحاسبي وشبكة الميكروتك لتقديم تقارير دقيقة بصيغة جداول ونسب مالية رسمية.
                    
                    يمكنك سؤالي مباشرة عن:
                    * **أرباح ومبيعات الشهر الحالي أو أي شهر سابق**
                    * **موقف الصندوق والتدفقات النقدية الداخلة والخارجة**
                    * **ديون العملاء والوكلاء ومستحقات الموردين الآجلة**
                    * **قيمة الأصول الثابتة ومجمع إهلاكها**
                    * **حالة أجهزة الميكروتك وأبراج البث المتصلة والمنفصلة**
                    
                    اختر أحد الاقتراحات السريعة أدناه أو اكتب استفسارك مباشرة!
                """.trimIndent()
            )
        )
    }
}
