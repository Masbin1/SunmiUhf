package com.sunmi.uhf.fragment.deliveryorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class DeliveryMoveAdapter(private var items: List<DeliveryMoveItem>) :
    RecyclerView.Adapter<DeliveryMoveAdapter.ViewHolder>() {

    fun updateData(newItems: List<DeliveryMoveItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtProduct: TextView = view.findViewById(R.id.txtProduct)
        val txtLot: TextView = view.findViewById(R.id.txtLot)
        val txtQty: TextView = view.findViewById(R.id.txtQty)
        val txtUom: TextView = view.findViewById(R.id.txtUom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_move, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtProduct.text = item.productName
        holder.txtLot.text = "Lot: ${item.lotName}"
        holder.txtQty.text = "Qty: ${item.quantityDone}/${item.productUomQty}"
        holder.txtUom.text = "UoM: ${item.uomName}"
    }
}
