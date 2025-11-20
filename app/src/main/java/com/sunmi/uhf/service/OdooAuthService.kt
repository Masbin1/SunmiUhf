package com.sunmi.uhf.service

import com.sunmi.uhf.bean.LoginRequest
import com.sunmi.uhf.bean.LoginResponse
import com.sunmi.uhf.bean.OdooDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import org.json.JSONObject

class OdooAuthService {

    suspend fun getDatabases(url: String): List<OdooDatabase> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanUrl = url.trim().removeSuffix("/")
            val config = XmlRpcClientConfigImpl()
            config.serverURL = java.net.URL("$cleanUrl/xmlrpc/2/db")
            val client = XmlRpcClient()
            client.setConfig(config)
            
            val result = client.execute("list", arrayOf()) as Array<*>
            result.mapNotNull { dbName ->
                if (dbName is String) {
                    OdooDatabase(dbName, dbName)
                } else null
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch databases: ${e.message}")
        }
    }

    suspend fun login(request: LoginRequest): LoginResponse = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanUrl = request.url.trim().removeSuffix("/")
            
            val httpClient = OkHttpClient()
            val loginUrl = "$cleanUrl/web/session/authenticate"
            
            val params = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "call")
                put("params", JSONObject().apply {
                    put("db", request.database)
                    put("login", request.username)
                    put("password", request.password)
                })
                put("id", 1)
            }
            
            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                params.toString()
            )
            
            val httpRequest = Request.Builder()
                .url(loginUrl)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            
            val jsonResponse = JSONObject(responseBody)
            val result = jsonResponse.optJSONObject("result")
                ?: throw Exception("Login failed: ${jsonResponse.optString("error", "Unknown error")}")
            
            val uid = result.optInt("uid", -1)
            if (uid <= 0) {
                throw Exception("Authentication failed: Invalid credentials")
            }
            
            val sessionIdFromResponse = result.optString("session_id", null)
            val sessionId = sessionIdFromResponse ?: extractSessionIdFromCookie(response)
                ?: throw Exception("No session ID received from server")
            
            LoginResponse(
                uid = uid,
                sessionId = sessionId,
                database = request.database,
                username = request.username,
                url = cleanUrl
            )
        } catch (e: Exception) {
            throw Exception("Login error: ${e.message}")
        }
    }

    private fun extractSessionIdFromCookie(response: okhttp3.Response): String? {
        val setCookie = response.header("Set-Cookie") ?: return null
        val sessionMatch = Regex("session_id=([^;]+)").find(setCookie)
        return sessionMatch?.groupValues?.get(1)
    }
}
