package com.sunmi.uhf.fragment.deliveryorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DeliveryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DeliveryAdapter
    private lateinit var contentLayout: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_order, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewDelivery)
        progressBar = view.findViewById(R.id.progressBarDelivery)
        contentLayout = view.findViewById(R.id.contentLayoutDelivery)


        adapter = DeliveryAdapter(emptyList()) { item ->
            val fragment = DeliveryDetailFragment.newInstance(item.id)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadDeliveryOrders()
        return view
    }

    private fun loadDeliveryOrders() {
        progressBar.visibility = View.VISIBLE
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.SERVER_URL}/get/stock/picking/delivery")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return

                try {
                    val jsonObject = JSONObject(jsonData)
                    if (jsonObject.getString("status") == "success") {
                        val jsonArray = jsonObject.getJSONArray("pickings")
                        val deliveryList = mutableListOf<DeliveryItem>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            deliveryList.add(
                                DeliveryItem(
                                    id = obj.getInt("id"),
                                    name = obj.getString("name"),
                                    scheduledDate = obj.getString("scheduled_date"),
                                    partnerName = obj.optString("partner_name", "-"),
                                    state = obj.getString("state")
                                )
                            )
                        }

                        requireActivity().runOnUiThread {
                            progressBar.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                            adapter.updateData(deliveryList)
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Failed: ${jsonObject.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error parsing JSON: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    companion object {
        fun newInstance(nothing: Nothing?) = DeliveryFragment()
    }
}
