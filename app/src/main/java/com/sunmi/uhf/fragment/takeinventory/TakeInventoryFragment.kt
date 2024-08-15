package com.sunmi.uhf.fragment.takeinventory

import BatchItem
import DeliveryItemList
import StockPickingItem
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunmi.rfid.RFIDManager
import com.sunmi.rfid.constant.CMD
import com.sunmi.rfid.constant.ParamCts
import com.sunmi.rfid.entity.DataParameter
import com.sunmi.uhf.App
import com.sunmi.uhf.MainActivity
import com.sunmi.uhf.R
import com.sunmi.uhf.adapter.LabelInfoAdapter
import com.sunmi.uhf.adapter.TakeModelAdapter
import com.sunmi.uhf.bean.LabelInfoBean
import com.sunmi.uhf.constants.Config
import com.sunmi.uhf.constants.Constant
import com.sunmi.uhf.constants.EventConstant
import com.sunmi.uhf.databinding.FragmentTakeInventoryBinding
import com.sunmi.uhf.dialog.SureBackDialog
import com.sunmi.uhf.event.SimpleViewEvent
import com.sunmi.uhf.fragment.ReadBaseFragment
import com.sunmi.uhf.utils.*
import com.sunmi.uhf.view.RecycleDivider
import com.sunmi.widget.dialog.InputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

import okhttp3.*
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import kotlinx.coroutines.*


/**
 * @ClassName: TakeInventoryFragment
 * @Description: 盘存 页面
 * @Author: clh
 * @CreateDate: 20-9-9 下午1:38
 * @UpdateDate: 20-9-9 下午1:38
 */
class TakeInventoryFragment : ReadBaseFragment<FragmentTakeInventoryBinding>() {
    private lateinit var model: TakeInventoryModel
    private var batchItem: BatchItem? = null
    private var dialog: SureBackDialog? = null
    lateinit var vm: TakeInventoryModel
    private var isLoop = false
    private var allCount = 0
    private val list = mutableListOf<LabelInfoBean>()
    private lateinit var adapter: LabelInfoAdapter
    private var takeModelPw: PopupWindow? = null
    private var modelAdapter: TakeModelAdapter? = null
    private var exportExcelType = 0
    private var mode = Constant.INT_BALANCE_MODE
    private var tagFocus = Config.DEF_TAKE_TAG_FOCUS
    private var seesion = Config.DEF_TAKE_SESSION
    private var tagFlag = Config.DEF_TAKE_FLAG
    private var link = Config.DEF_TAKE_LINK
    private var power = 30
    private var rate = -1
    private var autoPower = Config.DEF_TAKE_AUTO_POWER
    private lateinit var stockPickingList: List<StockPickingItem>
    private lateinit var deliveryItemList: List<DeliveryItemList>
    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ParamCts.BROADCAST_SN -> {
                    val sn = intent.getStringExtra(ParamCts.SN)
                    handleSN(sn)
                }
                ParamCts.BROADCAST_BATTERY_REMAINING_PERCENTAGE,
                ParamCts.BROADCAST_BATTER_LOW_ELEC -> {
                    val elec = intent.getIntExtra(ParamCts.BATTERY_REMAINING_PERCENT, 100)
                    LogUtils.d("darren", "BroadcastReceiver battery-remaining-percent:$elec%")
                    if (elec <= Config.LOW_ELEC) {
                        showShort(getString(R.string.hint_please_charge, elec))
                    }
                }
                ParamCts.BROADCAST_ON_CONNECT,
                ParamCts.BROADCAST_READER_BOOT -> {
                    handleData()
                }
            }
        }
    }

    override fun getLayoutResource() = R.layout.fragment_take_inventory

    override fun initVM() {
        vm = getViewModel(TakeInventoryModel::class.java)
        binding.vm = vm
    }

    override fun initView() {
        adapter = LabelInfoAdapter()
        binding.labelRv.layoutManager = LinearLayoutManager(activity)
        binding.labelRv.adapter = adapter
        binding.labelRv.addItemDecoration(
            RecycleDivider(
                activity,
                RecycleDivider.HORIZONTAL_LIST,
                resources.getDimensionPixelSize(R.dimen.sunmi_1px),
                ContextCompat.getColor(App.mContext, R.color.dividerColor)
            )
        )
    }

    override fun initData() {
        super.initData()
        adapter.setNewInstance(list)
        vm.topSearchEn.value = !list.isNullOrEmpty()
        vm.start.observe(viewLifecycleOwner, Observer { startStop(it) })
        vm.editModel.observe(viewLifecycleOwner, Observer {
            adapter.editable = it
            adapter.notifyDataSetChanged()
        })
        vm.selectModel.observe(viewLifecycleOwner, Observer {
            modelAdapter?.selected = it
            modelAdapter?.notifyDataSetChanged()
        })
        vm.selectAll.observe(viewLifecycleOwner, Observer {
            if (adapter.selectAll == it) return@Observer
            adapter.selectAll = it
            if (it) {
                if (list.size != adapter.selectData.size) {
                    for (b in list) {
                        adapter.selectData[b.epc!!] = b
                    }
                }
            } else {
                adapter.selectData.clear()
            }
            adapter.notifyDataSetChanged()
            handleBottomStatus()
        })
        adapter.selectAllCall = object : ((Boolean) -> Unit) {
            override fun invoke(en: Boolean) {
                if (isVisible) {
                    vm.selectAll.value = en
                }
            }
        }
        adapter.clickCall = object : (() -> Unit) {
            override fun invoke() {
                if (isVisible) {
                    handleBottomStatus()
                }
            }
        }
        registerBr()
        App.getPref().apply {
            mode = getParam(Config.KEY_TAKE_MODE, Config.DEF_TAKE_MODE)
            tagFocus = getParam(Config.KEY_TAKE_TAG_FOCUS, Config.DEF_TAKE_TAG_FOCUS)
            seesion = getParam(Config.KEY_TAKE_SESSION, Config.DEF_TAKE_SESSION)
            tagFlag = getParam(Config.KEY_TAKE_FLAG, Config.DEF_TAKE_FLAG)
            link = getParam(Config.KEY_TAKE_LINK, Config.DEF_TAKE_LINK)
            autoPower = getParam(Config.KEY_TAKE_AUTO_POWER, Config.DEF_TAKE_AUTO_POWER)
            handleData()
        }
        vm.selectModel.value = vm.modelList[mode - 1]
        RFIDManager.getInstance().apply {
            if (isConnect()) {
                when (getHelper()?.getScanModel()) {
                    RFIDManager.UHF_R2000, RFIDManager.UHF_S7100 -> {
                        vm.labelVisibility.postValue(true)
                    }
                    RFIDManager.INNER -> {
                        vm.labelVisibility.postValue(false)
                    }
                }
            }
        }
    }

    override fun onSimpleViewEvent(event: SimpleViewEvent) {
        super.onSimpleViewEvent(event)
        when (event.event) {
            EventConstant.EVENT_BACK -> {
                if (isLoop) {
                    showSureDialog()
                } else {
                    performBackClick()
                }
            }
            EventConstant.EVENT_TAKE_MODEL -> {
                showModelPopupWindow()
            }
            EventConstant.EVENT_TAKE_MODEL_SEARCH -> {
                if (list.size == 0) {
                    mainScope.launch { showShort(getString(R.string.please_take_inventory_before_proceeding)) }
                    return
                }
                val bundle = Bundle().apply {
                    putParcelableArrayList(Constant.KEY_TAG_LIST, ArrayList<LabelInfoBean>(list))
                }
                switchFragment(
                    SearchModelFragment.newInstance(bundle),
                    addToBackStack = true,
                    clearStack = false
                )
            }
            EventConstant.EVENT_INVENTORY_COPY_EPC -> {
                copyEpcToClipboard()
            }
            EventConstant.EVENT_INVENTORY_SHARE -> {
                shareToApp()
            }
            EventConstant.EVENT_INVENTORY_EXPORT_EXCEL -> {
                exportExcelType = 1
                if (adapter.selectData.size == 0) {
                    mainScope.launch { showShort(getString(R.string.please_take_select_before_proceeding)) }
                    return
                }
                exportExcel()
            }
            EventConstant.EVENT_INVENTORY_EXPORT_EXCEL_ALL -> {
                exportExcelType = 0
                if (list.size == 0) {
                    mainScope.launch { showShort(getString(R.string.please_take_inventory_before_proceeding)) }
                    return
                }
                exportExcel()
            }
            EventConstant.EVENT_TAKE_LABEL_INFO -> {

            }
        }
    }

    fun handleBottomStatus() {
        vm.editEnExport.postValue(adapter.selectData.size > 0)
    }

    /**
     * 弹出显示 盘存模式列表
     */
    private fun showModelPopupWindow() {
        if (takeModelPw == null) {
            val root = View.inflate(context, R.layout.pop_take_model, null)
            val recyclerView = root.findViewById<RecyclerView>(R.id.take_mode_rv)
            modelAdapter = TakeModelAdapter()
            takeModelPw = PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.isOutsideTouchable = true
                modelAdapter?.setNewInstance(vm.createModel())
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = modelAdapter
                modelAdapter?.setOnItemClickListener { _, _, position ->
                    vm.selectModel.value = modelAdapter?.data?.get(position)
                    if (position in 0..3) {
                        mode = position + 1
                        rate = -1
                        handleData()
                    }
                    dismiss()
                }
            }
        }
        takeModelPw?.showAsDropDown(
            binding.filterLl.takeInventoryModelValueTv, 0,
            resources.getDimensionPixelSize(R.dimen.sunmi_8px)
        )
    }

    /**
     * 复制EPC到剪贴板
     */
    private fun copyEpcToClipboard() {
        mainScope.launch(Dispatchers.IO) {
            if (adapter.selectData.size == 0) {
                mainScope.launch { showShort(getString(R.string.please_take_select_before_proceeding)) }
                return@launch
            }
            val info = StringBuffer()
            for (epc in adapter.selectData.keys) {
                if (info.isNotEmpty()) {
                    info.append("\n")
                }
                info.append(epc)
            }
            LogUtils.i("darren", "copy to clipboard: $info")
            mainScope.launch {
                ClipboardUtils.copyStrToClipboard(context, info.toString())
                showShort(getString(R.string.hint_copy_epc_clipboard))
            }
        }
    }

    /**
     * 分享到其他App
     */
    private fun shareToApp() {
        mainScope.launch(Dispatchers.IO) {
            if (adapter.selectData.size == 0) {
                mainScope.launch { showShort(getString(R.string.please_take_select_before_proceeding)) }
                return@launch
            }
            var dir = App.mContext.externalCacheDir ?: App.mContext.cacheDir
            val data = ArrayList<LabelInfoBean>(adapter.selectData.values)
            var file = ExcelUtils.writeTagToExcel("${dir.absolutePath}/tagList", data)
            mainScope.launch {
                ShareUtils.shareFile(activity, file)
            }
        }
    }

    /**
     *  导出Excel 权限/文件名
     *
     *  @param type 类型 0：全部，1：选择的
     */
    private fun exportExcel() {
        context?.let {
            if (ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_ID)
                showShort(R.string.please_allow_read_write_sd_card)
                return
            }
        }
        val dialog = InputDialog.Builder()
            .setTitle(getString(R.string.please_input_file_name))
            .setHint(getString(R.string.please_input_file_name))
            .setEditType(true)
            .setLeftText(getString(R.string.cancel_text))
            .setRightText(getString(R.string.sure_text))
            .build(context)
        dialog.setCallback(object : InputDialog.DialogOnClickCallback {
            override fun left(text: String?) {
                dialog.cancel()
            }

            override fun middle(text: String?) {
            }

            override fun right(text: String?) {
                LogUtils.i("darren", "file name: $text")
                if (text != null) {
                    if (text.trim().isEmpty()) {
                        dialog.inputError()
                        return
                    } else {
                        //exportExcel(text, Environment.getExternalStorageDirectory().absolutePath)
                        exportExcel(text, context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString())
                        dialog.dismiss()
                    }
                } else {
                    dialog.inputError()
                }
            }
        })
        dialog.show()
    }

    /**
     * 保存Excel文件到SD卡
     */
    private fun exportExcel(fileName: String, path: String) {
        mainScope.launch(Dispatchers.IO) {
            LogUtils.i("darren", "export Excel dir:$path")
            val file = "$path/$fileName"
            val data = ArrayList<LabelInfoBean>()
            if (exportExcelType == 0) {
                data.addAll(list)
            } else if (exportExcelType == 1) {
                data.addAll(adapter.selectData.values)
            }
            if (data.size == 0) {
                return@launch
            }
            ExcelUtils.writeTagToExcel(file, data)
            mainScope.launch {
                showShort(getString(R.string.hint_excel_save_to_sd))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_ID) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportExcel()
            } else {
                showShort(R.string.please_allow_read_write_sd_card)
            }
        }
    }

    /**
     * 点击返回健后，弹出二次确认弹窗
     * 点击退出 ，退出页面
     */
    private fun showSureDialog() {
        val showing = (dialog?.isAdded ?: false || dialog?.dialog?.isShowing ?: false)
        if (showing) dialog?.dismiss()
        if (dialog == null) {
            dialog = SureBackDialog.newInstance(null)
        }
        dialog?.listener = object : (() -> Unit) {
            override fun invoke() {
                dialog?.dismiss()
                stop()
                handler.post(Runnable { performBackClick() })
            }
        }
        dialog?.show(parentFragmentManager, SureBackDialog::class.java.name)
    }

    override fun onBackPress(): Boolean {
        if (isLoop) {
            showSureDialog()
            return true
        }
        return super.onBackPress()
    }

    private fun registerBr() {
        context?.registerReceiver(br, IntentFilter().apply {
            addAction(ParamCts.BROADCAST_SN)
            addAction(ParamCts.BROADCAST_BATTERY_REMAINING_PERCENTAGE)
            addAction(ParamCts.BROADCAST_BATTER_LOW_ELEC)
            addAction(ParamCts.BROADCAST_ON_CONNECT)
            addAction(ParamCts.BROADCAST_READER_BOOT)
        })
        RFIDManager.getInstance().apply {
            if (isConnect()) getHelper()?.getReaderSN()
        }
    }

    private fun unregisterBr() {
        context?.unregisterReceiver(br)
    }

    override fun onPause() {
        startStop(false)
        vm.start.postValue(false)
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
        takeModelPw?.dismiss()
        unregisterBr()
    }


    private fun startStop(en: Boolean) {
        if (isLoop == en) return
//        vm.start.postValue(en)
        if (en) {
            tidList.clear()
            tagList.clear()
            list.clear()
            allCount = 0
            rate = -1
            vm.labelNum.value = 0
            vm.totalNum.value = 0
            vm.speed.value = 0
            binding.basicLl.timeValueTv.base = SystemClock.elapsedRealtime()
            binding.basicLl.timeValueTv.start()
            notifyTagDataChange()
            start()
        } else {
            binding.basicLl.timeValueTv.stop()
            stop()
        }
    }

    private fun getPowerSave(): Byte {
        try {
            val time = (SystemClock.elapsedRealtime() - binding.basicLl.timeValueTv.base) / 1000 / 60
            return if (autoPower && time > 10) {
                min((time - 10) * 5, 100).toByte()
            } else {
                0.toByte()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.toByte()
    }

    override fun handleBottomStart() {
        vm.start.value = true
    }

    override fun handleBottomStop() {
        vm.start.value = false
    }

    override fun start() {
        super.start()
        if (!isLoop) {
            RFIDManager.getInstance().getHelper()?.apply {
                when (App.getPref().getParam(Config.KEY_LABEL, Config.DEF_LABEL)) {
                    1 -> {
                        // 6C标签盘存
                        registerReaderCall(call)
                        when (mode) {
                            Constant.INT_BALANCE_MODE -> {
                                customizedSessionTargetInventory(
                                    0x01.toByte(),
                                    0x00.toByte(),
                                    0x00.toByte(),
                                    0x00.toByte(),
                                    getPowerSave(),
                                    1
                                )
                            }
                            Constant.INT_SPEED_MODE -> {
                                realTimeInventory(1)
                            }
                            Constant.INT_ITERATOR_MODE -> {
                                customizedSessionTargetInventory(
                                    0x02.toByte(),
                                    0x00.toByte(),
                                    0x00.toByte(),
                                    0x00.toByte(),
                                    getPowerSave(),
                                    1
                                )
                            }
                            Constant.INT_CUSTOM_MODE -> {
                                setOutputAllPower(App.getPref().getParam(Config.KEY_RF_POWER, Config.DEF_INNER_POWER_MAX).toByte())
                                customizedSessionTargetInventory(
                                    seesion.toByte(),
                                    tagFlag.toByte(),
                                    0x00.toByte(),
                                    0x00.toByte(),
                                    getPowerSave(),
                                    1
                                )
                            }
                        }
                        isLoop = true
                    }
                    else -> {
                        LogUtils.e("darren", "error label index")
                    }
                }
            }
        }
    }

    override fun stop() {
        super.stop()
        if (isLoop) {
            RFIDManager.getInstance().getHelper()?.apply {
                inventory(1)
                unregisterReaderCall()
                isLoop = false
            }
        }
    }

    override fun onCallSuccess(cmd: Byte, params: DataParameter?) {
        when (cmd) {
            CMD.REAL_TIME_INVENTORY,
            CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY -> {
                isLoop = false
                if (state) {
                    start()
                }
                if (params != null) {
                    rate = params.getInt(ParamCts.READ_RATE, -1)
                    rate = if (rate == 0) -1 else rate
                    notifyTagDataChange()
                }
            }
            /*CMD.ISO18000_6B_INVENTORY -> {
                isLoop = false
                if (state) {
                    start()
                }
            }*/
            CMD.SET_OUTPUT_POWER -> {
                LogUtils.i("darren", "set out power success.")
            }
            CMD.SET_AND_SAVE_IMPINJ_FAST_TID_TAG_FOCUS -> {
                LogUtils.i("darren", "set tag focus success.")
            }
            CMD.SET_RF_LINK_PROFILE -> {
                LogUtils.i("darren", "set rf link profile success.")
            }
            else -> {
                LogUtils.d("darren", "other success.")
            }
        }
    }

    object TemporaryStorage {
        private val epcList = mutableListOf<String>()

        fun addEpc(epc: String) {
            if (!epcList.contains(epc)) {
                epcList.add(epc)
            }
        }

        fun getAllEpcs(): List<String> {
            return epcList.toList()
        }

        fun clearEpcs() {
            epcList.clear()
        }

        fun isEmpty(): Boolean {
            return epcList.isEmpty()
        }
    }

    private fun updateSendDataRepairVisibility(sendDataRepair: TextView) {
        if (TemporaryStorage.isEmpty() or stockPickingList.isNotEmpty() ) {
            sendDataRepair.visibility = View.INVISIBLE
        } else {
            sendDataRepair.visibility = View.VISIBLE
        }
    }


    data class RfidStatus(val idLine: Int, val rfid: String, val status: String, val quantity_done: Float)

    private fun sendDataDeliverToOdoo() {
        val listOfRfidTemporary = TemporaryStorage.getAllEpcs()
        val listDeliveryOrder = deliveryItemList

        val matchedRfidListDelivery = listDeliveryOrder.map { deliveryItemList ->
            if (listOfRfidTemporary.contains(deliveryItemList.rfid)) {
                RfidStatus(deliveryItemList.idLine, deliveryItemList.rfid, "match", 1.0F)
            } else {
                RfidStatus(deliveryItemList.idLine, deliveryItemList.rfid, "not_match", 0.0F)
            }
        }

        sendDeliverToServer(matchedRfidListDelivery)
    }

    private fun sendDataPickingToOdoo() {
        val listOfRfidTemporary = TemporaryStorage.getAllEpcs()
        val listStockPicking = stockPickingList

        val matchedRfidList = listStockPicking.map { stockPickingItem ->
            if (listOfRfidTemporary.contains(stockPickingItem.rfid)) {
                RfidStatus(stockPickingItem.idLine, stockPickingItem.rfid, "match", 1.0F)
            } else {
                RfidStatus(stockPickingItem.idLine, stockPickingItem.rfid, "not_match", 0.0F)
            }
        }

        sendToServer(matchedRfidList)
    }

    private fun sendDeliverToServer(matchedRfidListDelivery: List<RfidStatus>) {
        val formBuilder = FormBody.Builder()
        val client = OkHttpClient()
        val url = "https://infinite-suitable-quetzal.ngrok-free.app/delivery/order"

        matchedRfidListDelivery.forEachIndexed { index, rfidStatus ->
            formBuilder.add("idLine[$index]", rfidStatus.idLine.toString())
            formBuilder.add("rfid[$index]", rfidStatus.rfid)
            formBuilder.add("status[$index]", rfidStatus.status)
            formBuilder.add("quantity_done[$index]", rfidStatus.quantity_done.toString())
        }

        val requestBody = formBuilder.build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("Sending request to server: $url with data: $matchedRfidListDelivery")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                println("Raw response: $responseBody")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        println("Response successful: ${response.code}")
                        handleSuccessResponse(responseBody)
                        activity?.runOnUiThread {
                            Toast.makeText(activity, responseBody, Toast.LENGTH_LONG).show()
                            TemporaryStorage.clearEpcs()
                            deliveryItemList = emptyList()
                            (activity as? MainActivity)?.navigateToHomeFragment()
                        }
                    } else {
                        println("Error response: ${response.code} - ${response.message}")
                        handleErrorResponse(response.code)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    println("Network error: ${e.message}")
                    handleFailure(e)
                    activity?.runOnUiThread {
                        Toast.makeText(activity, "Network error", Toast.LENGTH_LONG).show()
                        TemporaryStorage.clearEpcs()
                        deliveryItemList = emptyList()
                        (activity as? MainActivity)?.navigateToHomeFragment()
                    }
                }
            }
        }
    }


    private fun sendToServer(matchedRfidList: List<RfidStatus>) {
        val formBuilder = FormBody.Builder()
        val client = OkHttpClient()
        val url = "https://infinite-suitable-quetzal.ngrok-free.app/arrive/product/picking"

        matchedRfidList.forEachIndexed { index, rfidStatus ->
            formBuilder.add("idLine[$index]", rfidStatus.idLine.toString())
            formBuilder.add("rfid[$index]", rfidStatus.rfid)
            formBuilder.add("status[$index]", rfidStatus.status)
            formBuilder.add("quantity_done[$index]", rfidStatus.quantity_done.toString())
        }

        val requestBody = formBuilder.build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        println("Server response: $responseBody") // Log respons server
                        handleSuccessResponse(responseBody)
                        activity?.runOnUiThread {
                            Toast.makeText(activity, responseBody, Toast.LENGTH_LONG).show()
                            TemporaryStorage.clearEpcs()
                            stockPickingList = emptyList()
                            // Navigate to HomeFragment
                            (activity as? MainActivity)?.let { mainActivity ->
                                mainActivity.navigateToHomeFragment()
                            }
                        }

                    } else {
                        println("Error response: ${response.code} - ${response.message}") // Log error
                        handleErrorResponse(response.code)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    println("Network error: ${e.message}") // Log network error
                    handleFailure(e)
                }
            }
        }
    }

    private fun handleSuccessResponse(responseBody: String?) {
        println("Data berhasil dikirim: $responseBody")
        // Tambahkan logika tambahan di sini jika diperlukan
    }

    private fun handleErrorResponse(code: Int) {
        println("Gagal mengirim data: $code")
        // Tambahkan logika penanganan error di sini
    }

    private fun handleFailure(e: Exception) {
        println("Request Failed: $e")
        // Tambahkan logika penanganan kegagalan di sini
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.sendDataEvent.observe(viewLifecycleOwner) {
            sendDataToOdoo()
        }
        model.sendDataEventPicking.observe(viewLifecycleOwner){
            sendDataPickingToOdoo()
        }
        model.sendDataEventDeliver.observe(viewLifecycleOwner){
            sendDataDeliverToOdoo()
        }
    }


    private fun sendDataToOdoo() {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("rfid_tags", TemporaryStorage.getAllEpcs().joinToString(","))
            .add("api_id", "repair_order_drbags")
            .build()

        val request = Request.Builder()
            .url("https://infinite-suitable-quetzal.ngrok-free.app/rfid/scan/repairing")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
                // You might want to update UI or log the error here
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // Handle unsuccessful response
                        throw IOException("Unexpected code $response")
                    }

                    val responseBody = response.body?.string()

                    activity?.runOnUiThread {
                        Toast.makeText(activity, responseBody, Toast.LENGTH_LONG).show()
                        TemporaryStorage.clearEpcs()

                        // Navigate to HomeFragment
                        (activity as? MainActivity)?.let { mainActivity ->
                            mainActivity.navigateToHomeFragment()
                        }
                    }
                }
            }
        })
    }


    override fun onCallTag(cmd: Byte, state: Byte, tag: DataParameter?) {
        if (tag == null) return
        playTips()
        when (cmd) {
            CMD.REAL_TIME_INVENTORY,
            CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY -> {
                // 6C标签盘存
                allCount++
                // ANT_ID、TAG_PC、TAG_EPC、TAG_RSSI、TAG_READ_COUNT、TAG_FREQ、TAG_TIME
                val epc = tag.getString(ParamCts.TAG_EPC) ?: ""
                TemporaryStorage.addEpc(epc)
                view?.findViewById<TextView>(R.id.send_data_repair)?.let {
                    updateSendDataRepairVisibility(it)
                }
                val pc = tag.getString(ParamCts.TAG_PC) ?: ""
                val rssi = "${(Integer.parseInt(tag.getString(ParamCts.TAG_RSSI, "129")) - 129)}"
                val freq = tag.getString(ParamCts.TAG_FREQ) ?: ""
                LogUtils.i("darren", "found tag:$epc => $tag")
                val index = tidList.indexOf(epc)
                if (index != -1) {
                    val c = tagList[index].getInt(ParamCts.TAG_READ_COUNT, 1) + 1
                    tag.put(ParamCts.TAG_READ_COUNT, c)
                    tagList[index] = tag
                    list[index] = LabelInfoBean(epc, pc, c, rssi, freq)
                } else {
                    tidList.add(0, epc)
                    tagList.add(0, tag)
                    list.add(0, LabelInfoBean(epc, pc, 1, rssi, freq))
                }
                notifyTagDataChange()
            }
            else -> {
                LogUtils.d("darren", "other found tag.")
            }
        }
    }

    override fun onCallFailed(cmd: Byte, errorCode: Byte, msg: String?) {
        when (cmd) {
            CMD.REAL_TIME_INVENTORY,
            CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY -> {
                isLoop = false
                if (state) {
                    start()
                }
            }
            /*CMD.ISO18000_6B_INVENTORY -> {
                isLoop = false
                if (state) {
                    start()
                }
            }*/
            CMD.SET_OUTPUT_POWER -> {
                LogUtils.i("darren", "set out power failed.")
            }
            CMD.SET_AND_SAVE_IMPINJ_FAST_TID_TAG_FOCUS -> {
                LogUtils.i("darren", "set tag focus failed.")
            }
            CMD.SET_RF_LINK_PROFILE -> {
                LogUtils.i("darren", "set rf link profile failed.")
            }
            else -> {
                LogUtils.d("darren", "other failed.")
            }
        }
    }

    private fun handleSN(sn: String?) {
        val rfBand = ParamCts.getRFFrequencyBand(sn ?: "")
        when (rfBand[3]) {
            1 -> {
                power = 30
            }
            2 -> {
                power = 28
            }
            3 -> {
                power = 29
            }
        }
        RFIDManager.getInstance().apply {
            if (isConnect() && rfBand[0] == 1) {
                getHelper()?.setOutputAllPower(power.toByte())
            }
        }
    }

    private fun handleData() {
        RFIDManager.getInstance().apply {
            if (isConnect()) {
                getHelper()?.apply {
                    registerReaderCall(call)
                    when (mode) {
                        Constant.INT_CUSTOM_MODE -> {
                            setRfLinkProfile((0xD0 + link).toByte())
                            setImpinjFastTid(seesion == 1 && tagFocus, false)
                        }
                        else -> {
                            setRfLinkProfile(0xD1.toByte())
                            setImpinjFastTid(mode == Constant.INT_BALANCE_MODE, false)
                        }
                    }
                    setMask1Tag()
                    setMask2Tag()
                }
            }
        }
    }

    private fun setMask1Tag() {
        var en: Boolean = App.getPref().getParam(Config.KEY_FILTER_ENABLE_1, false)
        if (!en) return
        App.getPref().setParam(Config.KEY_FILTER_ENABLE_1, en)
        RFIDManager.getInstance().apply {
            if (isConnect()) {
                if (en) {
                    val info = App.getPref().getParam(Config.KEY_FILTER_INFO_1, Config.DEF_FILTER_INFO)
                    val area = App.getPref().getParam(Config.KEY_FILTER_AREA_1, Config.DEF_FILTER_AREA)
                    val startAdd = App.getPref().getParam(Config.KEY_FILTER_START_ADD_1, Config.DEF_FILTER_START_ADD)
                    val rule = App.getPref().getParam(Config.KEY_FILTER_RULE_1, Config.DEF_FILTER_RULE)
                    val target = App.getPref().getParam(Config.KEY_FILTER_TARGET_1, Config.DEF_FILTER_TARGET)
                    val infoList = StrUtils.stringToStringArray(info, 2)
                    val maskValue = StrUtils.stringArrayToByteArray(infoList, infoList?.size ?: 0)
                    getHelper()?.setTagMask(
                        0x01,
                        target.toByte(),
                        rule.toByte(),
                        area.toByte(),
                        startAdd.toByte(),
                        ((maskValue?.size ?: 0) * 8).toByte(),
                        maskValue
                    )
                }
            }
        }
    }

    private fun setMask2Tag() {
        var en: Boolean = App.getPref().getParam(Config.KEY_FILTER_ENABLE_2, false)
        if (!en) return
        RFIDManager.getInstance().apply {
            if (isConnect()) {
                if (en) {
                    val info = App.getPref().getParam(Config.KEY_FILTER_INFO_2, Config.DEF_FILTER_INFO)
                    val area = App.getPref().getParam(Config.KEY_FILTER_AREA_2, Config.DEF_FILTER_AREA)
                    val startAdd = App.getPref().getParam(Config.KEY_FILTER_START_ADD_2, Config.DEF_FILTER_START_ADD)
                    val rule = App.getPref().getParam(Config.KEY_FILTER_RULE_2, Config.DEF_FILTER_RULE)
                    val target = App.getPref().getParam(Config.KEY_FILTER_TARGET_2, Config.DEF_FILTER_TARGET)
                    val infoList = StrUtils.stringToStringArray(info, 2)
                    val maskValue = StrUtils.stringArrayToByteArray(infoList, infoList?.size ?: 0)
                    getHelper()?.setTagMask(
                        0x02,
                        target.toByte(),
                        rule.toByte(),
                        area.toByte(),
                        startAdd.toByte(),
                        ((maskValue?.size ?: 0) * 8).toByte(),
                        maskValue
                    )
                }
            }
        }
    }


    private fun notifyTagDataChange() {
        mainScope.launch {
            vm.labelNum.value = tidList.size
            vm.totalNum.value = allCount
            vm.topSearchEn.value = !list.isNullOrEmpty()
            if (rate == -1) {
                val time = (SystemClock.elapsedRealtime() - binding.basicLl.timeValueTv.base) / 1000
                if (time < 1) {
                    vm.speed.value = allCount
                } else {
                    vm.speed.value = (allCount / time).toInt()
                }
            } else {
                vm.speed.value = rate
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this).get(TakeInventoryModel::class.java)
        arguments?.let {
            batchItem = it.getParcelable(ARG_PICKUP_ITEM)
        }
        arguments?.let {
            stockPickingList = it.getParcelableArrayList(ARG_STOCK_PICKING_LIST) ?: emptyList()
        }
        arguments?.let {
            deliveryItemList = it.getParcelableArrayList(ARG_DELIVERY_ITEM_LIST) ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Helper function to set TextView visibility based on a list's content
        fun setViewVisibility(view: View, condition: Boolean) {
            view.visibility = if (condition) View.VISIBLE else View.INVISIBLE
        }

        // Initialize views
        val pickupInfoTextView: TextView = view!!.findViewById(R.id.pickupInfoTextView)
        val cardViewPickupInfo: CardView = view.findViewById(R.id.cardViewPickupInfo)
        val canStockPickingMatchTextView: TextView = view.findViewById(R.id.can_stock_picking_match)
        val canDeliveryMatchTextView: TextView = view.findViewById(R.id.checkout_delivery_order)
        val sendDataRepair: Button = view.findViewById(R.id.send_data_repair)

        // Update pickupInfoTextView and cardViewPickupInfo based on batchItem
        batchItem?.let { item ->
            pickupInfoTextView.text = """
            ID Pickup: ${item.idBatch}
            Name: ${item.name}
            Responsible: ${item.responsible}
        """.trimIndent()
            cardViewPickupInfo.visibility = View.VISIBLE
        } ?: run {
            cardViewPickupInfo.visibility = View.GONE
        }

        // Update visibility based on stockPickingList and deliveryItemList
        setViewVisibility(canStockPickingMatchTextView, stockPickingList.isNotEmpty())
        setViewVisibility(canDeliveryMatchTextView, deliveryItemList.isNotEmpty())
        // Update visibility for sendDataRepair based on TemporaryStorage content
        updateSendDataRepairVisibility(sendDataRepair)

        return view
    }



    companion object {
        private const val ARG_PICKUP_ITEM = "pickup_item"
        fun newInstance(batchItem: BatchItem?) = TakeInventoryFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_PICKUP_ITEM, batchItem)
            }
        }
        private const val ARG_STOCK_PICKING_LIST = "stock_picking_list"
        fun newInstance(stockPickingList: List<StockPickingItem>): TakeInventoryFragment {
            val fragment = TakeInventoryFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_STOCK_PICKING_LIST, ArrayList(stockPickingList))
            fragment.arguments = args
            return fragment
        }

        private const val ARG_DELIVERY_ITEM_LIST = "delivery_item_list"

        fun newInstanceFromDelivery(deliveryItemOrderList: List<DeliveryItemList>): TakeInventoryFragment {
            val fragment = TakeInventoryFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_DELIVERY_ITEM_LIST, ArrayList(deliveryItemOrderList))
            fragment.arguments = args
            return fragment
        }

        const val REQUEST_PERMISSION_ID = 101
    }
}