package com.sunmi.uhf.fragment.deliveryorder

data class DeliveryMoveItem(
    val productName: String,
    val productUomQty: Double,
    val quantityDone: Double,
    val uomName: String,
    val lotName: String
)

