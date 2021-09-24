package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjSalesTeamReportBinding
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WjSalesTeamReportActivity : AppCompatActivity() {
     private lateinit var bindingA: ActivityWjSalesTeamReportBinding
    private var selDateFr : String = ""
    private var selDateTo : String = ""
    private var selCcode : String = ""
    private var selTeam : String = ""
    private var searchView: SearchView? = null

    private var layoutManager : LinearLayoutManager? = null
    private val arTeamSalesList : ArrayList<TeamSalesList> = ArrayList()
    private val arOriginList : ArrayList<TeamSalesList> = ArrayList()

    private var teamSalesAdapter : TeamSalesAdapter? = null

    private var totQtPln = 0L
    private var totAtPln = 0L
    private var totQtSale = 0L
    private var totAtSale = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_sales_team_report)
        bindingA = ActivityWjSalesTeamReportBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        teamReportInit()

        bindingA.bindingContent.btnSearchTeam.setOnClickListener(({
            if (checkDate())
                getTeamSalesData()
        }))

        bindingA.bindingContent.clTeam.setOnClickListener(({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bindingA.bindingContent.etxtDateFr.windowToken, 0)
            imm.hideSoftInputFromWindow(bindingA.bindingContent.etxtDateTo.windowToken, 0)
            bindingA.bindingContent.etxtDateFr.clearFocus()
            bindingA.bindingContent.etxtDateTo.clearFocus()
        }))
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        bindingA.bindingContent.rvTeam.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                    bindingA.bindingContent.etxtDateFr.clearFocus()
                    bindingA.bindingContent.etxtDateTo.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuFax = menu.findItem(R.id.toolbar_cust_select)
        menuFax.isVisible = false

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = "거래처명 입력"
        searchView!!.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
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
            val filterList : ArrayList<TeamSalesList> = ArrayList()
            var textTeam : String?
            var textNmItem : String?
            var textNmCust : String?
            var textDaeri : String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until arOriginList.size){
                textTeam = nullChk(arOriginList[idx].team)
                textNmItem = nullChk(arOriginList[idx].nmItem)
                textNmCust = nullChk(arOriginList[idx].nmCust)
                textDaeri = nullChk(arOriginList[idx].daeri)
                if (textTeam.toLowerCase(Locale.ROOT).contains(searchText) || textNmItem.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText) ||
                    textNmCust.toLowerCase(Locale.ROOT).contains(searchText) || textDaeri.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText))
                    filterList.add(arOriginList[idx])
            }
            teamSalesAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }
    }

    private fun nullChk(originval:String?) : String {
        return if (originval.isNullOrEmpty()) "" else originval
    }

    private fun teamReportInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        // company array/spinner setting
        val spinCompanyAdapter = ArrayAdapter(this,  R.layout.spinnerlayout, resources.getStringArray(R.array.company_list))
        spinCompanyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bindingA.bindingContent.spinCompanyTeam.adapter = spinCompanyAdapter
        bindingA.bindingContent.spinCompanyTeam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position) {
                    0 -> selCcode = "%"
                    1 -> selCcode = "1000"
                    2 -> selCcode = "3000"
                    3 -> selCcode = "5000"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // team array/spinner setting
        val spinTeamAdapter = ArrayAdapter(this, R.layout.spinnerlayout, resources.getStringArray(R.array.team_list))
        spinTeamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bindingA.bindingContent.spinTeamTeam.adapter = spinTeamAdapter
        bindingA.bindingContent.spinTeamTeam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selTeam = resources.getStringArray(R.array.team_list)[position]
                if (selTeam == "전체") selTeam = "%"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        // date init setting
        val calendarNow = Calendar.getInstance()
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        selDateFr = selDateTo.substring(0,6) + "01"
        bindingA.bindingContent.etxtDateFr.setText(selDateFr)
        bindingA.bindingContent.etxtDateTo.setText(selDateTo)

        // recyclerView init setting
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingContent.rvTeam.layoutManager = layoutManager
        teamSalesAdapter = TeamSalesAdapter(this, arTeamSalesList)
        bindingA.bindingContent.rvTeam.adapter = teamSalesAdapter
    }

    private fun checkDate() : Boolean {
        val dateFr = bindingA.bindingContent.etxtDateFr.text.toString()
        val dateTo = bindingA.bindingContent.etxtDateTo.text.toString()
        if (dateFr.length != 8 || dateTo.length != 8) {
            Toast.makeText(this, "날짜는 8자리로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.isLenient = false
        try {
            sdf.parse(dateFr)
            sdf.parse(dateTo)
        }catch (e:ParseException){
            Toast.makeText(this, "정확한 날짜로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        selDateFr = dateFr
        selDateTo = dateTo
        return true
    }

    private fun getTeamSalesData() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJTeamSales_Report.asp")
        val body = FormBody.Builder().add("ccode", selCcode).add("frdt", selDateFr).add("todt", selDateTo).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        arTeamSalesList.clear()
        arOriginList.clear()
        totAtPln = 0
        totAtSale = 0
        totQtPln = 0
        totQtSale = 0
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbTeamSalesList = gson.fromJson(body1, DBTeamSalseList::class.java)

                var sumQtPln = 0L
                var sumAtPln = 0L
                var sumQtSale = 0L
                var sumAtSale = 0L

                for (idx in dbTeamSalesList.results.indices)
                    if (selCcode == "%" || selCcode == dbTeamSalesList.results[idx].cCode)
                        if (selTeam == "%" || selTeam == dbTeamSalesList.results[idx].team) {
                            arTeamSalesList.add(dbTeamSalesList.results[idx])
                            totAtSale += dbTeamSalesList.results[idx].atSale!!.toLong()
                            totAtPln += dbTeamSalesList.results[idx].atPln!!.toLong()
                            totQtSale += dbTeamSalesList.results[idx].qtSale!!.toLong()
                            totQtPln += dbTeamSalesList.results[idx].qtPln!!.toLong()

                            //middle sum(nmcust) add
                            sumQtPln += dbTeamSalesList.results[idx].qtPln!!.toLong()
                            sumQtSale += dbTeamSalesList.results[idx].qtSale!!.toLong()
                            sumAtPln += dbTeamSalesList.results[idx].atPln!!.toLong()
                            sumAtSale += dbTeamSalesList.results[idx].atSale!!.toLong()
                            if (idx < dbTeamSalesList.results.size -1 && dbTeamSalesList.results[idx].nmCust == dbTeamSalesList.results[idx + 1].nmCust){

                            } else {
                                arTeamSalesList.add(TeamSalesList("소계", dbTeamSalesList.results[idx].nmCust +" 소계", sumQtPln.toString(), sumAtPln.toString(), sumQtSale.toString(), sumAtSale.toString(), dbTeamSalesList.results[idx].cdCust, dbTeamSalesList.results[idx].nmCust, dbTeamSalesList.results[idx].daeri, dbTeamSalesList.results[idx].team))
                                sumAtPln = 0
                                sumAtSale = 0
                                sumQtPln = 0
                                sumQtSale = 0
                            }
                        }
                arTeamSalesList.add(TeamSalesList("총계", "총 합계", totQtPln.toString(), totAtPln.toString(), totQtSale.toString(), totAtSale.toString(), "", "", "", ""))
                arOriginList.addAll(arTeamSalesList)
                runOnUiThread {
                    teamSalesAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }

        })
    }

    data class DBTeamSalseList(var results:List<TeamSalesList>)
    data class TeamSalesList(var cCode:String?, var nmItem:String?, var qtPln:String?, var atPln:String?, var qtSale:String?, var atSale:String?, var cdCust:String?, var nmCust:String?,  var daeri:String?,var team:String?)

    inner class TeamSalesAdapter(var context :Context, private var itemList:ArrayList<TeamSalesList>) : RecyclerView.Adapter<TeamSalesAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_sales_team_report, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            when (itemList[position].cCode) {
                "총계" -> holder.bottomLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.tot_sum_color))
                "소계" -> holder.bottomLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.middle_sum_color))
                else -> {
                    holder.topLayout.setBackgroundResource(R.drawable.list_item_3)
                    holder.bottomLayout.setBackgroundResource(R.drawable.list_item_2)
                }
            }

            if (position == 0)
                holder.topLayout.visibility = View.VISIBLE
            else if (itemList[position].cCode == "총계"||itemList[position].cCode == "소계")
                holder.topLayout.visibility = View.GONE
            else if (itemList[position].nmCust != itemList[position-1].nmCust)
                holder.topLayout.visibility = View.VISIBLE
            else
                holder.topLayout.visibility = View.GONE

            if (position < itemList.size -1  && itemList[position].nmCust != itemList[position+1].nmCust)
                holder.txtBottomLine.setBackgroundColor(ContextCompat.getColor(context, R.color.dialog_bottom))
            else
                holder.txtBottomLine.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))

            if (itemList[position].cCode == "총계" || itemList[position].cCode == "소계")
                holder.txtItem.gravity = Gravity.END
            else
                holder.txtItem.gravity = Gravity.START

            holder.txtTeam.text = itemList[position].team
            holder.txtDaeri.text = itemList[position].daeri
            holder.txtNmCust.text = itemList[position].nmCust
            if (itemList[position].cCode == "총계" || itemList[position].cCode == "소계")
                holder.txtItem.text = String.format("▶ ${itemList[position].nmItem}")
            else
                holder.txtItem.text = String.format("- ${itemList[position].nmItem}")

            if (itemList[position].qtPln.isNullOrBlank() || itemList[position].qtPln!!.isEmpty() || itemList[position].qtPln == "0")
                holder.txtQtPln.text = ""
            else {
                val qty = itemList[position].qtPln!!.toLong()
                holder.txtQtPln.text = String.format("%,d", qty)
            }
            if (itemList[position].qtSale.isNullOrBlank() || itemList[position].qtSale!!.isEmpty() || itemList[position].qtSale == "0")
                holder.txtQtsale.text = ""
            else {
                val qty = itemList[position].qtSale!!.toLong()
                holder.txtQtsale.text = String.format("%,d", qty)
            }
            if (itemList[position].atPln.isNullOrBlank() || itemList[position].atPln!!.isEmpty() || itemList[position].atPln == "0")
                holder.txtAtPln.text = ""
            else {
                val qty = itemList[position].atPln!!.toLong()
                holder.txtAtPln.text = String.format("%,d", qty)
            }
            if (itemList[position].atSale.isNullOrBlank() || itemList[position].atSale!!.isEmpty() || itemList[position].atSale == "0")
                holder.txtAtsale.text = ""
            else {
                val qty = itemList[position].atSale!!.toLong()
                holder.txtAtsale.text = String.format("%,d", qty)
            }

        }

        inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            //val teamSalesLayout : ConstraintLayout = itemView.findViewById(R.id.cl_row_team)
            val txtTeam: TextView = itemView.findViewById(R.id.txt_team_team)
            val txtDaeri : TextView = itemView.findViewById(R.id.txt_daeri_team)
            val txtItem : TextView = itemView.findViewById(R.id.txt_item_team)
            val txtQtPln : TextView = itemView.findViewById(R.id.txt_qt_pln_team)
            val txtQtsale : TextView = itemView.findViewById(R.id.txt_qt_sale_team)
            val txtAtPln : TextView = itemView.findViewById(R.id.txt_at_pln_team)
            val txtAtsale : TextView = itemView.findViewById(R.id.txt_at_sale_team)
            val txtNmCust : TextView = itemView.findViewById(R.id.txt_cust_team)
            val topLayout : LinearLayout = itemView.findViewById(R.id.ll_row_top_team)
            val bottomLayout : LinearLayout = itemView.findViewById(R.id.ll_row_bottom_team)
            val txtBottomLine : TextView = itemView.findViewById(R.id.txt_bottom_line_team)
        }

        fun setFilter(items : ArrayList<TeamSalesList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }
    }
}
