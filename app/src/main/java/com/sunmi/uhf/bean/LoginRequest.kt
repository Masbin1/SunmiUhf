package com.sunmi.uhf.bean

import java.io.Serializable

data class LoginRequest(
    val url: String,
    val username: String,
    val password: String,
    val database: String
) : Serializable
