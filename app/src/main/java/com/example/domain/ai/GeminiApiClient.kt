package com.example.domain.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiApiClient"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

sealed interface AiChatTurn {
    data class User(val text: String) : AiChatTurn
    data class Assistant(val text: String) : AiChatTurn
}

/**
 * Custom Exception carrying HTTP status, response body, and request URL for precise diagnostics.
 */
class AiApiException(
    val statusCode: Int,
    val responseBody: String,
    val requestUrl: String,
    override val message: String
) : Exception(message)

/**
 * Dynamic Multi-Provider AI Client for Sammi ERP Assistant.
 *
 * Capabilities:
 * 1. Native Google Gemini direct endpoint with strict model name sanitization.
 * 2. OpenAI-Compatible Providers (OpenRouter, Groq, OpenAI, Ollama) with dynamic /chat/completions and required headers.
 * 3. Comprehensive Logcat diagnostics (full URL, headers, and error response body).
 * 4. Smooth, automated fallback to secondary model on HTTP 429 (Rate Limit) or HTTP 404 (Model Not Found / Deprecated).
 * 5. Interactive connection testing with detailed feedback.
 */
class GeminiApiClient(
    private val toolExecutor: AiToolExecutor,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Executes a chat turn with function calling capability and automatic fallback on HTTP 429 / 404.
     */
    suspend fun sendChatMessage(
        apiKey: String,
        history: List<AiChatTurn>,
        latestUserMessage: String,
        settings: AiSettings? = null,
        onToolExecuting: (toolName: String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val resolvedSettings = settings?.copy(
            apiKey = settings.apiKey.ifBlank { apiKey }
        ) ?: AiSettings(apiKey = apiKey)

        if (resolvedSettings.cleanApiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لم يتم ضبط مفتاح API. يرجى إدخال المفتاح في إعدادات المساعد الذكي.")
            )
        }

        val primaryModel = resolvedSettings.cleanModel.ifBlank { resolvedSettings.provider.defaultModel }
        val fallbackModel = resolvedSettings.cleanFallbackModel.ifBlank { resolvedSettings.provider.defaultFallbackModel }

        Log.i(TAG, "Starting chat request. Provider=${resolvedSettings.provider}, PrimaryModel=$primaryModel, FallbackModel=$fallbackModel")

        // 1. Attempt with Primary Model
        val primaryResult = executeSingleChatRequest(
            settings = resolvedSettings.copy(model = primaryModel),
            history = history,
            latestUserMessage = latestUserMessage,
            onToolExecuting = onToolExecuting
        )

        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // 2. Diagnostics & Automatic Fallback on HTTP 429 or 404
        val ex = primaryResult.exceptionOrNull()
        val statusCode = (ex as? AiApiException)?.statusCode ?: 0
        val errorMsg = ex?.message.orEmpty()
        val responseBody = (ex as? AiApiException)?.responseBody.orEmpty()
        val requestUrl = (ex as? AiApiException)?.requestUrl.orEmpty()

        val isFallbackEligible = statusCode == 429 || statusCode == 404 ||
                errorMsg.contains("429") || errorMsg.contains("404") ||
                errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                errorMsg.contains("quota", ignoreCase = true) ||
                errorMsg.contains("not found", ignoreCase = true) ||
                errorMsg.contains("no longer available", ignoreCase = true) ||
                errorMsg.contains("NOT_FOUND", ignoreCase = true) ||
                responseBody.contains("NOT_FOUND", ignoreCase = true) ||
                responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                responseBody.contains("is no longer available", ignoreCase = true)

        if (isFallbackEligible && fallbackModel.isNotBlank() && fallbackModel != primaryModel) {
            val reason = if (statusCode == 404 || errorMsg.contains("404") || responseBody.contains("NOT_FOUND")) {
                "النموذج '$primaryModel' غير متوفر (404)"
            } else {
                "تجاوز حد الاستهلاك للنموذج '$primaryModel' (429)"
            }

            Log.w(
                TAG,
                "Primary model '$primaryModel' failed ($reason). URL was: $requestUrl. Response: $responseBody. " +
                        "Smoothly falling back to secondary model: '$fallbackModel'"
            )

            withContext(Dispatchers.Main) {
                onToolExecuting("التحويل التلقائي للنموذج الاحتياطي ($fallbackModel)...")
            }

            val fallbackSettings = resolvedSettings.copy(model = fallbackModel)
            val fallbackResult = executeSingleChatRequest(
                settings = fallbackSettings,
                history = history,
                latestUserMessage = latestUserMessage,
                onToolExecuting = onToolExecuting
            )

            if (fallbackResult.isSuccess) {
                val note = "\n\n*(تمت الإجابة عبر النموذج البديل: $fallbackModel)*"
                return@withContext Result.success(fallbackResult.getOrThrow() + note)
            } else {
                val fallbackEx = fallbackResult.exceptionOrNull()
                Log.e(TAG, "Fallback model '$fallbackModel' also failed: ${fallbackEx?.message}")
                return@withContext Result.failure(
                    Exception("تعذر استجابة النموذج الأساسي ($primaryModel) والنموذج البديل ($fallbackModel):\n${fallbackEx?.localizedMessage ?: errorMsg}")
                )
            }
        }

        primaryResult
    }

    private suspend fun executeSingleChatRequest(
        settings: AiSettings,
        history: List<AiChatTurn>,
        latestUserMessage: String,
        onToolExecuting: (toolName: String) -> Unit
    ): Result<String> {
        return when (settings.provider) {
            AiProvider.GEMINI -> sendGeminiChatMessage(settings, history, latestUserMessage, onToolExecuting)
            AiProvider.OPENAI_COMPATIBLE -> sendOpenAiChatMessage(settings, history, latestUserMessage, onToolExecuting)
        }
    }

    /**
     * Executes chat with native Google Gemini API:
     * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:generateContent?key={API_KEY}
     */
    private suspend fun sendGeminiChatMessage(
        settings: AiSettings,
        history: List<AiChatTurn>,
        latestUserMessage: String,
        onToolExecuting: (toolName: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanModel = settings.cleanModel.ifBlank { settings.provider.defaultModel }
        val cleanKey = settings.cleanApiKey
        val requestUrl = settings.getGeminiGenerateContentUrl(cleanModel)

        Log.d(TAG, "Executing Gemini chat request. URL=$requestUrl, Model=$cleanModel")

        // Build contents array starting with conversation history
        val contentsArray = JSONArray()
        for (turn in history) {
            when (turn) {
                is AiChatTurn.User -> {
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply { put("text", turn.text) }))
                    })
                }
                is AiChatTurn.Assistant -> {
                    contentsArray.put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().apply { put("text", turn.text) }))
                    })
                }
            }
        }

        // Add current user prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().apply { put("text", latestUserMessage) }))
        })

        val tools = AiToolDefinitions.getToolsJsonArray()
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().apply { put("text", AiToolDefinitions.SYSTEM_INSTRUCTION) }))
        }
        val generationConfig = JSONObject().apply {
            put("temperature", 0.1)
        }

        var round = 0
        val maxRounds = 5

        while (round < maxRounds) {
            round++

            val requestBodyJson = JSONObject().apply {
                put("systemInstruction", systemInstruction)
                put("contents", contentsArray)
                put("tools", tools)
                put("generationConfig", generationConfig)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", cleanKey)
                .post(requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.e(TAG, "Gemini network call failed for URL: $requestUrl", e)
                return@withContext Result.failure(
                    Exception("فشل الاتصال بخادم Gemini ($requestUrl): ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}")
                )
            }

            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini HTTP error ${response.code} from URL: $requestUrl. Response Body: $responseBody")
                val errorMsg = parseHttpErrorMessage(response.code, responseBody, cleanModel, requestUrl)
                return@withContext Result.failure(
                    AiApiException(
                        statusCode = response.code,
                        responseBody = responseBody,
                        requestUrl = requestUrl,
                        message = errorMsg
                    )
                )
            }

            val parsedResponse = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Gemini JSON response from $requestUrl: $responseBody", e)
                return@withContext Result.failure(Exception("تعذر قراءة رد الخادم"))
            }

            val candidates = parsedResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("لم يُرجع النموذج أي محتوى أو قد تم حظر الطلب."))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return@withContext Result.failure(
                Exception("محتوى الرد فارغ")
            )
            val parts = content.optJSONArray("parts") ?: JSONArray()

            var functionCallObj: JSONObject? = null
            val textBuilder = StringBuilder()

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("functionCall")) {
                    functionCallObj = part.getJSONObject("functionCall")
                    break
                }
                if (part.has("text")) {
                    textBuilder.append(part.getString("text"))
                }
            }

            if (functionCallObj != null) {
                val functionName = functionCallObj.getString("name")
                val functionArgs = functionCallObj.optJSONObject("args") ?: JSONObject()
                Log.d(TAG, "Gemini invoked function: $functionName with args: $functionArgs")

                withContext(Dispatchers.Main) {
                    onToolExecuting(functionName)
                }

                val toolResult = toolExecutor.execute(functionName, functionArgs)

                // 1. Add model functionCall to contents history
                contentsArray.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("functionCall", JSONObject().apply {
                            put("name", functionName)
                            put("args", functionArgs)
                        })
                    }))
                })

                // 2. Add functionResponse to contents history
                contentsArray.put(JSONObject().apply {
                    put("role", "function")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", functionName)
                            put("response", JSONObject().apply {
                                put("content", toolResult)
                            })
                        })
                    }))
                })

                continue
            }

            val finalText = textBuilder.toString().trim()
            if (finalText.isNotEmpty()) {
                return@withContext Result.success(finalText)
            } else {
                return@withContext Result.failure(Exception("الرد المستلم من المساعد كان فارغاً"))
            }
        }

        Result.failure(Exception("تجاوز الطلب الحد الأقصى لجولات الاستعلام"))
    }

    /**
     * Executes chat with OpenAI-compatible API via /chat/completions:
     * Endpoint: {BASE_URL}/chat/completions
     * Headers:
     * - Authorization: Bearer <API_KEY>
     * - Content-Type: application/json
     * - HTTP-Referer: https://samtecai.sammikrotik
     * - X-Title: Sammi ERP AI
     */
    private suspend fun sendOpenAiChatMessage(
        settings: AiSettings,
        history: List<AiChatTurn>,
        latestUserMessage: String,
        onToolExecuting: (toolName: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanModel = settings.cleanModel.ifBlank { settings.provider.defaultModel }
        val cleanKey = settings.cleanApiKey
        val requestUrl = settings.getOpenAiChatCompletionsUrl()

        Log.d(TAG, "Executing OpenAI-compatible chat request. URL=$requestUrl, Model=$cleanModel")

        val messagesArray = JSONArray()
        // Add system message
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", AiToolDefinitions.SYSTEM_INSTRUCTION)
        })

        // Add history
        for (turn in history) {
            when (turn) {
                is AiChatTurn.User -> messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", turn.text)
                })
                is AiChatTurn.Assistant -> messagesArray.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", turn.text)
                })
            }
        }

        // Add user prompt
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", latestUserMessage)
        })

        val tools = AiToolDefinitions.getOpenAiToolsJsonArray()

        var round = 0
        val maxRounds = 5

        while (round < maxRounds) {
            round++

            val requestBodyJson = JSONObject().apply {
                put("model", cleanModel)
                put("messages", messagesArray)
                put("tools", tools)
                put("temperature", 0.1)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://samtecai.sammikrotik")
                .addHeader("X-Title", "Sammi ERP AI")
                .post(requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI network call failed for URL: $requestUrl", e)
                return@withContext Result.failure(
                    Exception("فشل الاتصال بمزود OpenAI ($requestUrl): ${e.localizedMessage ?: "تأكد من الرابط والاتصال"}")
                )
            }

            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "OpenAI HTTP error ${response.code} from URL: $requestUrl. Response Body: $responseBody")
                val errorMsg = parseHttpErrorMessage(response.code, responseBody, cleanModel, requestUrl)
                return@withContext Result.failure(
                    AiApiException(
                        statusCode = response.code,
                        responseBody = responseBody,
                        requestUrl = requestUrl,
                        message = errorMsg
                    )
                )
            }

            val parsedResponse = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse OpenAI JSON response from $requestUrl: $responseBody", e)
                return@withContext Result.failure(Exception("تعذر قراءة رد الخادم"))
            }

            val choices = parsedResponse.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("لم يُرجع النموذج أي خيارات استجابة."))
            }

            val firstChoice = choices.getJSONObject(0)
            val messageObj = firstChoice.optJSONObject("message") ?: return@withContext Result.failure(
                Exception("محتوى الرسالة المستلمة فارغ")
            )

            val toolCalls = messageObj.optJSONArray("tool_calls")
            if (toolCalls != null && toolCalls.length() > 0) {
                // Add the assistant's tool_calls message to history
                messagesArray.put(messageObj)

                for (i in 0 until toolCalls.length()) {
                    val call = toolCalls.getJSONObject(i)
                    val callId = call.optString("id", "call_$i")
                    val fnObj = call.getJSONObject("function")
                    val fnName = fnObj.getString("name")
                    val fnArgsStr = fnObj.optString("arguments", "{}")
                    val fnArgs = try { JSONObject(fnArgsStr) } catch (e: Exception) { JSONObject() }

                    withContext(Dispatchers.Main) {
                        onToolExecuting(fnName)
                    }

                    val toolResult = toolExecutor.execute(fnName, fnArgs)

                    // Append tool response
                    messagesArray.put(JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", callId)
                        put("content", toolResult.toString())
                    })
                }

                continue
            }

            val contentText = messageObj.optString("content", "").trim()
            if (contentText.isNotEmpty()) {
                return@withContext Result.success(contentText)
            } else {
                return@withContext Result.failure(Exception("الرد المستلم من المساعد كان فارغاً"))
            }
        }

        Result.failure(Exception("تجاوز الطلب الحد الأقصى لجولات الاستعلام"))
    }

    /**
     * Tests connectivity, credentials, and model responsiveness for the given settings.
     * Logs full HTTP request URL and response body in Logcat.
     */
    suspend fun testConnection(settings: AiSettings): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = settings.cleanApiKey
        val cleanModel = settings.cleanModel

        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال مفتاح API أولاً"))
        }
        if (cleanModel.isBlank()) {
            return@withContext Result.failure(Exception("يرجى تحديد أو كتابة اسم النموذج"))
        }

        val pingPrompt = "أجب بكلمة واحدة فقط: جاهز"

        try {
            when (settings.provider) {
                AiProvider.GEMINI -> {
                    val requestUrl = settings.getGeminiGenerateContentUrl(cleanModel)
                    Log.d(TAG, "Testing Gemini Connection: URL=$requestUrl, Model=$cleanModel")

                    val bodyJson = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().apply { put("text", pingPrompt) }))
                        }))
                        put("generationConfig", JSONObject().apply {
                            put("maxOutputTokens", 20)
                            put("temperature", 0.1)
                        })
                    }

                    val request = Request.Builder()
                        .url(requestUrl)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("x-goog-api-key", cleanKey)
                        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val response = client.newCall(request).execute()
                    val respBody = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        Log.i(TAG, "Gemini Test Connection successful from $requestUrl")
                        Result.success("تم الاتصال بنجاح بنموذج $cleanModel على منصة ${settings.provider.displayName} ✓")
                    } else {
                        Log.e(TAG, "Gemini Test Connection failed HTTP ${response.code} from URL: $requestUrl. Body: $respBody")
                        val errMsg = parseHttpErrorMessage(response.code, respBody, cleanModel, requestUrl)
                        Result.failure(Exception(errMsg))
                    }
                }
                AiProvider.OPENAI_COMPATIBLE -> {
                    val requestUrl = settings.getOpenAiChatCompletionsUrl()
                    Log.d(TAG, "Testing OpenAI-Compatible Connection: URL=$requestUrl, Model=$cleanModel")

                    val bodyJson = JSONObject().apply {
                        put("model", cleanModel)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", pingPrompt)
                        }))
                        put("max_tokens", 20)
                        put("temperature", 0.1)
                    }

                    val request = Request.Builder()
                        .url(requestUrl)
                        .addHeader("Authorization", "Bearer $cleanKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("HTTP-Referer", "https://samtecai.sammikrotik")
                        .addHeader("X-Title", "Sammi ERP AI")
                        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    val response = client.newCall(request).execute()
                    val respBody = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        Log.i(TAG, "OpenAI-Compatible Test Connection successful from $requestUrl")
                        Result.success("تم الاتصال بنجاح بنموذج $cleanModel على ${settings.provider.displayName} ✓")
                    } else {
                        Log.e(TAG, "OpenAI-Compatible Test Connection failed HTTP ${response.code} from URL: $requestUrl. Body: $respBody")
                        val errMsg = parseHttpErrorMessage(response.code, respBody, cleanModel, requestUrl)
                        Result.failure(Exception(errMsg))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection error: ${e.message}", e)
            Result.failure(Exception("تعذر الاتصال بالخادم: ${e.localizedMessage ?: "تأكد من صحة الرابط ومفتاح API والإنترنت"}"))
        }
    }

    private fun parseHttpErrorMessage(code: Int, body: String, model: String, requestUrl: String): String {
        val serverMsg = try {
            val root = JSONObject(body)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message") ?: root.optString("message", "")
        } catch (e: Exception) {
            ""
        }

        return when (code) {
            400 -> "طلب غير صالح ($code): ${serverMsg.ifBlank { "تأكد من صحة اسم النموذج أو معاملات الطلب" }}"
            401, 403 -> "مفتاح API غير صالح أو غير مصرح له ($code): ${serverMsg.ifBlank { "تحقق من صحة وصلاحيات المفتاح" }}"
            404 -> "النموذج '$model' غير متوفر أو الرابط غير صحيح ($code): ${serverMsg.ifBlank { "الرابط المطلوب: $requestUrl" }}"
            429 -> "تم تجاوز حد الطلبات للنموذج '$model' ($code). ${serverMsg.ifBlank { "يرجى تعيين نموذج بديل." }}"
            else -> "فشل الاستجابة ($code): ${serverMsg.ifBlank { "خطأ من الخادم" }}"
        }
    }
}
