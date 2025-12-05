package com.sunmi.uhf.fragment.receivingnotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.ApiHelper
import com.sunmi.uhf.service.OdooApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class ReceivingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReceivingAdapter
    private lateinit var contentLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receiving_notes, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewReceiving)
        progressBar = view.findViewById(R.id.progressBarReceiving)
        contentLayout = view.findViewById(R.id.contentLayoutReceiving)

        adapter = ReceivingAdapter(emptyList()) { item ->
            val fragment = ReceivingDetailFragment.newInstance(item)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadReceivingNotes()
        return view
    }

    private fun loadReceivingNotes() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val jsonObject = ApiHelper.getJsonObject(
                    "${AuthUtils.getServerUrl()}/get/stock/picking/receiving",
                    useCache = true
                )
                
                if (jsonObject.getString("status") == "success") {
                    val jsonArray = jsonObject.getJSONArray("pickings")
                    val receivingList = mutableListOf<ReceivingItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        receivingList.add(
                            ReceivingItem(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                scheduledDate = obj.optString("scheduled_date", "-"),
                                partnerName = obj.optString("partner_name", "-"),
                                state = obj.optString("state", "-")
                            )
                        )
                    }

                    progressBar.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE
                    adapter.updateData(receivingList)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed: ${jsonObject.optString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun newInstance(nothing: Nothing?) = ReceivingFragment()
    }
}
