package com.sunmi.uhf.fragment.productAsset

import android.os.Bundle
import android.util.Log
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
import com.sunmi.uhf.R.id.recyclerViewProductAsset
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.ApiHelper
import kotlinx.coroutines.launch

class ProductAssetFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ProductAssetAdapter
    private lateinit var contentLayout: LinearLayout

    // Pagination state
    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var isLastPage = false

    // Track already seen item IDs to prevent duplicates when server returns repeated data
    private val seenIds = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_asset, container, false)

        recyclerView = view.findViewById(recyclerViewProductAsset)
        progressBar = view.findViewById(R.id.progressBarProductAsset)
        contentLayout = view.findViewById(R.id.contentLayoutProductAsset)

        adapter = ProductAssetAdapter(mutableListOf()) { item ->
            val fragment = ProductAssetDetailFragment.newInstance(item)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy <= 0) return

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize
                    ) {
                        loadProductAssetNotes(page = currentPage + 1)
                    }
                }
            }
        })

        loadProductAssetNotes(page = 1)
        return view
    }

    private fun loadProductAssetNotes(page: Int = 1) {
        // show top progress only for first page
        if (page == 1) {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            // clear seen IDs when reloading first page
            seenIds.clear()
            isLastPage = false
            currentPage = 1
        } else {
            progressBar.visibility = View.VISIBLE
        }

        isLoading = true
        lifecycleScope.launch {
            try {
                // disable cache for subsequent pages to avoid stale repeated responses
                val useCache = page == 1
                val offset = (page - 1) * pageSize
                Log.d(TAG, "request page=$page offset=$offset useCache=$useCache")
                val url = "${AuthUtils.getServerUrl()}/get/product/asset/?offset=$offset&limit=$pageSize"
                val jsonObject = ApiHelper.getJsonObject(
                    url,
                    useCache = useCache
                )

                if (jsonObject.getString("status") == "success") {
                    // API returns 'assets' array
                    val jsonArray = jsonObject.getJSONArray("assets")
                    val productAssetList = mutableListOf<ProductAssetItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        // skip items we've already seen to avoid duplicates
                        if (seenIds.contains(id)) continue

                        seenIds.add(id)
                        productAssetList.add(
                            ProductAssetItem(
                                id,
                                obj.optString("name", "-"),
                                obj.optString("product_id", "-"),
                                obj.optString("asset_code", ""),
                                obj.optString("asset_category", "")
                            )
                        )
                    }
                    Log.d(TAG, "fetched total=${jsonArray.length()} new=${productAssetList.size} seenTotal=${seenIds.size}")

                    progressBar.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE

                    // if no new items were returned for this page, treat as last page
                    if (productAssetList.isEmpty() && page > 1) {
                        isLastPage = true
                        isLoading = false
                        return@launch
                    }

                    if (page == 1) {
                        adapter.updateData(productAssetList)
                    } else {
                        adapter.appendData(productAssetList)
                    }

                    isLoading = false
                    if (productAssetList.size < pageSize) {
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
        private const val TAG = "productAssetFragment"

        fun newInstance(nothing: Nothing?) = ProductAssetFragment()
    }
}
