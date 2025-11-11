package com.sunmi.uhf.fragment.deliveryorder

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
                progressBar.visibility = View.GONE
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return
                val jsonObj = JSONObject(jsonData)

                val moveLines = jsonObj.getJSONArray("move_lines")
                val list = mutableListOf<DeliveryMoveItem>()

                for (i in 0 until moveLines.length()) {
                    val line = moveLines.getJSONObject(i)
                    list.add(
                        DeliveryMoveItem(
                            line.getString("product_name"),
                            line.getDouble("demand_qty"),
                            line.getDouble("done_qty")
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    txtDeliveryName.text = jsonObj.getString("name")
                    txtPartner.text = jsonObj.getString("partner_name")
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                }
            }
        })
    }
}
