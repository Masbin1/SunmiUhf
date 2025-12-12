package com.sunmi.uhf.fragment.productAsset

import java.io.Serializable

data class ProductAssetMoveItem(
    val moveId: Int,
    val productName: String,
    val productUomQty: Double,
    val quantityDone: Double,
    val uomName: String,
    val lotName: String,
    val rfid: String,
    var pendingRfid: String? = null
) : Serializable
