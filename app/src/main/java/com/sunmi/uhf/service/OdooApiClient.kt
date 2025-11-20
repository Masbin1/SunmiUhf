package com.sunmi.uhf.service

import okhttp3.OkHttpClient

object OdooApiClient {
    private var instance: OkHttpClient? = null
    
    fun getClient(): OkHttpClient {
        if (instance == null) {
            instance = OkHttpClient.Builder()
                .addInterceptor(OdooHttpInterceptor())
                .retryOnConnectionFailure(true)
                .build()
        }
        return instance!!
    }
    
    fun refreshClient() {
        instance = null
    }
}
