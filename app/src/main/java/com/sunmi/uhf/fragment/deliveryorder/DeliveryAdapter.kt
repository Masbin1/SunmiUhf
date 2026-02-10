package com.sunmi.uhf.fragment.deliveryorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class DeliveryAdapter(
    initialList: MutableList<DeliveryItem>,
    private val onItemClick: (DeliveryItem) -> Unit
) : RecyclerView.Adapter<DeliveryAdapter.ViewHolder>() {

    private val fullList: MutableList<DeliveryItem> = mutableListOf()
    private val displayList: MutableList<DeliveryItem> = mutableListOf()

    init {
        fullList.addAll(initialList)
        displayList.addAll(initialList)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardDelivery)
        val name: TextView = view.findViewById(R.id.txtDeliveryName)
        val partner: TextView = view.findViewById(R.id.txtPartner)
        val date: TextView = view.findViewById(R.id.txtScheduledDate)
        val state: TextView = view.findViewById(R.id.txtState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        holder.name.text = item.name
        holder.partner.text = item.partnerName
        holder.date.text = item.scheduledDate
        holder.state.text = item.state
        holder.card.setOnClickListener { onItemClick(item) }
    }

    // Replace the whole dataset
    fun updateData(newList: List<DeliveryItem>) {
        fullList.clear()
        fullList.addAll(newList)

        displayList.clear()
        displayList.addAll(newList)
        notifyDataSetChanged()
    }

    // Append a page of data
    fun appendData(newList: List<DeliveryItem>) {
        if (newList.isEmpty()) return
        val startFull = fullList.size
        fullList.addAll(newList)
        if (displayList.size == startFull) {
            val start = displayList.size
            displayList.addAll(newList)
            notifyItemRangeInserted(start, newList.size)
        } else {
            notifyDataSetChanged()
        }
    }

    /**
     * Filter loaded items by name/partner/state. Case-insensitive contains.
     */
    fun filter(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            displayList.clear()
            displayList.addAll(fullList)
            notifyDataSetChanged()
            return
        }

        val lower = q.lowercase()
        val filtered = fullList.filter { item ->
            item.name.lowercase().contains(lower)
                    || item.partnerName.lowercase().contains(lower)
                    || item.state.lowercase().contains(lower)
        }
        displayList.clear()
        displayList.addAll(filtered)
        notifyDataSetChanged()
    }
}
