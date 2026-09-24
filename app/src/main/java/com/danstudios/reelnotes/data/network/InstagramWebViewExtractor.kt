package com.danstudios.reelnotes.data.network

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import com.danstudios.reelnotes.domain.util.CaptionSanitizer

data class WebViewExtractionResult(
    val caption: String = "",
    val author: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val isAgeRestricted: Boolean = false,
    val isLoginRequired: Boolean = false
)

object InstagramWebViewExtractor {
    private const val TAG = "InstaWebViewExtractor"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    suspend fun extract(
        context: Context,
        shortcode: String,
        cleanUrl: String,
        timeoutMs: Long = 10000L
    ): WebViewExtractionResult = withContext(Dispatchers.Main) {
        val resultDeferred = CompletableDeferred<WebViewExtractionResult>()
        var webView: WebView? = null
        val activity = context.findActivity()
        val decorView = activity?.window?.decorView as? ViewGroup

        var detectedVideoUrl: String? = null
        var detectedAudioUrl: String? = null

        try {
            Log.d(TAG, "Starting extraction for shortcode: $shortcode, url: $cleanUrl")
            webView = WebView(context).apply {
                layout(0, 0, 1080, 1920)
                alpha = 0.01f // Attached to window for full media engine activation, invisible to user
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    userAgentString = USER_AGENT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                onResume()
                resumeTimers()

                val cookieMgr = CookieManager.getInstance()
                cookieMgr.setAcceptCookie(true)
                cookieMgr.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null
                        val isMedia = url.contains(".mp4") ||
                                      url.contains("dash_") ||
                                      url.contains("bytestart=") ||
                                      url.contains("/v/t50.") ||
                                      url.contains("/o1/v/t")

                        if (isMedia) {
                            val isAudio = url.contains("audio") ||
                                          url.contains("dash_ln_heaac") ||
                                          url.contains("dash_a")
                            val cleaned = cleanMediaUrl(url)
                            if (isAudio) {
                                if (detectedAudioUrl == null) {
                                    Log.d(TAG, "Audio stream intercepted: $cleaned")
                                    detectedAudioUrl = cleaned
                                }
                            } else {
                                if (detectedVideoUrl == null) {
                                    Log.d(TAG, "Video stream intercepted: $cleaned")
                                    detectedVideoUrl = cleaned
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "onPageFinished for $url")

                        val checkExtraction: () -> Unit = {
                            if (!resultDeferred.isCompleted) {
                                evaluateExtractionJs(view, shortcode) { res ->
                                    val effectiveVideo = detectedVideoUrl ?: res.videoUrl
                                    val effectiveAudio = detectedAudioUrl ?: res.audioUrl
                                    val finalRes = res.copy(
                                        videoUrl = effectiveVideo,
                                        audioUrl = effectiveAudio
                                    )
                                    Log.d(TAG, "Interim extraction: author=${finalRes.author}, capLen=${finalRes.caption.length}, hasAudio=${finalRes.audioUrl != null}, hasVideo=${finalRes.videoUrl != null}")
                                    if ((finalRes.caption.isNotBlank() && (finalRes.videoUrl != null || finalRes.audioUrl != null)) ||
                                        finalRes.isAgeRestricted || finalRes.isLoginRequired
                                    ) {
                                        if (!resultDeferred.isCompleted) {
                                            resultDeferred.complete(finalRes)
                                        }
                                    }
                                }
                            }
                        }

                        // Check at multiple intervals as React hydrates and video buffers
                        Handler(Looper.getMainLooper()).postDelayed({ checkExtraction() }, 1500L)
                        Handler(Looper.getMainLooper()).postDelayed({ checkExtraction() }, 3500L)
                        Handler(Looper.getMainLooper()).postDelayed({ checkExtraction() }, 6000L)
                    }
                }
            }

            // Attach to decorView so Chromium treats document as active and visible
            try {
                decorView?.addView(webView, ViewGroup.LayoutParams(1080, 1920))
            } catch (e: Exception) {
                Log.w(TAG, "Could not attach WebView to DecorView: ${e.message}")
            }

            webView.loadUrl(cleanUrl)

            val result = withTimeoutOrNull(timeoutMs) {
                resultDeferred.await()
            }

            if (result != null) {
                Log.d(TAG, "Extraction succeeded within timeout: author=${result.author}, capLen=${result.caption.length}, hasAudio=${result.audioUrl != null}, hasVideo=${result.videoUrl != null}")
                return@withContext result
            }

            // Fallback one last check at timeout
            Log.d(TAG, "Timeout reached, running final fallback extraction")
            val fallbackDeferred = CompletableDeferred<WebViewExtractionResult>()
            evaluateExtractionJs(webView, shortcode) { res ->
                val finalRes = res.copy(
                    videoUrl = detectedVideoUrl ?: res.videoUrl,
                    audioUrl = detectedAudioUrl ?: res.audioUrl
                )
                fallbackDeferred.complete(finalRes)
            }
            withTimeoutOrNull(1500L) { fallbackDeferred.await() } ?: WebViewExtractionResult(
                videoUrl = detectedVideoUrl,
                audioUrl = detectedAudioUrl
            )

        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed with exception", e)
            WebViewExtractionResult()
        } finally {
            try {
                decorView?.removeView(webView)
                webView?.stopLoading()
                webView?.destroy()
            } catch (_: Exception) {}
        }
    }

    private fun cleanMediaUrl(url: String): String {
        return url.replace(Regex("""&bytestart=\d+&byteend=\d+"""), "")
    }

    private fun evaluateExtractionJs(
        webView: WebView?,
        shortcode: String,
        onResult: (WebViewExtractionResult) -> Unit
    ) {
        if (webView == null) {
            onResult(WebViewExtractionResult())
            return
        }

        val js = """
            (function() {
                try {
                    var targetShortcode = "$shortcode";

                    // 0. Force document visibility & simulate active playback environment
                    try {
                        Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                        Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                        window.dispatchEvent(new Event('visibilitychange'));
                    } catch(_) {}

                    // Unmute and play any video elements
                    try {
                        var vids = document.querySelectorAll('video');
                        for (var v = 0; v < vids.length; v++) {
                            vids[v].muted = true;
                            var p = vids[v].play();
                            if (p && p.catch) p.catch(function(){});
                        }
                        var playBtns = document.querySelectorAll('[aria-label="Play"], [aria-label="Lire"], svg[aria-label="Play"], svg[aria-label="Lire"]');
                        for (var b = 0; b < playBtns.length; b++) {
                            var clickable = playBtns[b].closest('button, [role="button"]') || playBtns[b];
                            if (clickable && clickable.click) clickable.click();
                        }
                    } catch(_) {}

                    var bodyText = document.body ? document.body.innerText : '';
                    
                    var isAgeRestricted = bodyText.indexOf('restrictions d’âge') !== -1 ||
                                          bodyText.indexOf("restrictions d'âge") !== -1 ||
                                          bodyText.indexOf('age-restricted') !== -1 ||
                                          bodyText.indexOf('Restricted Content') !== -1;
                    
                    var isLoginRequired = (bodyText.indexOf('Connectez-vous pour continuer') !== -1 ||
                                           bodyText.indexOf('Log in to continue') !== -1) && 
                                          !document.querySelector('video');

                    // Find container specifically associated with target shortcode
                    var targetContainer = null;
                    if (targetShortcode) {
                        var links = document.querySelectorAll('a[href*="' + targetShortcode + '"]');
                        for (var l = 0; l < links.length; l++) {
                            var container = links[l].closest('article, section, div[role="dialog"]');
                            if (container) {
                                targetContainer = container;
                                break;
                            }
                        }
                    }
                    if (!targetContainer) {
                        targetContainer = document.querySelector('article') || document.querySelector('section');
                    }

                    var author = '';
                    var caption = '';
                    var jsAudioUrl = null;
                    var jsVideoUrl = null;

                    // 1. Script tag extraction strictly scoped to targetShortcode
                    if (targetShortcode) {
                        try {
                            var scripts = document.querySelectorAll('script');
                            for (var s = 0; s < scripts.length; s++) {
                                var scText = scripts[s].textContent || '';
                                var scIdx = scText.indexOf(targetShortcode);
                                if (scIdx !== -1) {
                                    var startIdx = Math.max(0, scIdx - 2000);
                                    var endIdx = Math.min(scText.length, scIdx + 8000);
                                    var snippet = scText.substring(startIdx, endIdx);

                                    // Video stream in target snippet
                                    var vm = snippet.match(/"video_url"\s*:\s*"([^"]+)"/) ||
                                             snippet.match(/"playable_url"\s*:\s*"([^"]+)"/) ||
                                             snippet.match(/"playable_url_quality_hd"\s*:\s*"([^"]+)"/);
                                    if (vm && vm[1]) {
                                        var vcand = vm[1].replace(/\\u0026/g, '&').replace(/\\\//g, '/');
                                        if (vcand.indexOf('http') === 0 && !jsVideoUrl) {
                                            jsVideoUrl = vcand;
                                        }
                                    }

                                    // Author in target snippet
                                    if (!author) {
                                        var om = snippet.match(/"owner"\s*:\s*\{[^}]*"username"\s*:\s*"([^"]+)"/) ||
                                                 snippet.match(/"user"\s*:\s*\{[^}]*"username"\s*:\s*"([^"]+)"/);
                                        if (om && om[1]) {
                                            author = '@' + om[1];
                                        }
                                    }

                                    // Caption in target snippet
                                    if (!caption) {
                                        var cm = snippet.match(/"edge_media_to_caption"\s*:\s*\{\s*"edges"\s*:\s*\[\s*\{\s*"node"\s*:\s*\{\s*"text"\s*:\s*"([^"]+)"/);
                                        if (!cm) {
                                            cm = snippet.match(/"caption"\s*:\s*\{\s*[^}]*"text"\s*:\s*"([^"]+)"/);
                                        }
                                        if (cm && cm[1]) {
                                            try {
                                                caption = JSON.parse('"' + cm[1] + '"');
                                            } catch(_) {
                                                caption = cm[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
                                            }
                                        }
                                    }
                                }
                            }
                        } catch(_) {}
                    }

                    // 2. Author from scoped container or DOM
                    if (!author && targetContainer) {
                        var authorLinks = Array.from(targetContainer.querySelectorAll('header a, h2 a, a[role="link"]'));
                        for (var i = 0; i < authorLinks.length; i++) {
                            var a = authorLinks[i];
                            var h = a.getAttribute('href') || '';
                            var t = a.innerText ? a.innerText.trim() : '';
                            if (h.startsWith('/') && !h.startsWith('/p/') && !h.startsWith('/reel/') && !h.startsWith('/explore/') && !h.startsWith('/accounts/') && !h.startsWith('/popular/') && !h.startsWith('/direct/') && t && t.indexOf(' ') === -1 && t.length > 2 && t !== 'media') {
                                author = '@' + t.replace('@', '');
                                break;
                            }
                        }
                    }
                    if (!author) {
                        var authorMatch = bodyText.match(/(?:^|\n)\s*([A-Za-z0-9_.]+)\s*(?:•|\n)\s*(?:Suivre|Follow)/i) ||
                                          bodyText.match(/Ne manquez aucune publication de\s+([A-Za-z0-9_.]+)/i) ||
                                          bodyText.match(/See more posts from\s+([A-Za-z0-9_.]+)/i);
                        if (authorMatch && authorMatch[1]) {
                            author = '@' + authorMatch[1].replace('@', '');
                        }
                    }

                    // 3. Caption strictly scoped to the FIRST reel in the feed
                    if (!caption) {
                        var followMatch = bodyText.match(/(?:^|\n)\s*(?:Suivre|Follow|Following|Abonné\(e\)|S’abonner)\s*\n/i);
                        if (followMatch && followMatch.index !== undefined) {
                            var textAfterFollow = bodyText.substring(followMatch.index + followMatch[0].length);
                            var moreIdx = textAfterFollow.search(/(?:\n|^)\s*(?:…\s*more|…\s*plus|\.\.\.\s*more|\.\.\.\s*plus)\b/i);
                            var countsIdx = textAfterFollow.search(/\n\s*[0-9.,]+[KkMmb]?\s*\n\s*[0-9.,]+[KkMmb]?\s*\n/);
                            var nextAuthorIdx = textAfterFollow.search(/(?:\n|^)\s*[A-Za-z0-9_.]+\s*(?:\n|\s*•\s*)(?:Suivre|Follow)/i);
                            var likeBtnIdx = textAfterFollow.search(/\n\s*(?:J’aime|Like|Comments?|Commentaires?)\b/i);

                            var cutList = [moreIdx, countsIdx, nextAuthorIdx, likeBtnIdx].filter(function(idx) { return idx !== -1; });
                            var cutAt = cutList.length > 0 ? Math.min.apply(null, cutList) : 500;
                            var cand = textAfterFollow.substring(0, cutAt).trim();
                            if (cand.length > 0) {
                                caption = cand;
                            }
                        }
                    }

                    // 4. Media stream extraction from window.performance (CHRONOLOGICAL FORWARD SEARCH)
                    // The earliest media entries belong to the target reel loaded first.
                    try {
                        var entries = window.performance.getEntriesByType('resource');
                        for (var k = 0; k < entries.length; k++) {
                            var u = entries[k].name;
                            var isMedia = u.indexOf('.mp4') !== -1 ||
                                          u.indexOf('dash_') !== -1 ||
                                          u.indexOf('bytestart=') !== -1 ||
                                          u.indexOf('/v/t50.') !== -1 ||
                                          u.indexOf('/o1/v/t') !== -1;
                            if (isMedia) {
                                var isAudio = u.indexOf('audio') !== -1 ||
                                              u.indexOf('dash_ln_heaac') !== -1 ||
                                              u.indexOf('dash_a') !== -1;
                                if (isAudio) {
                                    if (!jsAudioUrl) jsAudioUrl = u;
                                } else {
                                    if (!jsVideoUrl) jsVideoUrl = u;
                                }
                                if (jsAudioUrl && jsVideoUrl) break;
                            }
                        }
                    } catch(_) {}

                    // 5. Video element direct src fallback
                    if (!jsVideoUrl) {
                        var vEl = targetContainer ? targetContainer.querySelector('video') : document.querySelector('video');
                        if (vEl) {
                            var vSrc = vEl.currentSrc || vEl.src;
                            if (!vSrc) {
                                var sEl = vEl.querySelector('source');
                                if (sEl) vSrc = sEl.src;
                            }
                            if (vSrc && vSrc.indexOf('blob:') === -1) {
                                jsVideoUrl = vSrc;
                            }
                        }
                    }

                    // 6. Thumbnail
                    var videoElForThumb = targetContainer ? targetContainer.querySelector('video') : document.querySelector('video');
                    var ogImg = document.querySelector('meta[property="og:image"]');
                    var ogImgSrc = ogImg ? ogImg.getAttribute('content') : '';
                    var posterSrc = videoElForThumb ? videoElForThumb.getAttribute('poster') : '';
                    var thumb = posterSrc || ogImgSrc || '';

                    return JSON.stringify({
                        isAgeRestricted: isAgeRestricted,
                        isLoginRequired: isLoginRequired,
                        author: author,
                        caption: caption,
                        bodyText: bodyText,
                        thumbnailUrl: thumb,
                        title: document.title || '',
                        audioUrl: jsAudioUrl ? jsAudioUrl.replace(/&bytestart=\d+&byteend=\d+/, '') : null,
                        videoUrl: jsVideoUrl ? jsVideoUrl.replace(/&bytestart=\d+&byteend=\d+/, '') : null
                    });
                } catch(e) {
                    return JSON.stringify({ error: e.toString() });
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { raw ->
            if (raw == null || raw == "null") {
                onResult(WebViewExtractionResult())
                return@evaluateJavascript
            }
            try {
                val unquoted = if (raw.startsWith("\"") && raw.endsWith("\"")) {
                    org.json.JSONTokener(raw).nextValue().toString()
                } else {
                    raw
                }
                val obj = JSONObject(unquoted)

                fun optNullableString(key: String): String? {
                    if (obj.isNull(key)) return null
                    val s = obj.optString(key, "").trim()
                    return if (s.isBlank() || s.equals("null", ignoreCase = true) || s.equals("undefined", ignoreCase = true)) null else s
                }

                val author = optNullableString("author")
                val rawCaption = obj.optString("caption", "")
                val bodyText = obj.optString("bodyText", "")

                val cleanCaption = when {
                    rawCaption.isNotBlank() -> CaptionSanitizer.sanitize(rawCaption, author)
                    bodyText.isNotBlank() -> CaptionSanitizer.extractFirstReelCaptionFromFeedText(bodyText)
                    else -> ""
                }

                val result = WebViewExtractionResult(
                    caption = cleanCaption,
                    author = author,
                    title = optNullableString("title"),
                    thumbnailUrl = optNullableString("thumbnailUrl"),
                    videoUrl = optNullableString("videoUrl"),
                    audioUrl = optNullableString("audioUrl"),
                    isAgeRestricted = obj.optBoolean("isAgeRestricted", false),
                    isLoginRequired = obj.optBoolean("isLoginRequired", false)
                )
                onResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing extraction JS result: $raw", e)
                onResult(WebViewExtractionResult())
            }
        }
    }
}
