package com.sunmi.uhf.utils

import android.content.Intent
import com.sunmi.uhf.App
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.LoginActivity

object AuthUtils {

    fun logout() {
        val pref = App.getPref()
        pref.clearPreference("login_uid")
        pref.clearPreference("login_session_id")
        pref.clearPreference("login_database")
        pref.clearPreference("login_username")
        pref.clearPreference("login_url")
        pref.clearPreference("is_logged_in")
    }

    fun isLoggedIn(): Boolean {
        return App.getPref().getParam("is_logged_in", false)
    }

    fun getLoginInfo(): Map<String, String> {
        val pref = App.getPref()
        return mapOf(
            "uid" to pref.getParam("login_uid", 0).toString(),
            "sessionId" to (pref.getParam("login_session_id", "") ?: ""),
            "database" to (pref.getParam("login_database", "") ?: ""),
            "username" to (pref.getParam("login_username", "") ?: ""),
            "url" to (pref.getParam("login_url", "") ?: "")
        )
    }

    fun getApiBaseUrl(): String {
        val pref = App.getPref()
        val savedUrl = pref.getParam("login_url", "")
        return if (savedUrl.isNotEmpty()) {
            "$savedUrl/api"
        } else {
            "${BuildConfig.SERVER_URL}/api"
        }
    }

    fun getServerUrl(): String {
        val pref = App.getPref()
        val savedUrl = pref.getParam("login_url", "")
        return if (savedUrl.isNotEmpty()) {
            savedUrl
        } else {
            BuildConfig.SERVER_URL
        }
    }

    fun goToLoginActivity() {
        logout()
        val intent = Intent(App.mContext, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        App.mContext.startActivity(intent)
    }
}
