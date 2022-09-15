package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityJegoJosaBinding
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class JegoJosaActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityJegoJosaBinding
    private var cdloc = "WA"
    private var selCdPln = ""
    private var tagBefSize = 0
    private lateinit var mTimeHandler : Handler
    private var startOK = true
    private var batteryCount = 10
    private var sendCnt = 1
    private var tagArray : Array<String> = Array(10){""}

    //제품들 어뎁터와 리스트
    private var josaProductAdapter: JosaProductAdapter? = null
    private var josaProductList : ArrayList<JosaProducts> = ArrayList()

    //선택제품 어뎁터와 리스트
    private var josaItemAdapter: JosaItemAdapter? = null
    private var josaItemList: ArrayList<JosaItem> = ArrayList()

    //롱클릭 이벤트시 담을 어뎁터와 리스트
    private var josaDetailAdapter: CDialogJosaAdapter? = null
    private var josaDetailList: ArrayList<JosaDetailItem> = ArrayList()

    // SQLite DB관련
    private var db: SQLiteDatabase? = null
    private var dbName = "WJDB2"

    private lateinit var loadingDialog : LodingDialog

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingA = ActivityJegoJosaBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        jegoJosaInit()

        bindingA.bindingJjosa.btnJosaReaderInit.setOnClickListener(({
            if (bindingA.bindingJjosa.btnJosaReaderInit.text == "리더기 연결") {

                bindingA.bindingJjosa.btnJosaReaderInit.text = "리더기 초기화"
            } else {
                if (InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
                    InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
                } else {
                    Toast.makeText(this, "Prisma 리더기만 해당됩니다.", Toast.LENGTH_LONG).show()
                }
                println("xxxxxxxxxxxxxxx" + dbcount())
                println("xxxxxxxxxxxxxxx2" + ReadingJosaRFID.mTagItem.size)
            }
        }))

        bindingA.bindingJjosa.btnJosaTagInit.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("화면/리딩 초기화")
                .setMessage("스캔한 데이타를 초기화 하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("확인") { _, _ ->
                    ReadingJosaRFID.mTagItem.clear()
                    josaItemList.clear()
                    josaItemAdapter!!.notifyDataSetChanged()
                    tagBefSize = 0
                    dbdelete("ALL")
                }
                .setNegativeButton("취소", null).show()
        }))

        bindingA.bindingJjosa.cbJosaDiff.setOnClickListener(({
            josaItemAdapter!!.notifyDataSetChanged()
        }))

        bindingA.bindingJjosa.btnJosaSendResult.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("중간전송")
                .setMessage("스캔한 데이타를 전송하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("확인") { _, _ ->
                    makeSendTagList("ALLTAG")
                    for (i in 0 until sendCnt+1) {
                        sendTagReadingResult(tagArray[i], i+1)
                    }
                }
                .setNegativeButton("취소", null).show()
        }))

        bindingA.bindingJjosa.btnJosaSendTotResult.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("완료전송")
                .setMessage("해당 제품 완료 데이타를 생성하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("확인") { _, _ ->
                    sendTagTotResult()
                }
                .setNegativeButton("취소", null).show()
        }))

        bindingA.bindingJjosa.btnRfPowerUp.setOnClickListener(({
            setRfPower("UP")
        }))

        bindingA.bindingJjosa.btnRfPowerDown.setOnClickListener(({
            setRfPower("DOWN")
        }))
        //DB관련
        db = openOrCreateDatabase(dbName, MODE_PRIVATE, null)
    }


    private fun jegoJosaInit() {
        // 리딩결과 값을 받을 화면을 알려준다
        InventoryMenuActivity.reading_loc.topPage = "JEGOJOSA"

        loadingDialog = LodingDialog(this)

        //spin_loc setting
        val spinAdapter : ArrayAdapter<String> = ArrayAdapter(
            this, R.layout.spinnerlayout, resources.getStringArray(
                R.array.loc_list
            )
        )
        bindingA.bindingJjosa.spinJosaLoc.adapter = spinAdapter
        bindingA.bindingJjosa.spinJosaLoc.setSelection(0)
        bindingA.bindingJjosa.spinJosaLoc.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when(position) {
                    0 -> cdloc = "WA"
                    1 -> cdloc = "WD"
                    2 -> cdloc = "WE"
                    3 -> cdloc = "WF"
                }
                //창고가 바뀌면 제품리스트도 바껴야한다, 제품리스트 하단리스트 전부 초기화
                josaProductList.clear()
                josaProductAdapter!!.notifyDataSetChanged()
                josaItemList.clear()
//                josaTotItemList.clear()
                josaItemAdapter!!.notifyDataSetChanged()
                bindingA.bindingJjosa.txtJosaRlmSum.text = ""
                bindingA.bindingJjosa.txtJosaTagSum.text = ""
                tagBefSize = 0
                getJosaProductList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // product list setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bindingA.bindingJjosa.rvProductlistJosa.layoutManager = layoutManager
        josaProductAdapter = JosaProductAdapter(this, josaProductList, onClickItem)
        bindingA.bindingJjosa.rvProductlistJosa.adapter = josaProductAdapter

        // recycle view Setting
        val josaListManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingJjosa.rvJosalist.layoutManager = josaListManager

        //선택제품 어뎁터 세팅
        josaItemAdapter = JosaItemAdapter(this, josaItemList, onLongClick)
        bindingA.bindingJjosa.rvJosalist.adapter = josaItemAdapter

        //리더기가 연결되어 있으면 타이머 시작
        if (swingConnChk()) {
            mTimeHandler = object : Handler(Looper.getMainLooper()) { // 5초 간격 무한 실행
                override fun handleMessage(msg: Message) {
                    makeSendTagList("TIMER")
                    if (InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true) && batteryCount == 12) { //프리즈마 리더기일경우 배터리 상태는 1분마다 체크
                        batteryCount = 0
                        InventoryMenuActivity.SwingInfo.mSwing!!.prisma_getBattery()
                        setBatteryImg(ReadingJosaRFID.batteryStatus)
                    }
                    batteryCount ++
                    this.sendEmptyMessageDelayed(0, 5000)
                }
            }
            mTimeHandler.sendEmptyMessage(0)
        }

        //프리즈마면 화면 집입시 초기화
        if(InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
            InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
            InventoryMenuActivity.SwingInfo.mSwing!!.prisma_setPower("300")
//            InventoryMenuActivity.SwingInfo.mSwing!!.prisma_getPower()
        }

        //당일 누적 일자표시
        bindingA.bindingJjosa.txtJosaToday.text = LocalDate.now().toString()
    }

    private fun makeSendTagList(gbn: String) : Array<String> {
        var tagList = ""
        val tagNowSize = ReadingJosaRFID.mTagItem.size
        var imsiStart: Int
        var imsiEnd: Int
        bindingA.bindingJjosa.txtTotalRead.text = "$tagNowSize"
///println("befsize : $tagBefSize")
        when(gbn) {
//            "ALL" -> {  //전체는 최종 결과 전송시
//                for (i in 0 until tagNowSize) {
//                    if (i == 0) {
//                        tagList =
//                            "'" + ReadingJosaRFID.mTagItem[i].epcID_Ascii + "'"//.get(i).epcID_Ascii+"'"
//                    }
//                    tagList += ",'" + ReadingJosaRFID.mTagItem[i].epcID_Ascii + "'"//.get(i).epcID_Ascii+"'"
//                }
//                tagArray[0] = tagList
//                return tagArray
//            }
            "ALLTAG" -> {
                sendCnt = (tagNowSize / 3000)
                for (i in 0 until sendCnt+1) {
                    imsiStart = i * 3000
                    imsiEnd = if (i == sendCnt)
                        tagNowSize
                    else
                        (i*3000)+2999

                    if (imsiStart != 0)
                        imsiStart -= 1

                    for (j in imsiStart until imsiEnd) {
                        if (j == imsiStart)
                            tagList = "'" + ReadingJosaRFID.mTagItem[j].epcID_Ascii + "'"
                        else
                            tagList += ",'" + ReadingJosaRFID.mTagItem[j].epcID_Ascii + "'"
                    }
                    tagArray[i] = tagList
                }

                return tagArray
            }
            else -> {   //기타는 타이머 순간순간시
                for (i in tagBefSize until tagNowSize) {
                    if (i == 0) {
                        tagList = "'"+ ReadingJosaRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                    }
                    tagList += ",'"+ ReadingJosaRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                }

                if (josaItemList.size > 0 && tagList.isNotEmpty() && startOK) {
                    println("josaitemlist: ${josaItemList.size}")
                    println("tagList: $tagList")
                    println("taginsert start $tagNowSize end : $tagBefSize")
                    readTagInsertDB(tagNowSize, tagBefSize)
                    tagBefSize = tagNowSize
                    getReadingtagList(tagList)
                } else {
                    if (!startOK) {
                        Toast.makeText(this, "이전 응답 대기중입니다.", Toast.LENGTH_LONG).show()
                    }
                }

                return tagArray
            }
        }
    }

    private fun setBatteryImg(status: String) {
        when (status) {
            "0" -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery00)
            "1" -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery20)
            "2" -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery40)
            "3" -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery60)
            "4" -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery80)
            else -> bindingA.bindingJjosa.imgJosaBattery.setImageResource(R.drawable.battery00)
        }
    }

    private fun swingConnChk() : Boolean {
        //블루투스 장비 연결 체크
        if (InventoryMenuActivity.SwingInfo.swing_ADDRESS == "") {
            Toast.makeText(this, "리더기를 연결해주세요", Toast.LENGTH_LONG).show()
            bindingA.bindingJjosa.btnJosaReaderInit.text = "리더기 연결"
            return false
        }
        bindingA.bindingJjosa.btnJosaReaderInit.text = "리더기 초기화"
        return true
    }

    private fun getJosaProductList() {
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/jegoJosa_ProductList.asp")
        val body = FormBody.Builder().add("loc", cdloc).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBJosaProductList = gson.fromJson(body1, DBJosaProductList::class.java)

                for (idx in dBJosaProductList.results.indices) {
                    josaProductList.add(
                        JosaProducts(
                            dBJosaProductList.results[idx].cdPln,
                            dBJosaProductList.results[idx].nmPln,
                            false
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    josaProductAdapter!!.notifyDataSetChanged()
                    loadingDialog.hide()
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
                runOnUiThread { //UI에 알려줌
                    josaProductAdapter!!.notifyDataSetChanged()
                    loadingDialog.hide()
                }
            }
        })
    }

    private fun getJosaItemList() {
        startOK = false
        loadingDialog.show()
//        val url = URL("http://iclkorea.com/android/JegoJosa_ItemJegoList.asp")
        val url = URL("http://iclkorea.com/android/JegoJosa_ItemJegoList_new.asp")
        val body = FormBody.Builder().add("loc", cdloc).add("cdpln", selCdPln).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    var rlmSumQty = 0
                    var tagSumQty = 0
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBJosaItemList = gson.fromJson(body1, DBJosaItemList::class.java)

                    josaItemList.addAll(dBJosaItemList.results)
                    for (idx in dBJosaItemList.results.indices) {
                        rlmSumQty += dBJosaItemList.results[idx].rlmQty!!.toInt()
                        tagSumQty += dBJosaItemList.results[idx].tagInQty!!.toInt() + dBJosaItemList.results[idx].tagOutQty!!.toInt()
                    }

                    runOnUiThread { //UI에 알려줌
                        josaItemAdapter!!.notifyDataSetChanged()
                        loadingDialog.hide()
                        bindingA.bindingJjosa.txtJosaRlmSum.text = rlmSumQty.toString()
                        bindingA.bindingJjosa.txtJosaTagSum.text = tagSumQty.toString()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                    runOnUiThread { //UI에 알려줌
                        loadingDialog.hide()
                    }
                    startOK = true
                }
            })
        startOK = true
    }


    //읽은 태그 정보를 가져와서 기존 리스트에 추가한다
    private fun getReadingtagList(tagLists: String) {
        startOK = false
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoJosa_ReadingTagList_new.asp")
        val body = FormBody.Builder().add("tags", tagLists).add("cdpln", selCdPln).add("loc", cdloc).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    var tagSumQty = 0

                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBReadingTagList = gson.fromJson(body1, DBReadingTagList::class.java)

                    for (idx in dBReadingTagList.results.indices) {
                        for (idx2 in josaItemList.indices) {
                            if (josaItemList[idx2].cdItem!!.trim() == dBReadingTagList.results[idx].cdItem!!.trim() &&
                                josaItemList[idx2].cdSpec!!.trim() == dBReadingTagList.results[idx].cdSpec!!.trim() &&
                                josaItemList[idx2].dcSpec!!.trim() == dBReadingTagList.results[idx].dcSpec!!.trim()
                            ) {
                                if (dBReadingTagList.results[idx].gbnInOut.equals("IN"))
                                    josaItemList[idx2].inQty =
                                        (josaItemList[idx2].inQty!!.toInt() + dBReadingTagList.results[idx].tagQty!!.toInt()).toString()
                                else
                                    josaItemList[idx2].outQty =
                                        (josaItemList[idx2].outQty!!.toInt() + dBReadingTagList.results[idx].tagQty!!.toInt()).toString()
                                tagSumQty += dBReadingTagList.results[idx].tagQty!!.toInt()
                            }
                        }
                    }

                    runOnUiThread { //UI에 알려줌
                        val temp = bindingA.bindingJjosa.txtJosaTagSum.text
                        if (temp.isBlank() || temp.isEmpty())
                            bindingA.bindingJjosa.txtJosaTagSum.text = tagSumQty.toString()
                        else
                            bindingA.bindingJjosa.txtJosaTagSum.text =
                                (Integer.parseInt(bindingA.bindingJjosa.txtJosaTagSum.text.toString()) + tagSumQty).toString()
                        josaItemAdapter!!.notifyDataSetChanged()
                        loadingDialog.hide()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                    startOK = true
                    runOnUiThread { //UI에 알려줌
                        loadingDialog.hide()
                    }
                }
            })
        startOK = true
    }

    private fun sendTagReadingResult(tagLists: String, cnt: Int) {
        startOK = false
        loadingDialog.show()
//        val url = URL("http://iclkorea.com/android/JegoJosa_TagResultSend.asp")
        println("josa result : $tagLists")
        val url = URL("http://iclkorea.com/android/JegoJosa_TagScanSend.asp")
        val body = FormBody.Builder().add("tags", tagLists).add("cdpln", selCdPln).add(
            "readerNo", cdloc ).add("cdUsr", WjmMain.LoginUser.sno).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBSendTagList = gson.fromJson(body1, DBSendTagList::class.java)

                    if (dBSendTagList.results == "OK") {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(baseContext, "전송이 완료되엇습니다." + cnt.toString() + "/" + (sendCnt + 1).toString(), Toast.LENGTH_LONG).show()
                            dbdelete(tagLists) //전송한만큼 삭제
                            tagmatch("DB")
                            loadingDialog.hide()
                        }
                    } else {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(
                                baseContext,
                                "전송이 실패되엇습니다.(" + dBSendTagList.status + ")",
                                Toast.LENGTH_LONG
                            ).show()
                            loadingDialog.hide()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                    startOK = true
                }
            })
        startOK = true
    }

    private fun sendTagTotResult() {
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoJosa_TagResultSend_new.asp")
        val body = FormBody.Builder().add("cdpln", selCdPln).add("loc", cdloc).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBSendTagList = gson.fromJson(body1, DBSendTagList::class.java)

                    if (dBSendTagList.results == "OK") {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(baseContext, "전송이 완료되엇습니다.", Toast.LENGTH_LONG).show()
                            loadingDialog.hide()
                        }
                    } else {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(
                                baseContext,
                                "전송이 실패되엇습니다.(" + dBSendTagList.status + ")",
                                Toast.LENGTH_LONG
                            ).show()
                            loadingDialog.hide()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }
            })
    }
    private fun getDetailList(arg1: String, arg2: String, arg3: String) {
        startOK = false
        loadingDialog.show()
        josaDetailList.clear()
        //println(arg1.toString())
        val url = URL("http://iclkorea.com/android/JegoJosa_DetailList_new.asp")
        val body = FormBody.Builder().add("cdItem", arg1).add("cdSpec", arg2).add("dcSpec", arg3)
            .add("loc", cdloc).build()//.add("tags", tags).build()
//        println(arg1 + " " + arg2 + " " + arg3 + " "+ tags)
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBJosaDetailItem = gson.fromJson(body1, DBJosaDetailItem::class.java)

                josaDetailList.addAll(dBJosaDetailItem.results)

                runOnUiThread { //UI에 알려줌
                    //println(cdialogList.size)
                    cDialog("$arg1 $arg2 $arg3").show()
                    josaDetailAdapter!!.notifyDataSetChanged()
                    loadingDialog.hide()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
                startOK = true
                runOnUiThread { //UI에 알려줌
                    loadingDialog.hide()
                }
            }
        })
        startOK = true
    }
    data class DBJosaProductList(val results: List<JosaProducts>)
    data class JosaProducts(var cdPln: String?, var nmPln: String?, var flag: Boolean?)
    data class DBJosaItemList(val results: List<JosaItem>)
    data class JosaItem(
        var cdItem: String?,
        var nmItem: String?,
        var cdSpec: String?,
        var nmSpec: String?,
        var inQty: String?,
        var outQty: String?,
        var tagInQty: String?,
        var tagOutQty: String?,
        var rlmQty: String?,
        var dcSpec: String?
    )
    data class DBReadingTagList(val results: List<ReadingTagItem>)
    data class ReadingTagItem(
        var cdItem: String?,
        var cdSpec: String?,
        var dcSpec: String?,
        var gbnInOut: String?,
        var tagQty: String?
    )
    data class DBSendTagList(val results: String, val status: String)
    data class DBJosaDetailItem(val results: List<JosaDetailItem>)
    data class JosaDetailItem(
        var tagSerial: String?,
        var tagInOut: String?,
        var tagLoc: String?,
        var rlmSerial: String?
    )

    inner class JosaProductAdapter(
        private val context: Context,
        private val itemList: ArrayList<JosaProducts>,
        private val onClickItem: View.OnClickListener
    ) : RecyclerView.Adapter<JosaProductAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // context 와 parent.getContext() 는 같다.
            val view = LayoutInflater.from(context)
                .inflate(R.layout.row_vertical_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textNm.text = itemList[position].nmPln
           holder.textRemark.text = itemList[position].nmPln
            holder.productLayout.tag = itemList[position].cdPln
//            holder.productLayout.transitionName = itemList[position].nmCol
            holder.productLayout.setOnClickListener(onClickItem)

            if (josaProductList[position].flag == true)
                holder.productLayout.setBackgroundResource(R.drawable.product_button_on)
            else
                holder.productLayout.setBackgroundResource(R.drawable.product_button)
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var productLayout : LinearLayout = itemView.findViewById(R.id.layout_Product)
            var textNm : TextView = itemView.findViewById(R.id.text_itemNm)
            var textRemark : TextView = itemView.findViewById(R.id.text_remark)
        }
    }

    inner class JosaItemAdapter(
        private val context: Context,
        private val itemList: ArrayList<JosaItem>,
        private val onLongClick: View.OnLongClickListener
    ) : RecyclerView.Adapter<JosaItemAdapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_4col, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: JosaItemAdapter.ViewHolder, position: Int) {
            //레이아웃 파라메타 변경
            val itemparams : LinearLayout.LayoutParams  = holder.txtItem.layoutParams as LinearLayout.LayoutParams
            itemparams.weight = 20f
            holder.txtItem.layoutParams = itemparams
            holder.txtItem.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            val specparams : LinearLayout.LayoutParams  = holder.txtSpec.layoutParams as LinearLayout.LayoutParams
            specparams.weight = 50f
            holder.txtSpec.layoutParams = specparams
            holder.txtSpec.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            val tagparams : LinearLayout.LayoutParams  = holder.txtTagQty.layoutParams as LinearLayout.LayoutParams
            tagparams.weight = 15f
            holder.txtTagQty.layoutParams = tagparams
            holder.txtTagQty.setTextColor(Color.BLUE)
            val rlmparams : LinearLayout.LayoutParams  = holder.txtRlmQty.layoutParams as LinearLayout.LayoutParams
            rlmparams.weight = 15f
            holder.txtRlmQty.layoutParams = rlmparams

            //값 세팅
            holder.txtItem.text = itemList[position].nmItem
            holder.txtSpec.text = itemList[position].nmSpec
            holder.txtTagQty.text = (itemList[position].tagInQty!!.toInt() + itemList[position].tagOutQty!!.toInt() + itemList[position].inQty!!.toInt() + itemList[position].outQty!!.toInt()).toString()
            holder.txtRlmQty.text = itemList[position].rlmQty

            if ((itemList[position].outQty!!.toInt() > 0) ||  (itemList[position].tagOutQty!!.toInt() >0 ))
                holder.txtTagQty.setTextColor(Color.RED)
            else if ((itemList[position].tagInQty!!.toInt()+itemList[position].tagOutQty!!.toInt() + itemList[position].inQty!!.toInt() + itemList[position].outQty!!.toInt() != itemList[position].rlmQty!!.toInt()))
                holder.txtTagQty.setTextColor(Color.rgb(120,86,0))
            else
                holder.txtTagQty.setTextColor(Color.BLUE)

            //롱클릭이벤트용 세팅
            holder.txtTagQty.tag = itemList[position].cdItem + "," + itemList[position].cdSpec + "," + itemList[position].dcSpec
            holder.txtTagQty.setOnLongClickListener(onLongClick)


            //row visible세팅
            if (bindingA.bindingJjosa.cbJosaDiff.isChecked) {
                if ((itemList[position].tagInQty!!.toInt() + itemList[position].tagOutQty!!.toInt() + itemList[position].inQty!!.toInt() + itemList[position].outQty!!.toInt()) == itemList[position].rlmQty!!.toInt()) {
                    holder.llrow.visibility = View.GONE
                    holder.txttemp.visibility = View.GONE
                }
                else {
                    holder.llrow.visibility = View.VISIBLE
                    holder.txttemp.visibility = View.VISIBLE
                }
            } else {
                holder.llrow.visibility = View.VISIBLE
                holder.txttemp.visibility = View.VISIBLE
            }

        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var txtItem : TextView = itemView.findViewById(R.id.txt_Spec)
            var txtSpec : TextView = itemView.findViewById(R.id.txt_OutDt)
            var txtTagQty : TextView = itemView.findViewById(R.id.txt_Serial)
            var txtRlmQty : TextView = itemView.findViewById(R.id.txt_ScanDt)
            var llrow : LinearLayout = itemView.findViewById(R.id.ll_4col_row)
            var txttemp : TextView = itemView.findViewById(R.id.textView177)
        }
    }

    inner class CDialogJosaAdapter(
        private var context: Context,
        private var itemList: ArrayList<JosaDetailItem>
    ) : RecyclerView.Adapter<CDialogJosaAdapter.ViewHolder>(){

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_josa_cdialog,
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            if (position and 1 == 0)
//                holder.clayout.setBackgroundResource(R.drawable.list_item_2)
//            else
//                holder.clayout.setBackgroundResource(R.drawable.list_item_3)

            holder.txtTagSerial.text = itemList[position].tagSerial
            holder.txtInOut.text = itemList[position].tagInOut
            holder.txtLoc.text = itemList[position].tagLoc
            holder.txtRlmSerial.text = itemList[position].rlmSerial
            if (!itemList[position].rlmSerial.equals(itemList[position].tagSerial))
                holder.txtRlmSerial.setTextColor(Color.RED)
            else
                holder.txtRlmSerial.setTextColor(Color.BLACK)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val txtTagSerial : TextView = itemView.findViewById(R.id.txtTagSerial)
            val txtInOut : TextView = itemView.findViewById(R.id.txtTagInOut)
            val txtLoc : TextView = itemView.findViewById(R.id.txtTagLoc)
            val txtRlmSerial : TextView = itemView.findViewById(R.id.txtRlmSerial)
            //           val clayout : ConstraintLayout = itemView.findViewById(R.id.cl_cdialog_row)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun cDialog(title: String): Dialog {
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.cdialog_josa, nullParent)
        val builder = AlertDialog.Builder(this)

        builder.setView(view)
        val rvCDialog : RecyclerView = view.findViewById(R.id.rvJosaCDialog)
        val txtTitle : TextView = view.findViewById(R.id.txtCDJosaTitle)
        val btnClose : Button = view.findViewById(R.id.btnCloseJosa)
        txtTitle.text = title

        val josaListManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvCDialog.layoutManager = josaListManager
        // custom dialog lsit view Setting
        josaDetailAdapter = CDialogJosaAdapter(this, josaDetailList)
        rvCDialog.adapter = josaDetailAdapter

        josaDetailAdapter!!.notifyDataSetChanged()

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener(({
            startOK = true
            dialog.dismiss()
        }))

        return dialog
    }

    private fun setRfPower(gbn : String) {
        var rfPower = bindingA.bindingJjosa.txtRfPower.text.toString()
        when (gbn) {
            "UP" ->
                if (rfPower != "30")
                    rfPower = ((rfPower.toInt() + 3) * 10).toString()

            "DOWN" ->
                if (rfPower != "0")
                    rfPower = ((rfPower.toInt() - 3) * 10).toString()
        }
        bindingA.bindingJjosa.txtRfPower.text = (rfPower.toInt() / 10).toString()
        when (rfPower.toInt() / 10) {
            in 0..6 -> bindingA.bindingJjosa.imgJosaRfPower.setImageResource(R.drawable.rfpower20)
            in 7..12 -> bindingA.bindingJjosa.imgJosaRfPower.setImageResource(R.drawable.rfpower40)
            in 13..18 -> bindingA.bindingJjosa.imgJosaRfPower.setImageResource(R.drawable.rfpower60)
            in 19..24 -> bindingA.bindingJjosa.imgJosaRfPower.setImageResource(R.drawable.rfpower80)
            in 25..30 -> bindingA.bindingJjosa.imgJosaRfPower.setImageResource(R.drawable.rfpower100)
        }
        InventoryMenuActivity.SwingInfo.mSwing!!.prisma_setPower(rfPower)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val onClickItem = View.OnClickListener { v ->
        selCdPln  = v.tag as String
        startOK = false

        for (idx in 0 until josaProductList.size)
            josaProductList[idx].flag = (josaProductList[idx].cdPln == selCdPln)
        josaProductAdapter!!.notifyDataSetChanged()

        tagBefSize = 0
        bindingA.bindingJjosa.txtJosaTagSum.text = ""
        josaItemList.clear()
        josaItemAdapter!!.notifyDataSetChanged()

        getJosaItemList()
        swingConnChk()
        println("mtag size : ${ReadingJosaRFID.mTagItem.size}")
        if (ReadingJosaRFID.mTagItem.size > 0)
            makeSendTagList("TIMER")

        dbchk()
        startOK = true
    }

    private val onLongClick = View.OnLongClickListener { v->
        println(v.tag)
        val temp = v.tag.toString().split(",")
        val cditem = temp[0]
        val cdspec = temp[1]
        val dcspec = temp[2]

        getDetailList(cditem, cdspec, dcspec)
        true
    }

    class ReadingJosaRFID {
        companion object {
            var mTagItem: ArrayList<TagItem> = ArrayList()//스윙기로 읽은 데이타를 담을 그릇
            var batteryStatus : String = ""
            var powerStatus : String = "300"
            var powerRange : String = ""

            fun showDataJepum(Item: TagItem) {
                val pos = getPosition(Item)
                println("Position : $pos")
                if (pos < 0) {              //읽은 태그가 중복이 아니면 변수 등록
                    mTagItem.add(Item)
                }
            }

            //검색된것의 중복체크를 위한 함수 START : -1이면 신규 아니면 읽은것
            private fun getPosition(item: TagItem): Int {
                var position = -1

                var titem: TagItem?
                var oldID: String?

                for (i in 0 until mTagItem.size) {
                    titem = mTagItem[i]
                    oldID = titem.getEpcID_Ascii()
                    if (oldID == item.getEpcID_Ascii()) {
                        position = i
                        break
                    }
                }
                return position
            }
            //검색된것의 중복체크를 위한 함수 END

            fun setBattery(status: String) {
                batteryStatus = status
            }
            fun getPower(status: String) {
                powerStatus = status
            }
            fun getPowerRange(status: String) { // default 000(0) ~ 300(30)
                powerRange = status
            }
        }
    }

    private fun tagmatch(gbn: String) {
        var noTag: String
        var tempTagitem : TagItem
        if (gbn == "DB") {
            Toast.makeText(this, "TagMatching Start", Toast.LENGTH_SHORT).show()
            //db가 많으면 예기치 못한 튕김으로 판단 db의 태그를 mtag로 복사
            ReadingJosaRFID.mTagItem.clear()
            val cur1 = db!!.rawQuery("SELECT DISTINCT NO_TAG FROM JOSA_INFO;", null)

            while (cur1.moveToNext()) {
                noTag = cur1.getString(0)
                tempTagitem = TagItem("EPC", noTag)
                ReadingJosaRFID.mTagItem.add(tempTagitem)
                println("MATCH : $noTag")
            }
            cur1.close()
            dbdelete("ALL")
            Toast.makeText(this, "TagMatching End", Toast.LENGTH_SHORT).show()
        } else {

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish(); return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun readTagInsertDB(newCnt: Int, oldCnt: Int) {
        for (i in oldCnt until newCnt) {
            db!!.execSQL(
                "INSERT INTO JOSA_INFO (NO_TAG) values('" + ReadingJosaRFID.mTagItem[i].epcID_Ascii + "');"
            )
        }
    }

    private fun dbdelete(item: String) {
        if (item == "ALL") {
            db!!.delete("JOSA_INFO", "1=1", null)
        } else {
//            val whereArg = arrayOf(item)
            println("전송 tag $item")
//            db!!.delete("JOSA_INFO", "NO_TAG IN (?)", whereArg)
            val sql = "DELETE FROM JOSA_INFO WHERE NO_TAG IN (${item})"
            db!!.execSQL(sql)
        }
    }

    private fun dbcount() : Int {
        val rtnval: Int
        val cur1 = db!!.rawQuery("SELECT COUNT(*) FROM JOSA_INFO", null)
            cur1.moveToNext()
            rtnval = cur1.getString(0).toInt()
        cur1.close()
        return rtnval
    }

    private fun dbchk() {
        val dbcnt = dbcount()
        val mtagcnt = ReadingJosaRFID.mTagItem.size
        if (mtagcnt == dbcnt) return

        if (dbcnt > mtagcnt)
//            if (mtagcnt == 0)
                tagmatch("DB")
        else
            tagmatch("mTAG")

    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.dismiss()
//        dbdelete("ALL")
        ReadingJosaRFID.mTagItem.clear()
        if (swingConnChk())
            mTimeHandler.removeMessages(0)
    }
}