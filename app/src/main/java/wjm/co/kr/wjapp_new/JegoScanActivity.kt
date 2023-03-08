package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.JegoScanActivity.ReadingRFID.Companion.mTagItem
import wjm.co.kr.wjapp_new.databinding.ActivityJegoScanBinding
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class JegoScanActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityJegoScanBinding
    private var cdCust : String? = null
    private var nmCust : String? = null
 //   lateinit var myCustomerAdapter : CustomerAdapter
    private lateinit var myScanAdapter : ScanAdapter
 //   private var arCustomerList : ArrayList<CustomerA> = ArrayList()
    private var arTagList : ArrayList<ScanItem> = ArrayList()

//    private lateinit var myCustomerList : ListView
//    private lateinit var btnCustomerSch : Button
//    private lateinit var edCustomer: TextView

    private lateinit var mTimeHandler : Handler
    private var querying = "N"
    private var sending = "N"

    private var scanCnt = 0
    private var scanCntBef = 0

    // SQLite DB관련
    private var db: SQLiteDatabase? = null
    private var dbName = "WJDB2"
    private lateinit var loadingDialog : LodingDialog

    private var mHandler : Handler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_jego_scan)
        bindingA = ActivityJegoScanBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        InventoryMenuActivity.reading_loc.topPage = "JEGOSCAN"

        if (!this.isFinishing) {
            loadingDialog = LodingDialog(bindingA.bindingJscan.JegoScanList.context)
        }

        if (WjmMain.LoginUser.sno == "") {
            Toast.makeText(baseContext, "로그인 정보가 사라졌어요!", Toast.LENGTH_LONG).show()
            val intent = Intent(baseContext, WjmMain::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
        }

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingJscan.JegoScanList.layoutManager = layoutManager
        myScanAdapter = ScanAdapter(this, arTagList)
        bindingA.bindingJscan.JegoScanList.adapter = myScanAdapter

        // 초기화 버튼
        bindingA.bindingJscan.btnInit.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("확인")
                .setMessage("스캔한 데이타를 초기화하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("확인") { _, _ ->
                    initData()
                    dbdelete()
                    Toast.makeText(baseContext, "초기화 하였습니다.", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("취소", null).show()
        }))

        // 읽은 데이터 전송
        bindingA.bindingJscan.btnSend.setOnClickListener(({
            when {
                nmCust!!.isEmpty() -> myToast(resources.getString(R.string.tag_nocust))
                mTagItem.size == 0 -> myToast(resources.getString(R.string.tag_nodata))
                else -> {
                    AlertDialog.Builder(this)
                        .setTitle("확인")
                        .setMessage("스캔한 데이타를 전송하시겠습니까?")
                        .setIcon(R.drawable.wjicon)
                        .setPositiveButton("확인") { _, _ ->
                            if (sending == "N") {
                                loadingDialog.show()
                                sending = "Y"
                                sendTags(makeTagList("ALL"))
                            } else Toast.makeText(this, R.string.tag_sending, Toast.LENGTH_LONG)
                                .show()
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        }))


        mTimeHandler = object : Handler(Looper.getMainLooper()) { // 5초 간격 무한 실행
            override fun handleMessage(msg: Message) {
                if (querying === "N" && cdCust != null) { //화면이 놀고있으면 돌고 쿼리~화면뿌리는중이라면 돌지말라..
                    scanCnt = mTagItem.size

                    if (scanCnt != scanCntBef) { //스윙기로 읽은수량에 차이가 있으면 진행
                        readTagInsertDB(scanCnt, scanCntBef)
                        loadingDialog.show()
                        querying = "Y"
                        scanCntBef = scanCnt

                        val tags = makeTagList("1")
                        getTags(tags)
//                        println(Tags)
                    }
                }
                this.sendEmptyMessageDelayed(0, 5000)
            }
        }
        mTimeHandler.sendEmptyMessage(0)

        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    0 -> {
                        cdCust = message.data.getString("custCd")
                        nmCust = message.data.getString("custNm")
                        bindingA.toolbar.title = "$nmCust 재고조사"
                        initData()
             //           chkNoneSendData() //이전에 전송하지 못하고 읽은 데이타가 있으면 표시
                    }
                }
            }
        }

        customerDialog(layoutInflater, mHandler!!, this, "").show()

        //DB관련
        db = openOrCreateDatabase(dbName, MODE_PRIVATE, null)
        dbdelete() //최초 진입시 db초기화
    }


    // 거래처 다이얼로그 Start
//    private fun customerDialog(): Dialog {
//        val layout = layoutInflater
//        val nullParent : ViewGroup? = null
//        val customerview = layout.inflate(R.layout.select_customer, nullParent)
//        val builder = AlertDialog.Builder(this)
//        builder.setIcon(R.drawable.search)
//        builder.setTitle(R.string.dia_T_cust)
//        builder.setCancelable(false)
//        builder.setPositiveButton(R.string.dia_confirm) { dialog, which ->
//            //확인버튼 터치시
//
//            val chkArr = myCustomerList.checkedItemPositions
//            if (chkArr.size() != 0) {
//                for (i in myCustomerList.count - 1 downTo -1 + 1) {
//                    if (chkArr.get(i)) {
//                        cdCust = myCustomerAdapter.getItem(i)?.custCd
//                        nmCust = myCustomerAdapter.getItem(i)?.custNm
//                        txt_scantitle.text = nmCust
//                        initData()
//                        chkNoneSendData() //이전에 전송하지 못하고 읽은 데이타가 있으면 표시
//                    }
//                }
//            } else {
//                Toast.makeText(applicationContext, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show()
//                customerDialog().show()
// //               dialog.cancel()
//            }
//        }
//        builder.setNegativeButton(R.string.dia_cancle) { dialog, which ->
//            //취소버튼 터치시.
//        }
//        //builder.setInverseBackgroundForced(true)
//        builder.setView(customerview)
//
//        myCustomerList = customerview.findViewById(R.id.listCustomer)
//        myCustomerList.adapter = myCustomerAdapter
//        myCustomerList.itemsCanFocus = false
//        myCustomerList.choiceMode = ListView.CHOICE_MODE_SINGLE
//
//
//        //검색버튼 액션 실행
//        btnCustomerSch = customerview.findViewById(R.id.btnCustomerSch)
//        btnCustomerSch.setOnClickListener(({
//            val str = edCustomer.text.toString().trim()
//            arCustomerList.clear()
//            myCustomerAdapter.notifyDataSetChanged()
//
//            if (str != "") {
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.hideSoftInputFromWindow(edCustomer.windowToken, 0)
//
//                if (getCustomers(str)) {
//
//                }
//                else
//                    myToast(resources.getString(R.string.cust_search_fail))
//            } else
//                myToast(resources.getString(R.string.cust_input_require))
//
//        }))
//
//
//        edCustomer = customerview.findViewById(R.id.edCustomer)
//        edCustomer.hint = "거래처명 입력부분"
//        edCustomer.imeOptions = EditorInfo.IME_ACTION_SEARCH
//        edCustomer.setOnEditorActionListener(object  : TextView.OnEditorActionListener {
//            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
//                when (actionId) {
//                    EditorInfo.IME_ACTION_SEARCH -> btnCustomerSch.performClick()
//                }
//                return false
//            }
//        })
//
//        return builder.create()
//    }
    // 거래처 다이얼로그 End

    // 거래처 리스트 불러오기(DB) START
//    private fun getCustomers(str: String): Boolean {
//        val url = URL("http://iclkorea.com/android/Common_Cust.asp")
//        val body = FormBody.Builder().add("search_word", str).build()
//        val request = Request.Builder().url(url).post(body).build()
//        val client = OkHttpClient()
//        client.newCall(request).enqueue(object : Callback {
//            override fun onResponse(call: Call?, response: Response?) {
//                val body = response?.body()?.string()
//                println("Success to execute request! : $body")
//
//                //Gson으로 파싱
//                val gson = GsonBuilder().create()
//                val dbCustomerList = gson.fromJson(body, DBCustomerList::class.java)
//
//                for (idx in 0 until dbCustomerList.results.size -1 )
//                    arCustomerList.add(CustomerA(dbCustomerList.results[idx].custNm, dbCustomerList.results[idx].custCd))
//
//                runOnUiThread { //UI에 알려줌
//                    myCustomerAdapter.notifyDataSetChanged()
//                }
//            }
//            override fun onFailure(call: Call?, e: IOException) {
//                println("Failed to execute request!")
//                println(e.message)
//            }
//        })
//        return true
//    }
    // 거래처 리스트 불러오기(DB) END

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchItem.isVisible = false
//        searchView = searchItem.actionView as SearchView
//        searchView!!.queryHint = "검색어 입력"
//        searchView!!.setOnQueryTextListener(queryTextListener)
//        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
//
//        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(this.componentName))
//        searchView!!.setIconifiedByDefault(true)
//        searchView!!.requestFocusFromTouch()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish(); return true
            }
            R.id.toolbar_cust_select -> customerDialog(layoutInflater, mHandler!!, this, "").show()
        }
        return super.onOptionsItemSelected(item)
    }

    // 태그 정보 불러오기(DB) START
    private fun getTags(str: String) {
        val url = URL("http://iclkorea.com/android/JegoScan_TagInfo.asp")
        val body = FormBody.Builder().add("tags", str).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbTagList = gson.fromJson(returnbody, DBTagList::class.java)

                for (idx in dbTagList.results.indices) {
                    arTagList.add(
                        ScanItem(
                            dbTagList.results[idx].cdItem,
                            dbTagList.results[idx].nmItem,
                            dbTagList.results[idx].nmSpec,
                            dbTagList.results[idx].dtVld,
                            dbTagList.results[idx].serialNo,
                            dbTagList.results[idx].tagNo,
                            dbTagList.results[idx].dtIO,
                            dbTagList.results[idx].recentStatus
                        )
                    )
                }
                querying = "N"

                val comp: Comparator<ScanItem> =
                    Comparator<ScanItem> { o1, o2 ->
                        val v1 = o1.nmItem + o1.nmSpec
                        val v2 = o2.nmItem + o2.nmSpec
                        v1.compareTo(v2)
                    }
                Collections.sort(arTagList, comp)

                runOnUiThread { //UI에 알려줌
                    myScanAdapter.notifyDataSetChanged()
                    bindingA.bindingJscan.txtScanqty.text =
                        "총 스캔수량 : " + myScanAdapter.itemCount + "개"
                    loadingDialog.hide()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }
    // 태그 정보 불러오기(DB) END

    // 읽은 태그 전송하기(DB) START
    private fun sendTags(str: String): Boolean {
//        println(WjmMain.LoginUser.sno)
        val url = URL("http://iclkorea.com/android/JegoScan_TagSend.asp")
        val body = FormBody.Builder().add("send_tags", str).add("ccode", WjmMain.LoginUser.ccode).add(
            "cd_usr",
            WjmMain.LoginUser.sno
        ).add("reader_no", InventoryMenuActivity.SwingInfo.swing_NAME).add("cust", cdCust!!).build()
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
                    dbsendOK()
                    runOnUiThread {
                        myToast(resources.getString(R.string.tag_sendOK))
                        initData()
                        sending = "N"
                        loadingDialog.hide()
                    }
                } else {
                    runOnUiThread {
                        //UI에 알려줌
                        myToast(resources.getString(R.string.tag_sendingErr))
                        loadingDialog.hide()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 읽은 태그 전송하기(DB) END

//    data class DBCustomerList(val results : List<Customer>)
//    data class Customer (var custNm:String?, var custCd:String?)
    data class DBTagList(val results: List<ScanItem>)
    data class ScanItem(
        var cdItem: String?,
        var nmItem: String?,
        var nmSpec: String?,
        var dtVld: String?,
        var serialNo: String?,
        var tagNo: String?,
        var dtIO: String?,
        var recentStatus: String?
    )
    data class DBSendOK(val results: String?)

    @SuppressLint("NotifyDataSetChanged")
    private fun initData() {
        scanCnt = 0
        scanCntBef = 0
        tagBefSize = 0
        arTagList.clear()
        mTagItem.clear()
        bindingA.bindingJscan.txtScanqty.text = "총 스캔수량 : 0개"
        myScanAdapter.notifyDataSetChanged()
        if(InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
            InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
    //        InventoryMenuActivity.mSwing!!.prisma_tagDel()
        }
//        MainMenu.mSwing.swing_clear_inventory()

    }

    inner class ScanAdapter(private val context: Context, private var itemList: ArrayList<ScanItem>) : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ScanAdapter.ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.llrow.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.llrow.setBackgroundResource(R.drawable.list_item_3)

            holder.col4.text = itemList[position].nmItem
            holder.col5.text = itemList[position].nmSpec
            holder.col6.text = itemList[position].serialNo
            holder.col7.text = itemList[position].dtVld
            holder.col8.text = itemList[position].dtIO
            if (itemList[position].recentStatus == "CS") {
                holder.llrow.setBackgroundResource(R.drawable.list_item_4)
                holder.col4.setText("저장-> ${itemList[position].nmItem} <-정리")
            }

            val day = checkDate(itemList[position].dtVld!!.trim())
            if (day <= 180) {
                holder.col4.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
            }else if (day <= 365) {
                holder.col4.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
            }else {
                holder.col4.setTextColor(Color.BLACK)
            }
            holder.col5.text = itemList[position].nmSpec + " : " + day.toString()
            val totWidth = bindingA.bindingJscan.JegoScanList.width
            val temp = totWidth/3/20
            holder.col4.width = totWidth/3
            holder.col5.width = totWidth/3-(temp * 2)
            holder.col6.width = totWidth/3/2 + temp
            holder.col7.width = totWidth/3/2 + temp
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ScanAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.jego_scan_row, parent, false)

            return ViewHolder(view)
        }

        private fun checkDate(eDt: String?): Long {
            var eDate = eDt
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
            sDate.set(
                sDate.get(Calendar.YEAR),
                sDate.get(Calendar.MONTH) + 1,
                sDate.get(Calendar.DAY_OF_MONTH)
            )
            eDate1.set(y, m, d)

            val b = (eDate1.timeInMillis - sDate.timeInMillis) / 1000

            return b / (60 * 60 * 24)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val col4: TextView = itemView.findViewById(R.id.scanCol4)
            val col5: TextView = itemView.findViewById(R.id.scanCol5)
            val col6: TextView = itemView.findViewById(R.id.scanCol6)
            val col7: TextView = itemView.findViewById(R.id.scanCol7)
            val col8: TextView = itemView.findViewById(R.id.scanCol8)
            val llrow : LinearLayout = itemView.findViewById(R.id.ll_jaego_row)
        }
    }

//    class ScanAdapter(val context: Context, private var scanListItem:ArrayList<ScanItem>) : BaseAdapter() {
//
//        override fun getItem(position: Int): Any {
//            return  scanListItem[position]
//        }
//
//        override fun getItemId(position: Int): Long {
//            return position.toLong()
//        }
//
//        override fun getCount(): Int {
//            return scanListItem.size
//        }
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val layoutInflater = LayoutInflater.from(context)
//            val res = R.layout.jego_scan_row
//            val convertView = layoutInflater.inflate(res, parent, false)
//
//            if (position and 1 == 0)
//                convertView.setBackgroundResource(R.drawable.list_item_2)
//            else
//                convertView.setBackgroundResource(R.drawable.list_item_3)
//
//            convertView.col4.text = scanListItem[position].nmItem
//            convertView.col5.text = scanListItem[position].nmSpec
//            convertView.col6.text = scanListItem[position].serialNo
//            convertView.col7.text = scanListItem[position].dtVld
//            convertView.col8.text = scanListItem[position].dtIO
//
//            val day = checkDate(scanListItem[position].dtVld!!.trim())
//            if (day <= 180) {
//                convertView.col4.setTextColor(ContextCompat.getColor(context,R.color.dtVal_1))
//            }else if (day <= 365) {
//                convertView.col4.setTextColor(ContextCompat.getColor(context,R.color.dtVal_2))
//            }
//
//            val totWidth = parent!!.rootView.JegoScanList.width
//            val temp = totWidth/3/20
//            convertView.col4.width = totWidth/3
//            convertView.col5.width = totWidth/3-(temp * 2)
//            convertView.col6.width = totWidth/3/2 + temp
//            convertView.col7.width = totWidth/3/2 + temp
//
//            return convertView
//        }
//
//        fun checkDate(eDate: String?): Long {
//            var eDate = eDate
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
//            sDate.set(sDate.get(Calendar.YEAR), sDate.get(Calendar.MONTH) + 1, sDate.get(Calendar.DAY_OF_MONTH))
//            eDate1.set(y, m, d)
//
//            val b = (eDate1.timeInMillis - sDate.timeInMillis) / 1000
//
//            return b / (60 * 60 * 24)
//        }
//    }

//    class ItemAdapterSingle(val context: Context, var arItem:ArrayList<Customer>):BaseAdapter(), Filterable{
//
//        internal var mOriginalValues:ArrayList<Customer>? = null // Original Values
////
////        fun clear() {
////            arItem.clear()
////            this.notifyDataSetChanged()
////        }
//
//        override fun getCount():Int {
//            return arItem.size
//        }
//
//        override fun getItem(position:Int):Customer? {
//            return arItem[position]
//        }
//
//        override fun getItemId(position:Int):Long {
//            return position.toLong()
//        }
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val layoutInflater = LayoutInflater.from(context)
//            val res = R.layout.list_item_radiobutton
//            val convertView = layoutInflater.inflate(res, parent, false)
//
//            if (position and 1 == 0) {
//                convertView.setBackgroundResource(R.drawable.list_item_2)
//            } else {
//                convertView.setBackgroundResource(R.drawable.list_item_3)
//            }
//
//            val col1 = convertView.findViewById(android.R.id.text1) as TextView
//            col1.text = arItem[position].custNm
//
//            val col2 = convertView.findViewById(android.R.id.text2) as TextView
//            col2.text = arItem[position].custCd
//
//            return convertView
//        }
//
//        override fun getFilter(): Filter {
//            return  object : Filter() {
//                override fun publishResults(constraint: CharSequence, results: FilterResults) {
//
//                    //myData = (List<MyDataType>) results.values;
//                    //MyCustomAdapter.this.notifyDataSetChanged();
//                    arItem = results.values as ArrayList<Customer>
//                    notifyDataSetChanged()
//                }
//
//                override fun performFiltering(constraint: CharSequence?): FilterResults {
//                    var constraint = constraint
//                    val results = FilterResults()        // Holds the results of a filtering operation in values
//                    val filteredArrList = ArrayList<Customer>()
//
//                    if (mOriginalValues == null) {
//                        mOriginalValues = ArrayList(arItem) // saves the original data in mOriginalValues
//                    }
//
//                    if (constraint == null || constraint.isEmpty()) {
//
//                        // set the Original result to return
//                        results.count = mOriginalValues!!.size
//                        results.values = mOriginalValues
//                    } else {
//                        constraint = constraint.toString().toLowerCase()
//                        for (i in 0 until mOriginalValues!!.size) {
//                            val data : String? = mOriginalValues!![i].custNm
//                            if (data!!.toLowerCase().startsWith(constraint.toString())) {
//                                filteredArrList.add(mOriginalValues!![i])
//                            }
//                        }
//                        // set the Filtered result to return
//                        results.count = filteredArrList.size
//                        results.values = filteredArrList
//                    }
//                    return results
//                }
//            }
//          //  return filter
//        }
//    }

    class ReadingRFID {
        companion object {
            var mTagItem: ArrayList<TagItem> = ArrayList()//스윙기로 읽은 데이타를 담을 그릇

            fun showDataJepum(Item: TagItem) {
                val pos = getPosition(Item)
                if (pos < 0) {                //읽은 태그가 중복이 아니면
                    mTagItem.add(Item)
                    //            SetTagItem(Item);
                    //println("mTagItem" + mTagItem.size.toString())

                }
//                else {                    //읽은 태그가 중복이면 아무것도 안한다.
//                    // play sound
//                    //checkAndSound(readMessage);
//                }
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
//                    titem = null
                }

                return position
            }
            //검색된것의 중복체크를 위한 함수 END
        }
    }


    //Tag 리스트 만들기 Start
    private var tagBefSize = 0
    private fun makeTagList(gbn: String): String {
        var tagList = ""
        val tagNowSize = mTagItem.size

        when (gbn) {
            "ALL" -> {
                for (i in 0 until tagNowSize) {
                    if (i == 0) {
                        tagList = "'" + mTagItem[i].epcID_Ascii + "'"
                    }
                    tagList = tagList + "," + "'" + mTagItem[i].epcID_Ascii + "'"
                }
            }
            else -> {
                for (i in tagBefSize until tagNowSize) {
                    if (i == 0) {
                        tagList = "'"+mTagItem[i].epcID_Ascii+"'"
                    }
                    tagList = tagList + "," + "'"+mTagItem[i].epcID_Ascii+"'"
                }
                tagBefSize = tagNowSize
            }
        }
        return tagList
    }
    //Tag 리스트 만들기 End


// 데이타 중복 타병원에서 읽히는 증상 이 있어 기능 끔
//    private fun chkNoneSendData() {
//        val cs1 = db!!.rawQuery(
//            "select no_tag from reader_info where yn = 'N' and cd_cust = '$cdCust'",
//            null
//        )
//
//        if (cs1!!.count != 0) {
//            for (i in 0 until cs1.count) {
//              //  loadingDialog.show()
//                cs1.moveToNext()
//                val imsiValue = TagItem(cs1.getString(0))
//                //                Toast.makeText(getBaseContext(),cs1.getString(0), Toast.LENGTH_SHORT).show();
//                imsiValue.setEpcID_Ascii(cs1.getString(0))
//                ReadingRFID.showDataJepum(imsiValue)
//            }
//        }
//  //      myScanAdapter.notifyDataSetChanged()
//        cs1.close()
//
//    }

//    private fun dbDeleteDialog() {
//        val adb = AlertDialog.Builder(this)
//
//        adb.setTitle(R.string.dia_T_imsitag)
//        adb.setMessage("Clinic : $nmCust\n의 전송되지 않은 Tag정보를 삭제 및 초기화합니다.")
//        adb.setPositiveButton(R.string.dia_yes) { dialog, which ->

//            val whereArg = arrayOf(cdCust)
//            val affectRow = db!!.delete("reader_info", "cd_cust =?", whereArg)
//
//            if (affectRow == 0)
//                Toast.makeText(baseContext, R.string.db_delTagNOT, Toast.LENGTH_SHORT).show()
//            else {
//                Toast.makeText(baseContext, R.string.db_delTagOK, Toast.LENGTH_SHORT).show()
//                initData()
//            }
//        }.setNegativeButton(
//            R.string.dia_no
//        ) { dialog, which -> dialog.dismiss() }
//        adb.show()
//    }

    private fun readTagInsertDB(newCnt: Int, oldCnt: Int) {
        val today = Date()
        val sdformat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dtday = sdformat.format(today).toString()

        for (i in oldCnt until newCnt) {
            db!!.execSQL(
                "insert into reader_info(dt_day, cd_cust, no_tag, yn) values('" + dtday + "', '" + cdCust + "', '" + mTagItem[i].epcID_Ascii + "', 'N');"
            )
        }
    }

    private fun dbsendOK() {
        val whereArg = arrayOf(cdCust)
        db!!.delete("reader_info", "cd_cust =?", whereArg)
        //        Toast.makeText(getBaseContext(), String.valueOf(affectRow), Toast.LENGTH_SHORT).show();
    }

    private fun dbdelete() {
        val whereArg = arrayOf(cdCust)
        db!!.delete("reader_info", "cd_cust =?", whereArg)
    }

    private fun myToast(msg: String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.dismiss()
        mTimeHandler.removeMessages(0)
    }


}
