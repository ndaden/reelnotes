package com.danstudios.reelnotes.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class FetchedReelMetadata(
    val caption: String = "",
    val author: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val mediaBytes: ByteArray? = null,
    val mediaMimeType: String? = null,
    val isAgeRestricted: Boolean = false,
    val isLoginRequired: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FetchedReelMetadata
        if (caption != other.caption) return false
        if (author != other.author) return false
        if (title != other.title) return false
        if (thumbnailUrl != other.thumbnailUrl) return false
        if (isAgeRestricted != other.isAgeRestricted) return false
        if (isLoginRequired != other.isLoginRequired) return false
        return true
    }

    override fun hashCode(): Int {
        var result = caption.hashCode()
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + isAgeRestricted.hashCode()
        result = 31 * result + isLoginRequired.hashCode()
        return result
    }
}

object InstagramMetadataFetcher {
    private const val TAG = "InstagramMetaFetcher"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetch(
        shortcode: String,
        cleanUrl: String,
        context: Context? = null
    ): FetchedReelMetadata = withContext(Dispatchers.IO) {
        // 1. If Context is available, try WebView extraction first (renders JS & intercepts media)
        if (context != null) {
            try {
                val webResult = InstagramWebViewExtractor.extract(context, shortcode, cleanUrl)
                var downloadedBytes: ByteArray? = null
                var mimeType: String? = null

                // 1. Try audio stream first (compact, high-fidelity speech recognition)
                if (webResult.audioUrl != null) {
                    Log.d(TAG, "Attempting audio stream download: ${webResult.audioUrl}")
                    downloadedBytes = downloadMedia(webResult.audioUrl)
                    if (downloadedBytes != null && downloadedBytes.isNotEmpty()) {
                        mimeType = "audio/mp4"
                        Log.d(TAG, "Audio stream download succeeded (${downloadedBytes.size} bytes)")
                    }
                }

                // 2. If audio was not available or failed, try video stream
                if (downloadedBytes == null && webResult.videoUrl != null) {
                    Log.d(TAG, "Attempting video stream download: ${webResult.videoUrl}")
                    downloadedBytes = downloadMedia(webResult.videoUrl)
                    if (downloadedBytes != null && downloadedBytes.isNotEmpty()) {
                        mimeType = "video/mp4"
                        Log.d(TAG, "Video stream download succeeded (${downloadedBytes.size} bytes)")
                    }
                }

                if (webResult.caption.isNotBlank() || downloadedBytes != null || webResult.isAgeRestricted || webResult.isLoginRequired) {
                    return@withContext FetchedReelMetadata(
                        caption = webResult.caption,
                        author = webResult.author,
                        title = webResult.title,
                        thumbnailUrl = webResult.thumbnailUrl,
                        mediaBytes = downloadedBytes,
                        mediaMimeType = mimeType,
                        isAgeRestricted = webResult.isAgeRestricted,
                        isLoginRequired = webResult.isLoginRequired
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebView extractor encountered error, falling back to HTTP", e)
            }
        }

        // 2. Fallback: HTTP embed endpoint
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
            // Ignore embed failure
        }

        // 3. Fallback: Main reel page via HTTP
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

    suspend fun downloadMedia(url: String, maxBytes: Long = 12 * 1024 * 1024): ByteArray? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true) || !trimmed.startsWith("http")) {
            return@withContext null
        }
        try {
            val req = Request.Builder()
                .url(trimmed)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Referer", "https://www.instagram.com/")
                .build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val stream = response.body?.byteStream() ?: return@withContext null
                val buffer = ByteArrayOutputStream()
                val temp = ByteArray(8192)
                var totalRead = 0L
                var read: Int
                while (stream.read(temp).also { read = it } != -1) {
                    buffer.write(temp, 0, read)
                    totalRead += read
                    if (totalRead >= maxBytes) break
                }
                return@withContext buffer.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download media stream", e)
            null
        }
    }

    fun parseHtml(html: String): FetchedReelMetadata {
        val isAgeRestricted = html.contains("restrictions d’âge") || html.contains("restrictions d'âge") || html.contains("age-restricted")
        val isLoginRequired = html.contains("Connectez-vous pour continuer") && !html.contains("video_url")

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
            thumbnailUrl = thumb,
            isAgeRestricted = isAgeRestricted,
            isLoginRequired = isLoginRequired
        )
    }
}
