package com.sunmi.uhf.fragment.receivingnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ReceivingAdapter(
    private var receivingList: List<ReceivingItem>,
    private val onItemClick: (ReceivingItem) -> Unit
) : RecyclerView.Adapter<ReceivingAdapter.ViewHolder>() {

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

    override fun getItemCount(): Int = receivingList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = receivingList[position]
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

    fun updateData(newList: List<ReceivingItem>) {
        receivingList = newList
        notifyDataSetChanged()
    }
}
