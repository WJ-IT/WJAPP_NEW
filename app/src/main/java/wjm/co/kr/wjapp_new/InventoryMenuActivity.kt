package wjm.co.kr.wjapp_new

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.Toast

import wjm.co.kr.wjapp_new.bluetooth.DeviceListActivity
import wjm.co.kr.wjapp_new.databinding.ContentInventoryMenuBinding
import java.lang.Thread.sleep

class InventoryMenuActivity : AppCompatActivity() {
    private lateinit var binding: ContentInventoryMenuBinding
    private val TAG = "WjmAPP"
    private val D = true

    class SwingInfo {
        companion object {
            var mSwing : SwingAPI? = null
            var swing_ADDRESS = ""
            var swing_NAME = ""
        }
    }

    val TOAST = "toast"

    // Intent request codes
    private val REQUEST_CONNECT_DEVICE = 1
    private val REQUEST_ENABLE_BT = 2
    private val SCAN_GBN = 1

    val MESSAGE_STATE_CHANGE = 1
    val MESSAGE_READ = 2
    val MESSAGE_WRITE = 3
    val MESSAGE_DEVICE_NAME = 4
    val MESSAGE_TOAST = 5
    val MESSAGE_START = 6
    val MESSAGE_STOP = 7
    val MESSAGE_FOUND = 8
    val MESSAGE_TAG = 9
    val MESSAGE_BATTERY = 11
    val MESSAGE_POWER = 12
    val MESSAGE_POWERRANGE = 13

    var BTDevice: BluetoothDevice? = null
    private var BT_enable_onStart = false

    private var mchoice: Int = 0

    class bt_data {
        companion object {
            var BTadapter: BluetoothAdapter? = null
        }
    }

    class reading_loc {
        companion object {
            var topPage = "" //스캔화면(맨위화면)을 알기위한 변수
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_inventory_menu)
        binding = ContentInventoryMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setSupportActionBar(toolbar)

//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }

        if ( SwingInfo.mSwing != null) binding.btnBluetooth.setImageResource(R.drawable.blue_tooth_on)
        onStartBluetooth()

        binding.btnBluetooth.setOnClickListener(({
            if (SwingInfo.mSwing == null)  {
                if (bt_data.BTadapter == null) {
                    MyToast(resources.getString(R.string.bt_not_machine))
                }
                else if (BTDevice == null) {
                    if (bt_data.BTadapter!!.isDiscovering) {
                        bt_data.BTadapter!!.cancelDiscovery()
                    }
                    val intent = Intent(this, DeviceListActivity::class.java)
                    startActivityForResult(intent, REQUEST_CONNECT_DEVICE)
                }
            } else {
                MyToast(resources.getString(R.string.swing_already_conn))
                if (bt_data.BTadapter!!.isDiscovering) {
                    bt_data.BTadapter!!.cancelDiscovery()
                }
                val intent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE)
            }
        }))

        binding.btnScan.setOnClickListener(({
            if (SwingInfo.mSwing != null) {
                choiceDlg(SCAN_GBN)!!.show()
            } else MyToast(resources.getString(R.string.swing_noconn))
        }))

        binding.btnResult.setOnClickListener(({
            val intent = Intent(this, JegoScanResultActivity::class.java)
            startActivityForResult(intent, REQUEST_CONNECT_DEVICE)
        }))

        binding.btnSearch.setOnClickListener(({
            //            val intent_etc = Intent(baseContext, JegoFind::class.java)
//            startActivity(intent_etc)
        }))

        binding.btnVisit.setOnClickListener(({
            //            val intent_visit = Intent(baseContext, VisitSchedule::class.java)
//            startActivity(intent_visit)
        }))

        //물류실 재고조사
        binding.btnEtc.setOnClickListener(({
            val intent = Intent(this, JegoJosaActivity::class.java)
            startActivity(intent)
        }))

        val anim = AnimationUtils.loadAnimation(this, R.anim.logo)
        binding.mainLogo.animation = anim

    }

    private fun onStartBluetooth() {
        bt_data.BTadapter = BluetoothAdapter.getDefaultAdapter()
        if (bt_data.BTadapter == null) { //블루투스 기능이 없으면 리턴
            MyToast(resources.getString(R.string.bt_not_machine))
            return
        }

        BT_enable_onStart = bt_data.BTadapter!!.isEnabled()  //블루투스가 활성화되어있는가?
        if (!BT_enable_onStart) {    //활성화안되있다면...
            val BT_turnOn = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)  //활성화시킴
            startActivityForResult(BT_turnOn, 7001)
        }
    }

    //startActivityForResult 의 결과를가지고 코딩하는 부분
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CONNECT_DEVICE // 1
            ->
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {

                    // Get the device MAC address
                    val address = data!!.extras!!
                        .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
                    // Get the BLuetoothDevice object
                    BTDevice = bt_data.BTadapter!!.getRemoteDevice(address)
                    SwingInfo.swing_ADDRESS = BTDevice!!.name
                    //                    Toast.makeText(getBaseContext(),BTDevice.getAddress(),Toast.LENGTH_SHORT).show();
                    // Attempt to connect to the device
                    if (SwingInfo.mSwing == null) initSwing()
                    SwingInfo.mSwing!!.connect(BTDevice)
                }
            REQUEST_ENABLE_BT // 2
            ->
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //                    setupUI();
                } else {
                    // User did not enable Bluetooth or an error occured
                    //                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                MESSAGE_STATE_CHANGE -> {
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1)
                    when (msg.arg1) {
                        SwingAPI.STATE_CONNECTED -> {
                            binding.btnBluetooth.setImageResource(R.drawable.blue_tooth_on)
                            val toast = Toast.makeText(baseContext, "스윙 장비가 확인 되었습니다.", Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.BOTTOM, 0, 0)
                            toast.show()

                            if(SwingInfo.swing_NAME.contains("RFPrisma", true)) {
                                SwingInfo.mSwing!!.prisma_setInit()
                                sleep(20)
                                SwingInfo.mSwing!!.prisma_getBattery()
                            } else {
                                SwingInfo.mSwing!!.swing_setBuzzerVolume(1) //무음으로 설정
                                SwingInfo.mSwing!!.swing_setContinuous(true)
                                SwingInfo.mSwing!!.swing_setAllTagReport(false)
                            }
                        }
                        SwingAPI.STATE_CONNECTING -> {
                        }
                        SwingAPI.STATE_LISTEN, SwingAPI.STATE_NONE -> {
                        }
                    }
                }
                MESSAGE_WRITE -> if (D) Log.i(TAG, "MESSAGE_WRITE: " + msg.arg1)
                MESSAGE_TAG -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer

                    val readMessage = String(readBuf, 0, msg.arg1)

                    val inComming = TagItem(readMessage)
                    if (inComming.epcID_Ascii.isNotEmpty()) {
                        val strPre = inComming.epcID_Ascii.substring(0, 2)
                        //inComming.setEpcID_Ascii("OR1306002223");
                        //System.out.println("xxx" + topPage.toString());

                        //                        if((strPre.equals("RF") && !inComming.epcID_Ascii.trim().contains(" ")) || (strPre.equals("OR") && !inComming.epcID_Ascii.trim().contains(" ")))
                        if (strPre == "RF" && !inComming.epcID_Ascii.trim().contains(" ")) {
                            //                            mTagItem.add(inComming);
                            if (reading_loc.topPage == "JEGOSCAN") {        // 제품검색 일때
                                if (inComming.epcID_Ascii.substring(0, 3) != "RFR" && inComming.epcID_Ascii.substring(0, 2) != "OR") {
                                    JegoScanActivity.ReadingRFID.showDataJepum(inComming)
                                }
                            }
                            if (reading_loc.topPage.equals("JEGOINJOSA")) {        // 사내 비품/소모품 검색 일때
                                if (inComming.epcID_Ascii.substring(0, 3).equals("RFR"))
                                    JegoInJosaActivity.ReadingInJosaRFID.showDataJepum(inComming)
                            }
//                            if (topPage.equals("JEGOICLSCAN")) {
//                                JegoICLScan.ShowDataJepum(inComming)
//                            }
//                            if (topPage.equals("JEGOFIND")) {
//                                Toast.makeText(baseContext, inComming.getEpcID(), Toast.LENGTH_LONG).show()
//                                JegoFind.CountJepm(inComming)
//                            }
                            if (reading_loc.topPage == "JEGOJJSCAN") {        // 제품검색 일때
                                if (inComming.epcID_Ascii.substring(0, 3) != "RFR" && inComming.epcID_Ascii.substring(0, 2) != "OR")
                                    JegoJJScanActivity.ReadingRFID.showDataJepum(inComming)
                            }
                            if (reading_loc.topPage == "JEGOJOSA") {
                                if (inComming.epcID_Ascii.substring(0, 3) != "RFR" && inComming.epcID_Ascii.substring(0, 2) != "OR")
                                    JegoJosaActivity.ReadingJosaRFID.showDataJepum(inComming)
                            }
                        }
                    }
                }
                MESSAGE_DEVICE_NAME -> {
                    if (D) Log.i(TAG, "MESSAGE_DEVICE_NAME: " + msg.obj)
                    SwingInfo.swing_NAME = msg.obj as String
                }
                MESSAGE_BATTERY -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer

                    val readMessage = String(readBuf, 0, msg.arg1)
                    val ran = IntRange(4, 4)
                    if(reading_loc.topPage == "JEGOJOSA") {
                        JegoJosaActivity.ReadingJosaRFID.setBattery(readMessage.slice(ran))
                    } else if (reading_loc.topPage == "JEGOINJOSA") {
                        JegoInJosaActivity.ReadingInJosaRFID.setBattery(readMessage.slice(ran))
                    } else {
                        when (readMessage.slice(ran)) {
                            "0" -> Toast.makeText(baseContext, "리더기 즉시 충전해주세요", Toast.LENGTH_LONG)
                                .show()
                            "1" -> Toast.makeText(baseContext, "리더기 충전해주세요", Toast.LENGTH_LONG)
                                .show()
                            "2" -> Toast.makeText(baseContext, "리더기 배터리 40%이상", Toast.LENGTH_LONG)
                                .show()
                            "3" -> Toast.makeText(baseContext, "리더기 배터리 60%이상", Toast.LENGTH_LONG)
                                .show()
                            "4" -> Toast.makeText(baseContext, "리더기 배터리 80%이상", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
                MESSAGE_POWER -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer

                    val readMessage = String(readBuf, 0, msg.arg1)
                    val ran = IntRange(4, 7)
                    if(reading_loc.topPage == "JEGOJOSA") {
                        JegoJosaActivity.ReadingJosaRFID.getPower(readMessage.slice(ran))
                    }
                }
                MESSAGE_POWERRANGE -> { //000~300
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer

                    val readMessage = String(readBuf, 0, msg.arg1)
                    val ran = IntRange(4, 4)
                    if(reading_loc.topPage == "JEGOJOSA") {
                        JegoJosaActivity.ReadingJosaRFID.getPowerRange(readMessage.slice(ran))
                    }
                }
                MESSAGE_TOAST -> if (msg.getData().getString(TOAST).equals("장치 연결에 실패하였습니다.")) {
                    SwingInfo.mSwing!!.stop()
                    SwingInfo.mSwing = null
                    BTDevice = null
                    Toast.makeText(baseContext, "Swing기 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
                MESSAGE_START -> if (D) Log.i(TAG, "MESSAGE_START: " + msg.arg1)
                MESSAGE_STOP -> if (D) Log.i(TAG, "MESSAGE_STOP: " + msg.arg1)
            }
        }
    }

    private fun initSwing() {
        Log.d(TAG, "Initial Swing")
        SwingInfo.mSwing = SwingAPI(this, mHandler)
        try {
            //            mSwing.swing_multiReadStart();
            SwingInfo.mSwing!!.swing_readStop()
        } catch (e: UnsupportedOperationException) {
            if (D) Log.e(TAG, "ReadStartButton.setOnClinkListener", e)
        }

    }

    private fun choiceDlg(id: Int): Dialog? {
        when (id) {
            SCAN_GBN -> return AlertDialog.Builder(this@InventoryMenuActivity)
                .setIcon(R.drawable.wjicon)
                .setTitle(R.string.scan_dlg_title)
                .setSingleChoiceItems(R.array.choice_dlg, -1
                ) { _, which ->
                    mchoice = which
                }
                .setPositiveButton("선택"
                ) { _, _ ->
                    if (mchoice == 0) { //거래처 일반 스캔시
                        val intentScan = Intent(baseContext, JegoScanActivity::class.java)
                        startActivity(intentScan)
                    } else if (mchoice == 1) { //저장재고 스캔시
                        val intentScan = Intent(baseContext, JegoJJScanActivity::class.java)
                        startActivity(intentScan)
                    } else if (mchoice == 2) {
                        val intent_scan = Intent(baseContext, JegoInJosaActivity::class.java)
                        startActivity(intent_scan)
                    } else {
//                            val intent_scan = Intent(baseContext, JegoICLScan::class.java)
//                            startActivity(intent_scan)
                    }
                }
                .setNegativeButton("돌아가기"
                ) { dialog, _ -> dialog.dismiss() }
                .create()
        }
        return null
    }

    private fun MyToast(msg:String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

}
