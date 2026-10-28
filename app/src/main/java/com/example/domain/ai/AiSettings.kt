package com.example.domain.ai

/**
 * Supported AI API Providers for Sammi ERP Assistant.
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val defaultFallbackModel: String,
    val popularModels: List<String>
) {
    GEMINI(
        id = "GEMINI",
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models",
        defaultModel = "gemini-2.5-flash",
        defaultFallbackModel = "gemini-1.5-flash",
        popularModels = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro")
    ),
    OPENAI_COMPATIBLE(
        id = "OPENAI_COMPATIBLE",
        displayName = "OpenAI-Compatible / مخصص",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        defaultFallbackModel = "gpt-3.5-turbo",
        popularModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo", "meta-llama/llama-3.3-70b-instruct", "deepseek/deepseek-chat")
    );

    companion object {
        fun fromId(id: String?): AiProvider = entries.find { it.id.equals(id, ignoreCase = true) } ?: GEMINI
    }
}

/**
 * Complete dynamic configuration for AI Assistant.
 */
data class AiSettings(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = AiProvider.GEMINI.defaultModel,
    val fallbackModel: String = AiProvider.GEMINI.defaultFallbackModel,
    val customBaseUrl: String = ""
) {
    val cleanModel: String
        get() = sanitizeModel(model)

    val cleanFallbackModel: String
        get() = sanitizeModel(fallbackModel)

    val cleanApiKey: String
        get() = apiKey.trim()

    val effectiveBaseUrl: String
        get() = if (customBaseUrl.isNotBlank()) {
            customBaseUrl.trim().removeSuffix("/")
        } else {
            provider.defaultBaseUrl.trim().removeSuffix("/")
        }

    /**
     * Resolves the full URL for native Google Gemini endpoint:
     * https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:generateContent?key={API_KEY}
     */
    fun getGeminiGenerateContentUrl(targetModel: String = cleanModel): String {
        val sanitized = sanitizeModel(targetModel).ifBlank { provider.defaultModel }
        val base = if (customBaseUrl.isNotBlank()) {
            val b = customBaseUrl.trim().removeSuffix("/")
            if (b.endsWith("/models")) b else "$b/models"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models"
        }
        return "$base/$sanitized:generateContent?key=$cleanApiKey"
    }

    /**
     * Resolves the full chat completions URL for OpenAI-compatible providers:
     * dynamically appends /chat/completions to the base URL (handling trailing slashes and duplicates).
     * Example: https://openrouter.ai/api/v1 -> https://openrouter.ai/api/v1/chat/completions
     */
    fun getOpenAiChatCompletionsUrl(): String {
        val base = effectiveBaseUrl
        return if (base.endsWith("/chat/completions")) {
            base
        } else {
            "$base/chat/completions"
        }
    }

    companion object {
        /**
         * Strictly sanitizes model string by:
         * - Stripping leading/trailing double quotes, single quotes, backticks
         * - Trimming leading/trailing whitespace
         * - Stripping leading "models/" prefix (e.g. "models/gemini-1.5-flash" -> "gemini-1.5-flash")
         */
        fun sanitizeModel(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            var s = raw.trim().trim('"', '\'', '`').trim()
            if (s.startsWith("models/")) {
                s = s.removePrefix("models/").trim()
            }
            return s.trim('"', '\'', '`').trim()
        }
    }
}
