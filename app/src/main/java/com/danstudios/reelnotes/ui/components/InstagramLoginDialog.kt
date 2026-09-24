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
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }

                                val cookieMgr = CookieManager.getInstance()
                                cookieMgr.setAcceptCookie(true)
                                cookieMgr.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false

                                        // Check if logged in
                                        if (InstagramSessionManager.isLoggedIn()) {
                                            onLoginSuccess()
                                        }
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
