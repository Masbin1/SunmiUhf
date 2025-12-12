package com.sunmi.uhf.fragment.productAsset

import android.os.Parcel
import android.os.Parcelable

data class ProductAssetItem(
    val id: Int,
    val name: String,
    val partnerName: String,
    val scheduledDate: String,
    val state: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(partnerName)
        parcel.writeString(scheduledDate)
        parcel.writeString(state)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ProductAssetItem> {
        override fun createFromParcel(parcel: Parcel) = ProductAssetItem(parcel)
        override fun newArray(size: Int): Array<ProductAssetItem?> = arrayOfNulls(size)
    }
}
