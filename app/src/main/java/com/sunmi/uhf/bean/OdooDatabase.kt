package com.sunmi.uhf.bean

import java.io.Serializable

data class OdooDatabase(
    val name: String,
    val displayName: String
) : Serializable
