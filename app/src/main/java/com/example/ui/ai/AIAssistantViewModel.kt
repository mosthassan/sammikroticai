package com.example.ui.ai

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.network.NetworkRepository
import com.example.domain.ai.AiChatTurn
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
    USER,
    ASSISTANT
}

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val toolExecutionNote: String? = null
)

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

    private val _apiKey = MutableStateFlow(loadApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

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
        if (!saved.isNullOrBlank()) return saved

        return runCatching {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = (field.get(null) as? String).orEmpty()
            if (key == "YOUR_GEMINI_API_KEY" || key.startsWith("MY_NEW_API_KEY")) "" else key
        }.getOrDefault("")
    }

    fun saveApiKey(newKey: String) {
        prefs.edit().putString("custom_gemini_api_key", newKey.trim()).apply()
        _apiKey.value = newKey.trim()
        _errorMessage.value = null
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

        val currentKey = _apiKey.value.ifBlank { loadApiKey() }
        if (currentKey.isBlank()) {
            _isLoading.value = false
            _messages.value = _messages.value + ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "⚠️ **مفتاح Gemini API غير مضبوط.**\n\nيرجى إدخال مفتاحك عبر النقر على أيقونة الإعدادات (⚙️) في الأعلى، أو إضافته في لوحة الأسرار (Secrets Panel) تحت اسم `GEMINI_API_KEY`.",
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
                    apiKey = currentKey,
                    history = history,
                    latestUserMessage = prompt,
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
        // Remove the last failed assistant message if any
        if (_messages.value.isNotEmpty() && _messages.value.last().status == MessageStatus.ERROR) {
            _messages.value = _messages.value.dropLast(1)
        }
        // Remove the user message duplicate so it doesn't appear twice
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
            else -> "جاري استعلام البيانات بدقة..."
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
