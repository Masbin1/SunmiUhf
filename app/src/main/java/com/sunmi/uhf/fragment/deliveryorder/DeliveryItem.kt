package com.sunmi.uhf.fragment.deliveryorder

import android.os.Parcel
import android.os.Parcelable

data class DeliveryItem(
    val id: Int,
    val name: String,
    val scheduledDate: String,
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
        parcel.writeString(scheduledDate)
        parcel.writeString(partnerName)
        parcel.writeString(state)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DeliveryItem> {
        override fun createFromParcel(parcel: Parcel) = DeliveryItem(parcel)
        override fun newArray(size: Int): Array<DeliveryItem?> = arrayOfNulls(size)
    }
}
