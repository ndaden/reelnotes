package com.danstudios.reelnotes.data.network

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
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseAiJson(rawJson: String): GeminiAiOutput? {
        val clean = rawJson
            .trim()
            .replace(Regex("""^```(?:json)?\s*"""), "")
            .replace(Regex("""\s*```$"""), "")
            .trim()
        return try {
            jsonParser.decodeFromString<GeminiAiOutput>(clean)
        } catch (_: Exception) {
            null
        }
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
            Return ONLY the raw JSON object, without any commentary or conversational text.
        """.trimIndent()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val escapedPrompt = Json.encodeToString(prompt)
        val requestJson = """
            {
              "contents": [{
                "parts": [{"text": $escapedPrompt}]
              }]
            }
        """.trimIndent()

        try {
            val req = Request.Builder()
                .url(endpoint)
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null

                val textRegex = Regex(""""text":\s*"((?:[^"\\]|\\.)*)"""")
                val match = textRegex.find(body)
                val rawAiText = match?.groupValues?.get(1)
                    ?.replace("\\n", "\n")
                    ?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\")
                    ?: return@withContext null

                return@withContext parseAiJson(rawAiText)
            }
        } catch (_: Exception) {
            null
        }
    }
}
