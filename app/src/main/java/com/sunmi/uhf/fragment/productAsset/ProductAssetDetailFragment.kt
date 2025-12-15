package com.sunmi.uhf.fragment.productAsset

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sunmi.uhf.R
import com.sunmi.uhf.fragment.takeinventory.TakeInventoryFragment
import com.sunmi.uhf.utils.AuthUtils
import com.sunmi.uhf.service.OdooApiClient
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ProductAssetDetailFragment : Fragment() {

    private var productAssetId: Int = 0
    private var productAssetItem: ProductAssetItem? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var txtAssetName: TextView
    private lateinit var txtProductTemplate: TextView
    private lateinit var txtAssetCode: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtSerialNo: TextView
    private lateinit var txtRfid: TextView
    private lateinit var txtHeldBy: TextView

    private lateinit var btnScan: FloatingActionButton
    private lateinit var btnSaveProductAsset: FloatingActionButton

    private var shouldRefreshOnResume = false

    companion object {
        fun newInstance(id: Int): ProductAssetDetailFragment {
            val fragment = ProductAssetDetailFragment()
            val args = Bundle()
            args.putInt("productAsset_id", id)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(item: ProductAssetItem): ProductAssetDetailFragment {
            val fragment = ProductAssetDetailFragment()
            val args = Bundle()
            args.putInt("productAsset_id", item.id)
            args.putParcelable("productAsset_item", item)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productAssetId = arguments?.getInt("productAsset_id") ?: 0
        productAssetItem = arguments?.getParcelable("productAsset_item")
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume) {
            shouldRefreshOnResume = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_product_asset_detail, container, false)

        txtAssetName = view.findViewById(R.id.txtAssetName)
        txtProductTemplate = view.findViewById(R.id.txtProductTemplate)
        txtAssetCode = view.findViewById(R.id.txtAssetCode)
        txtCategory = view.findViewById(R.id.txtCategory)
        txtSerialNo = view.findViewById(R.id.txtSerialNo)
        txtRfid = view.findViewById(R.id.txtRfid)
        txtHeldBy = view.findViewById(R.id.txtHeldBy)
        progressBar = view.findViewById(R.id.progressBarProductAssetDetail)
        btnSaveProductAsset = view.findViewById(R.id.btnSaveProductAsset)

        // No update functionality here by default — hide save button
        btnSaveProductAsset.visibility = View.VISIBLE
        btnScan = view.findViewById(R.id.btnScan)

        btnScan.setOnClickListener {
            val args = Bundle().apply {
                putInt(TakeInventoryFragment.ARC_KEY_PRODUCT_ASSET_ID, productAssetId)
            }

            val fragment = TakeInventoryFragment.newInstance(args)
            fragment.setProductAssetScanResultListener { rfids ->
                handleProductAssetScanResult(rfids)
            }

            (activity as? com.sunmi.uhf.base.BaseActivity<*>)?.switchFragment(
                fragment,
                addToBackStack = true,
                clearStack = false
            )
        }

        // If fragment was created with a ProductAssetItem from the list, display basic info immediately
        if (productAssetItem != null) {
            displayHeaderImmediately()
        }

        // Always fetch latest asset detail from server (to get serial/rfid/held_by)
        loadAssetDetail()

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun displayHeaderImmediately() {
        productAssetItem?.let {
            txtAssetName.text = it.name
            txtProductTemplate.text = it.productName
            txtAssetCode.text = it.assetCode
            txtCategory.text = it.assetCategory

            // Unknown on list, leave empty until detail loads
            txtSerialNo.text = it.serialNo
            txtRfid.text = it.rfid
            txtHeldBy.text = it.heldBy
        }
    }

    private fun loadAssetDetail() {
        progressBar.visibility = View.VISIBLE

        val client = OdooApiClient.getClient()
        val request = Request.Builder()
            .url("${AuthUtils.getServerUrl()}/get/product/asset/detail/$productAssetId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                if (jsonData.isNullOrEmpty()) return

                try {
                    val jsonObj = JSONObject(jsonData)
                    if (jsonObj.getString("status") != "success") {
                        activity?.runOnUiThread {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Failed: ${jsonObj.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val assetObj = jsonObj.getJSONObject("asset")

                    val name = assetObj.optString("name", "")
                    val productTemplate = assetObj.optString("product_template_id", "")
                    val assetCode = assetObj.optString("asset_code", "")
                    val category = assetObj.optString("category_id", "")
                    val serialNo = assetObj.optString("serial_no", "")
                    val rfid = assetObj.optString("rfid", "")
                    val heldBy = assetObj.optString("held_by", "")

                    activity?.runOnUiThread {
                        txtAssetName.text = name
                        txtProductTemplate.text = productTemplate
                        txtAssetCode.text = assetCode
                        txtCategory.text = category
                        txtSerialNo.text = serialNo
                        txtRfid.text = rfid
                        txtHeldBy.text = heldBy

                        progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error parsing data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun handleProductAssetScanResult(rfids: List<String>) {
        if (rfids.isEmpty()) return

        // Set flag to refresh data when fragment resumes after scanning
        shouldRefreshOnResume = true

        Toast.makeText(requireContext(), "Processed ${rfids.size} RFIDs successfully", Toast.LENGTH_SHORT).show()
    }
}
