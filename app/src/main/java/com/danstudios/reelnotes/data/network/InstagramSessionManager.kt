package com.danstudios.reelnotes.data.network

import android.webkit.CookieManager

object InstagramSessionManager {

    fun isLoggedIn(): Boolean {
        return try {
            val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com") ?: return false
            cookies.contains("sessionid=")
        } catch (_: Exception) {
            false
        }
    }

    fun logout(callback: (() -> Unit)? = null) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
                callback?.invoke()
            }
        } catch (_: Exception) {
            callback?.invoke()
        }
    }
}
