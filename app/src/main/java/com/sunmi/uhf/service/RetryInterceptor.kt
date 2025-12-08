package com.sunmi.uhf.service

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMillis: Long = 1000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                // For application interceptors it's OK to call proceed multiple times.
                val response = chain.proceed(request)
                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) {
                    break
                }
                attempt++
                try {
                    Thread.sleep(retryDelayMillis)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        throw lastException ?: IOException("Unknown network error during retry")
    }
}

