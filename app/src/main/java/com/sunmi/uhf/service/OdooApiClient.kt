package com.sunmi.uhf.service

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

object OdooApiClient {
    private var instance: OkHttpClient? = null
    private val cookieStore = mutableListOf<Cookie>()
    
    fun getClient(): OkHttpClient {
        if (instance == null) {
            instance = OkHttpClient.Builder()
                .cookieJar(SimpleCookieJar(cookieStore))
                .addInterceptor(OdooHttpInterceptor())
                .retryOnConnectionFailure(true)
                .build()
        }
        return instance!!
    }
    
    fun refreshClient() {
        instance = null
        cookieStore.clear()
    }
}

class SimpleCookieJar(private val cookies: MutableList<Cookie>) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        cookies.removeAll { it.domain == url.host }
        cookies.addAll(newCookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.filter { it.domain == url.host && !it.expiresAt.isPast() }
    }
    
    private fun Long.isPast() = this < System.currentTimeMillis()
}
