package com.sunmi.uhf.fragment.asset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class AssetAdapter(
    initialList: MutableList<AssetItem>,
    private val onItemClick: (AssetItem) -> Unit
) : RecyclerView.Adapter<AssetAdapter.ViewHolder>() {

    // Keep master and display lists for filtering
    private val fullList: MutableList<AssetItem> = mutableListOf()
    private val displayList: MutableList<AssetItem> = mutableListOf()

    init {
        fullList.addAll(initialList)
        displayList.addAll(initialList)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardAsset)
        val name: TextView = view.findViewById(R.id.txtAssetName)
        val partner: TextView = view.findViewById(R.id.txtPartner)
        val date: TextView = view.findViewById(R.id.txtScheduledDate)
        val state: TextView = view.findViewById(R.id.txtState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        holder.name.text = item.name
        holder.partner.text = item.partnerName
        holder.date.text = item.dueDate
        holder.state.text = item.state
        holder.card.setOnClickListener { onItemClick(item) }
    }

    // Replace the whole dataset (master + display)
    fun updateData(newList: List<AssetItem>) {
        fullList.clear()
        fullList.addAll(newList)

        displayList.clear()
        displayList.addAll(newList)
        notifyDataSetChanged()
    }

    // Append page
    fun appendData(newList: List<AssetItem>) {
        if (newList.isEmpty()) return
        val startFull = fullList.size
        fullList.addAll(newList)
        if (displayList.size == startFull) {
            val start = displayList.size
            displayList.addAll(newList)
            notifyItemRangeInserted(start, newList.size)
        } else {
            // If currently filtered, keep displayList as filtered subset and notify so caller may re-filter
            notifyDataSetChanged()
        }
    }

    /**
     * Filter by name (case-insensitive contains) and also search partner/state optionally.
     * If query blank, restore fullList.
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
