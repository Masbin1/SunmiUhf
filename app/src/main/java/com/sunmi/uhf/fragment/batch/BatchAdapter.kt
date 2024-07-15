import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class BatchAdapter(private val batchList: List<BatchItem>, private val onItemClick: (BatchItem) -> Unit) :
    RecyclerView.Adapter<BatchAdapter.batchViewHolder>() {

    class batchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewIdbatch: TextView = itemView.findViewById(R.id.textViewIdBatch)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewResponsible: TextView = itemView.findViewById(R.id.textViewResponsible)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): batchViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_batch, parent, false)
        return batchViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: batchViewHolder, position: Int) {
        val currentItem = batchList[position]
        holder.textViewIdbatch.text = "ID batch: ${currentItem.idBatch}"
        holder.textViewName.text = "Name: ${currentItem.name}"
        holder.textViewResponsible.text = "Responsible: ${currentItem.responsible}"

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount() = batchList.size
}