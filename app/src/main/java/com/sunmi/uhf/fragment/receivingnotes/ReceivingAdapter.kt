package com.sunmi.uhf.fragment.receivingnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ReceivingAdapter(
    initialList: MutableList<ReceivingItem>,
    private val onItemClick: (ReceivingItem) -> Unit
) : RecyclerView.Adapter<ReceivingAdapter.ViewHolder>() {

    private val fullList: MutableList<ReceivingItem> = mutableListOf()
    private val displayList: MutableList<ReceivingItem> = mutableListOf()

    init {
        fullList.addAll(initialList)
        displayList.addAll(initialList)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardReceiving)
        val name: TextView = view.findViewById(R.id.txtReceivingName)
        val partner: TextView = view.findViewById(R.id.txtPartner)
        val date: TextView = view.findViewById(R.id.txtScheduledDate)
        val state: TextView = view.findViewById(R.id.txtState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receiving, parent, false)
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

        // Tambahkan animasi lembut untuk setiap item (opsional)
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .start()
    }

    // Replace the whole dataset
    fun updateData(newList: List<ReceivingItem>) {
        fullList.clear()
        fullList.addAll(newList)

        displayList.clear()
        displayList.addAll(newList)
        notifyDataSetChanged()
    }

    // Append page
    fun appendData(newList: List<ReceivingItem>) {
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
