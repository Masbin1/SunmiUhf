package com.sunmi.uhf.fragment.deliveryorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.fragment.receivingnotes.ReceivingFragment
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class DeliveryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DeliveryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewDelivery)
        progressBar = view.findViewById(R.id.progressBarDelivery)

        adapter = DeliveryAdapter(emptyList()) { item ->
            val fragment = DeliveryDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayoutDelivery, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadDeliveryOrders()
        return view
    }

    private fun loadDeliveryOrders() {
        progressBar.visibility = View.VISIBLE
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_URL}/get/stock/picking/delivery")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return
                val jsonArray = JSONArray(jsonData)
                val deliveryList = mutableListOf<DeliveryItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    deliveryList.add(
                        DeliveryItem(
                            obj.getInt("id"),
                            obj.getString("name"),
                            obj.getString("scheduled_date"),
                            obj.getString("partner_name"),
                            obj.getString("state")
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    adapter.updateData(deliveryList)
                }
            }
        })
    }


    companion object {
        fun newInstance(nothing: Nothing?) = DeliveryFragment()
    }
}
