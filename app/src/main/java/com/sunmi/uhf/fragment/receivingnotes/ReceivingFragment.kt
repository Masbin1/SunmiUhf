package com.sunmi.uhf.fragment.receivingnotes

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
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.ApiHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.appcompat.widget.AppCompatEditText
import android.view.inputmethod.InputMethodManager

class ReceivingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReceivingAdapter
    private lateinit var contentLayout: LinearLayout
    private lateinit var editSearch: AppCompatEditText

    // Pagination state
    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var isLastPage = false

    // Track already seen item IDs to prevent duplicates when server returns repeated data
    private val seenIds = mutableSetOf<Int>()

    private var searchJob: Job? = null
    private var currentQuery: String = ""
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receiving_notes, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewReceiving)
        progressBar = view.findViewById(R.id.progressBarReceiving)
        contentLayout = view.findViewById(R.id.contentLayoutReceiving)
        editSearch = view.findViewById(R.id.editSearchReceiving)

        adapter = ReceivingAdapter(mutableListOf()) { item ->
            val fragment = ReceivingDetailFragment.newInstance(item)
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
                        loadReceivingNotes(page = currentPage + 1, query = currentQuery)
                    }
                }
            }
        })

        // Search listener
        editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (q != currentQuery) {
                        currentQuery = q
                        loadJob?.cancel()
                        loadReceivingNotes(page = 1, query = currentQuery)
                    } else {
                        adapter.filter(currentQuery)
                    }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadReceivingNotes(page = 1)
        return view
    }

    private fun loadReceivingNotes(page: Int = 1, query: String = "") {
        loadJob?.cancel()

        if (page == 1) {
            val hadFocus = editSearch.hasFocus()
            val selPos = try { editSearch.selectionStart.coerceAtLeast(0) } catch (_: Exception) { -1 }

            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.VISIBLE
            contentLayout.isEnabled = false
            contentLayout.alpha = 0.6f

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
                    } catch (_: Exception) {}
                }
            }

            seenIds.clear()
            isLastPage = false
            currentPage = 1
        } else {
            progressBar.visibility = View.VISIBLE
        }

        isLoading = true
        loadJob = lifecycleScope.launch {
            try {
                val useCache = page == 1
                val offset = (page - 1) * pageSize
                val queryParam = if (query.isNotBlank()) "&search=${java.net.URLEncoder.encode(query, "UTF-8")}" else ""
                val url = "${AuthUtils.getServerUrl()}/get/stock/picking/receiving?offset=$offset&limit=$pageSize$queryParam"
                val jsonObject = ApiHelper.getJsonObject(
                    url,
                    useCache = useCache
                )

                if (jsonObject.getString("status") == "success") {
                    val jsonArray = jsonObject.getJSONArray("pickings")
                    val receivingList = mutableListOf<ReceivingItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        if (seenIds.contains(id)) continue

                        seenIds.add(id)
                        receivingList.add(
                            ReceivingItem(
                                id = id,
                                name = obj.getString("name"),
                                scheduledDate = obj.optString("scheduled_date", "-"),
                                partnerName = obj.optString("partner_name", "-"),
                                state = obj.optString("state", "-")
                            )
                        )
                    }
                    Log.d(TAG, "fetched total=${jsonArray.length()} new=${receivingList.size} seenTotal=${seenIds.size}")

                    progressBar.visibility = View.GONE
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
                    if (receivingList.isEmpty() && page > 1) {
                        isLastPage = true
                        isLoading = false
                        return@launch
                    }

                    if (page == 1) {
                        adapter.updateData(receivingList)
                    } else {
                        adapter.appendData(receivingList)
                    }

                    isLoading = false
                    if (receivingList.size < pageSize) {
                        isLastPage = true
                    } else {
                        currentPage = page
                    }

                    if (currentQuery.isNotBlank()) adapter.filter(currentQuery)
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
        private const val TAG = "ReceivingFragment"

        fun newInstance(nothing: Nothing?) = ReceivingFragment()
    }
}
