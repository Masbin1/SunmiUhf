package com.sunmi.uhf.fragment.asset

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class AssetMoveAdapter(private var items: List<AssetMoveItem>) :
    RecyclerView.Adapter<AssetMoveAdapter.ViewHolder>() {

    fun updateData(newItems: List<AssetMoveItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtProduct: TextView = view.findViewById(R.id.txtProduct)
        val txtLot: TextView = view.findViewById(R.id.txtLot)
        val txtCategory: TextView = view.findViewById(R.id.txtCategory)
        val txtEmployee: TextView = view.findViewById(R.id.txtEmployee)
        val txtHeldBy: TextView = view.findViewById(R.id.txtHeldBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset_move, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtProduct.text = item.asset
        holder.txtCategory.text = item.category
        holder.txtEmployee.text = item.employee
        holder.txtHeldBy.text = item.heldBy
        holder.txtLot.text = item.rfid

//        holder.txtQty.text = "Qty: ${item.quantityDone}/${item.productUomQty}"
    }
}
