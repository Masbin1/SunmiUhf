package com.sunmi.uhf.fragment.deliveryorder

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
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.ApiHelper
import kotlinx.coroutines.launch

class DeliveryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DeliveryAdapter
    private lateinit var contentLayout: LinearLayout

    // Pagination state
    private var currentPage = 1
    private val pageSize = 20 // adjust if needed / based on server
    private var isLoading = false
    private var isLastPage = false

    // Track already seen item IDs to prevent duplicates
    private val seenIds = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_order, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewDelivery)
        progressBar = view.findViewById(R.id.progressBarDelivery)
        contentLayout = view.findViewById(R.id.contentLayoutDelivery)


        adapter = DeliveryAdapter(mutableListOf()) { item ->
            val fragment = DeliveryDetailFragment.newInstance(item)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Add an onScrollListener to trigger pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy <= 0) return // only handle scroll down

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Trigger load more when reaching the end
                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize
                    ) {
                        loadDeliveryOrders(page = currentPage + 1)
                    }
                }
            }
        })

        loadDeliveryOrders(page = 1)
        return view
    }

    private fun loadDeliveryOrders(page: Int = 1) {
        // show top progress only for first page, otherwise show inline loading
        if (page == 1) {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            // reset seen ids on fresh load
            seenIds.clear()
            isLastPage = false
            currentPage = 1
        } else {
            // optional: show a loading footer. For now reuse progressBar
            progressBar.visibility = View.VISIBLE
        }

        isLoading = true
        lifecycleScope.launch {
            try {
                val useCache = page == 1
                val offset = (page - 1) * pageSize
                val url = "${AuthUtils.getServerUrl()}/get/stock/picking/delivery?offset=$offset&limit=$pageSize"
                val jsonObject = ApiHelper.getJsonObject(url, useCache = useCache)

                if (jsonObject.getString("status") == "success") {
                    val jsonArray = jsonObject.getJSONArray("pickings")
                    val deliveryList = mutableListOf<DeliveryItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        if (seenIds.contains(id)) continue
                        seenIds.add(id)

                        deliveryList.add(
                            DeliveryItem(
                                id = id,
                                name = obj.getString("name"),
                                scheduledDate = obj.getString("scheduled_date"),
                                partnerName = obj.optString("partner_name", "-"),
                                state = obj.getString("state")
                            )
                        )
                    }

                    progressBar.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE

                    // if no new items were returned for this page, treat as last page
                    if (deliveryList.isEmpty() && page > 1) {
                        isLastPage = true
                        isLoading = false
                        return@launch
                    }

                    if (page == 1) {
                        adapter.updateData(deliveryList)
                    } else {
                        adapter.appendData(deliveryList)
                    }

                    // update pagination state
                    isLoading = false
                    if (deliveryList.size < pageSize) {
                        isLastPage = true
                    } else {
                        currentPage = page
                    }
                } else {
                    isLoading = false
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed: ${jsonObject.optString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle?) = DeliveryFragment()
            .apply { arguments = args }
    }
}
