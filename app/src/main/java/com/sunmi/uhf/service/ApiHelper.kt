package com.sunmi.uhf.service

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

object ApiHelper {
    private val cache = LruCache<String, CachedResponse>(20)
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L

    data class CachedResponse(
        val data: String,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS
        }
    }

    suspend fun getJsonArray(
        url: String,
        useCache: Boolean = true,
        arrayKey: String = ""
    ): org.json.JSONArray = withContext(Dispatchers.IO) {
        val cachedData = if (useCache) {
            val cached = cache[url]
            if (cached != null && !cached.isExpired()) cached else null
        } else null
        
        val jsonString = cachedData?.data ?: fetchUrlInternal(url).also {
            if (useCache) cache.put(url, CachedResponse(it, System.currentTimeMillis()))
        }
        
        val jsonObject = JSONObject(jsonString)
        if (arrayKey.isEmpty()) {
            jsonObject.getJSONArray("data")
        } else {
            jsonObject.getJSONArray(arrayKey)
        }
    }

    suspend fun getJsonObject(
        url: String,
        useCache: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        val cachedData = if (useCache) {
            val cached = cache[url]
            if (cached != null && !cached.isExpired()) cached else null
        } else null
        
        val jsonString = cachedData?.data ?: fetchUrlInternal(url).also {
            if (useCache) cache.put(url, CachedResponse(it, System.currentTimeMillis()))
        }
        
        JSONObject(jsonString)
    }

    suspend fun postJson(
        url: String,
        body: String,
        clearRelatedCache: String = ""
    ): JSONObject = withContext(Dispatchers.IO) {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        
        val response = OdooApiClient.getClient().newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        
        if (clearRelatedCache.isNotEmpty()) {
            clearCache(clearRelatedCache)
        }
        
        JSONObject(responseBody)
    }

    private suspend fun fetchUrlInternal(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = OdooApiClient.getClient().newCall(request).execute()
        response.body?.string() ?: throw Exception("Empty response body from $url")
    }

    fun clearCache(pattern: String = "") {
        if (pattern.isEmpty()) {
            cache.evictAll()
        } else {
            cache.remove(pattern)
        }
    }
}
