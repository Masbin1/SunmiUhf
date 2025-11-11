package com.sunmi.uhf.fragment.receivingnotes

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import org.json.JSONObject

class ReceivingDetailFragment : Fragment() {

    private var pickingId: Int = 0

    private lateinit var textViewName: TextView
    private lateinit var textViewPartner: TextView
    private lateinit var textViewDate: TextView
    private lateinit var textViewState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReceivingMoveAdapter
    private val moveList = mutableListOf<ReceivingMoveItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickingId = arguments?.getInt(ARG_PICKING_ID) ?: 0
    }

    private lateinit var progressBar: View
    private lateinit var contentLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receiving_detail, container, false)

        progressBar = view.findViewById(R.id.progressBarDetail)
        contentLayout = view.findViewById(R.id.contentLayout)

        textViewName = view.findViewById(R.id.textViewDetailName)
        textViewPartner = view.findViewById(R.id.textViewDetailPartner)
        textViewDate = view.findViewById(R.id.textViewDetailScheduledDate)
        textViewState = view.findViewById(R.id.textViewDetailState)
        recyclerView = view.findViewById(R.id.recyclerViewMoves)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ReceivingMoveAdapter(moveList)
        recyclerView.adapter = adapter

        fetchDetailData()

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchDetailData() {
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        val queue = Volley.newRequestQueue(activity)
        val url = "${BuildConfig.SERVER_URL}/get/stock/picking/detail/$pickingId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        val picking = json.getJSONObject("picking")
                        textViewName.text = picking.getString("name")
                        textViewPartner.text = picking.optString("partner_name", "-")
                        textViewDate.text = picking.optString("scheduled_date", "-")
                        textViewState.text = picking.optString("state", "-")

                        val moves = picking.getJSONArray("moves")
                        moveList.clear()
                        for (i in 0 until moves.length()) {
                            val move = moves.getJSONObject(i)
                            moveList.add(
                                ReceivingMoveItem(
                                    productName = move.optString("product_name", "-"),
                                    productQty = move.optDouble("product_qty", 0.0),
                                    uom = move.optString("uom_name", "-")
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()

                        progressBar.visibility = View.GONE
                        contentLayout.visibility = View.VISIBLE
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            activity,
                            json.optString("message", "Error"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    e.printStackTrace()
                    Toast.makeText(activity, "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(activity, "Network error: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        )

        queue.add(request)
    }


    companion object {
        private const val ARG_PICKING_ID = "picking_id"

        fun newInstance(pickingId: Int): ReceivingDetailFragment {
            val fragment = ReceivingDetailFragment()
            val args = Bundle()
            args.putInt(ARG_PICKING_ID, pickingId)
            fragment.arguments = args
            return fragment
        }
    }
}
