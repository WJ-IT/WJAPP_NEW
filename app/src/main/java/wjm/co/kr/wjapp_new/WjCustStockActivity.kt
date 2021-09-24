package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.app.Dialog
import android.app.SearchManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjCustStockBinding
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WjCustStockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWjCustStockBinding
    private var monthList : ArrayList<String> = ArrayList()
    private var selJepum : String? = null
    private var selcdCls : String? = null
    private var searchView: SearchView? = null
    private var arJepumList : ArrayList<JepumList> = ArrayList()
    private var jepumListAdapter : JepumListAdapter? = null
    private var originList : ArrayList<CustStockList> = ArrayList()
    private var arCStockList : ArrayList<CustStockList> = ArrayList()
    private var cStockListAdapter : CStockListAdapter? = null

    private var cStockCdialogadapter : CDialogAdapter? = null
    private var cdialogList : ArrayList<CustStockDetail> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_wj_cust_stock)
        binding = ActivityWjCustStockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        custStockInit()
        getJepumList()
    }

    // 기본 화면 오픈 초기화작업(제품, 메인리스트 어댑터 연결작업, 날짜작업, 등)
    private fun custStockInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        cStockListAdapter = CStockListAdapter(this, arCStockList)
        binding.bindingCustStock.rvCStockReportList.layoutManager = layoutManager
        binding.bindingCustStock.rvCStockReportList.adapter = cStockListAdapter

        val layoutManager2 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        jepumListAdapter = JepumListAdapter(this, arJepumList, onClickItem)
        binding.bindingCustStock.rvJepumList.layoutManager = layoutManager2
        binding.bindingCustStock.rvJepumList.adapter = jepumListAdapter

        //month array setting
        val calendar = Calendar.getInstance()
        val dateformat = "MM"
        calendar.add(Calendar.MONTH, 1)
        for (i in 0 .. 6) {
            calendar.add(Calendar.MONTH, -1)
            monthList.add("${SimpleDateFormat(dateformat, Locale.getDefault()).format(calendar.time).toInt()}월")
        }
    }

    // 화면 상단 제품 리스트 클릭시 액션
    private val onClickItem = View.OnClickListener { v ->
        if (!selJepum.equals(v.transitionName)) {
            selJepum = v.transitionName
            selcdCls = v.tag.toString()
            val selItem = v.transitionName

            for (idx in 0 until arJepumList.size)
                arJepumList[idx].flag = (arJepumList[idx].nmItem == selItem)

            arCStockList.clear()
            originList.clear()
            cStockListAdapter!!.notifyDataSetChanged()
            jepumListAdapter!!.notifyDataSetChanged()
        }
    }

    // 메인 리스트 롱클릭시 액션
    private val onLongClick = View.OnLongClickListener {v->
        val cdCust = v.tag.toString()
        val nmCust = v.transitionName
        getCustStockDetailList(cdCust, nmCust)
        true
    }

    // 커스텀 다이얼로그 세팅
    private fun cDialog(title:String, totcnt:Int): Dialog {
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.cdialog_cust_stock_detail, nullParent)
        val builder = AlertDialog.Builder(this)

        builder.setView(view)
        val rvCDialog : RecyclerView = view.findViewById(R.id.rvCustStockDetail)
        val txtTitle : TextView = view.findViewById(R.id.txt_nmHosp)
        val txtTotCnt : TextView = view.findViewById(R.id.txt_totCnt)
//        val btnClose : Button = view.findViewById(R.id.btnClose)

        txtTitle.text = title
        txtTotCnt.text = "총 ${totcnt}개"

        val custStockManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvCDialog.layoutManager = custStockManager
        // custom dialog lsit view Setting
        cStockCdialogadapter = CDialogAdapter(this, cdialogList)
        rvCDialog.adapter = cStockCdialogadapter

        cStockCdialogadapter!!.notifyDataSetChanged()

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

//        btnClose.setOnClickListener(({
//            dialog.dismiss()
//        }))
        return dialog
    }

    // 롱클릭시 커스텀다이얼로그 안에 데이타 가져오기
    private fun getCustStockDetailList(cdCust: String, nmCust: String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        cdialogList.clear()
        //println(arg1.toString())
        val url = URL("http://iclkorea.com/android/WJCust_Stock_Detail_list.asp")
        val body = FormBody.Builder().add("cdCust", cdCust).add("cdCls", selcdCls!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBCustStockDetail = gson.fromJson(body1, DBCustStockDetail::class.java)

                for (idx in dBCustStockDetail.results.indices) {
                    cdialogList.add(
                        CustStockDetail(
                            dBCustStockDetail.results[idx].nmSpec,
                            dBCustStockDetail.results[idx].outDt,
                            dBCustStockDetail.results[idx].noSerial,
                            dBCustStockDetail.results[idx].lastScanDt
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    cDialog(nmCust, cdialogList.size).show()  //커스텀 다이얼로그 보이기
                    cStockCdialogadapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    // 화면 상단 제품 리스트 가져와서 ArrayList에 담기
    private fun getJepumList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        arJepumList.clear()
        val url = URL("http://iclkorea.com/android/WJCust_Stock_jepum_list.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBJepumList = gson.fromJson(body1, DBJepumList::class.java)

                for (idx in dBJepumList.results.indices) {
                    arJepumList.add(
                        JepumList(
                            dBJepumList.results[idx].nmItem,
                            dBJepumList.results[idx].cCode,
                            dBJepumList.results[idx].cdCls,
                            dBJepumList.results[idx].flag
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    jepumListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    // 메인 리스트 가져와서 ArrayList에 담기
    fun getCustStockList(cCode: String, cdCls: String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJCust_Stock_list.asp")
        val body = FormBody.Builder().add("cCode", cCode).add("cdCls", cdCls).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .connectTimeout(5, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBCustStockList = gson.fromJson(body1, DBCustStockList::class.java)

                for (idx in dBCustStockList.results.indices) {
                    arCStockList.add(
                        CustStockList(
                            dBCustStockList.results[idx].cdCust,
                            dBCustStockList.results[idx].nmCust,
                            dBCustStockList.results[idx].kname,
                            dBCustStockList.results[idx].qt0,
                            dBCustStockList.results[idx].qt1,
                            dBCustStockList.results[idx].qt2,
                            dBCustStockList.results[idx].qt3,
                            dBCustStockList.results[idx].qt4,
                            dBCustStockList.results[idx].qt5,
                            dBCustStockList.results[idx].qt6,
                            dBCustStockList.results[idx].qtAverage,
                            dBCustStockList.results[idx].qtSales,
                            dBCustStockList.results[idx].firstOut,
                            dBCustStockList.results[idx].lastOut,
                            dBCustStockList.results[idx].remark
                        )
                    )
                }
                originList.addAll(arCStockList)

                runOnUiThread { //UI에 알려줌
                    cStockListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBJepumList (var results : List<JepumList>)
    data class JepumList (var nmItem: String?, var cCode:String?, var cdCls:String?, var flag: Boolean?)
    data class DBCustStockList (var results : List<CustStockList>)
    data class CustStockList (var cdCust:String?, var nmCust:String?, var kname:String?, var qt0:String?, var qt1:String?, var qt2:String?, var qt3:String?, var qt4:String?, var qt5:String?, var qt6:String?, var qtAverage:String?, var qtSales:String?, var firstOut:String?, var lastOut:String, var remark:String?)
    data class DBCustStockDetail (var results : List<CustStockDetail>)
    data class CustStockDetail (var nmSpec:String?, var outDt:String?, var noSerial:String?, var lastScanDt:String?)

    // 화면상단에 보인 제품리스트 어댑터
    inner class JepumListAdapter(private val context: Context, private val itemList:ArrayList<JepumList>,
                                 private val onClickItem: View.OnClickListener) : RecyclerView.Adapter<JepumListAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onBindViewHolder(holder: JepumListAdapter.ViewHolder, position: Int) {
            holder.textNm.text = itemList[position].nmItem
            holder.productLayout.tag = itemList[position].cdCls
            holder.productLayout.transitionName = itemList[position].nmItem
            holder.productLayout.setOnClickListener(onClickItem)

            if (arJepumList[position].flag == true) {
                holder.productLayout.setBackgroundResource(R.drawable.product_button_on)
              //  selNmItem = itemList[position].nmItem!!
                getCustStockList(itemList[position].cCode!!, itemList[position].cdCls!!)
            } else
                holder.productLayout.setBackgroundResource(R.drawable.product_button)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup?,
            viewType: Int
        ): JepumListAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_vertical_list, parent, false)
            return ViewHolder(view)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var productLayout : LinearLayout = itemView.findViewById(R.id.layout_Product)
            var textNm : TextView = itemView.findViewById(R.id.text_itemNm)
        }
    }

    // 화면에 보일 메인리스트 어댑터
    inner class CStockListAdapter(private val context: Context, private val itemList:ArrayList<CustStockList>) : RecyclerView.Adapter<CStockListAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup?,
            viewType: Int
        ): CStockListAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_cust_stock, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: CStockListAdapter.ViewHolder, position: Int) {
            holder.itemView.isSelected = true //marquee use
            holder.txtNmCust.text = itemList[position].nmCust
            holder.txtKname.text = itemList[position].kname
            holder.txtFirstOut.text = itemList[position].firstOut
            val lastDay : String = if (itemList[position].lastOut == "1900-12-31")
                "X"
            else
                itemList[position].lastOut
            holder.txtLastOut.text = lastDay
            holder.txtStock.text = itemList[position].remark
            holder.txtCnt.text = itemList[position].qt0
            holder.txtM.text = itemList[position].qt1
            holder.txtM1.text = itemList[position].qt2
            holder.txtM2.text = itemList[position].qt3
            holder.txtM3.text = itemList[position].qt4
            holder.txtM4.text = itemList[position].qt5
            holder.txtM5.text = itemList[position].qt6
            holder.txtAverage.text = "${itemList[position].qtAverage!!.toFloat()}"
            holder.txtPercent.text = "${itemList[position].qtSales!!.toFloat()}"

            holder.txtMHeader.text = monthList[0]
            holder.txtM1Header.text = monthList[1]
            holder.txtM2Header.text = monthList[2]
            holder.txtM3Header.text = monthList[3]
            holder.txtM4Header.text = monthList[4]
            holder.txtM5Header.text = monthList[5]

            when {
                itemList[position].qtSales!!.toFloat() <= 5 -> {
                    holder.txtBadgeBottom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF0000"))
                    holder.txtBadgeTop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF0000"))
                }
                itemList[position].qtSales!!.toFloat() <= 10 -> {
                    holder.txtBadgeBottom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF7F00"))
                    holder.txtBadgeTop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF7F00"))
                }
                itemList[position].qtSales!!.toFloat() <= 20 -> {
                    holder.txtBadgeBottom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FCCC00"))
                    holder.txtBadgeTop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FCCC00"))
                }
                itemList[position].qtSales!!.toFloat() <= 30 -> {
                    holder.txtBadgeBottom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7DCD00"))
                    holder.txtBadgeTop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7DCD00"))
                }
                else -> {
                    holder.txtBadgeBottom.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0611F2"))
                    holder.txtBadgeTop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0611F2"))
                }
            }

            holder.clCustStockrow.tag = itemList[position].cdCust
            holder.clCustStockrow.transitionName = itemList[position].nmCust
            holder.clCustStockrow.setOnLongClickListener(onLongClick)
        }

        fun setFilter(items : ArrayList<CustStockList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val txtNmCust : TextView = itemView.findViewById(R.id.txt_nmHosp)
            val txtFirstOut : TextView = itemView.findViewById(R.id.txt_firstDate)
            val txtLastOut : TextView = itemView.findViewById(R.id.txt_lastDate)
            val txtKname : TextView = itemView.findViewById(R.id.txt_kname)
            val txtStock : TextView = itemView.findViewById(R.id.txt_stock)
            val txtCnt : TextView = itemView.findViewById(R.id.txt_cnt)
            val txtM : TextView = itemView.findViewById(R.id.txt_m)
            val txtM1 : TextView = itemView.findViewById(R.id.txt_m1)
            val txtM2 : TextView = itemView.findViewById(R.id.txt_m2)
            val txtM3 : TextView = itemView.findViewById(R.id.txt_m3)
            val txtM4 : TextView = itemView.findViewById(R.id.txt_m4)
            val txtM5 : TextView = itemView.findViewById(R.id.txt_m5)
            val txtAverage : TextView = itemView.findViewById(R.id.txt_average)

            val txtMHeader : TextView = itemView.findViewById(R.id.txt_m_head)
            val txtM1Header : TextView = itemView.findViewById(R.id.txt_m1_head)
            val txtM2Header : TextView = itemView.findViewById(R.id.txt_m2_head)
            val txtM3Header : TextView = itemView.findViewById(R.id.txt_m3_head)
            val txtM4Header : TextView = itemView.findViewById(R.id.txt_m4_head)
            val txtM5Header : TextView = itemView.findViewById(R.id.txt_m5_head)

            val txtBadgeTop : TextView = itemView.findViewById(R.id.txtBadgeTop)
            val txtBadgeBottom : TextView = itemView.findViewById(R.id.txtBadgeBottom)
            val txtPercent : TextView = itemView.findViewById(R.id.txt_percent)

            val clCustStockrow : ConstraintLayout = itemView.findViewById(R.id.clCustStockRow)
        }
    }

    // 롱클릭시 커스텀다이얼로그 어댑터
    inner class CDialogAdapter(private var context:Context, private var itemList:ArrayList<CustStockDetail>) : RecyclerView.Adapter<CDialogAdapter.ViewHolder>(){

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_4col, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.llrow.setBackgroundColor(ContextCompat.getColor(context, R.color.item_color_2))//setBackgroundResource(R.drawable.list_item_2)
            else
                holder.llrow.setBackgroundColor(ContextCompat.getColor(context, R.color.item_color_3))

            holder.txtSerial.text = itemList[position].noSerial
            holder.txtSpec.text = itemList[position].nmSpec
            holder.txtOutDt.text = itemList[position].outDt
            holder.txtScanDt.text = itemList[position].lastScanDt
//            holder.txtRemark.isSelected = true
        }

        inner class ViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView){
            val txtSerial : TextView = itemView.findViewById(R.id.txt_Serial)
            val txtSpec : TextView = itemView.findViewById(R.id.txt_Spec)
            val txtOutDt : TextView = itemView.findViewById(R.id.txt_OutDt)
            val txtScanDt : TextView = itemView.findViewById(R.id.txt_ScanDt)
            val llrow : LinearLayout = itemView.findViewById(R.id.ll_4col_row)
        }
    }

    // 화면 우측위 메뉴부분
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

    // 화면 우측위 메뉴부분 선택시
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.app_bar_search -> Toast.makeText(applicationContext, "검색눌럿네", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    // 글자 검색시 액션
    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            val filterList : ArrayList<CustStockList> = ArrayList()
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until originList.size){
                val txtkname = nullChk(originList[idx].kname)
                val txtnmcust = nullChk(originList[idx].nmCust)
                val txtcdcust = nullChk(originList[idx].cdCust)
                if (txtkname.toLowerCase(Locale.ROOT).contains(searchText) || txtnmcust.toLowerCase(Locale.ROOT).contains(searchText) || txtcdcust.toLowerCase(Locale.ROOT).contains(searchText))
                    filterList.add(originList[idx])
            }
            cStockListAdapter!!.setFilter(filterList)
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