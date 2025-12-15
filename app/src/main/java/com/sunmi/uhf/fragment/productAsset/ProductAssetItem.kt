package com.sunmi.uhf.fragment.productAsset

import android.os.Parcel
import android.os.Parcelable

// Updated to match API response: id, name, productName, assetCode, assetCategory
data class ProductAssetItem(
    val id: Int,
    val name: String,
    val productName: String,
    val assetCode: String,
    val assetCategory: String,
    val serialNo: String = "",
    val rfid: String = "",
    val heldBy: String = "",
    var pendingRfid: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(productName)
        parcel.writeString(assetCode)
        parcel.writeString(assetCategory)
        parcel.writeString(serialNo)
        parcel.writeString(rfid)
        parcel.writeString(heldBy)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ProductAssetItem> {
        override fun createFromParcel(parcel: Parcel) = ProductAssetItem(parcel)
        override fun newArray(size: Int): Array<ProductAssetItem?> = arrayOfNulls(size)
    }
}
