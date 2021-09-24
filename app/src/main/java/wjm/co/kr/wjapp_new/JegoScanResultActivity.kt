package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityJegoScanResultBinding
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class JegoScanResultActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var bindingA: ActivityJegoScanResultBinding
    private lateinit var spinnerDate : Spinner
    private lateinit var spinnerDateAdapter : ArrayAdapter<String>
    private lateinit var myScanCustomerAdapter : ItemAdapterSingle
    private var tagList : ArrayList<TagList> = ArrayList()
    private lateinit var tagListAdapter : DayScanAdapter

    private var monthlist = ArrayList<String>()
    private var selmonth : String? = null

    private var nmCust :String? = null
    private var cdCust :String? = null
//    private var scanSno : String? = ""


    class SearchDate {
        companion object {
            var strdate : String? = null
        }
    }

    private var arCustomerList : ArrayList<Customer> = ArrayList()
    private var arDate = ArrayList<String>()
    private var arName = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_jego_scan_result)
        bindingA = ActivityJegoScanResultBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        scanResultInit()
    }

    private fun scanResultInit() {
        myScanCustomerAdapter = ItemAdapterSingle(this, arCustomerList)
        scanCustomerDialog().show()

        spinnerDate = findViewById(R.id.spinner_result1)
        spinnerDate.onItemSelectedListener = this
        spinnerDate.id = Integer.parseInt("1")
        spinnerDateAdapter = SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, arDate)
        spinnerDate.adapter = spinnerDateAdapter

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingScanResult.JegoScanResultList.layoutManager = layoutManager
        tagListAdapter = DayScanAdapter(this, tagList)
        bindingA.bindingScanResult.JegoScanResultList.adapter = tagListAdapter
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
            R.id.toolbar_cust_select -> {
                initJegoResult()
                scanCustomerDialog().show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initJegoResult() {
        tagList.clear()
        monthlist.clear()
        arCustomerList.clear()
        arDate.clear()
        arName.clear()
        bindingA.bindingScanResult.txtDay.text = ""
        bindingA.bindingScanResult.txtDaytot.text = ""
        myScanCustomerAdapter.notifyDataSetChanged()
        spinnerDateAdapter.notifyDataSetChanged()
        tagListAdapter.notifyDataSetChanged()
    }

    // 스캔한 거래처 다이얼로그 Start
    private fun scanCustomerDialog(): Dialog {
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.select_scan_customer, nullParent)
        val builder = AlertDialog.Builder(this)
        // array set months Start
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        for (i in 0 .. 59) {
            calendar.add(Calendar.MONTH, -1)
            monthlist.add(SimpleDateFormat("yy年 MM月", Locale.getDefault()).format(calendar.time))
        }
        // array set months End
        val spinnerScanC : Spinner = view.findViewById(R.id.spinnerScanC)
        val adapter3 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, monthlist )
        spinnerScanC.id = Integer.parseInt("3")
        spinnerScanC.adapter = adapter3
        spinnerScanC.setSelection(0)
        spinnerScanC.onItemSelectedListener = this

        val myCustomerList : ListView = view.findViewById(R.id.listScanC) as ListView
        myCustomerList.adapter = myScanCustomerAdapter
        myCustomerList.itemsCanFocus = false
        myCustomerList.choiceMode = ListView.CHOICE_MODE_SINGLE

        builder.setCancelable(false)
        builder.setPositiveButton("확인") { _, _ ->
            val selectRow = myScanCustomerAdapter.getSelectRow()
            if (selectRow != -1){
                cdCust = myScanCustomerAdapter.getItem(selectRow).custCd
                nmCust = myScanCustomerAdapter.getItem(selectRow).custNm
                val actionBar = supportActionBar
                actionBar!!.title = nmCust

                getDateList(cdCust!!)
            } else {
                Toast.makeText(applicationContext, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show()
            }
//            val chkArr = myCustomerList.checkedItemPositions
//            if (chkArr.size() != 0) {
//                for (i in myCustomerList.count  downTo -1 + 1) {
//                    if (chkArr.get(i)) {
//                        cdCust = myScanCustomerAdapter.getSelectRow().custCd
//                        nmCust = myScanCustomerAdapter.getItem(i).custNm
//                        val actionBar = supportActionBar
//                        actionBar!!.title = nmCust
//                        //txt_resulttitle.text = nmCust
//                    }
//                }
//                getDateList(cdCust!!)
//
//            } else {
//                Toast.makeText(applicationContext, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show()
//            }
        }
        builder.setView(view)
        myScanCustomerAdapter.notifyDataSetChanged()

        return builder.create()
    }
    // 스캔한 거래처 다이얼로그 End

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent!!.id) {
            1 -> {
                tagList.clear()
                SearchDate.strdate = arDate[position]
                bindingA.bindingScanResult.txtScanName.text = String.format("스캔 : " + arName[position])
                getTagList(cdCust, SearchDate.strdate!!.substring(0, 10) )
            }

            3 -> { //month Selecting in first dialog
                selmonth = "20" + monthlist[position].substring(0,2) + monthlist[position].substring(4,6)
                myScanCustomerAdapter.clear()
                getCustList(selmonth)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
    }

    // 거래처 리스트 불러오기(DB) START
    private fun getCustList(str: String?): Boolean {
        val url = URL("http://iclkorea.com/android/JegoResult_CustList.asp")
        val body = FormBody.Builder().add("selmonth", str!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
//                println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbCustomerList = gson.fromJson(returnbody, DBCustomerList::class.java)

                for (idx in dbCustomerList.results.indices)
                    arCustomerList.add(
                        Customer(
                            dbCustomerList.results[idx].custNm,
                            dbCustomerList.results[idx].custCd
                        )
                    )

                runOnUiThread { //UI에 알려줌
                    myScanCustomerAdapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 거래처 리스트 불러오기(DB) END

    // 선택한 거래처 리딩 일자 불러오기(DB) START
    private fun getDateList(str: String): Boolean {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoResult_DateList.asp")
        val body = FormBody.Builder().add("cust", str).add("sno", WjmMain.LoginUser.sno).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
                //println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbDateList = gson.fromJson(returnbody, DBDateList::class.java)
                var dayposition = -1

                for (idx in dbDateList.results.indices) {
                    arDate.add(dbDateList.results[idx].outDate!!)
                    arName.add(dbDateList.results[idx].scanSno!!)
                    if (dayposition == -1 && selmonth!!.toInt() >= (dbDateList.results[idx].outDate!!.substring(0,4) + dbDateList.results[idx].outDate!!.substring(5,7)).toInt())
                        dayposition = idx
                }
                runOnUiThread { //UI에 알려줌
                    spinnerDateAdapter.notifyDataSetChanged()
                    spinnerDate.setSelection(dayposition)
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 선택한 거래처 리딩 일자 불러오기(DB) END

    // 선택한 일자의 태그 정보 불러오기(DB) START
    private fun getTagList(arg1: String?, arg2:String?): Boolean {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoResult_TagList.asp")
        val body = FormBody.Builder().add("cust", arg1!!).add("seldate", arg2!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val returnbody = response.body?.string()
//                println("Success to execute request! : $returnbody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbTagList = gson.fromJson(returnbody, DBTagList::class.java)

                for (idx in dbTagList.results.indices)
                    tagList.add(TagList(dbTagList.results[idx].cdItem, dbTagList.results[idx].nmItem, dbTagList.results[idx].nmSpec, dbTagList.results[idx].barcode, dbTagList.results[idx].dtVld, dbTagList.results[idx].TAG_NO, dbTagList.results[idx].dtIO))

                countSum(tagList)

                runOnUiThread { //UI에 알려줌
                    tagListAdapter.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
        return true
    }
    // 선택한 일자의 태그 정보 불러오기(DB) END

    private fun countSum(tagList : ArrayList<TagList>) {
        var tempcnt = 0
        var totcnt = 0
        var resulttext : String? = "X"
        var tempitem : String? = null

        for (idx in 0 until tagList.size) {
            if (tempitem.equals(tagList[idx].nmItem)) {
                tempcnt ++
                totcnt ++
            } else {
                if (resulttext.equals("X"))
                    resulttext = tagList[idx].nmItem + ":"
                else {
                    resulttext = resulttext + tempcnt + "개, " + tagList[idx].nmItem + ":"
                    tempcnt = 0
                }
                tempcnt ++
                totcnt ++
                tempitem = tagList[idx].nmItem
            }
        }
        resulttext += tempcnt.toString() + "개"
        runOnUiThread {
            bindingA.bindingScanResult.txtDay.text = resulttext
            bindingA.bindingScanResult.txtDaytot.text = String.format("총수량:"+totcnt+"개")
        }
    }

    data class DBCustomerList(val results : List<Customer>)
    data class Customer (var custNm:String?, var custCd:String?)
    data class DBDateList(val results : List<DateList>)
    data class DateList (var outDate:String?, var scanSno:String?)
    data class DBTagList(val results : List<TagList>)
    data class TagList (var cdItem:String?, var nmItem:String?, var nmSpec:String?, var barcode:String?, var dtVld:String?, var TAG_NO:String?, var dtIO:String?)

    inner class DayScanAdapter(private val context: Context, private val itemList:ArrayList<TagList>) : RecyclerView.Adapter<DayScanAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onBindViewHolder(holder: DayScanAdapter.ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.llrow.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.llrow.setBackgroundResource(R.drawable.list_item_3)

            holder.col2.text = itemList[position].nmItem
            holder.col3.text = itemList[position].nmSpec
            holder.col4.text = itemList[position].barcode
            if (itemList[position].dtVld!!.length < 6)
                holder.col5.text = itemList[position].dtVld
            else
                holder.col5.text = itemList[position].dtVld!!.substring(0, 6)
            holder.col6.text = itemList[position].dtIO

            val day = checkDate(itemList[position].dtVld!!.trim(), SearchDate.strdate)

            when {
                day<=180 -> {
                    holder.col5.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
                    holder.col1.text = "⑥"
                    holder.col1.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
                }
                day<=365 -> {
                    holder.col5.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
                    holder.col1.text = "⑫"
                    holder.col1.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
                }
                else -> {
                    holder.col1.text = ""
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup?,
            viewType: Int
        ): DayScanAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.day_result_row, parent, false)
            return ViewHolder(view)
        }

        private fun checkDate(eDate2: String?, sDate: String?): Long {
            var eDate = eDate2
            val sDate1 = Calendar.getInstance()
            val eDate1 = Calendar.getInstance()

            if (eDate!!.length < 6) {
                return 900
            } else if (eDate.length == 6) {
                eDate += "01"
            }
            val y = Integer.parseInt(eDate.substring(0, 4))
            val m = Integer.parseInt(eDate.substring(4, 6))
            val d = Integer.parseInt(eDate.substring(6, 8))
            val sy = Integer.parseInt(sDate!!.substring(0, 4))
            val sm = Integer.parseInt(sDate.substring(5, 7))
            val sd = Integer.parseInt(sDate.substring(8, 10))
            //        sDate1.set(sDate1.get(Calendar.YEAR), sDate1.get(Calendar.MONTH)+1, sDate1.get(Calendar.DAY_OF_MONTH)); //현재 조회날짜
            sDate1.set(sy, sm, sd) // 검사날짜기준 조회

            eDate1.set(y, m, d)
            val b = (eDate1.timeInMillis - sDate1.timeInMillis) / 1000

            return b / (60 * 60 * 24)
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val col1 : TextView = itemView.findViewById(R.id.day_col1)
            val col2 : TextView = itemView.findViewById(R.id.day_col2)
            val col3 : TextView = itemView.findViewById(R.id.day_col3)
            val col4 : TextView = itemView.findViewById(R.id.day_col4)
            val col5 : TextView = itemView.findViewById(R.id.day_col5)
            val col6 : TextView = itemView.findViewById(R.id.day_col6)
            val llrow : LinearLayout = itemView.findViewById(R.id.ll_day_row)
        }
    }
//    class DayScanAdapter(var context: Context, private var arProd: ArrayList<TagList>) : BaseAdapter() {
//
//        override fun getItem(position: Int): Any {
//            return arProd[position]
//        }
//
//        override fun getItemId(position: Int): Long {
//            return position.toLong()
//        }
//
//        override fun getCount(): Int {
//            return arProd.size
//        }
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val layoutInflater = LayoutInflater.from(context)
//            val res = R.layout.day_result_row
//            val convertView = layoutInflater.inflate(res, parent, false)
//
//            if (position and 1 == 0)
//                convertView.setBackgroundResource(R.drawable.list_item_2)
//            else
//                convertView.setBackgroundResource(R.drawable.list_item_3)
//
//            convertView.day_col2.text = arProd[position].nmItem
//            convertView.day_col3.text = arProd[position].nmSpec
//            convertView.day_col4.text = arProd[position].barcode
//            if (arProd[position].dtVld!!.length < 6)
//                convertView.day_col5.text = arProd[position].dtVld
//            else
//                convertView.day_col5.text = arProd[position].dtVld!!.substring(0, 6)
//            convertView.day_col6.text = arProd[position].dtIO
//
//            val day = checkDate(arProd[position].dtVld!!.trim(), SearchDate.strdate)
//            if (day <= 180) {
//                convertView.day_col5.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
//                convertView.day_col1.text = "⑥"
//                convertView.day_col1.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
//            } else if (day <= 365) {
//                convertView.day_col5.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
//                convertView.day_col1.text = "⑫"
//                convertView.day_col1.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
//            } else
//                convertView.day_col1.text = ""
//
//            return convertView
//        }
//
//        private fun checkDate(eDate2: String?, sDate: String?): Long {
//            var eDate = eDate2
//            val sDate1 = Calendar.getInstance()
//            val eDate1 = Calendar.getInstance()
//
//            if (eDate!!.length < 6) {
//                return 900
//            } else if (eDate.length == 6) {
//                eDate += "01"
//            }
//            val y = Integer.parseInt(eDate.substring(0, 4))
//            val m = Integer.parseInt(eDate.substring(4, 6))
//            val d = Integer.parseInt(eDate.substring(6, 8))
//            val sy = Integer.parseInt(sDate!!.substring(0, 4))
//            val sm = Integer.parseInt(sDate.substring(5, 7))
//            val sd = Integer.parseInt(sDate.substring(8, 10))
//            //        sDate1.set(sDate1.get(Calendar.YEAR), sDate1.get(Calendar.MONTH)+1, sDate1.get(Calendar.DAY_OF_MONTH)); //현재 조회날짜
//            sDate1.set(sy, sm, sd) // 검사날짜기준 조회
//
//            eDate1.set(y, m, d)
//            val b = (eDate1.timeInMillis - sDate1.timeInMillis) / 1000
//
//            return b / (60 * 60 * 24)
//        }
//    }

    class ItemAdapterSingle(val context: Context, var arItem:ArrayList<Customer>): BaseAdapter(), Filterable {
        var selectItem : Int = -1

        internal var mOriginalValues:ArrayList<Customer>? = null // Original Values

        fun clear() {
            arItem.clear()
            this.notifyDataSetChanged()
        }

        override fun getCount():Int {
            return arItem.size
        }

        override fun getItem(position:Int): Customer {
            return arItem[position]
        }

        override fun getItemId(position:Int):Long {
            return position.toLong()
        }

        fun getSelectRow() : Int {
            return selectItem
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(context)
            val res = R.layout.list_item_selectbutton
            val convertView1 = layoutInflater.inflate(res, parent, false)

            if (position and 1 == 0) {
                convertView1.setBackgroundResource(R.drawable.list_item_2)
            } else {
                convertView1.setBackgroundResource(R.drawable.list_item_3)
            }

            val col1 = convertView1.findViewById(android.R.id.text1) as TextView
            col1.text = arItem[position].custNm

            val col2 = convertView1.findViewById(android.R.id.text2) as TextView
            col2.text = arItem[position].custCd

            if (selectItem == -1 || selectItem != position)
                col1.setTextColor(Color.BLACK)
            else
                col1.setTextColor(Color.RED)

            val btnselect : Button = convertView1.findViewById(R.id.btnCustSelect)
            btnselect.setOnClickListener {
                selectItem = position
                this.notifyDataSetChanged()
            }

            return convertView1
        }

        override fun getFilter(): Filter {
            val filter = object : Filter() {
                override fun publishResults(constraint: CharSequence, results: FilterResults) {

                    //myData = (List<MyDataType>) results.values;
                    //MyCustomAdapter.this.notifyDataSetChanged();
                    arItem = results.values as ArrayList<Customer>
                    notifyDataSetChanged()
                }

                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    //var constraint = constraint
                    val results = FilterResults()        // Holds the results of a filtering operation in values
                    val filteredArrList = ArrayList<Customer>()

                    if (mOriginalValues == null) {
                        mOriginalValues = ArrayList(arItem) // saves the original data in mOriginalValues
                    }

                    if (constraint == null || constraint.isEmpty()) {

                        // set the Original result to return
                        results.count = mOriginalValues!!.size
                        results.values = mOriginalValues
                    } else {
                        val lowconst = constraint.toString().toLowerCase(Locale.ROOT)
                        for (i in 0 until mOriginalValues!!.size) {
                            val data : String? = mOriginalValues!![i].custNm
                            if (data!!.toLowerCase(Locale.ROOT).startsWith(lowconst)) {
                                filteredArrList.add(mOriginalValues!![i])
                            }
                        }
                        // set the Filtered result to return
                        results.count = filteredArrList.size
                        results.values = filteredArrList
                    }
                    return results
                }
            }
            return filter
        }
    }

    inner class SpinnerAdapter(internal var context: Context, textViewResourceId: Int, objects: ArrayList<String>) :
        ArrayAdapter<String>(context, textViewResourceId, objects) {

        private var items = objects

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cView = convertView

            if (cView == null) {
                val inflater = LayoutInflater.from(context)
                cView = inflater.inflate(
                    android.R.layout.simple_spinner_dropdown_item, parent, false
                )

            }

            val tv = cView!!.findViewById(android.R.id.text1) as TextView
            tv.text = items[position]
            tv.setBackgroundColor(Color.DKGRAY)
            tv.setTextColor(Color.WHITE)
            tv.textSize = 15f
            return cView

        }

        @SuppressLint("RtlHardcoded")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cView = convertView

            if (cView == null) {
                val inflater = LayoutInflater.from(context)
                cView = inflater.inflate(
                    android.R.layout.simple_spinner_item, parent, false
                )
            }

            val tv = cView!!.findViewById(android.R.id.text1) as TextView
            tv.text = items[position]
            tv.setTextColor(Color.WHITE)
            tv.textSize = 18f
            tv.gravity = Gravity.RIGHT
            return cView

        }

    }
}
