package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity

import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException
import java.net.URL
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import java.util.*
import kotlin.collections.ArrayList
import android.widget.Toast
import wjm.co.kr.wjapp_new.databinding.ActivityJegoJjscanBinding
import java.util.concurrent.TimeUnit


class JegoJJScanActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityJegoJjscanBinding
    private var cdCust : String? = null
    private var nmCust : String? = null
    private var sending : String = "N"

    private var mTagEtc : ArrayList<TagItem> = ArrayList()

    private var arJJList : ArrayList<JJItem> = ArrayList()
    private var myJJListAdapter : JJListAdapter? = null

    lateinit var loadingDialog : LodingDialog

    private var mTimeHandler : Handler? = null
    private var mHandler : Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_jego_jjscan)
        bindingA = ActivityJegoJjscanBinding.inflate(layoutInflater)
        setContentView(bindingA.root)

        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        InventoryMenuActivity.reading_loc.topPage = "JEGOJJSCAN"
        loadingDialog = LodingDialog(this)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingJjscan.JegoJJList.layoutManager = layoutManager
        myJJListAdapter = JJListAdapter(this, arJJList)
        bindingA.bindingJjscan.JegoJJList.adapter = myJJListAdapter
//        binding.JegoJJList.divider = null
        bindingA.bindingJjscan.JegoJJList.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        if (WjmMain.LoginUser.sno == "") {
            Toast.makeText(baseContext, "로그인 정보가 사라졌어요!", Toast.LENGTH_LONG).show()
            val intent = Intent(baseContext, WjmMain::class.java)
            startActivity(intent)
        }

        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    0 -> {
                        cdCust = message.data.getString("custCd")
                        nmCust = message.data.getString("custNm")
                        bindingA.toolbar.title = "$nmCust 저장재고"
                        initData("ALL")
                   //     getJJTags(cdCust)

                    }
                }
            }
        }
        customerDialog(layoutInflater, mHandler!!, this, "").show()

//        customerDialog().show()

        bindingA.bindingJjscan.btnInitJj.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("확인")
                .setMessage("스캔한 데이타를 초기화하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.dia_yes) { _, _ ->
                    initData("ALL")
                //    getJJTags(cdCust)
                    mTimeHandler!!.sendEmptyMessage(0)
                    Toast.makeText(baseContext, "초기화 하였습니다.", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.dia_no, null).show()
        }))

        bindingA.bindingJjscan.btnEtcJj.setOnClickListener(({
            if (mTagEtc.size == 0) {
                Toast.makeText(baseContext, "추가할 태그가 없습니다.", Toast.LENGTH_LONG).show()
            } else {
                val tags = makeTagList("1")
                if (tags.length > 10)
                    getJJEtcTags(tags)
            }
        }))

        bindingA.bindingJjscan.btnSendJj.setOnClickListener(({
            if (ReadingRFID.mTagItem.size == 0) Toast.makeText(this, R.string.tag_nodata, Toast.LENGTH_LONG ).show()
            else {
                AlertDialog.Builder(this)
                    .setTitle("확인")
                    .setMessage("스캔한 데이타를 전송하시겠습니까?")
                    .setIcon(R.drawable.wjicon)
                    .setPositiveButton(R.string.dia_yes){_, _ ->
                        val tags = makeTagList("ALL")
                        if (sending == "N") {
                            sending = "Y"
                            sendTags(tags)
                        }
                        else Toast.makeText(this,R.string.tag_sending, Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton(R.string.dia_no, null)
                    .show()

            }
        }))

        mTimeHandler = object : Handler(Looper.getMainLooper()) { // 5초 간격 무한 실행
            override fun handleMessage(msg: Message) {
                //   if(cdCust.length()>0) { //거래처를 선택되어있으면 돌린다
                //loadingDialog.show()
                setTagFind()
                this.sendEmptyMessageDelayed(0, 1000)
                //   }
            }
        }
        mTimeHandler!!.sendEmptyMessage(0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchItem.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.toolbar_cust_select -> customerDialog(layoutInflater, mHandler!!, this, "").show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setTagFind() {
        var tagno : String
        var k : Int
        var readcnt = 0
        val readingQty = ReadingRFID.mTagItem.size

        mTagEtc.clear()
        for (idx in 0 until readingQty){
            tagno = ReadingRFID.mTagItem[idx].epcID_Ascii
            k=0
            for (idx2 in 0 until arJJList.size)
                if (tagno == arJJList[idx2].tagNo) {
                    arJJList[idx2].reading = "√"
                    readcnt ++
                    k++
                }

            if (k==0) {
                mTagEtc.add(ReadingRFID.mTagItem[idx])
                bindingA.bindingJjscan.txtEtcqty.text = "Etc " + mTagEtc.size
            }

        }

        myJJListAdapter?.notifyDataSetChanged()
        bindingA.bindingJjscan.txtScanqty.text = "Scan " + readcnt + "/" + arJJList.size
        //loadingDialog.hide()
    }

    private fun initData(gbn:String) {
        if (gbn == "ALL") {
            ReadingRFID.mTagItem.clear()
            loadingDialog.show()
            getJJTags(cdCust)
        }
        arJJList.clear()
        mTagEtc.clear()
        bindingA.bindingJjscan.txtScanqty.text = "Scan 0/0"
        bindingA.bindingJjscan.txtEtcqty.text = "Etc 0"
        myJJListAdapter?.notifyDataSetChanged()
    }

    //Tag 리스트 만들기 Start
    private var tagBefSize = 0
    private fun makeTagList(gbn : String): String {
        var tagList = ""
        val tagNowSize = ReadingRFID.mTagItem.size

        when (gbn) {
            "ALL" -> {
                for (i in 0 until tagNowSize) {
                    if (i == 0) {
                        tagList = "'"+ ReadingRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                    }
                    tagList += ",'"+ ReadingRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                }
            }
            else -> {
                for (i in tagBefSize until tagNowSize) {
                    if (i == 0) {
                        tagList = "'"+ ReadingRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                    }
                    tagList += ",'"+ ReadingRFID.mTagItem[i].epcID_Ascii+"'"//.get(i).epcID_Ascii+"'"
                }
                tagBefSize = tagNowSize
            }
        }
        return tagList
    }
    //Tag 리스트 만들기 End

    // 저장태그 불러오기(DB) START
    private fun getJJTags(str: String?): Boolean {
        val url = URL("http://iclkorea.com/android/JegoJJScan_TagList.asp")
        val body = FormBody.Builder().add("cust", str!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
                println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbJJTagList = gson.fromJson(returnbody, DBJJTagList::class.java)

                for (idx in dbJJTagList.results.indices)
                    arJJList.add(JJItem(dbJJTagList.results[idx].cdItem, dbJJTagList.results[idx].nmItem, dbJJTagList.results[idx].cdSpec, dbJJTagList.results[idx].nmSpec, dbJJTagList.results[idx].dtVld, dbJJTagList.results[idx].serialNo, dbJJTagList.results[idx].tagNo, dbJJTagList.results[idx].ioDt, ""))

                runOnUiThread { //UI에 알려줌
                    myJJListAdapter?.notifyDataSetChanged()
                    bindingA.bindingJjscan.txtScanqty.text = "Scan 0/" + arJJList.size
                    loadingDialog.hide()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 저장태그 불러오기(DB) END

    // 기타태그 불러오기(DB) START
    private fun getJJEtcTags(str: String?): Boolean {
        val url = URL("http://iclkorea.com/android/JegoJJScan_EtcTagList.asp")
        val body = FormBody.Builder().add("tags", str!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
                println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbJJTagList = gson.fromJson(returnbody, DBJJTagList::class.java)

                for (idx in dbJJTagList.results.indices)
                    arJJList.add(JJItem(dbJJTagList.results[idx].cdItem, dbJJTagList.results[idx].nmItem, dbJJTagList.results[idx].cdSpec, dbJJTagList.results[idx].nmSpec, dbJJTagList.results[idx].dtVld, dbJJTagList.results[idx].serialNo, dbJJTagList.results[idx].tagNo, dbJJTagList.results[idx].ioDt, ""))

                val comp : Comparator<JJItem> = object : Comparator<JJItem> {
                    override fun compare(o1: JJItem, o2: JJItem): Int {
                        val v1 = o1.nmItem + o1.nmSpec
                        val v2 = o2.nmItem + o2.nmSpec
                        return v1.compareTo(v2)
                    }
                }
                Collections.sort(arJJList, comp)

                runOnUiThread { //UI에 알려줌
                    myJJListAdapter?.notifyDataSetChanged()
                    bindingA.bindingJjscan.txtScanqty.text = "Scan 0/" + arJJList.size
                    loadingDialog.hide()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 기타태그 불러오기(DB) END

    // 읽은 태그 전송하기(DB) START
    private fun sendTags(str: String): Boolean {
        val url = URL("http://iclkorea.com/android/JegoScan_TagSend.asp")
        val body = FormBody.Builder().add("send_tags", str).add("ccode", WjmMain.LoginUser.ccode).add("cd_usr", WjmMain.LoginUser.sno).add("reader_no", InventoryMenuActivity.SwingInfo.swing_NAME).add("cust", cdCust!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
                println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbSendOK = gson.fromJson(returnbody, DBSendOK::class.java)

                if (dbSendOK.results == "OK") {
                    runOnUiThread {
                        Toast.makeText(baseContext,R.string.tag_sendOK, Toast.LENGTH_LONG).show()
                        initData("ALL")
                    }
                }
                else {
                    runOnUiThread {
                        //UI에 알려줌
                        Toast.makeText(baseContext,R.string.tag_sendingErr, Toast.LENGTH_LONG).show()
                    }
                }
                sending="N"
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 읽은 태그 전송하기(DB) END

    data class DBJJTagList(val results : List<JJItem>)
    data class JJItem (var cdItem:String?, var nmItem:String?, var cdSpec:String?, var nmSpec:String?, var dtVld:String?, var serialNo:String?, var tagNo:String?, var ioDt:String?, var reading:String?)
    data class DBSendOK(val results : String?)

    inner class JJListAdapter(private val context: Context, arLmList: ArrayList<JJItem>) : RecyclerView.Adapter<JJListAdapter.ViewHolder>() {
        private val mList = arLmList

        override fun getItemCount(): Int {
            return mList.size
        }

        override fun onBindViewHolder(holder: JJListAdapter.ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.lljjscan.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.lljjscan.setBackgroundResource(R.drawable.list_item_3)

            holder.col1.text = mList[position].nmItem
            holder.col2.text = mList[position].nmSpec
            holder.col3.text = mList[position].serialNo
            holder.col4.text = mList[position].reading
            holder.col5.text = mList[position].ioDt

            var temp = mList[position].dtVld
            if (temp!!.isEmpty() || temp.length < 6) {

            } else {
                when(temp.length) {
                    6 -> temp = temp.substring(0, 4) + "/" + temp.substring(4, 6)
                    else -> temp = temp.substring(0, 4) + "/" + temp.substring(4, 6) + "/" + temp.substring(6, 8)
                }
            }
            holder.col6.text = temp

            val day = checkDate(mList[position].dtVld)

            if (day <= 0) {
                holder.col6.setTextColor(ContextCompat.getColor(context,R.color.dtVal_1))
            } else if (day <= 365) {
                holder.col6.setTextColor(ContextCompat.getColor(context,R.color.dtVal_2))
            }

            if ((mList[position].reading.toString() == "√") && bindingA.bindingJjscan.chkJj.isChecked )
                holder.lljjscan.visibility = View.GONE


        }

        override fun onCreateViewHolder(
            parent: ViewGroup?,
            viewType: Int
        ): JJListAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_jjscan,
                parent,
                false
            )
            return ViewHolder(view)
        }

        private fun checkDate(eDate_org: String?): Long {
            var eDate = eDate_org
            val sDate = Calendar.getInstance()
            val eDate1 = Calendar.getInstance()

            if (eDate!!.length < 6) {
                return 900
            } else if (eDate.length == 6) {
                eDate += "01"
            }

            val y = Integer.parseInt(eDate.substring(0, 4))
            val m = Integer.parseInt(eDate.substring(4, 6))
            val d = Integer.parseInt(eDate.substring(6, 8))

            sDate.set(sDate.get(Calendar.YEAR), sDate.get(Calendar.MONTH) + 1, sDate.get(Calendar.DAY_OF_MONTH))
            eDate1.set(y, m, d)

            val b = (eDate1.timeInMillis - sDate.timeInMillis) / 1000

            return b / (60 * 60 * 24)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val col1 : TextView = itemView.findViewById(R.id.col1)
            val col2 : TextView = itemView.findViewById(R.id.col2)
            val col3 : TextView = itemView.findViewById(R.id.col3)
            val col4 : TextView = itemView.findViewById(R.id.scanCol4)
            val col5 : TextView = itemView.findViewById(R.id.scanCol5)
            val col6 : TextView = itemView.findViewById(R.id.scanCol6)
            val lljjscan : LinearLayout = itemView.findViewById(R.id.ll_jjscan)
        }
    }
//    class JJListAdapter(val context: Context, private var jjListItem:ArrayList<JJItem>) : BaseAdapter() {
//
//        override fun getItem(position: Int): Any {
//            return  jjListItem[position]
//        }
//
//        override fun getItemId(position: Int): Long {
//            return position.toLong()
//        }
//
//        override fun getCount(): Int {
//            return jjListItem.size
//        }
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val layoutInflater = LayoutInflater.from(context)
//            val res = R.layout.row_jjscan
//            val cView = layoutInflater.inflate(res, parent, false)
//
//            if (position and 1 == 0)
//                cView.setBackgroundResource(R.drawable.list_item_2)
//            else
//                cView.setBackgroundResource(R.drawable.list_item_3)
//
//
//            cView.col1.text = jjListItem[position].nmItem
//            cView.col2.text = jjListItem[position].nmSpec
//            cView.col3.text = jjListItem[position].serialNo
//            cView.col4.text = jjListItem[position].reading
//            cView.col5.text = jjListItem[position].ioDt
//            cView.col6.text = jjListItem[position].nmItem
//
//            val col1 = cView.findViewById(R.id.col1) as TextView
//            val col2 = cView.findViewById(R.id.col2) as TextView
//            val col3 = cView.findViewById(R.id.col3) as TextView
//            val col4 = cView.findViewById(R.id.col4) as TextView
//            val col5 = cView.findViewById(R.id.col5) as TextView
//            val col6 = cView.findViewById(R.id.col6) as TextView
//            val lljjscan = cView.findViewById(R.id.ll_jjscan) as LinearLayout
//
//            col1.text = jjListItem[position].nmItem
//            col2.text = jjListItem[position].nmSpec
//            col3.text = jjListItem[position].serialNo
//            col4.text = jjListItem[position].reading
//            col5.text = jjListItem[position].ioDt
//
//            var temp = jjListItem[position].dtVld
//            if (temp!!.isEmpty() || temp.length < 6) {
//
//            } else {
//                when(temp.length) {
//                    6 -> temp = temp.substring(0, 4) + "/" + temp.substring(4, 6)
//                    else -> temp = temp.substring(0, 4) + "/" + temp.substring(4, 6) + "/" + temp.substring(6, 8)
//                }
//            }
//            col6.text = temp
//
//            val day = checkDate(jjListItem[position].dtVld)
//
//            if (day <= 0) {
//                col6.setTextColor(ContextCompat.getColor(context,R.color.dtVal_1))
//            } else if (day <= 365) {
//                col6.setTextColor(ContextCompat.getColor(context,R.color.dtVal_2))
//            }
//
//            if ((jjListItem[position].reading.toString() == "√") && parent!!.rootView.chk_jj.isChecked )
//                lljjscan.visibility = View.GONE
//
//            return cView
//        }
//
//        private fun checkDate(eDate_org: String?): Long {
//            var eDate = eDate_org
//            val sDate = Calendar.getInstance()
//            val eDate1 = Calendar.getInstance()
//
//            if (eDate!!.length < 6) {
//                return 900
//            } else if (eDate.length == 6) {
//                eDate += "01"
//            }
//
//            val y = Integer.parseInt(eDate.substring(0, 4))
//            val m = Integer.parseInt(eDate.substring(4, 6))
//            val d = Integer.parseInt(eDate.substring(6, 8))
//
//            sDate.set(sDate.get(Calendar.YEAR), sDate.get(Calendar.MONTH) + 1, sDate.get(Calendar.DAY_OF_MONTH))
//            eDate1.set(y, m, d)
//
//            val b = (eDate1.timeInMillis - sDate.timeInMillis) / 1000
//
//            return b / (60 * 60 * 24)
//        }
//    }


    class ReadingRFID {
        companion object {
            var mTagItem: ArrayList<TagItem> = ArrayList()//스윙기로 읽은 데이타를 담을 그릇

            fun showDataJepum(Item: TagItem) {
                val pos = getPosition(Item)
                if (pos < 0) {                //읽은 태그가 중복이 아니면
                    mTagItem.add(Item)

                } else {                    //읽은 태그가 중복이면 아무것도 안한다.
                    // play sound
                    //checkAndSound(readMessage);
                }
            }

            //검색된것의 중복체크를 위한 함수 START : -1이면 신규 아니면 읽은것
            private fun getPosition(item: TagItem): Int {
                var position = -1

                var titem: TagItem?
                var oldID: String?

                for (i in 0 until mTagItem.size) {
                    titem = mTagItem[i]
                    oldID = titem.getEpcID()
                    if (oldID == item.getEpcID()) {
                        position = i
                        break
                    }
                    titem = null
                }
                return position
            }
            //검색된것의 중복체크를 위한 함수 END
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.dismiss()
        mTimeHandler!!.removeMessages(0)
    }
}
