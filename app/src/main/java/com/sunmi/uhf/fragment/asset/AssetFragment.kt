package com.sunmi.uhf.fragment.asset

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
import com.sunmi.uhf.fragment.operation.LabelOperationFragment
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.OdooApiClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AssetFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AssetAdapter
    private lateinit var contentLayout: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asset_order, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewAsset)
        progressBar = view.findViewById(R.id.progressBarAsset)
        contentLayout = view.findViewById(R.id.contentLayoutAsset)


        adapter = AssetAdapter(emptyList()) { item ->
            val fragment = AssetDetailFragment.newInstance(item)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadAssetOrders()
        return view
    }

    private fun loadAssetOrders() {
        progressBar.visibility = View.VISIBLE
        val client = OdooApiClient.getClient()

        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/asset")
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
                        val jsonArray = jsonObject.getJSONArray("assets")
                        val AssetList = mutableListOf<AssetItem>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            AssetList.add(
                                AssetItem(
                                    id = obj.getInt("id"),
                                    name = obj.getString("name"),
                                    dueDate = obj.getString("due_date"),
                                    partnerName = obj.optString("partner_name", "-"),
                                    state = obj.getString("state")
                                )
                            )
                        }

                        requireActivity().runOnUiThread {
                            progressBar.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                            adapter.updateData(AssetList)
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
        fun newInstance(args: Bundle?) = AssetFragment()
            .apply { arguments = args }
    }
}
