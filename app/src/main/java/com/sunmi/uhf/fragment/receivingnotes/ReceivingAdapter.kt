package com.sunmi.uhf.fragment.receivingnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class ReceivingAdapter(
    private val receivingList: List<ReceivingItem>,
    private val onItemClick: (ReceivingItem) -> Unit
) : RecyclerView.Adapter<ReceivingAdapter.ReceivingViewHolder>() {

    class ReceivingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewPartner: TextView = itemView.findViewById(R.id.textViewPartner)
        val textViewScheduledDate: TextView = itemView.findViewById(R.id.textViewScheduledDate)
        val textViewState: TextView = itemView.findViewById(R.id.textViewState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceivingViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receiving, parent, false)
        return ReceivingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReceivingViewHolder, position: Int) {
        val currentItem = receivingList[position]
        holder.textViewName.text = "Name: ${currentItem.name}"
        holder.textViewPartner.text = "Partner: ${currentItem.partnerName}"
        holder.textViewScheduledDate.text = "Scheduled: ${currentItem.scheduledDate}"
        holder.textViewState.text = "State: ${currentItem.state}"

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        // Animasi muncul lembut
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }


    override fun getItemCount() = receivingList.size
}
