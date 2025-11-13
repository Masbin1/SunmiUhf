package com.sunmi.uhf.fragment.deliveryorder

data class DeliveryItem(
    val id: Int,
    val name: String,
    val scheduledDate: String,
    val partnerName: String,
    val state: String
)
