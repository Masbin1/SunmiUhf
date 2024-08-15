import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class DeliveryItemAdapter(private val deliveryItemOrderList: List<DeliveryItemList>, private val onItemClick: (DeliveryItemList) -> Unit) :
    RecyclerView.Adapter<DeliveryItemAdapter.DeliveryItemViewHolder>() {

    class DeliveryItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewIdPickup: TextView = itemView.findViewById(R.id.textViewIdPickup)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewIdLine: TextView = itemView.findViewById(R.id.textViewIdLine)
        val textViewPartner: TextView = itemView.findViewById(R.id.textViewPartner)
        val textViewState: TextView = itemView.findViewById(R.id.textViewState)
        val textViewProduct: TextView = itemView.findViewById(R.id.textViewProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_picking, parent, false)
        return DeliveryItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeliveryItemViewHolder, position: Int) {
        val currentItem = deliveryItemOrderList[position]
        holder.textViewIdPickup.text = "ID Pickup: ${currentItem.idPickup}"
        holder.textViewName.text = "Name: ${currentItem.name}"
        holder.textViewIdLine.text = "ID Line: ${currentItem.idLine}"
        holder.textViewPartner.text = "Partner: ${currentItem.partnerName}"
        holder.textViewState.text = "State: ${currentItem.state}"
        holder.textViewProduct.text = "Product: ${currentItem.productName} (ID: ${currentItem.productId})"

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount() = deliveryItemOrderList.size
}