package com.sunmi.uhf.fragment.receivingnotes

import java.io.Serializable

data class ReceivingMoveItem(
    val moveId: Int,
    val productName: String,
    val productUomQty: Double,
    val quantityDone: Double,
    val uomName: String,
    val lotName: String,
    val rfid: String
) : Serializable
