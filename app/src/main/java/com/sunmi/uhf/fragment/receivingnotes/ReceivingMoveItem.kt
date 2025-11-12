package com.sunmi.uhf.fragment.receivingnotes

data class ReceivingMoveItem(
    val productName: String,
    val productUomQty: Double,
    val quantityDone: Double,
    val uomName: String,
    val lotName: String,
    val rfid: String
)
