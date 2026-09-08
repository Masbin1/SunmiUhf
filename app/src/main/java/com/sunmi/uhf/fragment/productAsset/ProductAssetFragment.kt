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
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.os.Parcelable

class ProductAssetFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarSearch: ProgressBar
    private lateinit var adapter: ProductAssetAdapter
    private lateinit var contentLayout: LinearLayout
    private lateinit var editSearch: AppCompatEditText
    private lateinit var txtEmpty: android.widget.TextView

    // In-memory cache of loaded items & scroll state so back navigation does not reload
    private val cachedItems = mutableListOf<ProductAssetItem>()
    private var recyclerViewState: Parcelable? = null
    private var isRestoringState = false

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
        progressBarSearch = view.findViewById(R.id.progressBarSearchProductAsset)
        contentLayout = view.findViewById(R.id.contentLayoutProductAsset)
        editSearch = view.findViewById(R.id.editSearchProductAsset)
        txtEmpty = view.findViewById(R.id.txtEmptyProductAsset)

        // Make sure content is visible immediately
        contentLayout.visibility = View.VISIBLE
        txtEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        adapter = ProductAssetAdapter(cachedItems.toMutableList()) { item ->
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

        // Scroll listener for pagination
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

        // Keyboard "Search" or hardware Enter action listener
        editSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val q = editSearch.text?.toString()?.trim() ?: ""

                // 1. Immediate local filter
                adapter.filter(q)
                showEmptyIfNeeded()

                // 2. Immediate server request
                searchJob?.cancel()
                if (q != currentQuery) {
                    currentQuery = q
                    loadJob?.cancel()
                    loadProductAssetNotes(page = 1, query = currentQuery)
                }

                // Dismiss soft keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(editSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Handle tapping the clear icon (drawableEnd)
        editSearch.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawables = editSearch.compoundDrawablesRelative
                val drawableEnd: Drawable? = if (drawables != null && drawables.size >= 3) drawables[2] else null
                if (drawableEnd != null && editSearch.text?.isNotEmpty() == true) {
                    val iconWidth = drawableEnd.intrinsicWidth.coerceAtLeast(48)
                    val extraTouchPadding = 24
                    val touchTargetMinX = editSearch.width - editSearch.paddingEnd - iconWidth - extraTouchPadding
                    if (event.x >= touchTargetMinX) {
                        // Clear text and reload all assets from server
                        editSearch.setText("")
                        updateSearchDrawable(showClear = false)
                        searchJob?.cancel()
                        loadJob?.cancel()
                        currentQuery = ""
                        adapter.filter("")
                        showEmptyIfNeeded()
                        loadProductAssetNotes(page = 1, query = "")
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // Live typing listener: immediate local filter (0ms) + debounced server query
        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isRestoringState) return
                val q = s?.toString()?.trim() ?: ""

                // Show/hide clear icon immediately
                updateSearchDrawable(showClear = q.isNotEmpty())

                // 1. Instant local filter for immediate responsiveness (0ms latency!)
                adapter.filter(q)
                showEmptyIfNeeded()

                // 2. Debounced server-side query
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(350) // 350ms debounce for server query
                    if (q != currentQuery) {
                        currentQuery = q
                        loadJob?.cancel()
                        loadProductAssetNotes(page = 1, query = currentQuery)
                    }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        if (cachedItems.isNotEmpty()) {
            isRestoringState = true
            if (currentQuery.isNotEmpty()) {
                editSearch.setText(currentQuery)
                editSearch.setSelection(currentQuery.length)
                updateSearchDrawable(showClear = true)
                adapter.filter(currentQuery)
            }
            showEmptyIfNeeded()
            recyclerViewState?.let {
                recyclerView.post {
                    layoutManager.onRestoreInstanceState(it)
                }
            }
            isRestoringState = false
        } else {
            loadProductAssetNotes(page = 1)
        }
        return view
    }

    // Update compound drawables for the search EditText (search icon left, clear icon right)
    private fun updateSearchDrawable(showClear: Boolean) {
        try {
            val searchDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            val clearDrawable = if (showClear) ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear) else null
            editSearch.setCompoundDrawablesRelativeWithIntrinsicBounds(searchDrawable, null, clearDrawable, null)
        } catch (e: Exception) {
            // ignore drawable errors
        }
    }

    private fun loadProductAssetNotes(page: Int = 1, query: String = "") {
        // Cancel any previous load job
        loadJob?.cancel()

        if (page == 1) {
            seenIds.clear()
            isLastPage = false
            currentPage = 1

            if (query.isNotBlank()) {
                // When searching, show subtle inline progress bar without blocking the UI
                progressBarSearch.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            } else {
                // Initial full-page load
                progressBar.visibility = View.VISIBLE
                progressBarSearch.visibility = View.INVISIBLE
            }
        } else {
            // Subsequent pages
            progressBarSearch.visibility = View.VISIBLE
        }

        isLoading = true
        loadJob = lifecycleScope.launch {
            try {
                // Disable cache for searches or subsequent pages to ensure real-time server query
                val useCache = page == 1 && query.isBlank()
                val offset = (page - 1) * pageSize
                Log.d(TAG, "request page=$page offset=$offset useCache=$useCache query=$query")

                // Direct server-side query parameter
                val queryParam = if (query.isNotBlank()) "&search=${java.net.URLEncoder.encode(query, "UTF-8")}" else ""
                val url = "${AuthUtils.getServerUrl()}/get/product/asset/?offset=$offset&limit=$pageSize$queryParam"

                // Try to fetch a JSON object first; if server returns an array or raw array string, fall back to getJsonArray
                val jsonArray: JSONArray = try {
                    val obj = ApiHelper.getJsonObject(url, useCache)
                    if (obj.optString("status").isNotEmpty()) {
                        if (obj.optString("status") == "success") {
                            obj.optJSONArray("assets") ?: obj.optJSONArray("data") ?: JSONArray()
                        } else {
                            throw Exception("Failed: ${obj.optString("message")}")
                        }
                    } else {
                        obj.optJSONArray("assets") ?: obj.optJSONArray("data") ?: JSONArray()
                    }
                } catch (eObj: Exception) {
                    try {
                        ApiHelper.getJsonArray(url, useCache)
                    } catch (_: Exception) {
                        throw eObj
                    }
                }

                val productAssetList = mutableListOf<ProductAssetItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getInt("id")
                    if (seenIds.contains(id)) continue

                    seenIds.add(id)

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

                progressBar.visibility = View.GONE
                progressBarSearch.visibility = View.INVISIBLE
                contentLayout.visibility = View.VISIBLE

                // If no new items returned for page > 1, mark as last page
                if (productAssetList.isEmpty() && page > 1) {
                    isLastPage = true
                    isLoading = false
                    return@launch
                }

                if (page == 1) {
                    adapter.updateData(productAssetList, currentQuery)
                } else {
                    adapter.appendData(productAssetList, currentQuery)
                }
                cachedItems.clear()
                cachedItems.addAll(adapter.getFullList())

                isLoading = false
                if (productAssetList.size < pageSize) {
                    isLastPage = true
                } else {
                    currentPage = page
                }

                showEmptyIfNeeded()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "loadProductAssetNotes cancelled")
                    return@launch
                }
                isLoading = false
                progressBar.visibility = View.GONE
                progressBarSearch.visibility = View.INVISIBLE
                contentLayout.visibility = View.VISIBLE
                showEmptyIfNeeded()
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
        if (::adapter.isInitialized) {
            cachedItems.clear()
            cachedItems.addAll(adapter.getFullList())
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

            // 2) or it might be provided as a string (possibly serialized JSON like {"id":4281,"display_name":"TOOLS"})
            val prodStr = json.optString("product_id", json.optString("product", "")).trim()
            if (prodStr.isNotEmpty()) {
                if (prodStr.startsWith("{")) {
                    try {
                        val parsed = org.json.JSONObject(prodStr)
                        val displayName = parsed.optString("display_name", parsed.optString("name", ""))
                        if (displayName.isNotEmpty()) return displayName
                    } catch (_: Exception) { }
                }

                // If it contains a colon like "123: Name", take the part after colon
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
