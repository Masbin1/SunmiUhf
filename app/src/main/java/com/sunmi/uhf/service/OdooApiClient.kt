package com.sunmi.uhf.service

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OdooApiClient {
    private var instance: OkHttpClient? = null
    private val cookieStore = mutableListOf<Cookie>()
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    
    fun getClient(): OkHttpClient {
        if (instance == null) {
            instance = OkHttpClient.Builder()
                .cookieJar(SimpleCookieJar(cookieStore))
                .addInterceptor(OdooHttpInterceptor())
                .addInterceptor(RetryInterceptor()) // uses the standalone RetryInterceptor.kt
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
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
        synchronized(cookies) {
            cookies.removeAll { it.domain == url.host }
            cookies.addAll(newCookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return synchronized(cookies) {
            cookies.filter { it.domain == url.host && !it.expiresAt.isPast() }
        }
    }
    
    private fun Long.isPast() = this < System.currentTimeMillis()
}
