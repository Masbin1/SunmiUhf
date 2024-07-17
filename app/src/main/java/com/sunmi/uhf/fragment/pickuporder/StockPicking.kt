package com.sunmi.uhf.fragment.pickuporder
import StockPickingAdapter
import StockPickingItem
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

class StockPicking : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var stockPickingAdapter: StockPickingAdapter
    private val stockPickingList = mutableListOf<StockPickingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stock_picking, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewstockPicking)
        recyclerView.layoutManager = LinearLayoutManager(context)
        stockPickingAdapter = StockPickingAdapter(stockPickingList) { stockPickingItem ->
            // Handle individual item click if needed
            // For example, you could show details of the clicked item
        }
        recyclerView.adapter = stockPickingAdapter

        val getMyListTextView: TextView = view.findViewById(R.id.get_my_list_stockPicking)
        getMyListTextView.setOnClickListener {
            showPinInputDialog()
        }

        val scanstockPickingTextView: TextView = view.findViewById(R.id.scan_stockPicking)
        scanstockPickingTextView.setOnClickListener {
            if (stockPickingList.isNotEmpty()) {
                openTakeInventoryFragment(stockPickingList)
            } else {
                Toast.makeText(activity, "No stockPicking items available", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }


    private fun openTakeInventoryFragment(stockPickingList: List<StockPickingItem>) {
        val takeInventoryFragment = TakeInventoryFragment.newInstance(stockPickingList)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutstockPicking, takeInventoryFragment)
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
        val url = "https://loyal-martin-present.ngrok-free.app/get/stock/picking"

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
                val stockPickingOrders: JSONArray = jsonObject.getJSONArray("pickup_orders")
                stockPickingList.clear()

                for (i in 0 until stockPickingOrders.length()) {
                    val order = stockPickingOrders.getJSONObject(i)
                    val stockPickingItem = StockPickingItem(
                        idPickup = order.getInt("id_pickup"),
                        name = order.getString("name"),
                        idLine = order.getInt("id_line"),
                        partnerName = order.getString("partner_name"),
                        state = order.getString("state"),
                        productId = order.getInt("product_id"),
                        productName = order.getString("product_name"),
                        pin = order.getString("pin")
                    )
                    stockPickingList.add(stockPickingItem)
                }
                stockPickingAdapter.notifyDataSetChanged()
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
        fun newInstance(nothing: Nothing?) = StockPicking()
    }
}