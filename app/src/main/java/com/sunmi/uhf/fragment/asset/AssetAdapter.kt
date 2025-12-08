package com.sunmi.uhf.fragment.asset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class AssetAdapter(
    private var assetList: MutableList<AssetItem>,
    private val onItemClick: (AssetItem) -> Unit
) : RecyclerView.Adapter<AssetAdapter.ViewHolder>() {

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

    override fun getItemCount(): Int = assetList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = assetList[position]
        holder.name.text = item.name
        holder.partner.text = item.partnerName
        holder.date.text = item.dueDate
        holder.state.text = item.state
        holder.card.setOnClickListener { onItemClick(item) }
    }

    // Replace the whole dataset
    fun updateData(newList: List<AssetItem>) {
        assetList.clear()
        assetList.addAll(newList)
        notifyDataSetChanged()
    }

    // Append page
    fun appendData(newList: List<AssetItem>) {
        if (newList.isEmpty()) return
        val start = assetList.size
        assetList.addAll(newList)
        notifyItemRangeInserted(start, newList.size)
    }
}
