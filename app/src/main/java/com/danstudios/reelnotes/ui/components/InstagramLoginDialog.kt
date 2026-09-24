package com.danstudios.reelnotes.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danstudios.reelnotes.data.network.InstagramSessionManager

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    // Periodically poll for session cookie so we detect login immediately
    // even if Instagram uses AJAX or redirects without triggering full page reload.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(800)
            if (InstagramSessionManager.isLoggedIn()) {
                android.util.Log.d("InstaLoginDialog", "Session cookie detected via polling! Login success.")
                CookieManager.getInstance().flush()
                onLoginSuccess()
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connexion Instagram",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Connectez-vous pour débloquer les Reels soumis à restriction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    // Use Desktop Chrome User-Agent to render standard desktop web login
                                    // avoiding mobile app deep-linking redirects and broken mobile Bloks modals
                                    userAgentString =
                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }

                                val cookieMgr = CookieManager.getInstance()
                                cookieMgr.setAcceptCookie(true)
                                cookieMgr.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                        android.util.Log.d("InstaLoginDialog", "CONSOLE: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                                        return super.onConsoleMessage(consoleMessage)
                                    }
                                }

                                val autoHelperJs = """
                                    (function() {
                                        try {
                                            // 1. Auto-accept cookie banner if present so login form is immediately accessible
                                            const buttons = Array.from(document.querySelectorAll('button'));
                                            const cookieBtn = buttons.find(b => {
                                                const t = (b.innerText || '').toLowerCase();
                                                return t.includes('allow all') || 
                                                       t.includes('autoriser tous') || 
                                                       t.includes('allow essential') ||
                                                       t.includes('accepter tout');
                                            });
                                            if (cookieBtn) {
                                                cookieBtn.click();
                                            }

                                            // 2. If 'tablet app' prompt appears with desktop UA on small screen, click 'Log in'
                                            const bodyText = (document.body ? document.body.innerText : '').toLowerCase();
                                            if (bodyText.includes('tablet app') || bodyText.includes('full experience')) {
                                                const loginBtn = buttons.find(b => {
                                                    const t = (b.innerText || '').toLowerCase().trim();
                                                    return t === 'log in' || t === 'se connecter';
                                                });
                                                if (loginBtn) {
                                                    loginBtn.click();
                                                }
                                            }
                                        } catch (e) {}
                                    })();
                                """.trimIndent()

                                val handler = android.os.Handler(android.os.Looper.getMainLooper())

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        android.util.Log.d("InstaLoginDialog", "onPageStarted: $url")
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        android.util.Log.d("InstaLoginDialog", "onPageFinished: $url")
                                        isLoading = false

                                        // Inject helper JS immediately and after delays for dynamic React hydration
                                        view?.evaluateJavascript(autoHelperJs, null)
                                        handler.postDelayed({ view?.evaluateJavascript(autoHelperJs, null) }, 600)
                                        handler.postDelayed({ view?.evaluateJavascript(autoHelperJs, null) }, 1500)

                                        if (InstagramSessionManager.isLoggedIn()) {
                                            android.util.Log.d("InstaLoginDialog", "User logged in! Triggering onLoginSuccess")
                                            CookieManager.getInstance().flush()
                                            onLoginSuccess()
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        // Block intent:// or instagram:// deep links from throwing ActivityNotFoundException
                                        if (reqUrl.startsWith("intent:") || reqUrl.startsWith("instagram:")) {
                                            android.util.Log.d("InstaLoginDialog", "Blocked deep-link scheme: $reqUrl")
                                            return true
                                        }
                                        return false
                                    }
                                }

                                loadUrl("https://www.instagram.com/accounts/login/")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}
