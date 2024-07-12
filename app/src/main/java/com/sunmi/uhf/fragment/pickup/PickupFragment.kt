package com.sunmi.uhf.fragment.pickup

import PickupAdapter
import PickupItem
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.android.volley.toolbox.Volley
import com.sunmi.uhf.R
import org.json.JSONArray
import org.json.JSONObject

class PickupFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pickupAdapter: PickupAdapter
    private val pickupList = mutableListOf<PickupItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pickup, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewPickup)
        recyclerView.layoutManager = LinearLayoutManager(context)
        pickupAdapter = PickupAdapter(pickupList) { pickupItem ->
            openTakeInventoryFragment(pickupItem)
        }
        recyclerView.adapter = pickupAdapter

        val textView: TextView = view.findViewById(R.id.get_mylist_pickup)
        textView.setOnClickListener {
            fetchDataFromApi()
        }

        return view
    }

    private fun openTakeInventoryFragment(pickupItem: PickupItem) {
        val takeInventoryFragment = TakeInventoryFragment.newInstance(pickupItem)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutPickup, takeInventoryFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun fetchDataFromApi() {
        val queue: RequestQueue = Volley.newRequestQueue(activity)
        val url = "https://loyal-martin-present.ngrok-free.app/rfid/get/order/pickup"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                addDataToList(response)
            },
            Response.ErrorListener {
                Toast.makeText(activity, "That didn't work!", Toast.LENGTH_LONG).show()
            })

        queue.add(stringRequest)
    }

    private fun addDataToList(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val status = jsonObject.getString("status")

            if (status == "success") {
                val pickupOrders: JSONArray = jsonObject.getJSONArray("pickup_orders")
                pickupList.clear()

                for (i in 0 until pickupOrders.length()) {
                    val order = pickupOrders.getJSONObject(i)
                    val pickupItem = PickupItem(
                        idPickup = order.getInt("id_pickup"),
                        name = order.getString("name"),
                        idLine = order.getInt("id_line"),
                        partnerId = order.getInt("partner_id"),
                        partnerName = order.getString("partner_name"),
                        state = order.getString("state"),
                        productId = order.getInt("product_id"),
                        productName = order.getString("product_name")
                    )
                    pickupList.add(pickupItem)
                }
                pickupAdapter.notifyDataSetChanged()
            } else {
                val errorMessage = jsonObject.getString("message")
                Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Error parsing JSON", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun newInstance(nothing: Nothing?) = PickupFragment()
    }
}