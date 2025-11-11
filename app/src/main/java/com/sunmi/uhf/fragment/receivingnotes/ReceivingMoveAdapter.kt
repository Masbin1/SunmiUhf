package com.sunmi.uhf.fragment.receivingnotes

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ReceivingMoveAdapter(private val moveList: List<ReceivingMoveItem>) :
    RecyclerView.Adapter<ReceivingMoveAdapter.MoveViewHolder>() {

    class MoveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewProduct: TextView = itemView.findViewById(R.id.textViewProduct)
        val textViewQty: TextView = itemView.findViewById(R.id.textViewQty)
        val textViewUom: TextView = itemView.findViewById(R.id.textViewUom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receiving_move, parent, false)
        return MoveViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        val item = moveList[position]
        holder.textViewProduct.text = item.productName
        holder.textViewQty.text = "Qty: ${item.productQty}"
        holder.textViewUom.text = "UoM: ${item.uom}"

        // Animated fade-in
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }


    override fun getItemCount() = moveList.size
}
