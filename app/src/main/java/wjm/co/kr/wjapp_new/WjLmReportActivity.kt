package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.os.Looper
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjLmReportBinding
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.util.concurrent.TimeUnit


class WjLmReportActivity : AppCompatActivity() { //}, SlyCalendarDialog.Callback{
    private lateinit var bindingA: ActivityWjLmReportBinding

    private var custCd : String? = null
    private var custNm : String? = null
    private var selCcode : String? = null
    private var selDateFr : String = ""
    private var selDateTo : String = ""
    private var arLmList : ArrayList<LmList> = ArrayList()
    private var originList : ArrayList<LmList> = ArrayList()
    private var lmListAdapter : LmListAdapter? = null
    private var searchView: SearchView? = null
    private var mHandler : Handler? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_wj_lm_report)
        bindingA = ActivityWjLmReportBinding.inflate(layoutInflater)
        setContentView(bindingA.root)

        lmReportInit()

//        txt_date_lm.setOnClickListener(({
//            SlyCalendarDialog()
//                .setSingle(false)
//                .setCallback(this)
//                .show(supportFragmentManager, "TAG_SLYCALENDAR")
//        }))

        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    0 -> {
                        custCd = message.data.getString("custCd")
                        custNm = message.data.getString("custNm")
                        bindingA.toolbar.title = "$custNm 거래처 원장"
                        arLmList.clear()
                        originList.clear()
                        lmListAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }
        customerDialog(layoutInflater, mHandler!!, this, "").show()

        // recycler view adapter setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingLmReport.reViewLm.layoutManager = layoutManager
        lmListAdapter = LmListAdapter(this, arLmList)
        bindingA.bindingLmReport.reViewLm.adapter = lmListAdapter

        bindingA.bindingLmReport.btnSerarchLm.setOnClickListener(({
            if(checkDate() && checkSearch()) {
               // arLmList.clear()
                originList.clear()
                lmListAdapter!!.notifyDataSetChanged()
                if (!searchView!!.isIconified) {
                    searchView!!.isIconified = true
                    searchView!!.clearFocus()
                    searchView!!.onActionViewCollapsed()
                }
                getLmDate()
            }
        }))

        bindingA.bindingLmReport.layoutMainLm.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                    bindingA.bindingLmReport.etxtDtfrLm.clearFocus()
                    bindingA.bindingLmReport.etxtDttoLm.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        bindingA.bindingLmReport.reViewLm.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                    bindingA.bindingLmReport.etxtDtfrLm.clearFocus()
                    bindingA.bindingLmReport.etxtDttoLm.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuFax = menu.findItem(R.id.toolbar_cust_fax)
        menuFax.isVisible = true

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = "검색어 입력"
        searchView!!.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        searchView!!.setOnQueryTextListener(queryTextListener)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(this.componentName))
        searchView!!.isIconifiedByDefault = true
        searchView!!.requestFocusFromTouch()
        return super.onCreateOptionsMenu(menu)
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            val filterList : ArrayList<LmList> = ArrayList()
            var textRemark : String?
            var textNmItem : String?
            var textNmSpec : String?
            var textNoSerial : String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until originList.size){
                textRemark = nullChk(originList[idx].remark)
                textNmItem = nullChk(originList[idx].nmItem)
                textNmSpec = nullChk(originList[idx].nmSpec)
                textNoSerial = nullChk(originList[idx].noSerial)
                if (textRemark.toLowerCase(Locale.ROOT).contains(searchText) || originList[idx].dtLm!!.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText) || textNmItem.toLowerCase(Locale.ROOT).contains(searchText) || textNmSpec.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText) || textNoSerial.toLowerCase(Locale.ROOT).contains(searchText) || originList[idx].gubun!!.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText))
                    filterList.add(originList[idx])
            }
            lmListAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }
    }

    private fun nullChk(originval:String?) : String {
        return if (originval.isNullOrEmpty()) "" else originval
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.app_bar_search -> Toast.makeText(applicationContext, "검색눌럿네", Toast.LENGTH_LONG).show()
            R.id.toolbar_cust_select -> customerDialog(layoutInflater, mHandler!!, this, "").show()
            R.id.toolbar_cust_fax -> Toast.makeText(applicationContext, "팩스", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

//    override fun onCancelled() {
//
//    }
//
//    override fun onDataSelected(firstDate: Calendar?, secondDate: Calendar?, hours: Int, minutes: Int) {
//        if (firstDate != null) {
//            if (secondDate == null) {
//                selDateFr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(firstDate.time)
//                selDateTo = selDateFr
//            } else {
//                selDateFr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(firstDate.time)
//                selDateTo = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(secondDate.time)
//            }
//        }
//        updateDate()
//    }
    private fun checkDate() : Boolean {
        val dateFr = bindingA.bindingLmReport.etxtDtfrLm.text.toString()
        val dateTo = bindingA.bindingLmReport.etxtDttoLm.text.toString()
        if (dateFr.length != 8 || dateTo.length != 8) {
            Toast.makeText(this, "날짜는 8자리로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.isLenient = false
        try {
            sdf.parse(dateFr)
            sdf.parse(dateTo)
        }catch (e: ParseException){
            Toast.makeText(this, "정확한 날짜로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        selDateFr = dateFr
        selDateTo = dateTo
        return true
    }

    private fun getLmDate() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJLmReport_list.asp")
        val body = FormBody.Builder().add("ccode", selCcode!!).add("cdCust", custCd!!).add("frdt", selDateFr).add("todt", selDateTo).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        var balanceAmt = 0L
        arLmList.clear()
        originList.clear()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dblmList = gson.fromJson(body1, DBLmList::class.java)

                for (idx in dblmList.results.indices) {
                    var balancetemp : String? = dblmList.results[idx].balanceAmt
                    if (balancetemp.isNullOrBlank() || balancetemp.isEmpty()) balancetemp = "0"

                    if (dblmList.results[idx].gubun?.trim() == "이월")
                        balanceAmt = balancetemp.toLong()
                    else {
                        balanceAmt += dblmList.results[idx].salesAmt!!.toLong() - dblmList.results[idx].returnAmt!!.toLong() - dblmList.results[idx].acceptAmt!!.toLong() + dblmList.results[idx].atCm!!.toLong()
                    }
                    arLmList.add(
                        LmList(
                            dblmList.results[idx].dtLm?.trim(),
                            dblmList.results[idx].gubun?.trim(),
                            dblmList.results[idx].nmItem?.trim(),
                            dblmList.results[idx].nmSpec?.trim(),
                            dblmList.results[idx].qty?.trim(),
                            dblmList.results[idx].salesAmt?.trim(),
                            dblmList.results[idx].acceptAmt?.trim(),
                            dblmList.results[idx].cdWhs?.trim(),
                            dblmList.results[idx].noSerial?.trim(),
                            dblmList.results[idx].remark?.trim(),
                            dblmList.results[idx].priceAmt?.trim(),
                            balanceAmt.toString(),
                            dblmList.results[idx].returnAmt?.trim(),
                            dblmList.results[idx].atCm?.trim()
                        )
                    )

                }
                originList.addAll(arLmList)
                balanceAmt = 0L
                runOnUiThread {
                    lmListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }

        })
    }

    data class DBLmList(val results : List<LmList>)
    data class LmList(val dtLm:String?, val gubun:String?, val nmItem:String?, val nmSpec:String?, val qty:String?
                      , val salesAmt:String?, val acceptAmt:String?,val cdWhs:String?, val noSerial:String?
                      , val remark:String?, val priceAmt:String?, val balanceAmt:String?, val returnAmt:String?, val atCm:String?)

    class LmListAdapter(val context:Context, arLmList:ArrayList<LmList>) : RecyclerView.Adapter<LmListAdapter.ViewHolder>() {
        private val mList = arLmList

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_lm_report, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val gubun = mList[position].gubun

            var totQty : Long = 0
            var totAmt : Long = 0
            var totAccept : Long = 0

//            if (position and 1 == 0)
//                holder!!.layoutTable.setBackgroundResource(R.drawable.list_item_2)
//            else
//                holder!!.layoutTable.setBackgroundResource(R.drawable.list_item_3)

            if (gubun == "수금" || gubun == "이월") {
                holder.textGubunLm.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
            } else if (gubun!!.substring(2,4) == "반품" ) {
                holder.textGubunLm.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
            } else if (gubun.substring(2,4) == "정리" || gubun.substring(0,2) == "판매" ) {
                holder.textGubunLm.setTextColor(ContextCompat.getColor(context, R.color.dtVal_3))
            } else {
                holder.textGubunLm.setTextColor(ContextCompat.getColor(context, R.color.slycalendar_defTextColor))
            }

            var dtlm = mList[position].dtLm
            if (!dtlm.isNullOrEmpty())
                dtlm = dtlm.substring(2,4) + "." + dtlm.substring(4,6) + "." + dtlm.substring(6, 8)


            if (mList[position].qty.isNullOrBlank() || mList[position].qty!!.isEmpty() || mList[position].qty == "0")
                holder.textQtyLm.text = ""
            else {
                val qty = mList[position].qty!!.toLong()
                holder.textQtyLm.text = String.format("%,d", qty)
            }

            if (mList[position].priceAmt.isNullOrBlank() || mList[position].priceAmt!!.isEmpty() || mList[position].priceAmt == "0")
                holder.textPriceLm.text = ""
            else {
                val qty = mList[position].priceAmt!!.toLong()
                holder.textPriceLm.text = String.format("%,d", qty)
            }

            if (mList[position].salesAmt.isNullOrBlank() || mList[position].salesAmt!!.isEmpty() || mList[position].salesAmt == "0")
                holder.textAmtLm.text = ""
            else {
                val qty = mList[position].salesAmt!!.toLong()
                holder.textAmtLm.text = String.format("%,d", qty)
            }

            if (mList[position].acceptAmt.isNullOrBlank() || mList[position].acceptAmt!!.isEmpty() || mList[position].acceptAmt == "0")
                holder.textAcceptLm.text = ""
            else {
                val qty = mList[position].acceptAmt!!.toLong()
                holder.textAcceptLm.text = String.format("%,d", qty)
            }

            if (mList[position].balanceAmt.isNullOrBlank() || mList[position].balanceAmt!!.isEmpty() || mList[position].balanceAmt == "0")
                holder.textBalanceLm.text = ""
            else {
                val qty = mList[position].balanceAmt!!.toLong()
                holder.textBalanceLm.text = String.format("%,d", qty)
            }

            holder.textDtLm.text = dtlm
            holder.textItemLm.text = mList[position].nmItem
            holder.textSerialLm.text = mList[position].noSerial
            holder.textSpecLm.text = mList[position].nmSpec
            holder.textGubunLm.text = mList[position].gubun
            holder.textWhsLm.text = mList[position].cdWhs
            holder.textRemarkLm.text = mList[position].remark

            if (mList.size-1 == position) {
                holder.layoutTot.visibility = View.VISIBLE
           //     holder.layoutTot.setBackgroundResource(R.color.tot_sum_color)

                for (idx in 0 until mList.size) {
                    if (!mList[idx].qty.isNullOrBlank() && mList[idx].qty!!.isNotEmpty())
                        totQty += mList[idx].qty!!.toLong()
                    if (!mList[idx].acceptAmt.isNullOrBlank() && mList[idx].acceptAmt!!.isNotEmpty())
                        totAccept += mList[idx].acceptAmt!!.toLong()
                    if (!mList[idx].salesAmt.isNullOrBlank() && mList[idx].salesAmt!!.isNotEmpty())
                        totAmt += mList[idx].salesAmt!!.toLong()
                }
                holder.textTotAccept.text = String.format("%,d", totAccept)
                holder.textTotQty.text = String.format("%,d", totQty)
                holder.textTotAmt.text = String.format("%,d", totAmt)
            }
            else {
                holder.layoutTot.visibility = View.GONE
            }
        }

        fun setFilter(items : ArrayList<LmList>) {
            mList.clear()
            mList.addAll(items)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textDtLm : TextView =itemView.findViewById(R.id.txt_dtlm)
            val textItemLm : TextView = itemView.findViewById(R.id.txt_itemlm)
            val textSerialLm : TextView = itemView.findViewById(R.id.txt_seriallm)
            val textQtyLm : TextView = itemView.findViewById(R.id.txt_qtylm)
            val textPriceLm : TextView = itemView.findViewById(R.id.txt_pricelm)
            val textGubunLm : TextView = itemView.findViewById(R.id.txt_gubunlm)
            val textSpecLm : TextView = itemView.findViewById(R.id.txt_speclm)
            val textWhsLm : TextView = itemView.findViewById(R.id.txt_whslm)
            val textAmtLm : TextView = itemView.findViewById(R.id.txt_amtlm)
            val textRemarkLm : TextView = itemView.findViewById(R.id.txt_remarklm)
            val textAcceptLm : TextView = itemView.findViewById(R.id.txt_acceptlm)
//            val layoutTable : TableLayout = itemView.findViewById(R.id.tl_lm)
            val layoutTot : TableLayout = itemView.findViewById(R.id.tl_tot)
            val textTotQty : TextView = itemView.findViewById(R.id.txt_tot_qty)
            val textTotAmt : TextView = itemView.findViewById(R.id.txt_tot_amt)
            val textTotAccept : TextView = itemView.findViewById(R.id.txt_tot_accept)
            val textBalanceLm : TextView = itemView.findViewById(R.id.txt_balancelm)
        }
    }

    private fun checkSearch() : Boolean {
        if (custCd.isNullOrBlank() ||custCd!!.isEmpty()) {
            myToast(resources.getString(R.string.cust_choice))
            return false
        }
        return true
    }

//    private fun updateDate() {
//        et.text = selDateFr!!.substring(2, selDateFr!!.length) + " ~ " + selDateTo!!.substring(2, selDateTo!!.length)
//    }

    private fun lmReportInit() {
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        // company setting
        val ccodeList : ArrayList<String> = ArrayList()
        ccodeList.addAll(resources.getStringArray(R.array.company_list))
        ccodeList.removeAt(0)
        val spinCcodeAdapter : ArrayAdapter<String> = ArrayAdapter(this, R.layout.spinnerlayout, ccodeList)
        val spinCcodeLm : Spinner = findViewById(R.id.spin_Ccode_lm)
        spinCcodeLm.adapter = spinCcodeAdapter
        spinCcodeLm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position) {
                    0 -> selCcode = "1000"
                    1 -> selCcode = "3000"
                    2 -> selCcode = "5000"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        // date init setting
        val calendarNow = Calendar.getInstance()
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        selDateFr = selDateTo.substring(0,6) + "01"
        bindingA.bindingLmReport.etxtDtfrLm.setText(selDateFr)
        bindingA.bindingLmReport.etxtDttoLm.setText(selDateTo)
    }

    private fun myToast(msg:String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

}
