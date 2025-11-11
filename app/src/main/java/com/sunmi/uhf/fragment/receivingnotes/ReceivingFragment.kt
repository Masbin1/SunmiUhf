package com.sunmi.uhf.fragment.receivingnotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import org.json.JSONArray
import org.json.JSONObject

class ReceivingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var receivingAdapter: ReceivingAdapter
    private val receivingList = mutableListOf<ReceivingItem>()
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receiving_notes, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewReceiving)
        recyclerView.layoutManager = LinearLayoutManager(context)

        progressBar = view.findViewById(R.id.progressBarReceiving)
        contentLayout = view.findViewById(R.id.contentLayoutReceiving)

        receivingAdapter = ReceivingAdapter(receivingList) { receivingItem ->
            openReceivingDetailFragment(receivingItem)
        }
        recyclerView.adapter = receivingAdapter

        fetchReceivingNotes()

        return view
    }

    private fun fetchReceivingNotes() {
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val url = "${BuildConfig.SERVER_URL}/get/stock/picking/receiving"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status")

                    requireActivity().runOnUiThread {
                        if (status == "success") {
                            addDataToList(jsonResponse)
                            progressBar.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                        } else {
                            val message = jsonResponse.optString("message", "Unknown error")
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                }
            },
            { error ->
                error.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                }
            }
        )

        queue.add(stringRequest)
    }


    private fun addDataToList(jsonObject: JSONObject) {
        val pickingsArray: JSONArray = jsonObject.getJSONArray("pickings")
        receivingList.clear()

        for (i in 0 until pickingsArray.length()) {
            val picking = pickingsArray.getJSONObject(i)
            val receivingItem = ReceivingItem(
                id = picking.getInt("id"),
                name = picking.getString("name"),
                partnerName = picking.optString("partner_name", "-"),
                scheduledDate = picking.optString("scheduled_date", "-"),
                state = picking.optString("state", "-")
            )
            receivingList.add(receivingItem)
        }
        receivingAdapter.notifyDataSetChanged()

        // Tambahkan animasi smooth pada item
//        recyclerView.layoutAnimation =
//            android.view.animation.AnimationUtils.loadLayoutAnimation(context, android.R.anim.slide_in_left)
    }

    private fun openReceivingDetailFragment(item: ReceivingItem) {
        val fragment = ReceivingDetailFragment.newInstance(item.id)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.frameLayoutReceiving, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(nothing: Nothing?) = ReceivingFragment()
    }
}
