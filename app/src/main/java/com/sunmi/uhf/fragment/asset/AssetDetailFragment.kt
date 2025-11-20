package com.sunmi.uhf.fragment.asset

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
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.OdooApiClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AssetDetailFragment : Fragment() {

    private var assetId: Int = 0
    private var assetItem: AssetItem? = null
    private lateinit var adapter: AssetMoveAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarMoveLines: ProgressBar
    private lateinit var txtAssetName: TextView
    private lateinit var txtPartner: TextView
    private lateinit var txtScheduledDate: TextView
    private lateinit var txtOrigin: TextView
    private lateinit var txtState: TextView
    private var shouldRefreshOnResume = false

    companion object {
        fun newInstance(id: Int): AssetDetailFragment {
            val fragment = AssetDetailFragment()
            val args = Bundle()
            args.putInt("asset_id", id)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(item: AssetItem): AssetDetailFragment {
            val fragment = AssetDetailFragment()
            val args = Bundle()
            args.putInt("asset_id", item.id)
            args.putParcelable("asset_item", item)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assetId = arguments?.getInt("asset_id") ?: 0
        assetItem = arguments?.getParcelable("asset_item")
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asset_detail, container, false)

        // Init Views
        txtAssetName = view.findViewById(R.id.txtAssetName)
        txtPartner = view.findViewById(R.id.txtPartner)
        txtScheduledDate = view.findViewById(R.id.txtScheduledDate)
        txtOrigin = view.findViewById(R.id.txtOrigin)
        txtState = view.findViewById(R.id.txtState)
        recyclerView = view.findViewById(R.id.recyclerViewAssetMove)
        progressBar = view.findViewById(R.id.progressBarAssetDetail)

        progressBarMoveLines = try {
            view.findViewById(R.id.progressBarMoveLines)
        } catch (e: Exception) {
            ProgressBar(requireContext())
        }

        // Init Floating Buttons

        // RecyclerView
        adapter = AssetMoveAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter


        if (assetItem != null) {
            displayHeaderImmediately()
            loadMoveLines()
        } else {
            loadAssetDetail()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            shouldRefreshOnResume = false
            loadMoveLines()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayHeaderImmediately() {
        assetItem?.let {
            txtAssetName.text = "Employee Asset Transfer: ${it.name}"
//            txtPartner.text = "Customer: ${it.partnerName}"
            txtScheduledDate.text = "Scheduled Date: ${it.dueDate}"
            txtOrigin.text = "Origin: -"
            txtState.text = "State: ${it.state}"
        }
    }

    private fun loadMoveLines() {
        progressBar.visibility = View.VISIBLE
        progressBarMoveLines.visibility = View.VISIBLE
        val client = OdooApiClient.getClient()

        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/asset/detail/$assetId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    progressBarMoveLines.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return
                val jsonObj = JSONObject(jsonData)
                val assetObj = jsonObj.getJSONObject("asset")

                val moveLines = assetObj.getJSONArray("assets_line")
                val list = mutableListOf<AssetMoveItem>()

                for (i in 0 until moveLines.length()) {
                    val line = moveLines.getJSONObject(i)
                    list.add(
                        AssetMoveItem(
                            asset = line.getString("asset_id"),
                            category = line.getString("category"),
                            heldBy = line.getString("held_by"),
                            employee = line.getString("employee"),
                            rfid = line.getString("rfid"),
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    if (assetItem == null) {
                        displayHeaderImmediately()
                    }
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                    progressBarMoveLines.visibility = View.GONE
                }
            }
        })
    }

    private fun loadAssetDetail() {
        progressBar.visibility = View.VISIBLE
        val client = OdooApiClient.getClient()

        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/asset/detail/$assetId")
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
                val assetObj = jsonObj.getJSONObject("asset")

                val moveLines = assetObj.getJSONArray("assets_line")
                val list = mutableListOf<AssetMoveItem>()

                for (i in 0 until moveLines.length()) {
                    val line = moveLines.getJSONObject(i)
                    list.add(
                        AssetMoveItem(
                            asset = line.getString("asset_id"),
                            category = line.getString("category"),
                            heldBy = line.getString("held_by"),
                            employee = line.getString("employee"),
                            rfid = line.getString("rfid"),
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    txtAssetName.text = "Employee Asset Transfer: ${assetObj.getString("name")}"
//                    txtPartner.text = "Customer: ${assetObj.getString("partner_name")}"
                    txtScheduledDate.text = "Scheduled Date: ${assetObj.getString("due_date")}"
                    txtOrigin.text = "Origin: ${assetObj.optString("origin", "-")}"
                    txtState.text = "State: ${assetObj.getString("state")}"
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                }
            }
        })
    }

}
