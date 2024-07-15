package com.sunmi.uhf.fragment.batch
import BatchAdapter
import BatchItem
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.android.volley.toolbox.Volley
import com.sunmi.uhf.R
import org.json.JSONArray
import org.json.JSONObject

class BatchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var batchAdapter: BatchAdapter
    private val batchList = mutableListOf<BatchItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_batch, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewbatch)
        recyclerView.layoutManager = LinearLayoutManager(context)
        batchAdapter = BatchAdapter(batchList) { batchItem ->
            openTakeInventoryFragment(batchItem)
        }
        recyclerView.adapter = batchAdapter

        val getMyListTextView: TextView = view.findViewById(R.id.get_my_list_batch)
        getMyListTextView.setOnClickListener {
            fetchDataFromApi()
        }

        val scanBatchTextView: TextView = view.findViewById(R.id.scan_batch)
        scanBatchTextView.setOnClickListener {
            if (batchList.isNotEmpty()) {
                // Pass the first item in the list to the fragment, or modify this as needed
                openTakeInventoryFragment(batchList[0])
            } else {
                Toast.makeText(activity, "No batch items available", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }


    private fun openTakeInventoryFragment(batchItem: BatchItem) {
        val takeInventoryFragment = TakeInventoryFragment.newInstance(batchItem)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutbatch, takeInventoryFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun fetchDataFromApi() {
        val queue: RequestQueue = Volley.newRequestQueue(activity)
        val url = "https://loyal-martin-present.ngrok-free.app/get/batch/picking"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                addDataToList(response)
            },
            {
                Toast.makeText(activity, "That didn't work!", Toast.LENGTH_LONG).show()
            })

        queue.add(stringRequest)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addDataToList(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val status = jsonObject.getString("status")

            if (status == "success") {
                val batchOrders: JSONArray = jsonObject.getJSONArray("batch_datas")
                batchList.clear()

                for (i in 0 until batchOrders.length()) {
                    val order = batchOrders.getJSONObject(i)
                    val batchItem = BatchItem(
                        idBatch = order.getInt("id_batch"),
                        name = order.getString("name"),
                        responsible = order.getString("responsible"),
                    )
                    batchList.add(batchItem)
                }
                batchAdapter.notifyDataSetChanged()
            } else {
                val errorMessage = jsonObject.getString("message")
                Toast.makeText(activity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Error parsing JSON", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun newInstance() = BatchFragment()
    }
}