package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjBalanceManageBinding
import java.io.IOException
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class WjBalanceManageActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjBalanceManageBinding
    private var custCd : String? = null
    private var custNm : String? = null
    private var team : String? = null
    private var mHandler : Handler? = null
    private var seldate : String? = "20190301"

    private var arData : ArrayList<BMData> = ArrayList()
    private var bmAdapter : BMAdapter? = null
    private var bmMonAdapter:BMMonAdapter? = null
    lateinit var loadingDialog : LodingDialog

    private val decimalFormat = DecimalFormat("###,###")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_balance_manage)
        bindingA = ActivityWjBalanceManageBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        balaceManageInit()

        bindingA.bindingBalanceMng.btnBalanceManage.setOnClickListener(({
            loadingDialog.show()
            arData.clear()
            getBalanceManageData()
        }))

//        binding.clBalanceManage.setOnClickListener(({
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
////            imm.hideSoftInputFromWindow(edit_now_rmk.windowToken, 0)
////            imm.hideSoftInputFromWindow(txt_targetAmt.windowToken, 0)
//        }))

    }

    private fun balaceManageInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        loadingDialog = LodingDialog(this)

        // Customer Select
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    0 -> {
                        custCd = message.data.getString("custCd")
                        custNm = message.data.getString("custNm")
                        team = message.data.getString("team")
                        bindingA.toolbar.title = "$custNm 현잔고 회전관리"
                        bindingA.bindingBalanceMng.txtManager.text = team
                    }
                }
            }
        }
        customerDialog(layoutInflater, mHandler!!, this, "NF").show()

        // recycler view adapter setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingBalanceMng.rvBalaceManage.layoutManager = layoutManager
        bmAdapter = BMAdapter(this, arData)
        bindingA.bindingBalanceMng.rvBalaceManage.adapter = bmAdapter


        bindingA.bindingBalanceMng.txtBmDate.setOnClickListener(({
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, date ->
                seldate = String.format("%d.%02d.%02d", year, month+1, date)
                bindingA.bindingBalanceMng.txtBmDate.text = seldate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)
            )
            datePicker.show()
            updateDate()
        }))

        // date setting
        val calendarNow = Calendar.getInstance()
        seldate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(calendarNow.time)
        updateDate()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuSearch = menu.findItem(R.id.app_bar_search)
        menuSearch.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.toolbar_cust_select -> {
                customerDialog(layoutInflater, mHandler!!, this, "NF").show()
                arData.clear()
                bmAdapter!!.notifyDataSetChanged()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateDate() {
        bindingA.bindingBalanceMng.txtBmDate.text = seldate
    }

    private fun getBalanceManageData() {
        val nowdt = seldate!!.replace(".", "")
        val url = URL("http://iclkorea.com/android/WJBalanceManage_Data.asp")
        val body = FormBody.Builder().add("cdCust", custCd!!).add("nowdt", nowdt).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbBMData = gson.fromJson(body1, DBBMData::class.java)

                for (element in dbBMData.results)
                    arData.add(computeBalanceAmt(element))

                runOnUiThread {
                    bmAdapter!!.notifyDataSetChanged()
                    loadingDialog.hide()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }

        })
    }

    private fun saveAmtRmk(arg1:String, arg2:String, arg3:String) {
        var cCode = ""
        when(arg3){
            "우전메디칼"->cCode = "1000"
            "우전브이티"->cCode = "3000"
            "우전브이에스"->cCode = "5000"
        }
        val url = URL("http://iclkorea.com/android/WJBalanceManage_Save.asp")
        val body = FormBody.Builder().add("cCode", cCode).add("cdCust", custCd!!).add("seldate", seldate!!.replace(".","")).add("nowrmk", arg2).add("targetamt", arg1).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbResult = gson.fromJson(body1, DBResult::class.java)

                runOnUiThread {
                    loadingDialog.hide()
                    Toast.makeText(baseContext, dbResult.results, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }

        })
    }

    data class DBBMData(var results:List<BMData>)
    data class BMData(var company:String?, var befRmk:String?, var nowRmk:String?, var befRotation:String?, var nowRotation:String?
                      , var balanceAmt:String?, var targetAmt:String?, var colletAmt:String?
                      , var amt01:String?, var amt02:String?, var amt03:String?, var amt04:String?, var amt05:String?, var amt06:String?
                      , var amt07:String?, var amt08:String?, var amt09:String?, var amt10:String?, var amt11:String?, var amt12:String?, var amt13:String?)
    data class BMMonData(var mon:String?, var amt:String?)
    data class DBResult(var results:String?, var status:String?)

    private fun computeBalanceAmt(arItem : BMData) : BMData {
        val balanceAmt = arItem.balanceAmt!!.toLong()
        var remainAmt = balanceAmt
        val arAmt = arrayOf(arItem.amt01!!.toLong(),arItem.amt02!!.toLong(), arItem.amt03!!.toLong(), arItem.amt04!!.toLong()
            , arItem.amt05!!.toLong(), arItem.amt06!!.toLong(), arItem.amt07!!.toLong(), arItem.amt08!!.toLong(), arItem.amt09!!.toLong()
            , arItem.amt10!!.toLong(), arItem.amt11!!.toLong(), arItem.amt12!!.toLong(), arItem.amt13!!.toLong())

        for (idx in 0..12) {
            if (remainAmt > arAmt[idx]) remainAmt -= arAmt[idx]
            else if (remainAmt <= arAmt[idx]) {
                arAmt[idx] = remainAmt
                remainAmt = 0
            }
        }

        return BMData(arItem.company, arItem.befRmk, arItem.nowRmk, arItem.befRotation, arItem.nowRotation, arItem.balanceAmt, arItem.targetAmt, arItem.colletAmt
            , arAmt[0].toString(), arAmt[1].toString(), arAmt[2].toString(), arAmt[3].toString(), arAmt[4].toString(), arAmt[5].toString(), arAmt[6].toString(), arAmt[7].toString()
            , arAmt[8].toString(), arAmt[9].toString(), arAmt[10].toString(), arAmt[11].toString(), arAmt[12].toString())
    }

    inner class BMAdapter(val context:Context, private var itemList:ArrayList<BMData>) : RecyclerView.Adapter<BMAdapter.ViewHolder>() {
        var result = ""
        //var itemClick: ItemClick? = null

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_balance_manage, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

            holder.txtCompany.text = itemList[position].company
            holder.txtNowRotation.text = String.format(itemList[position].nowRotation+"일")
            holder.txtBefRotation.text = String.format(itemList[position].befRotation+"일")
            val balanceAmt = itemList[position].balanceAmt!!.toLong()
            holder.txtBalanceAmt.text = String.format("%,d", balanceAmt)
            holder.txtTargetAmt.setText(itemList[position].targetAmt)
            val colletAmt = itemList[position].colletAmt!!.toLong()
            holder.txtCollectAmt.text = String.format("%,d", colletAmt)
            holder.txtBeforeRmk.text = itemList[position].befRmk
            holder.txtNowRmk.setText(itemList[position].nowRmk)
            holder.btnSaveRmk.setOnClickListener(({
                saveAmtRmk(holder.txtTargetAmt.text.toString(), holder.txtNowRmk.text.toString(), holder.txtCompany.text.toString())
            }))

            val arMonData : ArrayList<BMMonData> = ArrayList()
            val arAmt = arrayOf(itemList[position].amt01,itemList[position].amt02, itemList[position].amt03, itemList[position].amt04, itemList[position].amt05, itemList[position].amt06
                , itemList[position].amt07, itemList[position].amt08, itemList[position].amt09, itemList[position].amt10, itemList[position].amt11, itemList[position].amt12, itemList[position].amt13 )
            val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = format.parse(seldate!!)!!
            calendar.add(Calendar.MONTH, 1)
            for (i in 0 .. 12) {
                calendar.add(Calendar.MONTH, -1)
                arMonData.add(BMMonData(SimpleDateFormat("MM月", Locale.getDefault()).format(calendar.time), arAmt[i]))
            }
            bmMonAdapter = BMMonAdapter(context, arMonData)
            holder.rvMonData.adapter = bmMonAdapter

            holder.txtTargetAmt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if(!TextUtils.isEmpty(s.toString()) && s.toString() != result && !s.isNullOrEmpty()){

                        result = decimalFormat.format(s.toString().replace(",","").toDouble())
                        itemList[position].targetAmt = result
                        holder.txtTargetAmt.setText(result)
                        holder.txtTargetAmt.setSelection(holder.txtTargetAmt.length())
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
            })
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtCompany : TextView = itemView.findViewById(R.id.txt_company)
            val txtNowRotation : TextView = itemView.findViewById(R.id.txt_nowrotation)
            val txtBefRotation : TextView = itemView.findViewById(R.id.txt_befrotation)
            val txtBalanceAmt : TextView = itemView.findViewById(R.id.txt_balanceamt)
            val txtTargetAmt : EditText = itemView.findViewById(R.id.txt_targetAmt)
            val txtCollectAmt : TextView = itemView.findViewById(R.id.txt_colletAmt)
            val txtBeforeRmk : TextView = itemView.findViewById(R.id.txt_before_rmk)
            val txtNowRmk : EditText = itemView.findViewById(R.id.edit_now_rmk)
            val btnSaveRmk : Button = itemView.findViewById(R.id.btn_savermk)
            val rvMonData : RecyclerView = itemView.findViewById(R.id.rvBMMonData)

            init {
                val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                rvMonData.layoutManager = layoutManager
            }
        }

//        private val watcher : TextWatcher = object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//            }
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if(!TextUtils.isEmpty(s.toString()) && s.toString() != result && !s.isNullOrEmpty()){
//                    result = decimalFormat.format(s.toString().replace(",","").toDouble())
//                    txt_targetAmt.setText(result)
//                    txt_targetAmt.setSelection(txt_targetAmt.length())
//                }
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//            }
//        }
    }

    inner class BMMonAdapter(val context: Context, private var itemList:ArrayList<BMMonData>) : RecyclerView.Adapter<BMMonAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_vertical_balance_manage, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.clBM.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.clBM.setBackgroundResource(R.drawable.list_item_3)

            holder.txtMon.text = itemList[position].mon
            val amt = itemList[position].amt!!.toLong()
            holder.txtAmt.text = String.format("%,d", amt)
            //holder.txtAmt.text = itemList[position].amt
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtMon : TextView = itemView.findViewById(R.id.txt_balance_mon)
            val txtAmt : TextView = itemView.findViewById(R.id.txt_balance_amt)
            val clBM : ConstraintLayout = itemView.findViewById(R.id.cl_balance_manage)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog.dismiss()
    }
}
