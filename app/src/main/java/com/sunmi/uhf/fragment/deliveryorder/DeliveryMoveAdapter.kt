package com.sunmi.uhf.fragment.deliveryorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class DeliveryMoveAdapter(
    private var moveList: List<DeliveryMoveItem>
) : RecyclerView.Adapter<DeliveryMoveAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val product: TextView = view.findViewById(R.id.txtProductName)
        val demandQty: TextView = view.findViewById(R.id.txtDemandQty)
        val doneQty: TextView = view.findViewById(R.id.txtDoneQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_move, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = moveList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = moveList[position]
        holder.product.text = item.productName
        holder.demandQty.text = "Demand: ${item.demandQty}"
        holder.doneQty.text = "Done: ${item.doneQty}"
    }

    fun updateData(newList: List<DeliveryMoveItem>) {
        moveList = newList
        notifyDataSetChanged()
    }
}
