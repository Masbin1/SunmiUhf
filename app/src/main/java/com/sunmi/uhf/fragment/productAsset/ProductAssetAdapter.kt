package com.sunmi.uhf.fragment.productAsset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ProductAssetAdapter(
    private var assetList: MutableList<ProductAssetItem>,
    private val onItemClick: (ProductAssetItem) -> Unit
) : RecyclerView.Adapter<ProductAssetAdapter.ViewHolder>() {

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

    override fun getItemCount(): Int = assetList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = assetList[position]
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
        assetList.clear()
        assetList.addAll(newList)
        notifyDataSetChanged()
    }

    fun appendData(newList: List<ProductAssetItem>) {
        if (newList.isEmpty()) return
        val start = assetList.size
        assetList.addAll(newList)
        notifyItemRangeInserted(start, newList.size)
    }
}
