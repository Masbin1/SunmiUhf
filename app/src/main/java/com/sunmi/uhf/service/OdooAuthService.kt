package com.sunmi.uhf.service

import com.sunmi.uhf.bean.LoginRequest
import com.sunmi.uhf.bean.LoginResponse
import com.sunmi.uhf.bean.OdooDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

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
            val config = XmlRpcClientConfigImpl()
            config.serverURL = java.net.URL("$cleanUrl/xmlrpc/2/common")
            val client = XmlRpcClient()
            client.setConfig(config)
            
            val params = arrayOf(
                request.database,
                request.username,
                request.password
            )
            
            val result = client.execute("authenticate", params)
            val uid = when (result) {
                is Int -> result
                is Double -> result.toInt()
                else -> throw Exception("Authentication failed: Invalid response type")
            }
            
            if (uid <= 0) {
                throw Exception("Authentication failed: Invalid credentials")
            }

            LoginResponse(
                uid = uid,
                sessionId = generateSessionId(),
                database = request.database,
                username = request.username,
                url = cleanUrl
            )
        } catch (e: Exception) {
            throw Exception("Login error: ${e.message}")
        }
    }

    private fun generateSessionId(): String {
        return System.currentTimeMillis().toString() + Math.random().toString().substring(2)
    }
}
