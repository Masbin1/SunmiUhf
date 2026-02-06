package com.sunmi.uhf.fragment.productAsset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ProductAssetAdapter(
    initialList: MutableList<ProductAssetItem>,
    private val onItemClick: (ProductAssetItem) -> Unit
) : RecyclerView.Adapter<ProductAssetAdapter.ViewHolder>() {

    // Keep a master list (all loaded items) and a displayed list which can be filtered
    private val fullList: MutableList<ProductAssetItem> = mutableListOf()
    private val displayList: MutableList<ProductAssetItem> = mutableListOf()

    init {
        fullList.addAll(initialList)
        displayList.addAll(initialList)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardProductAsset)
        val name: TextView = view.findViewById(R.id.txtAssetName)
        val product: TextView = view.findViewById(R.id.txtProductName)
        val code: TextView = view.findViewById(R.id.txtAssetCode)
        val category: TextView = view.findViewById(R.id.txtAssetCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_asset, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        // Bind ProductAssetItem fields to item_product_asset views
        holder.name.text = item.name
        holder.product.text = item.productName
        holder.code.text = item.assetCode
        holder.category.text = item.assetCategory

        holder.card.setOnClickListener { onItemClick(item) }

        // Optional subtle animation
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 20f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .start()
    }

    fun updateData(newList: List<ProductAssetItem>) {
        fullList.clear()
        fullList.addAll(newList)

        displayList.clear()
        displayList.addAll(newList)
        notifyDataSetChanged()
    }

    fun appendData(newList: List<ProductAssetItem>) {
        if (newList.isEmpty()) return
        // add to master list
        val startFull = fullList.size
        fullList.addAll(newList)
        // if not currently filtered (displayList mirrors fullList) append to display
        if (displayList.size == startFull) {
            val start = displayList.size
            displayList.addAll(newList)
            notifyItemRangeInserted(start, newList.size)
        } else {
            // If user has an active filter, keep displayList as filtered subset; do not automatically add
            // but keep fullList updated so future filtering includes new items
            // notify that data changed so caller can re-filter if desired
            notifyDataSetChanged()
        }
    }

    /**
     * Filter the currently loaded items by name (case-insensitive contains).
     * If query is blank, restores the full list.
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
        // Search across multiple fields so users can search by asset name, product name, code or category
        val filtered = fullList.filter { item ->
            item.name.lowercase().contains(lower)
                    || item.productName.lowercase().contains(lower)
                    || item.assetCode.lowercase().contains(lower)
                    || item.assetCategory.lowercase().contains(lower)
        }
        displayList.clear()
        displayList.addAll(filtered)
        notifyDataSetChanged()
    }
}
