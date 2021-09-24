package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjOrderReportBinding
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WjOrderReportActivity : AppCompatActivity() { //, SlyCalendarDialog.Callback {
    private lateinit var bindingA: ActivityWjOrderReportBinding
    private var selDateFr : String = ""
    private var selDateTo : String = ""
    private var searchView: SearchView? = null
    var originList : ArrayList<OrderReportList> = ArrayList()
    var arOrderList : ArrayList<OrderReportList> = ArrayList()
    private var orderListAdapter : OrderListAdapter? = null

    private var row = 99999

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_order_report)
        bindingA = ActivityWjOrderReportBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        orderReportInit()

        bindingA.bindingOrderReport.btnSerarchOrder.setOnClickListener(({
            if (checkDate()) {
                arOrderList.clear()
                originList.clear()
                orderListAdapter!!.notifyDataSetChanged()
                if (!searchView!!.isIconified) {
                    searchView!!.isIconified = true
                    searchView!!.clearFocus()
                    searchView!!.onActionViewCollapsed()
                }
                getOrderList()
            }
        }))

        bindingA.bindingOrderReport.layoutMainOrder.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        bindingA.bindingOrderReport.rvOrderReportList.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    private fun orderReportInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        orderListAdapter = OrderListAdapter(this, arOrderList)
        bindingA.bindingOrderReport.rvOrderReportList.layoutManager = layoutManager
        bindingA.bindingOrderReport.rvOrderReportList.adapter = orderListAdapter

//        linear_date_order.setOnClickListener(({
//            SlyCalendarDialog()
//                .setSingle(false)
//                .setCallback(this)
//                .show(supportFragmentManager, "TAG_SLYCALENDAR")
//        }))

        // date init setting
        val calendarNow = Calendar.getInstance()
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        calendarNow.add(Calendar.DATE, -7)
        selDateFr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        //etxt_dtfr_orderR.setText(selDateFr)
        bindingA.bindingOrderReport.etxtDtfrOrderR.setText(selDateTo)
        bindingA.bindingOrderReport.etxtDttoOrderR.setText(selDateTo)
    }

    private fun checkDate() : Boolean {
        val dateFr = bindingA.bindingOrderReport.etxtDtfrOrderR.text.toString()
        val dateTo = bindingA.bindingOrderReport.etxtDttoOrderR.text.toString()
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

    private fun getOrderList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJOrderReport_list.asp")
        val body = FormBody.Builder().add("frdt", selDateFr).add("todt", selDateTo).build()
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
                val dbOrderReportList = gson.fromJson(body1, DBOrderReportList::class.java)

                for (idx in dbOrderReportList.results.indices) {
                    arOrderList.add(
                        OrderReportList(
                            dbOrderReportList.results[idx].noReq?.trim(),
                            dbOrderReportList.results[idx].cdItem?.trim(),
                            dbOrderReportList.results[idx].nmItem?.trim(),
                            dbOrderReportList.results[idx].nmSpec?.trim(),
                            dbOrderReportList.results[idx].qty?.trim(),
                            dbOrderReportList.results[idx].remark?.trim(),
                            dbOrderReportList.results[idx].gubun?.trim(),
                            dbOrderReportList.results[idx].nmHosp?.trim(),
                            dbOrderReportList.results[idx].cdHosp?.trim(),
                            dbOrderReportList.results[idx].orderGbn?.trim(),
                            dbOrderReportList.results[idx].patient?.trim(),
                            dbOrderReportList.results[idx].exDt?.trim(),
                            dbOrderReportList.results[idx].dtOrd?.trim(),
                            dbOrderReportList.results[idx].dtTime?.trim(),
                            dbOrderReportList.results[idx].dtSurg?.trim()
                        )
                    )
                }
                originList.addAll(arOrderList)
                runOnUiThread {
                    orderListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }

        })
    }

    private fun deleteOrder(item:OrderReportList) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val noReq = item.noReq
        val url = URL("http://iclkorea.com/android/WJOrderReport_delete.asp")
        val body = FormBody.Builder().add("noReq", noReq!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbOrderDelete = gson.fromJson(body1, DBOrderDelete::class.java)
                val temp : String

                when (dbOrderDelete.results) {
                    "CANCEL" -> {
                        temp = "이미 취소된 주문입니다."
                        arOrderList.removeAt(row)
                    }
                    "OUT" -> temp = "이미 출고한 주문입니다."
                    "NO" -> temp = "삭제중 문제가 발생하였습니다."
                    else -> {
                        temp = "삭제되었습니다."
                        arOrderList.removeAt(row)
                    }
                }

                row = 99999

                runOnUiThread {
                    Toast.makeText(baseContext, temp, Toast.LENGTH_LONG).show()
                    orderListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBOrderReportList (var results : List<OrderReportList>)
    data class OrderReportList (var noReq:String?, var cdItem:String?, var nmItem:String?, var nmSpec:String?, var qty:String?, var remark:String?, var gubun:String?, var nmHosp:String?, var cdHosp:String?, var orderGbn:String?, var patient:String?, var exDt:String?, var dtOrd:String?, var dtTime:String?, var dtSurg:String?)
    data class DBOrderDelete (var results : String?)

    inner class OrderListAdapter(private val context:Context, private val itemList:ArrayList<OrderReportList>) : RecyclerView.Adapter<OrderListAdapter.ViewHolder>(){
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_order_report, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
            if (position and 1 == 0)
                holder.layoutTable.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.layoutTable.setBackgroundResource(R.drawable.list_item_3)

            holder.txtOrderDt.text = itemList[position].dtOrd
            holder.txtOrderGbn.text = itemList[position].orderGbn
            holder.txtNmHosp.text = itemList[position].nmHosp
            holder.txtQty.text = itemList[position].qty
            holder.txtSurgDt.text = itemList[position].dtSurg
            holder.txtNmItem.text = itemList[position].nmItem
            holder.txtNmSpec.text = itemList[position].nmSpec
            holder.txtExDt.text = itemList[position].exDt
            if (itemList[position].patient == "") {
                holder.txtRemark.text = itemList[position].remark
            } else {
                holder.txtRemark.text = "${itemList[position].patient};${itemList[position].remark}"
            }

            if (itemList[position].orderGbn == "오더") {
                holder.txtOrderGbn.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
                holder.txtNmItem.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
                holder.txtNmSpec.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
            } else {
                holder.txtOrderGbn.setTextColor(ContextCompat.getColor(context, android.R.color.tab_indicator_text))
                holder.txtNmItem.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                holder.txtNmSpec.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }

            if (itemList[position].exDt == "출고전")
                holder.btncancel.visibility = View.VISIBLE
            else
                holder.btncancel.visibility = View.GONE

            holder.btncancel.setOnClickListener(({
                //Toast.makeText(context, itemList[position].noReq, Toast.LENGTH_SHORT).show()
                val cancelDialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage("해당 주문을 취소하시겠습니까?"+ itemList[position].noReq)
                    .setIcon(R.drawable.wjicon).setTitle("주문 취소확인")
                    .setPositiveButton("네") { _, _ ->
                        deleteOrder(itemList[position])
                        row = position
                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
            }))
        }

        fun setFilter(items : ArrayList<OrderReportList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtOrderDt : TextView = itemView.findViewById(R.id.txt_dtorder)
            val txtOrderGbn : TextView = itemView.findViewById(R.id.txt_gbnorder)
            val txtNmHosp : TextView = itemView.findViewById(R.id.txt_custorder)
            val txtQty : TextView = itemView.findViewById(R.id.txt_qtyorder)
            val txtSurgDt : TextView = itemView.findViewById(R.id.txt_dtsurgorder)
            val txtNmItem : TextView = itemView.findViewById(R.id.txt_itemorder)
            val txtNmSpec : TextView = itemView.findViewById(R.id.txt_specorder)
            val txtExDt : TextView = itemView.findViewById(R.id.txt_dtexorder)
            val txtRemark : TextView = itemView.findViewById(R.id.txt_remarkorder)
            val btncancel : Button = itemView.findViewById(R.id.btn_cancleorder)
            val layoutTable : TableLayout = itemView.findViewById(R.id.tl_order)
        }
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
//
//    private fun updateDate() {
//        txt_date_order.text = selDateFr!!.substring(2, selDateFr!!.length) + " ~ " + selDateTo!!.substring(2, selDateTo!!.length)
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem1 = menu.findItem(R.id.toolbar_cust_select)
        searchItem1.isVisible = false

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = "검색어 입력"
        searchView!!.setOnQueryTextListener(queryTextListener)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(this.componentName))
        searchView!!.isIconifiedByDefault = true
        searchView!!.requestFocusFromTouch()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.app_bar_search -> Toast.makeText(applicationContext, "검색눌럿네", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            val filterList : ArrayList<OrderReportList> = ArrayList()
            var textRemark : String?
            var textNmItem : String?
            var textNmSpec : String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until originList.size){
                textRemark = nullChk(originList[idx].remark)
                textNmItem = nullChk(originList[idx].nmItem)
                textNmSpec = nullChk(originList[idx].nmSpec)
                if (textRemark.toLowerCase(Locale.ROOT).contains(searchText) || originList[idx].nmHosp!!.toLowerCase(Locale.ROOT).contains(searchText) ||
                    textNmItem.toLowerCase(Locale.ROOT).contains(searchText) || textNmSpec.toLowerCase(Locale.ROOT).contains(searchText) ||
                    originList[idx].orderGbn!!.toLowerCase(Locale.ROOT).contains(searchText)|| originList[idx].cdItem!!.toLowerCase(Locale.ROOT).contains(searchText) )
                    filterList.add(originList[idx])
            }
            orderListAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }

        private fun nullChk(originval:String?) : String {
            return if (originval.isNullOrEmpty()) "" else originval
        }
    }
}
