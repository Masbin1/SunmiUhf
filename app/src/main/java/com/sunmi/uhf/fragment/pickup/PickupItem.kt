import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PickupItem(
    val idPickup: Int,
    val name: String,
    val idLine: Int,
    val partnerId: Int,
    val partnerName: String,
    val state: String,
    val productId: Int,
    val productName: String
) : Parcelable