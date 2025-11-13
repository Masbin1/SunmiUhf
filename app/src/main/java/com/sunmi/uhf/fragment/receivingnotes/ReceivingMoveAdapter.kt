package com.sunmi.uhf.fragment.receivingnotes

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ReceivingMoveAdapter(
    private var items: List<ReceivingMoveItem>
) : RecyclerView.Adapter<ReceivingMoveAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<ReceivingMoveItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtProduct: TextView = view.findViewById(R.id.txtProduct)
        val txtLot: TextView = view.findViewById(R.id.txtLot)
        val txtQty: TextView = view.findViewById(R.id.txtQty)
        val txtUom: TextView = view.findViewById(R.id.txtUom)
        val btnScanRfid: Button = view.findViewById(R.id.btnScanRfid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receiving_move, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtProduct.text = item.productName
        holder.txtLot.text = "Lot: ${item.lotName}"
        holder.txtQty.text = "Qty: ${item.quantityDone}/${item.productUomQty}"
        holder.txtUom.text = "UoM: ${item.uomName}"

        holder.btnScanRfid.setOnClickListener {
            onScanClick(item)
        }
    }
}

private fun ReceivingMoveAdapter.onScanClick(item: ReceivingMoveItem) {}
