package com.sunmi.uhf.service

import com.sunmi.uhf.utils.AuthUtils
import okhttp3.Interceptor
import okhttp3.Response

class OdooHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val loginInfo = AuthUtils.getLoginInfo()
        val uid = loginInfo["uid"]?.toIntOrNull() ?: 0
        val sessionId = loginInfo["sessionId"] ?: ""
        val database = loginInfo["database"] ?: ""
        val username = loginInfo["username"] ?: ""
        
        val requestBuilder = originalRequest.newBuilder()
        
        if (originalRequest.body != null) {
            requestBuilder.addHeader("Content-Type", "application/json")
        }
        
        if (sessionId.isNotEmpty() && database.isNotEmpty()) {
            requestBuilder
                .addHeader("X-Odoo-UID", uid.toString())
                .addHeader("X-Odoo-Session", sessionId)
                .addHeader("X-Odoo-DB", database)
                .addHeader("X-Odoo-User", username)
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
