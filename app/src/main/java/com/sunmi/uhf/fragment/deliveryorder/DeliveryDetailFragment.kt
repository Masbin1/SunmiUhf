package com.sunmi.uhf.fragment.deliveryorder

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DeliveryDetailFragment : Fragment() {

    private var deliveryId: Int = 0
    private lateinit var adapter: DeliveryMoveAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtDeliveryName: TextView
    private lateinit var txtPartner: TextView
    private lateinit var txtScheduledDate: TextView
    private lateinit var txtOrigin: TextView
    private lateinit var txtState: TextView

    companion object {
        fun newInstance(id: Int): DeliveryDetailFragment {
            val fragment = DeliveryDetailFragment()
            val args = Bundle()
            args.putInt("delivery_id", id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliveryId = arguments?.getInt("delivery_id") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_detail, container, false)

        txtDeliveryName = view.findViewById(R.id.txtDeliveryName)
        txtPartner = view.findViewById(R.id.txtPartner)
        txtScheduledDate = view.findViewById(R.id.txtScheduledDate)
        txtOrigin = view.findViewById(R.id.txtOrigin)
        txtState = view.findViewById(R.id.txtState)
        recyclerView = view.findViewById(R.id.recyclerViewDeliveryMove)
        progressBar = view.findViewById(R.id.progressBarDeliveryDetail)

        adapter = DeliveryMoveAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadDeliveryDetail()
        return view
    }

    private fun loadDeliveryDetail() {
        progressBar.visibility = View.VISIBLE
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_URL}/get/stock/picking/delivery/detail/$deliveryId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return
                val jsonObj = JSONObject(jsonData)
                val pickingObj = jsonObj.getJSONObject("picking")

                val moveLines = pickingObj.getJSONArray("move_lines")
                val list = mutableListOf<DeliveryMoveItem>()

                for (i in 0 until moveLines.length()) {
                    val line = moveLines.getJSONObject(i)
                    list.add(
                        DeliveryMoveItem(
                            productName = line.getString("product_name"),
                            productUomQty = line.getDouble("product_uom_qty"),
                            quantityDone = line.getDouble("quantity_done"),
                            uomName = line.getString("uom_name"),
                            lotName = line.getString("lot_name")
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    txtDeliveryName.text = "Delivery Number: ${pickingObj.getString("name")}"
                    txtPartner.text = "Customer: ${pickingObj.getString("partner_name")}"
                    txtScheduledDate.text = "Scheduled Date: ${pickingObj.getString("scheduled_date")}"
                    txtOrigin.text = "Origin: ${pickingObj.optString("origin", "-")}"
                    txtState.text = "State: ${pickingObj.getString("state")}"
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                }
            }
        })
    }
}
