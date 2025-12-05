package com.sunmi.uhf.fragment.asset

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sunmi.uhf.BuildConfig
import com.sunmi.uhf.R
import com.sunmi.uhf.base.BaseActivity
import com.sunmi.uhf.fragment.operation.LabelOperationFragment
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.ApiHelper
import com.sunmi.uhf.service.OdooApiClient
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AssetFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AssetAdapter
    private lateinit var contentLayout: LinearLayout

    private lateinit var btnScan: FloatingActionButton

    private var shouldRefreshOnResume = false
    private var selectedUserId: Int? = null
    private var selectedUserName: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asset_order, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewAsset)
        progressBar = view.findViewById(R.id.progressBarAsset)
        contentLayout = view.findViewById(R.id.contentLayoutAsset)
        btnScan = view.findViewById(R.id.btnScan)

        adapter = AssetAdapter(emptyList()) { item ->
            val fragment = AssetDetailFragment.newInstance(item)
            (activity as? BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnScan.setOnClickListener {
            fetchAndShowUserDialog()
        }

        loadAssetOrders()
        return view
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            shouldRefreshOnResume = false
            loadAssetOrders()
        }
    }

    private fun fetchAndShowUserDialog() {
        lifecycleScope.launch {
            try {
                val userArray = ApiHelper.getJsonArray(
                    "${AuthUtils.getServerUrl()}/get/users",
                    useCache = true,
                    arrayKey = "users"
                )
                
                val userNames = mutableListOf<String>()
                val userIds = mutableListOf<Int>()

                for (i in 0 until userArray.length()) {
                    val user = userArray.getJSONObject(i)
                    userIds.add(user.getInt("id"))
                    userNames.add(user.getString("name"))
                }

                showUserSelectionDialog(userIds, userNames)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUserSelectionDialog(userIds: List<Int>, userNames: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, userNames)
        AlertDialog.Builder(requireContext())
            .setTitle("Select User")
            .setAdapter(adapter) { _, which ->
                selectedUserId = userIds[which]
                selectedUserName = userNames[which]
                goToTakeInventory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToTakeInventory() {
        val args = Bundle().apply {
            putInt(TakeInventoryFragment.ARC_KEY_ASSET_ID, 0)
            selectedUserId?.let { putInt("selected_user_id", it) }
            selectedUserName?.let { putString("selected_user_name", it) }
        }
        val fragment = TakeInventoryFragment.newInstance(args)
        (activity as? BaseActivity<*>)?.switchFragment(
            fragment,
            addToBackStack = true,
            clearStack = false
        )
    }

    private fun loadAssetOrders() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val jsonObject = ApiHelper.getJsonObject(
                    "${AuthUtils.getServerUrl()}/get/asset",
                    useCache = true
                )
                
                if (jsonObject.getString("status") == "success") {
                    val jsonArray = jsonObject.getJSONArray("assets")
                    val AssetList = mutableListOf<AssetItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        AssetList.add(
                            AssetItem(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                dueDate = obj.getString("due_date"),
                                partnerName = obj.optString("partner_name", "-"),
                                state = obj.getString("state")
                            )
                        )
                    }

                    progressBar.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE
                    adapter.updateData(AssetList)
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
        fun newInstance(args: Bundle?) = AssetFragment()
            .apply { arguments = args }
    }

    private fun handleAssetScanResult(rfids: List<String>) {
        if (rfids.isEmpty()) return
        shouldRefreshOnResume = true
        Toast.makeText(requireContext(), "Asset processed successfully", Toast.LENGTH_SHORT).show()
    }
}
