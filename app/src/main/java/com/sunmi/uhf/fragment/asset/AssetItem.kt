package com.sunmi.uhf.fragment.asset

import android.os.Parcel
import android.os.Parcelable

data class AssetItem(
    val id: Int,
    val name: String,
    val dueDate: String,
    val partnerName: String,
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
        parcel.writeString(dueDate)
        parcel.writeString(partnerName)
        parcel.writeString(state)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AssetItem> {
        override fun createFromParcel(parcel: Parcel) = AssetItem(parcel)
        override fun newArray(size: Int): Array<AssetItem?> = arrayOfNulls(size)
    }
}
