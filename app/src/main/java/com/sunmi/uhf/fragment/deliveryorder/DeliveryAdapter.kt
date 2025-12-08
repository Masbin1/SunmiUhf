package com.sunmi.uhf.fragment.deliveryorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.R

class DeliveryAdapter(
    private var deliveryList: MutableList<DeliveryItem>,
    private val onItemClick: (DeliveryItem) -> Unit
) : RecyclerView.Adapter<DeliveryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardDelivery)
        val name: TextView = view.findViewById(R.id.txtDeliveryName)
        val partner: TextView = view.findViewById(R.id.txtPartner)
        val date: TextView = view.findViewById(R.id.txtScheduledDate)
        val state: TextView = view.findViewById(R.id.txtState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = deliveryList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = deliveryList[position]
        holder.name.text = item.name
        holder.partner.text = item.partnerName
        holder.date.text = item.scheduledDate
        holder.state.text = item.state
        holder.card.setOnClickListener { onItemClick(item) }
    }

    // Replace the whole dataset
    fun updateData(newList: List<DeliveryItem>) {
        deliveryList.clear()
        deliveryList.addAll(newList)
        notifyDataSetChanged()
    }

    // Append a page of data
    fun appendData(newList: List<DeliveryItem>) {
        if (newList.isEmpty()) return
        val start = deliveryList.size
        deliveryList.addAll(newList)
        notifyItemRangeInserted(start, newList.size)
    }
}
