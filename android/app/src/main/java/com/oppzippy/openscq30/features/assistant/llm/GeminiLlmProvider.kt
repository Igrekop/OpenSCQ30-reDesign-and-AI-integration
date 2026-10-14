package com.oppzippy.openscq30.features.assistant.llm

import android.util.Log
import com.oppzippy.openscq30.features.assistant.storage.AssistantPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiLlmProvider @Inject constructor(
    private val preferences: AssistantPreferences,
) : LlmProvider {

    companion object {
        private const val TAG = "GeminiLlmProvider"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun streamCompletion(
        userMessage: String,
        systemPrompt: String?,
        onChunk: (String) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = preferences.apiKey
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Clé API Gemini non configurée. Veuillez renseigner votre clé dans les réglages de l'assistant."),
            )
        }

        val effectiveModel = preferences.model
        val effectiveSystemPrompt = systemPrompt ?: preferences.systemPrompt

        val requestBodyJson = buildJsonObject {
            putJsonObject("system_instruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", effectiveSystemPrompt) })
                }
            }
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", userMessage) })
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("maxOutputTokens", 600)
            }
        }.toString()

        var modelToUse = effectiveModel
        var url = "https://generativelanguage.googleapis.com/v1beta/models/$modelToUse:streamGenerateContent?alt=sse&key=$apiKey"

        var request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()

        try {
            var response = httpClient.newCall(request).execute()

            // Auto-fallback if the specified model is retired or not found on this API tier
            if (response.code == 404 && modelToUse != "gemini-1.5-flash") {
                Log.w(TAG, "Model $modelToUse returned 404, auto-falling back to gemini-1.5-flash")
                modelToUse = "gemini-1.5-flash"
                preferences.model = "gemini-1.5-flash"
                url = "https://generativelanguage.googleapis.com/v1beta/models/$modelToUse:streamGenerateContent?alt=sse&key=$apiKey"
                request = Request.Builder()
                    .url(url)
                    .post(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Content-Type", "application/json")
                    .build()
                response = httpClient.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Gemini API error ${response.code}: $errorBody")
                val userFriendlyMessage = when (response.code) {
                    400 -> "Clé API Gemini invalide ou requête incorrecte."
                    403 -> "Accès refusé à l'API Gemini. Vérifiez vos autorisations."
                    404 -> "Modèle Gemini indisponible (404). Vérifiez le modèle dans les réglages."
                    429 -> "Limite de requêtes atteinte pour votre clé Gemini. Réessayez dans un instant."
                    else -> "Erreur de connexion au serveur Gemini (${response.code})."
                }
                return@withContext Result.failure(Exception(userFriendlyMessage))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Réponse vide du serveur"))
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            val fullResponse = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val dataJson = currentLine.substring(6).trim()
                    if (dataJson == "[DONE]") break

                    try {
                        val parsed = json.parseToJsonElement(dataJson).jsonObject
                        val candidates = parsed["candidates"]?.jsonArray
                        val firstCandidate = candidates?.firstOrNull()?.jsonObject
                        val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
                        val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

                        if (!text.isNullOrEmpty()) {
                            fullResponse.append(text)
                            onChunk(text)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing SSE chunk: $dataJson", e)
                    }
                }
            }

            Result.success(fullResponse.toString())
        } catch (ex: Exception) {
            Log.e(TAG, "Network exception during Gemini stream", ex)
            Result.failure(Exception("Impossible de contacter Gemini : vérifiez votre connexion Internet."))
        }
    }
}
