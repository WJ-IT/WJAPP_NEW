package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
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
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class JegoJosaActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityJegoJosaBinding
    private var cdloc = "WA"
    private var selCdPln = ""
    private var tagBefSize = 0
    private lateinit var mTimeHandler : Handler
    private var startOK = true
    private var batteryCount = 10

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
            if (InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
                InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
            } else {
                Toast.makeText(this, "Prisma 리더기만 해당됩니다.", Toast.LENGTH_LONG).show()
            }
            println("xxxxxxxxxxxxxxx" + dbcount())
            println("xxxxxxxxxxxxxxx2" + ReadingJosaRFID.mTagItem.size)
        }))

        bindingA.bindingJjosa.btnJosaTagInit.setOnClickListener(({
            ReadingJosaRFID.mTagItem.clear()
            josaItemList.clear()
            josaItemAdapter!!.notifyDataSetChanged()
            tagBefSize = 0
            dbdelete("ALL")
        }))

        bindingA.bindingJjosa.cbJosaDiff.setOnClickListener(({
//            if (bindingA.bindingJjosa.cbJosaDiff.isChecked) {}
            josaItemAdapter!!.notifyDataSetChanged()
        }))

        bindingA.bindingJjosa.btnJosaSendResult.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("결과전송")
                .setMessage("스캔한 데이타를 전송하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("확인") { _, _ ->
                    sendTagReadingResult(makeSendTagList("ALL"))
                }
                .setNegativeButton("취소", null).show()
        }))

        //DB관련
        db = openOrCreateDatabase(dbName, MODE_PRIVATE, null)
    }

    private fun jegoJosaInit() {
        // 리딩결과 값을 받을 화면을 알려준다
        InventoryMenuActivity.reading_loc.topPage = "JEGOJOSA"

        //spin_loc setting
        val spinAdapter : ArrayAdapter<String> = ArrayAdapter(
            this, R.layout.spinnerlayout, resources.getStringArray(
                R.array.loc_list
            )
        )
        bindingA.bindingJjosa.spinJosaLoc.adapter = spinAdapter
        bindingA.bindingJjosa.spinJosaLoc.setSelection(0)
        bindingA.bindingJjosa.spinJosaLoc.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                }
                //창고가 바뀌면 제품리스트도 바껴야한다, 제품리스트 하단리스트 전부 초기화
                josaProductList.clear()
                josaProductAdapter!!.notifyDataSetChanged()
                josaItemList.clear()
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
        //초기 서울창고로 초기조회
        getJosaProductList()

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
        }

    }

    private fun makeSendTagList(gbn: String) : String {
        var tagList = ""
        val tagNowSize = ReadingJosaRFID.mTagItem.size
///println("befsize : $tagBefSize")
        when(gbn) {
            "ALL" -> {  //전체는 최종 결과 전송시
                for (i in 0 until tagNowSize) {
                    if (i == 0) {
                        tagList =
                            "'" + ReadingJosaRFID.mTagItem[i].epcID_Ascii + "'"//.get(i).epcID_Ascii+"'"
                    }
                    tagList += ",'" + ReadingJosaRFID.mTagItem[i].epcID_Ascii + "'"//.get(i).epcID_Ascii+"'"
                }
                return tagList
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
                    println("tagList: ${tagList}")
                    println("taginsert start ${tagNowSize} end : ${tagBefSize}")
                    readTagInsertDB(tagNowSize, tagBefSize)
                    tagBefSize = tagNowSize
                    getReadingtagList(tagList)
                } else {
                    if (!startOK) {
                        Toast.makeText(this, "이전 응답 대기중입니다.", Toast.LENGTH_LONG).show()
                    }
                }

                return ""
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
        }
    }

    private fun swingConnChk() : Boolean {
        //블루투스 장비 연결 체크
        if (InventoryMenuActivity.SwingInfo.swing_ADDRESS == "") {
            Toast.makeText(this, "리더기를 연결해주세요", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun getJosaProductList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/jegoJosa_ProductList.asp")
        val body = FormBody.Builder().add("loc", cdloc).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
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
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getJosaItemList() {
        startOK = false
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoJosa_ItemJegoList.asp")
        val body = FormBody.Builder().add("loc", cdloc).add("cdpln", selCdPln).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    var rlmSumQty = 0

                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBJosaItemList = gson.fromJson(body1, DBJosaItemList::class.java)

                    for (idx in dBJosaItemList.results.indices) {
                        josaItemList.add(
                            JosaItem(
                                dBJosaItemList.results[idx].cdItem,
                                dBJosaItemList.results[idx].nmItem,
                                dBJosaItemList.results[idx].cdSpec,
                                dBJosaItemList.results[idx].nmSpec,
                                dBJosaItemList.results[idx].inQty,
                                dBJosaItemList.results[idx].outQty,
                                dBJosaItemList.results[idx].rlmQty,
                                dBJosaItemList.results[idx].dcSpec
                            )
                        )
                        rlmSumQty += dBJosaItemList.results[idx].rlmQty!!.toInt()
                    }

                    runOnUiThread { //UI에 알려줌
                        josaItemAdapter!!.notifyDataSetChanged()
                        loadingDialog.dismiss()
                        bindingA.bindingJjosa.txtJosaRlmSum.text = rlmSumQty.toString()
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
    //읽은 태그 정보를 가져와서 기존 리스트에 추가한다
    private fun getReadingtagList(tagLists: String) {
        startOK = false
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoJosa_ReadingTagList.asp")
        val body = FormBody.Builder().add("tags", tagLists).add("cdpln", selCdPln).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
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
                        loadingDialog.dismiss()
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

    private fun sendTagReadingResult(tagLists: String) {
        startOK = false
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoJosa_TagResultSend.asp")
        println("josa result : $tagLists")
        val body = FormBody.Builder().add("tags", tagLists).add("cdpln", selCdPln).add(
            "readerNo",
            cdloc
        ).add("cdUsr", WjmMain.LoginUser.sno).build()
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
                            dbdelete(tagLists) //전송한만큼 삭제
                            loadingDialog.dismiss()

                        }
                    } else {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(
                                baseContext,
                                "전송이 실패되엇습니다.(" + dBSendTagList.status + ")",
                                Toast.LENGTH_LONG
                            ).show()
                            loadingDialog.dismiss()
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

    private fun getDetailList(arg1: String, arg2: String, arg3: String, tags: String) {
        startOK = false
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        josaDetailList.clear()
        //println(arg1.toString())
        val url = URL("http://iclkorea.com/android/JegoJosa_DetailList.asp")
        val body = FormBody.Builder().add("cdItem", arg1).add("cdSpec", arg2).add("dcSpec", arg3).add(
            "loc",
            cdloc
        ).add("tags", tags).build()
//        println(arg1 + " " + arg2 + " " + arg3 + " "+ tags)
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBJosaDetailItem = gson.fromJson(body1, DBJosaDetailItem::class.java)

                for (idx in dBJosaDetailItem.results.indices) {
                    josaDetailList.add(
                        JosaDetailItem(
                            dBJosaDetailItem.results[idx].tagSerial,
                            dBJosaDetailItem.results[idx].tagInOut,
                            dBJosaDetailItem.results[idx].tagLoc,
                            dBJosaDetailItem.results[idx].rlmSerial
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    //println(cdialogList.size)
                    cDialog("$arg1 $arg2 $arg3").show()
                    josaDetailAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
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
            holder.txtTagQty.text = (itemList[position].inQty!!.toInt() + itemList[position].outQty!!.toInt()).toString()
            holder.txtRlmQty.text = itemList[position].rlmQty

            if (itemList[position].outQty!!.toInt() > 0)
                holder.txtTagQty.setTextColor(Color.RED)
            else
                holder.txtTagQty.setTextColor(Color.BLUE)

            //롱클릭이벤트용 세팅
            holder.txtTagQty.tag = itemList[position].cdItem + "," + itemList[position].cdSpec + "," + itemList[position].dcSpec
            holder.txtTagQty.setOnLongClickListener(onLongClick)


            //row visible세팅
            if (bindingA.bindingJjosa.cbJosaDiff.isChecked) {
                if ((itemList[position].inQty!!.toInt() + itemList[position].outQty!!.toInt()) == itemList[position].rlmQty!!.toInt()) {
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

    private val onClickItem = View.OnClickListener { v ->
        selCdPln  = v.tag as String
        startOK = false
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()

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

        loadingDialog.dismiss()
        startOK = true
    }

    private val onLongClick = View.OnLongClickListener { v->
        println(v.tag)
        val temp = v.tag.toString().split(",")
        val cditem = temp[0]
        val cdspec = temp[1]
        val dcspec = temp[2]
        getDetailList(cditem, cdspec, dcspec, makeSendTagList("ALL"))
        true
    }

    class ReadingJosaRFID {
        companion object {
            var mTagItem: ArrayList<TagItem> = ArrayList()//스윙기로 읽은 데이타를 담을 그릇
            var batteryStatus : String = ""

            fun showDataJepum(Item: TagItem) {
                val pos = getPosition(Item)
                println("Position : ${pos}")
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
        }
    }

    private fun tagmatch(gbn: String) {
        var no_tag : String = ""
        var tempTagitem : TagItem
        if (gbn == "DB") {
            Toast.makeText(this, "TagMatching Start", Toast.LENGTH_SHORT).show()
            //db가 많으면 예기치 못한 튕김으로 판단 db의 태그를 mtag로 복사
            ReadingJosaRFID.mTagItem.clear()
            val cur1 = db!!.rawQuery("SELECT DISTINCT NO_TAG FROM JOSA_INFO;", null)

            while (cur1.moveToNext()) {
                no_tag = cur1.getString(0)
                tempTagitem = TagItem("EPC", no_tag)
                ReadingJosaRFID.mTagItem.add(tempTagitem)
                println("MATCH : ${no_tag}")
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
            val whereArg = arrayOf(item)
            println("전송 tag ${item}")
          //  db!!.delete("JOSA_INFO", "NO_TAG IN (?)", whereArg)
            val sql = "DELETE FROM JOSA_INFO WHERE NO_TAG IN (${item})"
            db!!.execSQL(sql)
        }
    }

    private fun dbcount() : Int {
        var rtnval = 0
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
        dbdelete("ALL")
        ReadingJosaRFID.mTagItem.clear()
        if (swingConnChk())
            mTimeHandler.removeMessages(0)
    }
}