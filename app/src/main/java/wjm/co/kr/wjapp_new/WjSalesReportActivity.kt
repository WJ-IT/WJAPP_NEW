package wjm.co.kr.wjapp_new

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
import wjm.co.kr.wjapp_new.databinding.ActivityWjSalesReportBinding
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WjSalesReportActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjSalesReportBinding
    var arSalesReportList : ArrayList<SalesReport> = ArrayList()
    private var salesReportAdapter : SalesReportAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_sales_report)
        bindingA = ActivityWjSalesReportBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        //network connecting
        val networkState = NetworkState(this)
        if (networkState.isConnected())
        else myToast( "네트워크에 연결되지 않았습니다.")

        //company array setting
        val spinCompanyAdapter = ArrayAdapter(this,  R.layout.spinnerlayout, resources.getStringArray(R.array.company_list))
        spinCompanyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bindingA.bindingSalesReport.spinCompany.adapter = spinCompanyAdapter

        //month array setting
        val monthList = ArrayList<String>()
        val calendar = Calendar.getInstance()
        var cha : Int = Calendar.MONTH
        cha = 11 - cha
        val dateformat = "yy.MM"
        calendar.add(Calendar.MONTH, cha)
        for (i in 0 .. 59) {
            calendar.add(Calendar.MONTH, -1)
            monthList.add(SimpleDateFormat(dateformat, Locale.getDefault()).format(calendar.time))
        }
        val spinMonthAdapter = ArrayAdapter(this, R.layout.spinnerlayout, monthList)
        spinMonthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bindingA.bindingSalesReport.spinMonth.adapter = spinMonthAdapter
        bindingA.bindingSalesReport.spinMonth2.adapter = spinMonthAdapter
        bindingA.bindingSalesReport.spinMonth.setSelection(cha - 1)
        bindingA.bindingSalesReport.spinMonth2.setSelection(cha - 1)

        //listView adapter setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingSalesReport.rvWjSales.layoutManager = layoutManager
        salesReportAdapter = SalesReportAdapter(this, arSalesReportList)
        bindingA.bindingSalesReport.rvWjSales.adapter = salesReportAdapter

        //search_button
        bindingA.bindingSalesReport.buttonWjSalesReport.setOnClickListener(({
            initData()
            getSalesReport(bindingA.bindingSalesReport.spinCompany.selectedItem.toString(), bindingA.bindingSalesReport.spinMonth.selectedItem.toString(), bindingA.bindingSalesReport.spinMonth2.selectedItem.toString())
        }))
    }

    private fun getSalesReport(company : String, month : String, month2 : String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()

        val mgbn = month.split(".")
        val mgbn2 = month2.split(".")
        val selMonth = "20" + mgbn[0] + mgbn[1]
        val selMonth2 = "20" + mgbn2[0] + mgbn2[1]
        var selCompany = ""

        when(company) {
            "전체" -> selCompany = "%"
            "우전메디칼" -> selCompany = "1000"
            "우전브이티" -> selCompany = "3000"
            "우전브이에스" -> selCompany = "5000"
        }

        val url = URL("http://iclkorea.com/android/WJSales_Report.asp")
        val body = FormBody.Builder().add("month", selMonth).add("month2", selMonth2).add("ccode", selCompany).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
//                println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWJSalesReportList = gson.fromJson(body1, DBWJSalesReportList::class.java)

                for (idx in dbWJSalesReportList.results.indices) {
                    arSalesReportList.add(
                        SalesReport(
                            dbWJSalesReportList.results[idx].ccode,
                            dbWJSalesReportList.results[idx].nmItem,
                            dbWJSalesReportList.results[idx].planQty,
                            dbWJSalesReportList.results[idx].planAmt,
                            dbWJSalesReportList.results[idx].salesQty,
                            dbWJSalesReportList.results[idx].salesAmt,
                            dbWJSalesReportList.results[idx].tot_sum,
                            dbWJSalesReportList.results[idx].middle_sum
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    salesReportAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBWJSalesReportList(val results : List<SalesReport>)
    data class SalesReport (var ccode:String?, var nmItem:String?, var planQty:String?, var planAmt:String?, var salesQty:String?, var salesAmt:String?,var tot_sum:String?, var middle_sum:String?)

    inner class SalesReportAdapter(val context:Context, private var itemList:ArrayList<SalesReport>) : RecyclerView.Adapter<SalesReportAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SalesReportAdapter.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_sales_report, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SalesReportAdapter.ViewHolder, position: Int) {
            var company : String? = null
            var companyName : String? = null
            when(itemList[position].ccode) {
                "1000"-> {company = "M" ; companyName = "우전메디칼"}
                "3000"-> {company = "VT"; companyName = "우전브이티"}
                "5000"-> {company = "VS"; companyName = "우전브이에스"}
            }

//            //layout color & name Setting (tot/middle)
            when {
                itemList[position].tot_sum == "1" -> {
                    holder.llrow.setBackgroundColor(ContextCompat.getColor(context, R.color.tot_sum_color))
                    holder.col1.text = "총계"
                    holder.col2.text = ""
                }
                itemList[position].middle_sum == "1" -> {
                    holder.llrow.setBackgroundColor(ContextCompat.getColor(context, R.color.middle_sum_color))
                    holder.col1.text = "소계"
                    holder.col2.text = companyName
                }
                else -> {
                    holder.llrow.setBackgroundColor(ContextCompat.getColor(context, R.color.item_color_2))
                    holder.col1.text = company
                    holder.col2.text = itemList[position].nmItem
                }
            }

            //data values setting
            val planQty = itemList[position].planQty!!.toLong()
            val planAmt = itemList[position].planAmt!!.toLong()
            val salesQty = itemList[position].salesQty!!.toLong()
            val salesAmt = itemList[position].salesAmt!!.toLong()

            holder.col3.text = String.format("%,d",planQty)
            holder.col4.text = String.format("%,d",planAmt)
            holder.col5.text = String.format("%,d",salesQty)
            holder.col6.text = String.format("%,d",salesAmt)

            val temp = (itemList[position].salesAmt!!.toFloat()/itemList[position].planAmt!!.toFloat() * 100)
            holder.col7.text = String.format("%.2f", temp)
            if (temp >= 100)
                holder.col7.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
            else
                holder.col7.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val col1: TextView = itemView.findViewById(R.id.salesCol1)
            val col2: TextView = itemView.findViewById(R.id.salesCol2)
            val col3: TextView = itemView.findViewById(R.id.salesCol31)
            val col4: TextView = itemView.findViewById(R.id.salesCol4)
            val col5: TextView = itemView.findViewById(R.id.salesCol5)
            val col6: TextView = itemView.findViewById(R.id.salesCol6)
            val col7: TextView = itemView.findViewById(R.id.salesCol7)
            val llrow : LinearLayout = itemView.findViewById(R.id.llSalesReport)
        }
    }
//    private class SalesReportAdapter(val context: Context, val salesReport:ArrayList<SalesReport>) : BaseAdapter() {
//
//        override fun getCount(): Int {
//            return salesReport.size
//        }
//
//        override fun getItem(position: Int): Any {
//            return salesReport[position]
//        }
//
//        override fun getItemId(position: Int): Long {
//            return position.toLong()
//        }
//
//        @SuppressLint("ViewHolder")
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val thisView = LayoutInflater.from(context).inflate(R.layout.row_wj_sales_report, parent, false)
//
////            if (position and 1 == 0)
////                thisView.setBackgroundResource(R.drawable.list_item_2)
////            else
////                thisView.setBackgroundResource(R.drawable.list_item_3)
//
//            var company : String? = null
//            var companyName : String? = null
//            when(salesReport[position].ccode) {
//                "1000"-> {company = "M" ; companyName = "우전메디칼"}
//                "3000"-> {company = "VT"; companyName = "우전브이티"}
//                "5000"-> {company = "VS"; companyName = "우전브이에스"}
//            }
//            //layout color & name Setting (tot/middle)
//            if (salesReport[position].tot_sum == "1") {
//                thisView.layout_row.setBackgroundColor(ContextCompat.getColor(context, R.color.tot_sum_color))
//                thisView.wjsr_col1.text = "총계"
//                thisView.wjsr_col2.text = ""
//            }
//            else if (salesReport[position].middle_sum == "1") {
//                thisView.layout_row.setBackgroundColor(ContextCompat.getColor(context, R.color.middle_sum_color))
//                thisView.wjsr_col1.text = "소계"
//                thisView.wjsr_col2.text = companyName
//            }
//            else {
//                thisView.wjsr_col1.text = company
//                thisView.wjsr_col2.text = salesReport[position].nmItem
//            }
//
//            //data values setting
//            val planQty = salesReport[position].planQty!!.toLong()
//            val planAmt = salesReport[position].planAmt!!.toLong()
//            val salesQty = salesReport[position].salesQty!!.toLong()
//            val salesAmt = salesReport[position].salesAmt!!.toLong()
//
//            thisView.wjsr_col3.text = String.format("%,d",planQty)
//            thisView.wjsr_col4.text = String.format("%,d",planAmt)
//            thisView.wjsr_col5.text = String.format("%,d",salesQty)
//            thisView.wjsr_col6.text = String.format("%,d",salesAmt)
//
//            val temp = (salesReport[position].salesAmt!!.toFloat()/salesReport[position].planAmt!!.toFloat() * 100)
//            thisView.wjsr_col7.text = String.format("%.2f", temp)
//            if (temp >= 100)
//                thisView.wjsr_col7.setTextColor(ContextCompat.getColor(context, R.color.dtVal_2))
//            else
//                thisView.wjsr_col7.setTextColor(ContextCompat.getColor(context, R.color.dtVal_1))
//
//            return thisView
//        }
//    }

    private fun initData() {
        arSalesReportList.clear()
        salesReportAdapter!!.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun myToast(msg:String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }
}
