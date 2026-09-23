package com.danstudios.reelnotes.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class FetchedReelMetadata(
    val caption: String = "",
    val author: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null
)

object InstagramMetadataFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetch(shortcode: String, cleanUrl: String): FetchedReelMetadata = withContext(Dispatchers.IO) {
        // Try embed endpoint first: https://www.instagram.com/p/{shortcode}/embed/captioned/
        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        try {
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val parsed = parseHtml(html)
                    if (parsed.caption.isNotBlank() || parsed.author != null) {
                        return@withContext parsed
                    }
                }
            }
        } catch (_: Exception) {
            // Embed failed or blocked, continue to fallback
        }

        // Fallback: OpenGraph / Main reel page
        try {
            val req = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    return@withContext parseHtml(html)
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        FetchedReelMetadata()
    }

    fun parseHtml(html: String): FetchedReelMetadata {
        // 1. Author
        val authorRegex = Regex("""(?:class="FeedbackAuthor-author"[^>]*>|@)([A-Za-z0-9_.]+)""")
        val authorMatch = authorRegex.find(html)
        val author = authorMatch?.groupValues?.get(1)?.let { "@$it" }

        // 2. Thumbnail
        val imgRegex = Regex("""<img[^>]+class="EmbeddedMediaImage"[^>]+src="([^">]+)"""")
        val ogImgRegex = Regex("""<meta\s+property="og:image"\s+content="([^">]+)"""")
        val thumb = imgRegex.find(html)?.groupValues?.get(1)
            ?: ogImgRegex.find(html)?.groupValues?.get(1)

        // 3. Caption / Description
        val captionRegex = Regex("""<div\s+class="Caption"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
        var caption = captionRegex.find(html)?.groupValues?.get(1) ?: ""
        if (caption.isBlank()) {
            val ogDescRegex = Regex("""<meta\s+property="og:description"\s+content="([^">]+)"""")
            caption = ogDescRegex.find(html)?.groupValues?.get(1) ?: ""
        }

        // Clean HTML tags inside caption
        val cleanCaption = caption
            .replace(Regex("""<br\s*/?>"""), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()

        // 4. Title
        val titleRegex = Regex("""<title>([^<]+)</title>""")
        val title = titleRegex.find(html)?.groupValues?.get(1)?.trim()

        return FetchedReelMetadata(
            caption = cleanCaption,
            author = author,
            title = title,
            thumbnailUrl = thumb
        )
    }
}
