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
private const val GEMINI_MODEL = "gemini-2.0-flash"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

sealed interface AiChatTurn {
    data class User(val text: String) : AiChatTurn
    data class Assistant(val text: String) : AiChatTurn
}

class GeminiApiClient(
    private val toolExecutor: AiToolExecutor,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Executes a chat turn with function calling capability.
     * @param apiKey The Gemini API Key.
     * @param history Prior user & assistant chat turns.
     * @param latestUserMessage The prompt entered by the user.
     * @param onToolExecuting Callback triggered when a tool execution starts (for UI feedback).
     */
    suspend fun sendChatMessage(
        apiKey: String,
        history: List<AiChatTurn>,
        latestUserMessage: String,
        onToolExecuting: (toolName: String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لم يتم ضبط مفتاح Gemini API. يرجى إدخال المفتاح في لوحة الأسرار أو إعدادات المساعد.")
            )
        }

        val requestUrl = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

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

        // Add the current user prompt
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

        // Loop for multi-turn Function Calling (max 5 rounds)
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
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                return@withContext Result.failure(
                    Exception("فشل الاتصال بخادم الذكاء الاصطناعي: ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}")
                )
            }

            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP error ${response.code}: $responseBody")
                val errorMsg = try {
                    val errJson = JSONObject(responseBody).optJSONObject("error")
                    errJson?.optString("message") ?: "رمز الخطأ: ${response.code}"
                } catch (e: Exception) {
                    "رمز الخطأ: ${response.code}"
                }
                return@withContext Result.failure(Exception("خطأ من خدمة Gemini: $errorMsg"))
            }

            val parsedResponse = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON response", e)
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

            // Check if model emitted a FunctionCall
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

                // Execute the read-only function
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

                // Continue loop with the response passed back to model
                continue
            }

            // No function call, we received final text
            val finalText = textBuilder.toString().trim()
            if (finalText.isNotEmpty()) {
                return@withContext Result.success(finalText)
            } else {
                return@withContext Result.failure(Exception("الرد المستلم من المساعد كان فارغاً"))
            }
        }

        Result.failure(Exception("تجاوز الطلب الحد الأقصى لجولات الاستعلام"))
    }
}
