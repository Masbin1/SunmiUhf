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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.appcompat.widget.AppCompatEditText
import org.json.JSONArray
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.content.Context
import android.graphics.drawable.Drawable

class ProductAssetFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ProductAssetAdapter
    private lateinit var contentLayout: LinearLayout
    private lateinit var editSearch: AppCompatEditText
    private lateinit var txtEmpty: android.widget.TextView

    // Pagination state
    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var isLastPage = false

    // Track already seen item IDs to prevent duplicates when server returns repeated data
    private val seenIds = mutableSetOf<Int>()

    // Search state
    private var searchJob: Job? = null
    private var currentQuery: String = ""

    // Job for the current load request so we can cancel in-flight network calls
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_asset, container, false)

        recyclerView = view.findViewById(recyclerViewProductAsset)
        progressBar = view.findViewById(R.id.progressBarProductAsset)
        contentLayout = view.findViewById(R.id.contentLayoutProductAsset)
        editSearch = view.findViewById(R.id.editSearchProductAsset)
        txtEmpty = view.findViewById(R.id.txtEmptyProductAsset)

        // make sure content is visible immediately (layout default may be GONE)
        contentLayout.visibility = View.VISIBLE
        txtEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

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
                        loadProductAssetNotes(page = currentPage + 1, query = currentQuery)
                    }
                }
            }
        })

        // Initialize search drawables (no clear icon at start)
        updateSearchDrawable(showClear = false)

        // handle tapping the clear (drawableEnd)
        editSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawables = editSearch.compoundDrawablesRelative
                // drawableEnd is index 2 when using compoundDrawablesRelative
                val drawableEnd: Drawable? = if (drawables != null && drawables.size >= 3) drawables[2] else null
                if (drawableEnd != null) {
                    val bounds = drawableEnd.bounds
                    val x = event.x.toInt()
                    val width = editSearch.width
                    val paddingEnd = editSearch.paddingEnd
                    if (x >= width - paddingEnd - bounds.width()) {
                        // clear text without losing focus
                        editSearch.setText("")
                        updateSearchDrawable(showClear = false)
                        currentQuery = ""
                        adapter.filter("")
                        showEmptyIfNeeded()
                        editSearch.requestFocus()
                        try {
                            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
                        } catch (_: Exception) {}
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // Search text listener with debounce
        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim() ?: ""

                // show/hide clear icon immediately
                updateSearchDrawable(showClear = q.isNotEmpty())

                // cancel previous job
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // debounce
                    // if query changed, reset pagination and reload from page 1 with query
                    if (q != currentQuery) {
                        currentQuery = q
                        // cancel any in-flight load (network) before starting a new one
                        loadJob?.cancel()
                        loadProductAssetNotes(page = 1, query = currentQuery)
                    } else {
                        // If same query but we want local filtering (e.g., when server doesn't support search), apply filter
                        adapter.filter(currentQuery)
                        showEmptyIfNeeded()
                    }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadProductAssetNotes(page = 1)
        return view
    }

    // update compound drawables for the search EditText (search icon left, clear icon right optional)
    private fun updateSearchDrawable(showClear: Boolean) {
        try {
            val searchDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            val clearDrawable = if (showClear) ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear) else null
            // use relative to support RTL
            editSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(searchDrawable, null, clearDrawable, null)
        } catch (e: Exception) {
            // ignore drawable errors
        }
    }

    private fun loadProductAssetNotes(page: Int = 1, query: String = "") {
        // cancel any previous load job
        loadJob?.cancel()

        // show top progress only for first page
        if (page == 1) {
            // Keep content visible so search EditText doesn't lose focus.
            // Show a progress indicator overlay and dim/disable the content to indicate loading.
            // Preserve current focus + cursor position so the user can continue typing smoothly.
            val hadFocus = editSearch.hasFocus()
            val selPos = try { editSearch.selectionStart.coerceAtLeast(0) } catch (_: Exception) { -1 }

            // make sure content is visible (was previously 'gone' in layout by default)
            contentLayout.visibility = View.VISIBLE

            progressBar.visibility = View.VISIBLE
            contentLayout.isEnabled = false
            contentLayout.alpha = 0.6f

            // Only restore focus/keyboard if the user already had focus in the search field
            // or if there is an active query (user expects to type)
            if (hadFocus || currentQuery.isNotBlank()) {
                editSearch.post {
                    try {
                        editSearch.requestFocus()
                        if (selPos >= 0) {
                            val length = editSearch.text?.length ?: 0
                            editSearch.setSelection(selPos.coerceAtMost(length))
                        }
                        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
                    } catch (_: Exception) {
                    }
                }
            }
            // clear seen IDs when reloading first page
            seenIds.clear()
            isLastPage = false
            currentPage = 1
        } else {
            progressBar.visibility = View.VISIBLE
        }

        isLoading = true
        loadJob = lifecycleScope.launch {
            try {
                // disable cache for subsequent pages to avoid stale repeated responses
                // If a search query is present, don't use cache even for page 1
                val useCache = page == 1 && query.isBlank()
                val offset = (page - 1) * pageSize
                Log.d(TAG, "request page=$page offset=$offset useCache=$useCache query=$query")

                // include search query if present (URL-encode)
                val queryParam = if (query.isNotBlank()) "&search=${java.net.URLEncoder.encode(query, "UTF-8")}" else ""
                val url = "${AuthUtils.getServerUrl()}/get/product/asset/?offset=$offset&limit=$pageSize$queryParam"

                // Try to fetch a JSON object first; if server returns an array or a raw array string, fall back to getJsonArray
                val jsonArray: JSONArray = try {
                    val obj = ApiHelper.getJsonObject(url, useCache)
                    // If the object contains explicit status + assets
                    if (obj.optString("status").isNotEmpty()) {
                        if (obj.optString("status") == "success") {
                            obj.optJSONArray("assets") ?: obj.optJSONArray("data") ?: JSONArray()
                        } else {
                            // server returned failure status
                            throw Exception("Failed: ${obj.optString("message")} ")
                        }
                    } else {
                        // no status field; try to extract arrays commonly named
                        obj.optJSONArray("assets") ?: obj.optJSONArray("data") ?: JSONArray()
                    }
                } catch (eObj: Exception) {
                    // as a fallback, try to parse as array directly
                    try {
                        ApiHelper.getJsonArray(url, useCache)
                    } catch (_: Exception) {
                        // propagate original error if both attempts fail
                        throw eObj
                    }
                }

                val productAssetList = mutableListOf<ProductAssetItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getInt("id")
                    // skip items we've already seen to avoid duplicates
                    if (seenIds.contains(id)) continue

                    seenIds.add(id)

                    // Defensive extraction of product name — backend may return product_id as string or object
                    val productName = extractProductName(obj)

                    productAssetList.add(
                        ProductAssetItem(
                            id,
                            obj.optString("name", "-"),
                            productName,
                            obj.optString("asset_code", ""),
                            obj.optString("asset_category", "")
                        )
                    )
                }
                Log.d(TAG, "fetched total=${jsonArray.length()} new=${productAssetList.size} seenTotal=${seenIds.size}")

                // restore content interactivity and hide progress
                progressBar.visibility = View.GONE
                // ensure visible (in case it was GONE initially)
                contentLayout.visibility = View.VISIBLE
                 contentLayout.isEnabled = true
                 contentLayout.alpha = 1f

                // if search is focused, ensure cursor position remains reasonable
                try {
                    if (editSearch.hasFocus()) {
                        val pos = editSearch.selectionStart.coerceAtLeast(0)
                        val length = editSearch.text?.length ?: 0
                        editSearch.setSelection(pos.coerceAtMost(length))
                    }
                } catch (_: Exception) { }

                // if no new items were returned for this page, treat as last page
                if (productAssetList.isEmpty() && page > 1) {
                    isLastPage = true
                    isLoading = false
                    return@launch
                }

                if (page == 1) {
                    adapter.updateData(productAssetList)
                    // apply local filter immediately if user has a query
                    if (currentQuery.isNotBlank()) adapter.filter(currentQuery)
                } else {
                    adapter.appendData(productAssetList)
                    // when appending, if there's an active query we should re-filter so new items are considered
                    if (currentQuery.isNotBlank()) adapter.filter(currentQuery)
                }

                isLoading = false
                if (productAssetList.size < pageSize) {
                    isLastPage = true
                } else {
                    currentPage = page
                }

                // show/hide empty view
                showEmptyIfNeeded()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // request was cancelled intentionally; don't show error to user
                    Log.d(TAG, "loadProductAssetNotes cancelled")
                    return@launch
                }
                isLoading = false
                // make sure UI restored on error
                progressBar.visibility = View.GONE
                // ensure visible on error as well (don't leave it GONE)
                contentLayout.visibility = View.VISIBLE
                 contentLayout.isEnabled = true
                 contentLayout.alpha = 1f
                try {
                    if (editSearch.hasFocus()) {
                        val pos = editSearch.selectionStart.coerceAtLeast(0)
                        val length = editSearch.text?.length ?: 0
                        editSearch.setSelection(pos.coerceAtMost(length))
                    }
                } catch (_: Exception) { }
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEmptyIfNeeded() {
        if (adapter.itemCount == 0) {
            txtEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // Helper to robustly extract product display name from possible shapes
    private fun extractProductName(json: org.json.JSONObject): String {
        try {
            // 1) product_id might be a JSON object with display_name or name
            val prodObj = json.optJSONObject("product_id")
            if (prodObj != null) {
                return prodObj.optString("display_name", prodObj.optString("name", "-"))
            }

            // 2) or it might be provided as a simple string like "123: Product Name" or just "Product Name"
            val prodStr = json.optString("product_id", json.optString("product", "")).trim()
            if (prodStr.isNotEmpty()) {
                // if it contains a colon like "123: Name", take the part after colon
                val parts = prodStr.split(":", limit = 2).map { it.trim() }
                return if (parts.size == 2) parts[1] else parts[0]
            }

            // 3) fallback to other keys that might exist
            return json.optString("product_name", json.optString("product_display", "-"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract product name: ${e.message}")
            return "-"
        }
    }

    companion object {
        private const val TAG = "productAssetFragment"

        fun newInstance() = ProductAssetFragment()
    }
}
