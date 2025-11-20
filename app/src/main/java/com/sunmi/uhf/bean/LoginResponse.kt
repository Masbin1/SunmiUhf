package com.sunmi.uhf.bean

import java.io.Serializable

data class LoginResponse(
    val uid: Int,
    val sessionId: String,
    val database: String,
    val username: String,
    val url: String
) : Serializable
