package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import android.widget.TableLayout
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjJaegoReportBinding
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit


class WjJaegoReportActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjJaegoReportBinding
    private var cdloc : String? = ""
    private var selCdPln : String? = null
    private var viewCol : Int = 0
    private var nmCol : String? = null
    private var productadapter: ProductAdapter? = null
    private var jaegolistadapter: JaegoListAdapter? = null
    private var jaegolistFTadapter: JaegoListFTAdapter? = null
    var productList : ArrayList<Products> = ArrayList()
    var jaegoList : ArrayList<JaegoList> = ArrayList()
    var jaegoListFT : ArrayList<JaegoListFT> = ArrayList()

    private var jaegocdialogadapter : CDialogAdapter? = null
    private var cdialogList : ArrayList<JaegoCDialog> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_jaego_report)
        bindingA = ActivityWjJaegoReportBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        jaegoReportInit()
    }

    private fun jaegoReportInit() {
        //spin_loc setting
        val spinAdapter : ArrayAdapter<String> = ArrayAdapter(
            this, R.layout.spinnerlayout, resources.getStringArray(
                R.array.loc_list
            )
        )
        bindingA.bindingJeagoReport.spinLoc.adapter = spinAdapter
        bindingA.bindingJeagoReport.spinLoc.setSelection(0)
        bindingA.bindingJeagoReport.spinLoc.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                jaegoList.clear()
                jaegoListFT.clear()
                jaegolistadapter!!.notifyDataSetChanged()
                jaegolistFTadapter!!.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // product list setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bindingA.bindingJeagoReport.productListview.layoutManager = layoutManager

        getProductList()

        productadapter = ProductAdapter(this, productList, onClickItem)
        bindingA.bindingJeagoReport.productListview.adapter = productadapter

        val decoration = MyListDecoration()
        bindingA.bindingJeagoReport.productListview.addItemDecoration(decoration)

        // jaego list Simple view Setting
        val jaegoListManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingJeagoReport.jaegoListview.layoutManager = jaegoListManager

        jaegolistadapter = JaegoListAdapter(this, jaegoList)
        bindingA.bindingJeagoReport.jaegoListview.adapter = jaegolistadapter

        // jaego list FT view Setting
        jaegolistFTadapter = JaegoListFTAdapter(this, jaegoListFT)
        bindingA.bindingJeagoReport.jaegoListview.adapter = jaegolistFTadapter
    }

    private val onClickItem = View.OnClickListener { v ->
        selCdPln  = v.tag as String?
        viewCol = v.findViewById<TextView>(R.id.text_remark).text.toString().toInt()
        nmCol = v.transitionName

        val arSubTitle = nmCol!!.split(",")
        val arSubCol = arrayOf(
            bindingA.bindingJeagoReport.txtSub01Multi,
            bindingA.bindingJeagoReport.txtSub02Multi,
            bindingA.bindingJeagoReport.txtSub03Multi,
            bindingA.bindingJeagoReport.txtSub04Multi,
            bindingA.bindingJeagoReport.txtSub05Multi,
            bindingA.bindingJeagoReport.txtSub06Multi,
            bindingA.bindingJeagoReport.txtSub07Multi,
            bindingA.bindingJeagoReport.txtSub08Multi,
            bindingA.bindingJeagoReport.txtSub09Multi,
            bindingA.bindingJeagoReport.txtSub10Multi
        )
        for (idx in arSubCol.indices) {
            if (viewCol <= idx)
                arSubCol[idx].visibility = View.GONE
            else {
                arSubCol[idx].visibility = View.VISIBLE
                if (viewCol == 1)
                    arSubCol[idx].text = "수량"
                else
                    arSubCol[idx].text = arSubTitle[idx]
            }
        }

        for (idx in 0 until productList.size)
            productList[idx].flag = (productList[idx].cdPln == selCdPln)

        productadapter!!.notifyDataSetChanged()

        jaegoList.clear()
        jaegolistadapter!!.notifyDataSetChanged()
        jaegoListFT.clear()
        jaegolistFTadapter!!.notifyDataSetChanged()

        if (selCdPln == "A41"||selCdPln == "A45") {
            bindingA.bindingJeagoReport.jaegoListview.adapter = jaegolistFTadapter
            getJaegoListFT()
        } else {
            bindingA.bindingJeagoReport.jaegoListview.adapter = jaegolistadapter
            getJaegoList()
        }
    }

    // simple jaego list
    private fun getJaegoList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJJaego_Report_jaego_list.asp")
        val body = FormBody.Builder().add("loc", cdloc!!).add("cd_pln", selCdPln!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    //println("Success to execute request! : $body")

                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBJaegoList = gson.fromJson(body1, DBJaegoList::class.java)

                    for (idx in dBJaegoList.results.indices) {
                        jaegoList.add(
                            JaegoList(
                                dBJaegoList.results[idx].cdSpec,
                                dBJaegoList.results[idx].col01,
                                dBJaegoList.results[idx].col02,
                                dBJaegoList.results[idx].col03,
                                dBJaegoList.results[idx].col04,
                                dBJaegoList.results[idx].col05,
                                dBJaegoList.results[idx].col06,
                                dBJaegoList.results[idx].col07,
                                dBJaegoList.results[idx].col08,
                                dBJaegoList.results[idx].col09,
                                dBJaegoList.results[idx].col10,
                                dBJaegoList.results[idx].scol01,
                                dBJaegoList.results[idx].scol02,
                                dBJaegoList.results[idx].scol03,
                                dBJaegoList.results[idx].scol04,
                                dBJaegoList.results[idx].tot_sum
                            )
                        )
                    }
                    runOnUiThread { //UI에 알려줌
                        jaegolistadapter!!.notifyDataSetChanged()
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }
            })
    }

    // POD FT jaego list
    private fun getJaegoListFT() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJJaego_Report_jaego_list.asp")
        val body = FormBody.Builder().add("loc", cdloc!!).add("cd_pln", selCdPln!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newBuilder().readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    //println("Success to execute request! : $body")

                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBJaegoListFT = gson.fromJson(body1, DBJaegoListFT::class.java)

                    for (idx in dBJaegoListFT.results.indices) {
                        jaegoListFT.add(
                            JaegoListFT(
                                dBJaegoListFT.results[idx].cdSpec,
                                dBJaegoListFT.results[idx].qty_n100,
                                dBJaegoListFT.results[idx].qty_n150,
                                dBJaegoListFT.results[idx].qty_n225,
                                dBJaegoListFT.results[idx].qty_n300,
                                dBJaegoListFT.results[idx].qty_n375,
                                dBJaegoListFT.results[idx].qty_n450,
                                dBJaegoListFT.results[idx].qty_n525,
                                dBJaegoListFT.results[idx].qty_n600,
                                dBJaegoListFT.results[idx].qty_S100,
                                dBJaegoListFT.results[idx].qty_S150,
                                dBJaegoListFT.results[idx].qty_S225,
                                dBJaegoListFT.results[idx].qty_S300,
                                dBJaegoListFT.results[idx].qty_S375,
                                dBJaegoListFT.results[idx].qty_S450,
                                dBJaegoListFT.results[idx].qty_S525,
                                dBJaegoListFT.results[idx].qty_S600,
                                dBJaegoListFT.results[idx].tot_sum
                            )
                        )
                    }
                    runOnUiThread { //UI에 알려줌
                        jaegolistFTadapter!!.notifyDataSetChanged()
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }
            })
    }

    private fun getProductList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJJaego_Report_product_list.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbProductList = gson.fromJson(body1, DBProductList::class.java)

                for (idx in dbProductList.results.indices) {
                    productList.add(
                        Products(
                            dbProductList.results[idx].cdPln,
                            dbProductList.results[idx].nmItem,
                            dbProductList.results[idx].viewCol,
                            dbProductList.results[idx].nmCol,
                            dbProductList.results[idx].flag
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    productadapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getToricReservation(arg1: Int, arg2: String, arg3: String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        cdialogList.clear()
        //println(arg1.toString())
        val url = URL("http://iclkorea.com/android/WJJaego_Report_reservation_list.asp")
        val body = FormBody.Builder().add("axis", arg1.toString()).add("spec", arg2).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbJaegoCDialog = gson.fromJson(body1, DBJaegoCDialog::class.java)

                for (idx in dbJaegoCDialog.results.indices) {
                    cdialogList.add(
                        JaegoCDialog(
                            dbJaegoCDialog.results[idx].noSerial,
                            dbJaegoCDialog.results[idx].axis,
                            dbJaegoCDialog.results[idx].loc,
                            dbJaegoCDialog.results[idx].remark
                        )
                    )
                }

                runOnUiThread { //UI에 알려줌
                    //println(cdialogList.size)
                    cDialog(arg3).show()
                    jaegocdialogadapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBProductList(val results: List<Products>)
    data class Products(
        var cdPln: String?,
        var nmItem: String?,
        var viewCol: String?,
        val nmCol: String?,
        var flag: Boolean?
    )
    data class DBJaegoList(val results: List<JaegoList>)
    data class JaegoList(
        var cdSpec: String?,
        var col01: String?,
        var col02: String?,
        var col03: String?,
        var col04: String?,
        var col05: String?,
        var col06: String?,
        var col07: String?,
        var col08: String?,
        var col09: String?,
        var col10: String?,
        var scol01: String?,
        var scol02: String?,
        var scol03: String?,
        var scol04: String?,
        var tot_sum: Int?
    )
    data class DBJaegoListFT(val results: List<JaegoListFT>)
    data class JaegoListFT(
        var cdSpec: String?, var qty_n100: String?, var qty_n150: String?, var qty_n225: String?,
        var qty_n300: String?, var qty_n375: String?, var qty_n450: String?, var qty_n525: String?,
        var qty_n600: String?, var qty_S100: String?, var qty_S150: String?, var qty_S225: String?,
        var qty_S300: String?, var qty_S375: String?, var qty_S450: String?, var qty_S525: String?,
        var qty_S600: String?, var tot_sum: Int?
    )
    data class DBJaegoCDialog(var results: List<JaegoCDialog>)
    data class JaegoCDialog(
        var noSerial: String?,
        var axis: String?,
        var loc: String?,
        var remark: String?
    )

    //재고 리스트 Simple 노출 recycler view
    inner class JaegoListAdapter(
        private val context: Context,
        private val jaegoList: ArrayList<JaegoList>
    ) : RecyclerView.Adapter<JaegoListAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_wj_jaego_report_multi,
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val textViewArray : Array<TextView> = arrayOf(
                holder.textQty1,
                holder.textQty2,
                holder.textQty3,
                holder.textQty4,
                holder.textQty5,
                holder.textQty6,
                holder.textQty7,
                holder.textQty8,
                holder.textQty9,
                holder.textQty10
            )
            val textViewArraySB : Array<TextView> = arrayOf(
                holder.textSQty1,
                holder.textSQty2,
                holder.textSQty3,
                holder.textSQty4,
                holder.textSQty5,
                holder.textSQty6,
                holder.textSQty7,
                holder.textSQty8,
                holder.textSQty9,
                holder.textSQty10
            )

            // row setting
            if (jaegoList[position].tot_sum == 1) {
                holder.llMultiJList.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.tot_sum_color
                    )
                )
                holder.textSpec.text = "총계"
            } else {
//                if (position and 1 == 0)
//                    holder.llMultiJList.setBackgroundResource(R.drawable.list_item_2)
//                else
//                    holder.llMultiJList.setBackgroundResource(R.drawable.list_item_3)
                holder.llMultiJList.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.white
                    )
                )
                holder.textSpec.text = jaegoList[position].cdSpec
            }

            // value setting
            var col01 = String.format("%,d", jaegoList[position].col01?.toLong())
            if (col01 == "0") col01 = ""
            var col02 = String.format("%,d", jaegoList[position].col02?.toLong())
            if (col02 == "0") col02 = ""
            var col03 = String.format("%,d", jaegoList[position].col03?.toLong())
            if (col03 == "0") col03 = ""
            var col04 = String.format("%,d", jaegoList[position].col04?.toLong())
            if (col04 == "0") col04 = ""
            var col05 = String.format("%,d", jaegoList[position].col05?.toLong())
            if (col05 == "0") col05 = ""
            var col06 = String.format("%,d", jaegoList[position].col06?.toLong())
            if (col06 == "0") col06 = ""
            var col07 = String.format("%,d", jaegoList[position].col07?.toLong())
            if (col07 == "0") col07 = ""
            var col08 = String.format("%,d", jaegoList[position].col08?.toLong())
            if (col08 == "0") col08 = ""
            var col09 = String.format("%,d", jaegoList[position].col09?.toLong())
            if (col09 == "0") col09 = ""
            var col10 = String.format("%,d", jaegoList[position].col10?.toLong())
            if (col10 == "0") col10 = ""
            var scol01 = String.format("%,d", jaegoList[position].scol01?.toLong())
            if (scol01 == "0"||scol01 == "null") scol01 = ""
            var scol02 = String.format("%,d", jaegoList[position].scol02?.toLong())
            if (scol02 == "0"||scol02 == "null") scol02 = ""
            var scol03 = String.format("%,d", jaegoList[position].scol03?.toLong())
            if (scol03 == "0"||scol03 == "null") scol03 = ""
            var scol04 = String.format("%,d", jaegoList[position].scol04?.toLong())
            if (scol04 == "0"||scol04 == "null") scol04 = ""

            holder.textQty1.text = col01
            holder.textQty2.text = col02
            holder.textQty3.text = col03
            holder.textQty4.text = col04
            holder.textQty5.text = col05
            holder.textQty6.text = col06
            holder.textQty7.text = col07
            holder.textQty8.text = col08
            holder.textQty9.text = col09
            holder.textQty10.text = col10
            holder.textSQty1.text = scol01
            holder.textSQty2.text = scol02
            holder.textSQty3.text = scol03
            holder.textSQty4.text = scol04
            // col visible setting
            for (idx in 1 until textViewArray.size) {
                if (viewCol <= idx) {
                    textViewArray[idx].visibility = View.GONE
                    textViewArraySB[idx].visibility = View.GONE
                }
            }

            holder.llJaegoV.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, viewCol*10f)

            // ticl long click
            if (selCdPln == "C06") {
                val row = position.toString()
                val spec = jaegoList[position].cdSpec
                holder.textQty1.tag = "+0.5,$row,$spec"
                holder.textQty1.setOnLongClickListener(onLongClick)
                holder.textQty2.tag = "+1.0,$row,$spec"
                holder.textQty2.setOnLongClickListener(onLongClick)
                holder.textQty3.tag = "+1.5,$row,$spec"
                holder.textQty3.setOnLongClickListener(onLongClick)
                holder.textQty4.tag = "+2.0,$row,$spec"
                holder.textQty4.setOnLongClickListener(onLongClick)
                holder.textQty5.tag = "+2.5,$row,$spec"
                holder.textQty5.setOnLongClickListener(onLongClick)
                holder.textQty6.tag = "+3.0,$row,$spec"
                holder.textQty6.setOnLongClickListener(onLongClick)
                holder.textQty7.tag = "+3.5,$row,$spec"
                holder.textQty7.setOnLongClickListener(onLongClick)
                holder.textQty8.tag = "+4.0,$row,$spec"
                holder.textQty8.setOnLongClickListener(onLongClick)
                holder.textQty9.tag = "+4.5,$row,$spec"
                holder.textQty9.setOnLongClickListener(onLongClick)
                holder.textQty10.tag = "+5.0,$row,$spec"
                holder.textQty10.setOnLongClickListener(onLongClick)
            }
        }

        override fun getItemCount(): Int {
            return jaegoList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textSpec : TextView = itemView.findViewById(R.id.txt_spec_multi)
            var textQty1 : TextView = itemView.findViewById(R.id.txt_col1_multi)
            var textQty2 : TextView = itemView.findViewById(R.id.txt_col2_multi)
            var textQty3 : TextView = itemView.findViewById(R.id.txt_col3_multi)
            var textQty4 : TextView = itemView.findViewById(R.id.txt_col4_multi)
            var textQty5 : TextView = itemView.findViewById(R.id.txt_col5_multi)
            var textQty6 : TextView = itemView.findViewById(R.id.txt_col6_multi)
            var textQty7 : TextView = itemView.findViewById(R.id.txt_col7_multi)
            var textQty8 : TextView = itemView.findViewById(R.id.txt_col8_multi)
            var textQty9 : TextView = itemView.findViewById(R.id.txt_col9_multi)
            var textQty10 : TextView = itemView.findViewById(R.id.txt_col10_multi)

            var textSQty1 : TextView = itemView.findViewById(R.id.txt_scol1_multi)
            var textSQty2 : TextView = itemView.findViewById(R.id.txt_scol2_multi)
            var textSQty3 : TextView = itemView.findViewById(R.id.txt_scol3_multi)
            var textSQty4 : TextView = itemView.findViewById(R.id.txt_scol4_multi)
            var textSQty5 : TextView = itemView.findViewById(R.id.txt_scol5_multi)
            var textSQty6 : TextView = itemView.findViewById(R.id.txt_scol6_multi)
            var textSQty7 : TextView = itemView.findViewById(R.id.txt_scol7_multi)
            var textSQty8 : TextView = itemView.findViewById(R.id.txt_scol8_multi)
            var textSQty9 : TextView = itemView.findViewById(R.id.txt_scol9_multi)
            var textSQty10 : TextView = itemView.findViewById(R.id.txt_scol10_multi)
            var llMultiJList : LinearLayout = itemView.findViewById(R.id.ll_jaego_multi)
            var llJaegoV : LinearLayout = itemView.findViewById(R.id.ll_jaego_v)
        }
    }

    private val onLongClick = View.OnLongClickListener { v->
       // println(v.tag)
        val temp = v.tag.toString().split(",")
        val axis = (temp[0].substring(1, 4).toFloat() * 100).toInt()
        val cdspec = temp[2]
        val cdTitle = temp[2] + " [" + temp[0] + "]"
        getToricReservation(axis, cdspec, cdTitle)
        true
    }

    private fun cDialog(title: String): Dialog {
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.cdialog_layout, nullParent)
        val builder = AlertDialog.Builder(this)

        builder.setView(view)
        val rvCDialog : RecyclerView = view.findViewById(R.id.rvJaegoCDialog)
        val txtTitle : TextView = view.findViewById(R.id.txt_dialog_title)
        val btnClose : Button = view.findViewById(R.id.btnClose)

        when (title.substring(title.length - 6)) {
            "[+5.0]" -> txtTitle.text = String.format("규격 : $title 이상")
            else -> txtTitle.text = String.format("규격 : $title")
        }


        val jaegoListManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvCDialog.layoutManager = jaegoListManager
        // custom dialog lsit view Setting
        jaegocdialogadapter = CDialogAdapter(this, cdialogList)
        rvCDialog.adapter = jaegocdialogadapter

        jaegocdialogadapter!!.notifyDataSetChanged()

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener(({
            dialog.dismiss()
        }))

        return dialog
    }

    //재고 리스트 FT 노출 recycler view
    inner class JaegoListFTAdapter(
        private val context: Context,
        private val jaegoListFT: ArrayList<JaegoListFT>
    ) : RecyclerView.Adapter<JaegoListFTAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_wj_jaego_report_ft,
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (jaegoListFT[position].tot_sum == 1) {
                holder.layoutJaegoListFT.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.tot_sum_color
                    )
                )
                holder.textSpec.text = "총계"
            } else {
//                if (position and 1 == 0)
//                    holder.layoutJaegoListFT.setBackgroundResource(R.drawable.list_item_2)
//                else
//                    holder.layoutJaegoListFT.setBackgroundResource(R.drawable.list_item_3)
                holder.layoutJaegoListFT.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.white
                    )
                )
                holder.textSpec.text = jaegoListFT[position].cdSpec
            }
            if (selCdPln == "A45") {
                holder.textQtyN100.visibility = View.GONE
                holder.textQtyS100.visibility = View.GONE
            } else {
                holder.textQtyN100.visibility = View.VISIBLE
                holder.textQtyS100.visibility = View.VISIBLE
            }
            var qtyn100 = jaegoListFT[position].qty_n100
            if (qtyn100 == "0") qtyn100 = ""
            var qtyn150 = jaegoListFT[position].qty_n150
            if (qtyn150 == "0") qtyn150 = ""
            var qtyn225 = jaegoListFT[position].qty_n225
            if (qtyn225 == "0") qtyn225 = ""
            var qtyn300 = jaegoListFT[position].qty_n300
            if (qtyn300 == "0") qtyn300 = ""
            var qtyn375 = jaegoListFT[position].qty_n375
            if (qtyn375 == "0") qtyn375 = ""
            var qtyn450 = jaegoListFT[position].qty_n450
            if (qtyn450 == "0") qtyn450 = ""
            var qtyn525 = jaegoListFT[position].qty_n525
            if (qtyn525 == "0") qtyn525 = ""
            var qtyn600 = jaegoListFT[position].qty_n600
            if (qtyn600 == "0") qtyn600 = ""

            var qtyS100 = jaegoListFT[position].qty_S100
            if (qtyS100 == "0") qtyS100 = ""
            var qtyS150 = jaegoListFT[position].qty_S150
            if (qtyS150 == "0") qtyS150 = ""
            var qtyS225 = jaegoListFT[position].qty_S225
            if (qtyS225 == "0") qtyS225 = ""
            var qtyS300 = jaegoListFT[position].qty_S300
            if (qtyS300 == "0") qtyS300 = ""
            var qtyS375 = jaegoListFT[position].qty_S375
            if (qtyS375 == "0") qtyS375 = ""
            var qtyS450 = jaegoListFT[position].qty_S450
            if (qtyS450 == "0") qtyS450 = ""
            var qtyS525 = jaegoListFT[position].qty_S525
            if (qtyS525 == "0") qtyS525 = ""
            var qtyS600 = jaegoListFT[position].qty_S600
            if (qtyS600 == "0") qtyS600 = ""
            holder.textQtyN100.text = qtyn100
            holder.textQtyN150.text = qtyn150
            holder.textQtyN225.text = qtyn225
            holder.textQtyN300.text = qtyn300
            holder.textQtyN375.text = qtyn375
            holder.textQtyN450.text = qtyn450
            holder.textQtyN525.text = qtyn525
            holder.textQtyN600.text = qtyn600
            holder.textQtyS100.text = qtyS100
            holder.textQtyS150.text = qtyS150
            holder.textQtyS225.text = qtyS225
            holder.textQtyS300.text = qtyS300
            holder.textQtyS375.text = qtyS375
            holder.textQtyS450.text = qtyS450
            holder.textQtyS525.text = qtyS525
            holder.textQtyS600.text = qtyS600
        }

        override fun getItemCount(): Int {
            return jaegoListFT.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textSpec : TextView = itemView.findViewById(R.id.txt_ft_spec)
            var textQtyN100 : TextView = itemView.findViewById(R.id.txt_n100)
            var textQtyN150 : TextView = itemView.findViewById(R.id.txt_n150)
            var textQtyN225 : TextView = itemView.findViewById(R.id.txt_n225)
            var textQtyN300 : TextView = itemView.findViewById(R.id.txt_n300)
            var textQtyN375 : TextView = itemView.findViewById(R.id.txt_n375)
            var textQtyN450 : TextView = itemView.findViewById(R.id.txt_n450)
            var textQtyN525 : TextView = itemView.findViewById(R.id.txt_n525)
            var textQtyN600 : TextView = itemView.findViewById(R.id.txt_n600)
            var textQtyS100 : TextView = itemView.findViewById(R.id.txt_S100)
            var textQtyS150 : TextView = itemView.findViewById(R.id.txt_S150)
            var textQtyS225 : TextView = itemView.findViewById(R.id.txt_S225)
            var textQtyS300 : TextView = itemView.findViewById(R.id.txt_S300)
            var textQtyS375 : TextView = itemView.findViewById(R.id.txt_S375)
            var textQtyS450 : TextView = itemView.findViewById(R.id.txt_S450)
            var textQtyS525 : TextView = itemView.findViewById(R.id.txt_S525)
            var textQtyS600 : TextView = itemView.findViewById(R.id.txt_S600)
            var layoutJaegoListFT : LinearLayout = itemView.findViewById(R.id.layout_JaegoListFT)

        }
    }

    //제품 리스트 노출 recycler view
    inner class MyListDecoration : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {

            if (parent.getChildAdapterPosition(view) != parent.adapter.itemCount - 1) {
                outRect.right = 30
            }
        }
    }

    inner class ProductAdapter(
        private val context: Context,
        private val itemList: ArrayList<Products>,
        private val onClickItem: View.OnClickListener
    ) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            // context 와 parent.getContext() 는 같다.
            val view = LayoutInflater.from(context)
                .inflate(R.layout.row_vertical_list, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textNm.text = itemList[position].nmItem
            holder.textRemark.text = itemList[position].viewCol
            holder.productLayout.tag = itemList[position].cdPln
            holder.productLayout.transitionName = itemList[position].nmCol
            holder.productLayout.setOnClickListener(onClickItem)

            if (productList[position].flag == true)
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

    inner class CDialogAdapter(
        private var context: Context,
        private var itemList: ArrayList<JaegoCDialog>
    ) : RecyclerView.Adapter<CDialogAdapter.ViewHolder>(){

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_wj_jaego_cdialog,
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

            holder.txtSerial.text = itemList[position].noSerial
            holder.txtAxis.text = itemList[position].axis
            holder.txtRemark.text = itemList[position].remark
            holder.txtLoc.text = itemList[position].loc
            holder.txtRemark.isSelected = true
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val txtSerial : TextView = itemView.findViewById(R.id.txt_jaego_cd_serial)
            val txtAxis : TextView = itemView.findViewById(R.id.txt_jaego_cd_axis)
            val txtRemark : TextView = itemView.findViewById(R.id.txt_jaego_cd_remark)
            val txtLoc : TextView = itemView.findViewById(R.id.txt_jaego_cd_loc)
 //           val clayout : ConstraintLayout = itemView.findViewById(R.id.cl_cdialog_row)
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
}
