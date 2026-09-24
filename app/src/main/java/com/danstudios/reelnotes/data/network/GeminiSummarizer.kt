package com.danstudios.reelnotes.data.network

import android.util.Base64
import android.util.Log
import com.danstudios.reelnotes.domain.model.IngredientItem
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.StepItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Serializable
data class GeminiAiOutput(
    val category: String = "GENERAL",
    val title: String = "",
    val summary: String = "",
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val ingredients: List<IngredientItem> = emptyList(),
    val steps: List<StepItem> = emptyList(),
    val keyTakeaways: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val tags: List<String> = emptyList()
) {
    val categoryEnum: NoteCategory get() = NoteCategory.fromString(category)
}

object GeminiSummarizer {
    private const val TAG = "GeminiSummarizer"
    private const val INTERACTIONS_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"
    private const val DEFAULT_MODEL = "gemini-3.6-flash"
    private const val FALLBACK_MODEL = "gemini-3.1-flash-lite"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseAiJson(rawJson: String): GeminiAiOutput? {
        var clean = rawJson.trim()
        val codeBlockMatch = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""").find(clean)
        if (codeBlockMatch != null) {
            clean = codeBlockMatch.groupValues[1].trim()
        } else {
            val firstBrace = clean.indexOf('{')
            val lastBrace = clean.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace > firstBrace) {
                clean = clean.substring(firstBrace, lastBrace + 1).trim()
            }
        }
        return try {
            jsonParser.decodeFromString<GeminiAiOutput>(clean)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI output JSON: $clean", e)
            null
        }
    }

    suspend fun summarizeMultimodal(
        mediaBytes: ByteArray,
        mimeType: String,
        captionContext: String,
        apiKey: String,
        preferredLanguage: String = "fr"
    ): GeminiAiOutput? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || mediaBytes.isEmpty()) return@withContext null

        val langPrompt = if (preferredLanguage.startsWith("fr")) "French (Français)" else "English"
        val prompt = """
            You are an expert AI assistant specialized in analyzing Instagram Reels and transforming them into structured, actionable notes.
            Output language: $langPrompt.
            
            IMPORTANT INSTRUCTIONS:
            1. Listen to the spoken audio and watch the video carefully. Transcribe all spoken instructions, ingredients, and exact measurements.
            2. Take into account any text overlays, labels, or captions visible in the clip.
            3. Context or caption provided: "$captionContext"
            
            Format your entire response as a single valid JSON object following this exact schema:
            {
              "category": "RECIPE" | "TUTORIAL" | "WORKOUT" | "TIPS_INFO" | "TRAVEL" | "PRODUCT" | "GENERAL",
              "title": "Clear, precise title describing the content",
              "summary": "1-2 sentence TL;DR of the reel",
              "prepTime": "e.g. 15 min or null",
              "cookTime": "e.g. 25 min or null",
              "servings": "e.g. 4 personnes or null",
              "ingredients": [
                {"name": "ingredient name", "amount": "number or fraction", "unit": "g, ml, c. à soupe, etc"}
              ],
              "steps": [
                {"stepNumber": 1, "instruction": "Clear, concise action step transcribed from video/audio"}
              ],
              "keyTakeaways": ["Key takeaway 1", "Key takeaway 2"],
              "tips": ["Pro tip or advice mentioned in reel"],
              "tags": ["tag1", "tag2"]
            }
            Return ONLY the raw JSON object, without commentary or markdown ticks.
        """.trimIndent()

        val base64Data = Base64.encodeToString(mediaBytes, Base64.NO_WRAP)
        val mediaType = if (mimeType.startsWith("audio")) "audio" else "video"

        val models = listOf(DEFAULT_MODEL, FALLBACK_MODEL)
        for (modelName in models) {
            val payloadObj = JSONObject().apply {
                put("model", modelName)
                val inputArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    put(JSONObject().apply {
                        put("type", mediaType)
                        put("data", base64Data)
                        put("mime_type", mimeType)
                    })
                }
                put("input", inputArray)
            }

            try {
                Log.d(TAG, "Calling Gemini Multimodal ($modelName) with ${mediaBytes.size} bytes ($mimeType)...")
                val req = Request.Builder()
                    .url(INTERACTIONS_ENDPOINT)
                    .header("x-goog-api-key", apiKey.trim())
                    .header("Content-Type", "application/json")
                    .post(payloadObj.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: return@use
                    if (resp.code == 429 || resp.code == 503) {
                        Log.w(TAG, "Gemini model $modelName returned HTTP ${resp.code}, trying fallback if available...")
                        return@use // continue to next model in loop
                    }
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "Gemini multimodal failed ($modelName) with HTTP ${resp.code}: $body")
                        return@use
                    }
                    val parsed = extractInteractionTextAndParse(body)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini multimodal exception ($modelName)", e)
            }
        }
        null
    }

    suspend fun summarize(
        caption: String,
        apiKey: String,
        preferredLanguage: String = "fr"
    ): GeminiAiOutput? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || caption.isBlank()) return@withContext null

        val langPrompt = if (preferredLanguage.startsWith("fr")) "French (Français)" else "English"
        val prompt = """
            You are an expert AI assistant that turns Instagram Reels into structured, highly actionable notes.
            Language of output: $langPrompt.
            
            Analyze the following Instagram Reel caption / text:
            "$caption"
            
            Extract the information into valid JSON with this exact schema:
            {
              "category": "RECIPE" | "TUTORIAL" | "WORKOUT" | "TIPS_INFO" | "TRAVEL" | "PRODUCT" | "GENERAL",
              "title": "Clear, concise title",
              "summary": "1-2 sentence TL;DR of the reel",
              "prepTime": "e.g. 15 min or null",
              "cookTime": "e.g. 25 min or null",
              "servings": "e.g. 4 personnes or null",
              "ingredients": [
                {"name": "ingredient name", "amount": "number or fraction", "unit": "g, ml, tbsp, etc"}
              ],
              "steps": [
                {"stepNumber": 1, "instruction": "Clear action instruction"}
              ],
              "keyTakeaways": ["Important point 1", "Important point 2"],
              "tips": ["Chef/Pro tip 1"],
              "tags": ["tag1", "tag2"]
            }
            Return ONLY the raw JSON object, without commentary or markdown code blocks.
        """.trimIndent()

        val models = listOf(DEFAULT_MODEL, FALLBACK_MODEL)
        for (modelName in models) {
            val payloadObj = JSONObject().apply {
                put("model", modelName)
                val inputArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                }
                put("input", inputArray)
            }

            try {
                val req = Request.Builder()
                    .url(INTERACTIONS_ENDPOINT)
                    .header("x-goog-api-key", apiKey.trim())
                    .header("Content-Type", "application/json")
                    .post(payloadObj.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: return@use
                    if (resp.code == 429 || resp.code == 503) {
                        Log.w(TAG, "Gemini text model $modelName returned HTTP ${resp.code}, trying fallback...")
                        return@use
                    }
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "Gemini text call failed ($modelName) with HTTP ${resp.code}: $body")
                        return@use
                    }
                    val parsed = extractInteractionTextAndParse(body)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini text call exception ($modelName)", e)
            }
        }
        null
    }

    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La clé API est vide."))
        }

        val models = listOf(DEFAULT_MODEL, FALLBACK_MODEL)
        var lastError = ""
        for (modelName in models) {
            val payloadObj = JSONObject().apply {
                put("model", modelName)
                val inputArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", "Bonjour, réponds 'OK'.")
                    })
                }
                put("input", inputArray)
            }

            try {
                val req = Request.Builder()
                    .url(INTERACTIONS_ENDPOINT)
                    .header("x-goog-api-key", trimmed)
                    .header("Content-Type", "application/json")
                    .post(payloadObj.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        return@withContext Result.success("Clé API Gemini validée avec succès ($modelName) !")
                    } else if (resp.code == 429 || resp.code == 503) {
                        lastError = "Modèle $modelName temporairement saturé (${resp.code})"
                    } else {
                        val detail = try {
                            val json = JSONObject(body)
                            json.optJSONObject("error")?.optString("message") ?: body
                        } catch (_: Exception) {
                            body.take(200)
                        }
                        return@withContext Result.failure(Exception("Erreur Gemini (HTTP ${resp.code}) : $detail"))
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Erreur réseau"
            }
        }
        Result.failure(Exception("Serveurs Gemini occupés : $lastError"))
    }

    private fun extractInteractionTextAndParse(responseBody: String): GeminiAiOutput? {
        try {
            val json = JSONObject(responseBody)
            val steps = json.optJSONArray("steps") ?: return null
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                if (step.optString("type") == "model_output") {
                    val contentArr = step.optJSONArray("content") ?: continue
                    for (j in 0 until contentArr.length()) {
                        val c = contentArr.getJSONObject(j)
                        val text = c.optString("text")
                        if (text.isNotBlank()) {
                            val parsed = parseAiJson(text)
                            if (parsed != null) return parsed
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting interaction text", e)
        }
        return null
    }
}
