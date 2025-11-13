package com.sunmi.uhf.fragment.receivingnotes

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ReceivingDetailFragment : Fragment() {

    private var receivingId: Int = 0
    private lateinit var adapter: ReceivingMoveAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtReceivingName: TextView
    private lateinit var txtPartner: TextView
    private lateinit var txtScheduledDate: TextView
    private lateinit var txtState: TextView
    private var moveLines: MutableList<ReceivingMoveItem> = mutableListOf()
    private val pendingRfidUpdates: MutableMap<Int, String> = mutableMapOf()

    companion object {
        fun newInstance(id: Int): ReceivingDetailFragment {
            val fragment = ReceivingDetailFragment()
            val args = Bundle()
            args.putInt("receiving_id", id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receivingId = arguments?.getInt("receiving_id") ?: 0
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

        adapter = ReceivingMoveAdapter(emptyList()) { item ->
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

        loadReceivingDetail()
        return view
    }

    private fun handleReceivingScanResult(moveId: Int, rfid: String) {
        if (moveId == -1 || rfid.isEmpty()) {
            return
        }
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

    fun getPendingRfidUpdates(): Map<Int, String> = pendingRfidUpdates.toMap()

    private fun loadReceivingDetail() {
        progressBar.visibility = View.VISIBLE
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BuildConfig.SERVER_URL}/get/stock/picking/receiving/detail/$receivingId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string() ?: return
                val jsonObj = JSONObject(jsonData)
                val pickingObj = jsonObj.getJSONObject("picking")
                val moveLinesArray = pickingObj.getJSONArray("move_lines")

                val list = mutableListOf<ReceivingMoveItem>()
                for (i in 0 until moveLinesArray.length()) {
                    val line = moveLinesArray.getJSONObject(i)
                    list.add(
                        ReceivingMoveItem(
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

                pendingRfidUpdates.forEach { (id, value) ->
                    val idx = list.indexOfFirst { it.moveId == id }
                    if (idx != -1) {
                        list[idx].pendingRfid = value
                    }
                }

                moveLines = list

                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    txtReceivingName.text = "Receiving Number: ${pickingObj.getString("name")}"
                    txtPartner.text = "Vendor: ${pickingObj.getString("partner_name")}"
                    txtScheduledDate.text = "Scheduled Date: ${pickingObj.getString("scheduled_date")}"
                    txtState.text = "State: ${pickingObj.getString("state")}"
                    adapter.updateData(list)
                    progressBar.visibility = View.GONE
                }
            }
        })
    }
}
