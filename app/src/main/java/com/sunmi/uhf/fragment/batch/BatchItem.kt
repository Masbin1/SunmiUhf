import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatchItem(
    val idBatch: Int,
    val name: String,
    val responsible: String,
) : Parcelable