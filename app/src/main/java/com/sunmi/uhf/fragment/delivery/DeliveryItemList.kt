import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.os.Parcel

data class DeliveryItemList(
    val idPickup: Int,
    val name: String,
    val idLine: Int,
    val partnerName: String,
    val state: String,
    val productId: Int,
    val productName: String,
    val pin:String,
    val rfid:String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()?:"",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(idPickup)
        parcel.writeString(name)
        parcel.writeInt(idLine)
        parcel.writeString(partnerName)
        parcel.writeString(state)
        parcel.writeInt(productId)
        parcel.writeString(productName)
        parcel.writeString(pin)
        parcel.writeString(rfid)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeliveryItemList> {
        override fun createFromParcel(parcel: Parcel): DeliveryItemList {
            return DeliveryItemList(parcel)
        }

        override fun newArray(size: Int): Array<DeliveryItemList?> {
            return arrayOfNulls(size)
        }
    }
}