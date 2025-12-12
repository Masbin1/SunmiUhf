package com.sunmi.uhf.fragment.productAsset

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.OdooApiClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProductAssetDetailFragment : Fragment() {

    private var receivingId: Int = 0
    private var receivingItem: ProductAssetItem? = null
    private lateinit var adapter: ProductAssetMoveAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarMoveLines: ProgressBar
    private lateinit var txtReceivingName: TextView
    private lateinit var txtPartner: TextView
    private lateinit var txtScheduledDate: TextView
    private lateinit var txtState: TextView
    private lateinit var btnSaveReceiving: FloatingActionButton

    private var moveLines: MutableList<ProductAssetMoveItem> = mutableListOf()
    private val pendingRfidUpdates: MutableMap<Int, String> = mutableMapOf()

    companion object {
        fun newInstance(id: Int): ProductAssetDetailFragment {
            val fragment = ProductAssetDetailFragment()
            val args = Bundle()
            args.putInt("receiving_id", id)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(item: ProductAssetItem): ProductAssetDetailFragment {
            val fragment = ProductAssetDetailFragment()
            val args = Bundle()
            args.putInt("receiving_id", item.id)
            args.putParcelable("receiving_item", item)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receivingId = arguments?.getInt("receiving_id") ?: 0
        receivingItem = arguments?.getParcelable("receiving_item")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_receiving_detail, container, false)

        txtReceivingName = view.findViewById(R.id.txtReceivingName)
        txtPartner = view.findViewById(R.id.txtPartner)
        txtScheduledDate = view.findViewById(R.id.txtScheduledDate)
        txtState = view.findViewById(R.id.txtState)
        recyclerView = view.findViewById(R.id.recyclerViewReceiving)
        progressBar = view.findViewById(R.id.progressBarReceivingDetail)
        progressBarMoveLines = view.findViewById(R.id.progressBarMoveLines)
        btnSaveReceiving = view.findViewById(R.id.btnSaveReceiving)

        adapter = ProductAssetMoveAdapter(emptyList()) { item ->
            val args = Bundle().apply {
                putSerializable(TakeInventoryFragment.ARG_KEY_RECEIVING_ITEM, item)
            }

            val fragment = TakeInventoryFragment.newInstance(args)
            fragment.setReceivingScanResultListener { moveId, rfid ->
                handleReceivingScanResult(moveId, rfid)
            }

            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnSaveReceiving.setOnClickListener { saveReceivingToServer() }

        if (receivingItem != null) {
            displayHeaderImmediately()
            loadMoveLines()
        } else {
            loadReceivingDetail()
        }
        return view
    }

    @SuppressLint("SetTextI18n")
    private fun displayHeaderImmediately() {
        receivingItem?.let {
            txtReceivingName.text = "Receiving Number: ${it.name}"
            txtPartner.text = "Vendor: ${it.partnerName}"
            txtScheduledDate.text = "Scheduled Date: ${it.scheduledDate}"
            txtState.text = "State: ${it.state}"
        }
    }

    /**
     * Menerima hasil scan RFID dari TakeInventoryFragment
     */
    private fun handleReceivingScanResult(moveId: Int, rfid: String) {
        if (moveId == -1 || rfid.isEmpty()) return

        val index = moveLines.indexOfFirst { it.moveId == moveId }
        if (index != -1) {
            if (rfid == moveLines[index].rfid) {
                pendingRfidUpdates.remove(moveId)
                moveLines[index].pendingRfid = null
            } else {
                pendingRfidUpdates[moveId] = rfid
                moveLines[index].pendingRfid = rfid
            }
            adapter.notifyItemChanged(index)
        }
    }

    /**
     * Kirim update RFID ke server Odoo
     */
    private fun saveReceivingToServer() {
        if (pendingRfidUpdates.isEmpty()) {
            Toast.makeText(requireContext(), "No updates to save", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        val client = OdooApiClient.getClient()

        // JSON body untuk dikirim ke Odoo
        val jsonBody = JSONObject().apply {
            val moveLinesArray = JSONArray()
            pendingRfidUpdates.forEach { (moveId, rfid) ->
                moveLinesArray.put(JSONObject().apply {
                    put("move_id", moveId)
                    put("rfid", rfid)
                })
            }
            put("move_lines", moveLinesArray)
        }

        val requestBody = RequestBody.create(
            "application/json".toMediaType(),
            jsonBody.toString()
        )

        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/update/product/asset")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to send data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && body?.contains("\"success\": true") == true) {
                        Toast.makeText(requireContext(), "Data saved successfully", Toast.LENGTH_SHORT).show()
                        pendingRfidUpdates.clear()
                    } else {
                        Toast.makeText(requireContext(), "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loadMoveLines() {
        progressBar.visibility = View.VISIBLE
        progressBarMoveLines.visibility = View.VISIBLE

        val client = OdooApiClient.getClient()
        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/product/asset/detail/$receivingId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    progressBarMoveLines.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                if (jsonData.isNullOrEmpty()) return

                val jsonObj = JSONObject(jsonData)
                val pickingObj = jsonObj.getJSONObject("picking")

                // Process move lines
                val moveLinesArray = pickingObj.getJSONArray("move_lines")
                val list = mutableListOf<ProductAssetMoveItem>()
                for (i in 0 until moveLinesArray.length()) {
                    val line = moveLinesArray.getJSONObject(i)
                    list.add(
                        ProductAssetMoveItem(
                            moveId = line.getInt("id"),
                            productName = line.getString("product_name"),
                            productUomQty = line.getDouble("product_uom_qty"),
                            quantityDone = line.getDouble("quantity_done"),
                            uomName = line.getString("uom_name"),
                            lotName = line.getString("lot_name"),
                            rfid = line.optString("rfid", "")
                        )
                    )
                }

                // Tandai pending RFID jika ada
                pendingRfidUpdates.forEach { (id, value) ->
                    val idx = list.indexOfFirst { it.moveId == id }
                    if (idx != -1) list[idx].pendingRfid = value
                }

                moveLines = list

                // Display move lines
                activity?.runOnUiThread {
                    if (receivingItem == null) {
                        displayHeaderImmediately()
                    }
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                    progressBarMoveLines.visibility = View.GONE
                }
            }
        })
    }

    /**
     * Ambil detail receiving dari server (fallback jika tidak ada item data)
     */
    private fun loadReceivingDetail() {
        progressBar.visibility = View.VISIBLE

        val client = OdooApiClient.getClient()
        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/product/asset/detail/$receivingId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                if (jsonData.isNullOrEmpty()) return

                val jsonObj = JSONObject(jsonData)
                val pickingObj = jsonObj.getJSONObject("picking")

                // Display header immediately
                activity?.runOnUiThread {
                    txtReceivingName.text = "Receiving Number: ${pickingObj.getString("name")}"
                    txtPartner.text = "Vendor: ${pickingObj.getString("partner_name")}"
                    txtScheduledDate.text = "Scheduled Date: ${pickingObj.getString("scheduled_date")}"
                    txtState.text = "State: ${pickingObj.getString("state")}"
                    progressBar.visibility = View.GONE
                    progressBarMoveLines.visibility = View.VISIBLE
                }

                // Process move lines
                val moveLinesArray = pickingObj.getJSONArray("move_lines")
                val list = mutableListOf<ProductAssetMoveItem>()
                for (i in 0 until moveLinesArray.length()) {
                    val line = moveLinesArray.getJSONObject(i)
                    list.add(
                        ProductAssetMoveItem(
                            moveId = line.getInt("id"),
                            productName = line.getString("product_name"),
                            productUomQty = line.getDouble("product_uom_qty"),
                            quantityDone = line.getDouble("quantity_done"),
                            uomName = line.getString("uom_name"),
                            lotName = line.getString("lot_name"),
                            rfid = line.optString("rfid", "")
                        )
                    )
                }

                // Tandai pending RFID jika ada
                pendingRfidUpdates.forEach { (id, value) ->
                    val idx = list.indexOfFirst { it.moveId == id }
                    if (idx != -1) list[idx].pendingRfid = value
                }

                moveLines = list

                // Display move lines
                activity?.runOnUiThread {
                    adapter.updateData(list)
                    progressBarMoveLines.visibility = View.GONE
                }
            }
        })
    }
}
