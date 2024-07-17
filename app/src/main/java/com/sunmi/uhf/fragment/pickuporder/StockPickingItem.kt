import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockPickingItem(
    val idPickup: Int,
    val name: String,
    val idLine: Int,
    val partnerName: String,
    val state: String,
    val productId: Int,
    val productName: String,
    val pin: String
) : Parcelable