package com.sunmi.uhf.fragment.delivery
import DeliveryItemAdapter
import DeliveryItemList
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
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

        val getMyListTextView: TextView = view.findViewById(R.id.get_my_list_delivery)
        getMyListTextView.setOnClickListener {
            showPinInputDialog()
        }

        val scanstockPickingTextView: TextView = view.findViewById(R.id.scan_delivery)
        scanstockPickingTextView.setOnClickListener {
            if (deliveryItemOrderList.isNotEmpty()) {
                openTakeInventoryFragment(deliveryItemOrderList)
            } else {
                Toast.makeText(activity, "No stockPicking items available", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }


    private fun openTakeInventoryFragment(deliveryItemOrderList: List<DeliveryItemList>) {
        val takeInventoryFragment = TakeInventoryFragment.newInstanceFromDelivery(deliveryItemOrderList)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutDelivery, takeInventoryFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showPinInputDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Enter PIN")

        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            val pin = input.text.toString()
            validatePin(pin)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun validatePin(pin: String) {
        val queue: RequestQueue = Volley.newRequestQueue(activity)
        val url = "https://infinite-suitable-quetzal.ngrok-free.app/get/stock/picking/delivery"

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener<String> { response ->
                val jsonResponse = JSONObject(response)
                val status = jsonResponse.getString("status")
                if (status == "success") {
                    addDataToList(response)
                } else {
                    val message = jsonResponse.getString("message")
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Toast.makeText(activity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["pin"] = pin
                return params
            }
        }

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