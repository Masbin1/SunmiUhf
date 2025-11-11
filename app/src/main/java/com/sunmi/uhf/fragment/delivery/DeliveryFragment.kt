package com.sunmi.uhf.fragment.delivery
import DeliveryItemAdapter
import DeliveryItemList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import org.json.JSONArray
import org.json.JSONObject

class DeliveryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var deliveryItemAdapter: DeliveryItemAdapter
    private val deliveryItemOrderList = mutableListOf<DeliveryItemList>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewDeliver)
        recyclerView.layoutManager = LinearLayoutManager(context)
        deliveryItemAdapter = DeliveryItemAdapter(deliveryItemOrderList) { deliveryItemList ->


        }
        recyclerView.adapter = deliveryItemAdapter

        val scanstockPickingTextView: TextView = view.findViewById(R.id.scan_delivery)
        scanstockPickingTextView.setOnClickListener {
            if (deliveryItemOrderList.isNotEmpty()) {
                openTakeInventoryFragment(deliveryItemOrderList)
            } else {
                Toast.makeText(activity, "No delivery items available", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (deliveryItemOrderList.isEmpty()) {
            fetchDeliveryOrders()
        }
    }

    private fun openTakeInventoryFragment(deliveryItemOrderList: List<DeliveryItemList>) {
        val takeInventoryFragment = TakeInventoryFragment.newInstanceFromDelivery(deliveryItemOrderList)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutDelivery, takeInventoryFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun fetchDeliveryOrders() {
        val queue: RequestQueue = Volley.newRequestQueue(activity)
        val server = BuildConfig.SERVER_URL
        val apiKey = BuildConfig.API_KEY
        val url = "$server/get/stock/picking/delivery"

        val stringRequest = StringRequest(
            Request.Method.POST, url,
            { response ->
                val jsonResponse = JSONObject(response)
                val status = jsonResponse.getString("status")
                if (status == "success") {
                    addDataToList(response)
                } else {
                    val message = jsonResponse.getString("message")
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(activity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        queue.add(stringRequest)
    }

    private fun addDataToList(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val status = jsonObject.getString("status")

            if (status == "success") {
                val deliveryItemOrders: JSONArray = jsonObject.getJSONArray("pickup_orders")
                deliveryItemOrderList.clear()

                for (i in 0 until deliveryItemOrders.length()) {
                    val order = deliveryItemOrders.getJSONObject(i)
                    val deliveryItemListSend = DeliveryItemList(
                        idPickup = order.getInt("id_pickup"),
                        name = order.getString("name"),
                        idLine = order.getInt("id_line"),
                        partnerName = order.getString("partner_name"),
                        state = order.getString("state"),
                        productId = order.getInt("product_id"),
                        productName = order.getString("product_name"),
                        pin = order.getString("pin"),
                        rfid = order.getString("rfid"),
                    )
                    deliveryItemOrderList.add(deliveryItemListSend)
                }
                deliveryItemAdapter.notifyDataSetChanged()
            } else {
                val errorMessage = jsonObject.getString("message")
                Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Error parsing JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    companion object {
        fun newInstance(nothing: Nothing?) = DeliveryFragment()
    }
}