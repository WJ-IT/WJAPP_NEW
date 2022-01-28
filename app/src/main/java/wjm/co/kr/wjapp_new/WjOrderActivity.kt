package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder

import okhttp3.*
import java.io.IOException
import java.net.URL
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import wjm.co.kr.wjapp_new.databinding.ActivityWjOrderBinding
import java.util.*
import kotlin.collections.ArrayList


class WjOrderActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjOrderBinding
    private var custCd = ""
    private var custNm = ""
    private var faxNo = ""
    private var selcdUseName : String? = ""
    private var selv2Dia : String? = ""
    private var selv3Bc : String? = ""
    private var selv4Power : String? = ""
    private var selv5Spec : String? = ""
    private var selcdItem : String? = ""
    private var selcdSpec : String? = ""
    private var selordergbn : String? = ""
    private var delrow = 0
    private var arSpinNmItem : ArrayList<String> = ArrayList()
    private var arSpinDia : ArrayList<String> = ArrayList()
    private var arSpinBc : ArrayList<String> = ArrayList()
    private var arSpinPower : ArrayList<String> = ArrayList()
    private var arSpinSpec : ArrayList<String> = ArrayList()


    private lateinit var trV2Dia : TableRow
//    private lateinit var trV3Bc : TableRow
    private lateinit var trV4Power : TableRow
    private lateinit var trV5Spec : TableRow
    private lateinit var spinItem : Spinner

    private lateinit var mHandler : Handler
    private val arProductList : ArrayList<WjOrderProdutList> = ArrayList()
    private val arViewList : ArrayList<WjOrderProdutList> = ArrayList()
    private var wjOrderAdapter : WjOrderAdapter? = null

    lateinit var spinItemAdapter : ArrayAdapter<String>
    lateinit var spinSpecDiaAdapter : ArrayAdapter<String>
    lateinit var spinSpecBcAdapter : ArrayAdapter<String>
    lateinit var spinSpecPowerAdapter : ArrayAdapter<String>
    lateinit var spinSpecPowerAdapter2 : ArrayAdapter<String>
    lateinit var spinSpecSpecAdapter : ArrayAdapter<String>

    private var arBasketGet : ArrayList<WjBasketGet> = ArrayList()
    private var wjBasketAdapter : WjBasketAdapter? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_order)
        bindingA = ActivityWjOrderBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        orderInit()

        // Pod FT surgery Date Select
        bindingA.bindingOrder.txtDtSurg.setOnClickListener(({
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, date ->
                bindingA.bindingOrder.txtDtSurg.text = String.format("%d.%02d.%02d", year, month+1, date)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)
            )
            datePicker.datePicker.minDate = cal.time.time
            datePicker.show()
        }))

        // keyboard related
        bindingA.bindingOrder.clOrdermain.setOnClickListener(({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bindingA.bindingOrder.txtQty.windowToken, 0)
            imm.hideSoftInputFromWindow(bindingA.bindingOrder.txtRemark.windowToken, 0)
            imm.hideSoftInputFromWindow(bindingA.bindingOrder.txtPatient.windowToken, 0)
            imm.hideSoftInputFromWindow(bindingA.bindingOrder.txtAddr.windowToken, 0)
        }))
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // jaego confirm
        bindingA.bindingOrder.btnJaego.setOnClickListener(({
            specConfirm()
        }))

        bindingA.bindingOrder.clBasket.setOnTouchListener { _, _ -> true}

        val constraintSet1 = ConstraintSet()
        constraintSet1.clone(bindingA.bindingOrder.clOrdermain)

        val constraintSet2 = ConstraintSet()
        constraintSet2.clone(this, R.layout.ani_basket)

        var changed = false
        bindingA.bindingOrder.txtBasketBar.setOnClickListener {
            if (changed) {
                bindingA.bindingOrder.tlBasket.alpha=0f
                bindingA.bindingOrder.textView54.alpha=0f
//                btn_basket.alpha=1f
//                btn_basket.isClickable=true
            }
            else {
                bindingA.bindingOrder.tlBasket.alpha=1f
                bindingA.bindingOrder.textView54.alpha=1f
//                btn_basket.alpha=0f
//                btn_basket.isClickable=false
            }
            TransitionManager.beginDelayedTransition(bindingA.bindingOrder.clOrdermain)
            val constraint = if (changed) constraintSet1 else constraintSet2
            constraint.applyTo(bindingA.bindingOrder.clOrdermain)
            changed = !changed
        }

        bindingA.bindingOrder.btnBasket.setOnClickListener(({
            if (chkInBasket()) {

            }
        }))

        bindingA.bindingOrder.btnOrder.setOnClickListener(({
            if (arBasketGet.size > 0) {
                val cancelDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage("장바구니 제품을 주문 하시겠습니까?")
                    .setIcon(R.drawable.wjicon).setTitle("주문 확인")
                    .setPositiveButton("네") { _, _ ->
                        var noReqList = ""
                        for (idx in 0 until arBasketGet.size) {
                            if (idx == 0) noReqList = "'" + arBasketGet[idx].noReq + "'"
                            else noReqList += ",'" + arBasketGet[idx].noReq + "'"
                        }
                        setOrderConfirm(noReqList)

                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
            }
        }))

        bindingA.bindingOrder.btnAllDelete.setOnClickListener(({
            if (arBasketGet.size > 0 && custCd != "") {
                val cancelDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage("장바구니 제품을 모두 삭제 하시겠습니까?")
                    .setIcon(R.drawable.wjicon).setTitle("장바구니 전체삭제")
                    .setPositiveButton("네") { _, _ ->
                        deleteBasket("ALL", custCd)

                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
            }
        }))
    }

    private fun orderInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        if (WjmMain.LoginUser.sno == "") {
            Toast.makeText(baseContext, "로그인 정보가 사라졌어요!", Toast.LENGTH_LONG).show()
            val intent = Intent(baseContext, WjmMain::class.java)
            startActivity(intent)
        }

        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    0 -> {
                        custCd = message.data.getString("custCd")!!
                        custNm = message.data.getString("custNm")!!
                        faxNo = message.data.getString("faxNo")!!
                        bindingA.toolbar.title = "$custNm 주문 등록"
                        getOrderItemList(custCd)
                        getBasketIn(custCd)
                    }
                }
            }
        }
        customerDialog(layoutInflater, mHandler, this, "").show()

        trV2Dia = findViewById(R.id.trV2Dia)
//        trV3Bc = findViewById(R.id.trV3Bc)
        trV4Power = findViewById(R.id.trV4Power)
        trV5Spec = findViewById(R.id.trV5Spec)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bindingA.bindingOrder.rvWjOrder.layoutManager = layoutManager
        wjOrderAdapter = WjOrderAdapter(this, arViewList)
        bindingA.bindingOrder.rvWjOrder.adapter = wjOrderAdapter

        val basketlayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingOrder.rvWjBasket.layoutManager = basketlayoutManager
        wjBasketAdapter= WjBasketAdapter(this, arBasketGet)
        bindingA.bindingOrder.rvWjBasket.adapter = wjBasketAdapter

        spinItem = findViewById(R.id.spin_nmItem)

        spinItemAdapter = SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, arSpinNmItem)
        spinItem.adapter=spinItemAdapter
        spinItem.dropDownVerticalOffset = dipToPixels(32f).toInt()
        spinItem.onItemSelectedListener = onItemSelectedListener
        spinSpecDiaAdapter = ArrayAdapter(this, R.layout.spinnerlayout_order, arSpinDia)
        bindingA.bindingOrder.spinSpecDia.adapter=spinSpecDiaAdapter
        spinSpecBcAdapter = ArrayAdapter(this, R.layout.spinnerlayout_order, arSpinBc)
        bindingA.bindingOrder.spinSpecBc.adapter=spinSpecBcAdapter
        spinSpecPowerAdapter = ArrayAdapter(this, R.layout.spinnerlayout_order, arSpinPower)
        bindingA.bindingOrder.spinSpecPower.adapter=spinSpecPowerAdapter
        spinSpecPowerAdapter2 = ArrayAdapter(this, R.layout.spinnerlayout_order, arSpinPower)
        bindingA.bindingOrder.spinSpecPower2.adapter=spinSpecPowerAdapter2
        spinSpecSpecAdapter = ArrayAdapter(this, R.layout.spinnerlayout_order, arSpinSpec)
        bindingA.bindingOrder.spinSpecSpec.adapter=spinSpecSpecAdapter

        bindingA.bindingOrder.spinSpecDia.onItemSelectedListener = speconItemSelectedListener
        bindingA.bindingOrder.spinSpecBc.onItemSelectedListener = speconItemSelectedListener
        bindingA.bindingOrder.spinSpecPower.onItemSelectedListener = speconItemSelectedListener
        bindingA.bindingOrder.spinSpecPower2.onItemSelectedListener = speconItemSelectedListener
        bindingA.bindingOrder.spinSpecSpec.onItemSelectedListener = speconItemSelectedListener
    }

    private fun specConfirm() {
        val selitem  = WjOrderProdutList("%", "%", "%", "%", "%", "%", "%", false)

        if (selcdItem.isNullOrEmpty()||selcdItem=="0")
            Toast.makeText(this, "주문할 제품을 전택하세요", Toast.LENGTH_LONG).show()
        else {
            selitem.cdItem = selcdItem
            if (selv2Dia == "Y"&& bindingA.bindingOrder.spinSpecDia.selectedItem != null)
                selitem.dcV2 = bindingA.bindingOrder.spinSpecDia.selectedItem.toString()
            else
                if (selcdItem == "10023" || selcdItem == "10028")
                    selitem.dcV2 = "12.5"
            if (selv3Bc == "Y" && bindingA.bindingOrder.spinSpecBc.selectedItem != null) selitem.dcV3 = bindingA.bindingOrder.spinSpecBc.selectedItem.toString()
            if (selv4Power == "Y" && bindingA.bindingOrder.spinSpecPower.selectedItem != null)
                selitem.dcV4 = bindingA.bindingOrder.spinSpecPower.selectedItem.toString()
            else
                if (selcdUseName == "HHV")
                    selitem.dcV4 = bindingA.bindingOrder.spinSpecDia.selectedItem.toString()
            if (selv5Spec == "Y" && bindingA.bindingOrder.spinSpecSpec.selectedItem != null) selitem.dcV5 = bindingA.bindingOrder.spinSpecSpec.selectedItem.toString()
        }

        if (selitem.cdItem != "%") {
            getJaegoQty(selitem)
        }
    }

    private fun chkInBasket() : Boolean {
        if (custCd == "") {
            Toast.makeText(this, "거래처를 선택하세요", Toast.LENGTH_LONG).show()
            return false
        }
        if (selcdUseName == "" || selcdItem == "" || selcdItem == "0") {
            Toast.makeText(this, "주문할 제품을 선택하세요", Toast.LENGTH_LONG).show()
            return false
        }


        if ((selcdItem == "10051" || selcdItem == "10055") && bindingA.bindingOrder.txtDtSurg.text.toString().length != 10) {
            Toast.makeText(this, "수술 일자를 선택하세요", Toast.LENGTH_LONG).show()
            return false
        }
        selordergbn = if (selcdItem != "ETC") {
            if (bindingA.bindingOrder.txtJaegoQty.text.toString() == "") {
                Toast.makeText(this, "제품 규격을 선택하세요", Toast.LENGTH_LONG).show()
                return false
            }
            if (bindingA.bindingOrder.txtQty.text.toString() == "") {
                Toast.makeText(this, "주문 할 수량을 입력하세요", Toast.LENGTH_LONG).show()
                return false
            }
            if (bindingA.bindingOrder.txtQty.text.toString().toLong() > bindingA.bindingOrder.txtJaegoQty.text.toString().toLong())
                "1"
            else
                "0"
        } else {
            "0"
        }


//        println("sno : " +  WjmMain.login_user.sno)
//        println("name : " +  WjmMain.login_user.name)
//        println("cd_hosp : " + custCd)
//        println("nm_hosp : " + custNm)
//        println("cd_item : " + selcdItem)
//        println("nm_item : " + selcdUseName)
//        println("dia : " + spin_specDia.selectedItem.toString())
//        println("bc : " + spin_specBc.selectedItem.toString())
//        println("power : " + spin_specPower.selectedItem.toString())
//        println("spec : " + spin_specSpec.selectedItem.toString())
//        println("qty : " + txt_qty.text.toString())
//        println("remark : " + txt_remark.text.toString())
//        println("dt_surg : " + txt_dt_surg.text.toString())
//        println("addr : " + txt_addr.text.toString())
//        println("patient : " + txt_patient.text.toString())
//        println("fg_conf : " + selordergbn)

        var spinDia = ""
        var spinBc = ""
        var spinPower = ""
        var spinPower2 = ""
        var spinSpec = ""
        var dtSurg = ""

        if (bindingA.bindingOrder.spinSpecDia.selectedItem != null) spinDia = bindingA.bindingOrder.spinSpecDia.selectedItem.toString()
        if (bindingA.bindingOrder.spinSpecBc.selectedItem != null) spinBc = bindingA.bindingOrder.spinSpecBc.selectedItem.toString()
        if (bindingA.bindingOrder.spinSpecPower.selectedItem != null) spinPower = bindingA.bindingOrder.spinSpecPower.selectedItem.toString()
        if (bindingA.bindingOrder.spinSpecPower2.selectedItem != null) spinPower2 = bindingA.bindingOrder.spinSpecPower2.selectedItem.toString()
        if (bindingA.bindingOrder.spinSpecSpec.selectedItem != null) spinSpec = bindingA.bindingOrder.spinSpecSpec.selectedItem.toString()
        if (bindingA.bindingOrder.txtDtSurg.text.toString().length == 10) dtSurg = bindingA.bindingOrder.txtDtSurg.text.toString().substring(0,4)+bindingA.bindingOrder.txtDtSurg.text.toString().substring(5,7)+bindingA.bindingOrder.txtDtSurg.text.toString().substring(8,10)

        val basketIn = WjBasketIn(WjmMain.LoginUser.sno, WjmMain.LoginUser.name, custCd, custNm, selcdItem, selcdUseName
            , spinDia, spinBc, spinPower, spinPower2, spinSpec, bindingA.bindingOrder.txtQty.text.toString(), bindingA.bindingOrder.txtRemark.text.toString(), dtSurg
            , bindingA.bindingOrder.txtAddr.text.toString(), bindingA.bindingOrder.txtPatient.text.toString(), selordergbn, selcdSpec)

        setBasketIn(basketIn)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchItem.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    private fun getOrderItemList(arg1:String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        arProductList.clear()
        arViewList.clear()
        val url = URL("http://iclkorea.com/android/WJOrder_product_list.asp")
        val body = FormBody.Builder().add("cd_cust", arg1).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderList = gson.fromJson(body1, DBWjOrderProductList::class.java)

                for (idx in dbWjOrderList.results.indices) {
                    arProductList.add(
                        WjOrderProdutList(
                            dbWjOrderList.results[idx].cdItem,
                            dbWjOrderList.results[idx].nmItem,
                            dbWjOrderList.results[idx].cdUseName,
                            dbWjOrderList.results[idx].dcV2,
                            dbWjOrderList.results[idx].dcV3,
                            dbWjOrderList.results[idx].dcV4,
                            dbWjOrderList.results[idx].dcV5, false
                        )
                    )
                    if (idx == 0)
                        arViewList.add(WjOrderProdutList(dbWjOrderList.results[idx].cdItem,
                            dbWjOrderList.results[idx].nmItem,
                            dbWjOrderList.results[idx].cdUseName,
                            dbWjOrderList.results[idx].dcV2,
                            dbWjOrderList.results[idx].dcV3,
                            dbWjOrderList.results[idx].dcV4,
                            dbWjOrderList.results[idx].dcV5, false))
                    else if (arViewList[arViewList.size-1].cdUseName != dbWjOrderList.results[idx].cdUseName)
                        arViewList.add(WjOrderProdutList(dbWjOrderList.results[idx].cdItem,
                            dbWjOrderList.results[idx].nmItem,
                            dbWjOrderList.results[idx].cdUseName,
                            dbWjOrderList.results[idx].dcV2,
                            dbWjOrderList.results[idx].dcV3,
                            dbWjOrderList.results[idx].dcV4,
                            dbWjOrderList.results[idx].dcV5, false))

                }

                runOnUiThread { //UI에 알려줌
                    wjOrderAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getSpecList(gbn:String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJOrder_Spec_list.asp")
        val body = FormBody.Builder().add("cdItem", selcdItem!!).add("gbn", gbn).build()

        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderSpecList = gson.fromJson(body1, DBWjOrderSpecList::class.java)

                for (idx in dbWjOrderSpecList.results.indices)
                    when (gbn) {
                        "dia" -> arSpinDia.add(dbWjOrderSpecList.results[idx].spec!!)
                        "bc" -> arSpinBc.add(dbWjOrderSpecList.results[idx].spec!!)
                        "power" -> arSpinPower.add(dbWjOrderSpecList.results[idx].spec!!)
                        "spec" -> arSpinSpec.add(dbWjOrderSpecList.results[idx].spec!!)
                    }

                runOnUiThread { //UI에 알려줌
                    spinSpecDiaAdapter.notifyDataSetChanged()
                    spinSpecBcAdapter.notifyDataSetChanged()
                    spinSpecPowerAdapter.notifyDataSetChanged()
                    spinSpecPowerAdapter2.notifyDataSetChanged()
                    spinSpecSpecAdapter.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getJaegoQty(item : WjOrderProdutList) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val cdItem = item.cdItem
        val cdDia = item.dcV2
        val cdBc = item.dcV3
        val cdPower = item.dcV4
        val cdSpec = item.dcV5
        println(cdItem)
println(cdDia)
       println(cdBc)
        println(cdPower)
        println(cdSpec)
        val url = URL("http://iclkorea.com/android/WJOrder_Jaego_qty.asp")
        val body = FormBody.Builder().add("cdItem", cdItem!!).add("dia", cdDia!!).add("bc", cdBc!!).add("power", cdPower!!).add("spec", cdSpec!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderJaegoQty = gson.fromJson(body1, DBWjOrderJaegoQty::class.java)

                if (dbWjOrderJaegoQty.status == "NO") {
                    runOnUiThread {
                        Toast.makeText(baseContext, "등록된 규격이 아닙니다.", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    selcdSpec = dbWjOrderJaegoQty.cdSpec.toString()
                    runOnUiThread {
                        if (dbWjOrderJaegoQty.results.isEmpty())
                            bindingA.bindingOrder.txtJaegoQty.text = "0"
                        else
                            bindingA.bindingOrder.txtJaegoQty.text = dbWjOrderJaegoQty.results[0].jaegoQty.toString()
                    }
                }

                runOnUiThread{
                    loadingDialog.dismiss()
                }

            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setBasketIn(item : WjBasketIn) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJOrder_SetBasketIn.asp")
        val body = FormBody.Builder().add("sno", item.sno!!).add("name", item.name!!).add("cd_hosp", item.cdHosp!!).add("nm_hosp", item.nmHosp!!)
            .add("cd_item", item.cdItem!!).add("nm_item", item.nmItem!!).add("dia", item.dia).add("bc", item.bc!!).add("power", item.power!!).add("power2", item.power2!!)
            .add("spec", item.spec!!).add("qty", item.qty!!).add("dt_surg", item.dtSurg!!).add("remark", item.remark!!)
            .add("addr", item.addr!!).add("patient", item.patient!!).add("cd_spec", item.cdSpec!!).add("ordergbn", item.ordergbn!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderBasketIn = gson.fromJson(body1, DBWjOrderBasketIn::class.java)

                if (dbWjOrderBasketIn.status == "NO") {
                    runOnUiThread {
                        Toast.makeText(baseContext, "장바구니 등록에 실패하였습니다..", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    getBasketIn(item.cdHosp)
                    runOnUiThread {
                        Toast.makeText(baseContext, "장바구니 등록하였습니다.", Toast.LENGTH_LONG).show()
                    }
                }

                runOnUiThread{
                    loadingDialog.dismiss()
                }

            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getBasketIn(cdHosp:String?) {
        arBasketGet.clear()
        val url = URL("http://iclkorea.com/android/WJOrder_GetBasketIn.asp")
        val body = FormBody.Builder().add("cd_hosp", cdHosp!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderBasketGet = gson.fromJson(body1, DBWjOrderBasketGet::class.java)

                for (idx in dbWjOrderBasketGet.results.indices) {
                   arBasketGet.add(WjBasketGet(dbWjOrderBasketGet.results[idx].noReq
                       , dbWjOrderBasketGet.results[idx].nmItem, dbWjOrderBasketGet.results[idx].nmSpec
                       , dbWjOrderBasketGet.results[idx].qty, dbWjOrderBasketGet.results[idx].ordergbn
                       , dbWjOrderBasketGet.results[idx].patient, dbWjOrderBasketGet.results[idx].remark
                       , dbWjOrderBasketGet.results[idx].addr))
                }

                runOnUiThread{
                    wjBasketAdapter!!.notifyDataSetChanged()
                }

            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })

    }

    private fun deleteBasket(noReq:String?, cdCust:String?) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJOrder_DelBasketIn.asp")
        val body = FormBody.Builder().add("no_req", noReq!!).add("cdCust", cdCust!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderBasketIn = gson.fromJson(body1, DBWjOrderBasketIn::class.java)

                if (dbWjOrderBasketIn.status == "NO") {
                    runOnUiThread {
                        Toast.makeText(baseContext, "주문 삭제에 실패하였습니다..", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    if (noReq == "ALL")
                        arBasketGet.clear()
                    else
                        arBasketGet.removeAt(delrow)
                    runOnUiThread {
                        Toast.makeText(baseContext, "해당 주문을 삭제하였습니다.", Toast.LENGTH_LONG).show()
                    }
                }

                runOnUiThread{
                    wjBasketAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }

            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setOrderConfirm(noReq:String?) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJOrder_Confirm.asp")
        val body = FormBody.Builder().add("no_req", noReq!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbWjOrderBasketIn = gson.fromJson(body1, DBWjOrderBasketIn::class.java)

                if (dbWjOrderBasketIn.status == "NO") {
                    runOnUiThread {
                        Toast.makeText(baseContext, "주문 등록에 실패하였습니다..", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(baseContext, "주문 등록을 완료하였습니다.", Toast.LENGTH_LONG).show()
                    }
                }

                runOnUiThread{
                    arBasketGet.clear()
                    wjBasketAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }

            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBWjOrderProductList(var results:List<WjOrderProdutList>)
    data class WjOrderProdutList(var cdItem:String?, var nmItem:String?,var cdUseName:String?, var dcV2:String?, var dcV3:String?, var dcV4:String?, var dcV5:String?, var flag:Boolean?)
    data class DBWjOrderSpecList(var results:List<WjOrderSpecList>)
    data class WjOrderSpecList(var spec:String?)
    data class DBWjOrderJaegoQty(var results:List<WjOrderJaegoQty>, var status:String?, var cdSpec:String?)
    data class WjOrderJaegoQty(var dcSpec:String?, var jaegoQty:Long?)
    data class DBWjOrderBasketIn(var results:List<WjBasketIn>, var status:String?)
    data class WjBasketIn(var sno:String?, var name:String?, var cdHosp:String?, var nmHosp:String?, var cdItem:String?
                          , var nmItem:String?, var dia:String, var bc:String?, var power:String?, var power2:String?, var spec:String?
                          , var qty:String?, var remark:String?, var dtSurg:String?, var addr:String?, var patient:String?
                          , var ordergbn:String?, var cdSpec:String?)
    data class DBWjOrderBasketGet(var results:List<WjBasketGet>, var status:String?)
    data class WjBasketGet(var noReq:String?, var nmItem:String?, var nmSpec:String?, var qty:String?, var ordergbn:String?
                           , var patient:String?, var remark:String?, var addr:String?)

    inner class WjOrderAdapter(private val context: Context, private val itemList:ArrayList<WjOrderProdutList>) : RecyclerView.Adapter<WjOrderAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_vertical_list , parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textNm.text = itemList[position].cdUseName
            holder.productLayout.setOnClickListener(({
                selcdUseName = itemList[position].cdUseName
                selv2Dia = itemList[position].dcV2
                selv3Bc = itemList[position].dcV3
                selv4Power = itemList[position].dcV4
                selv5Spec = itemList[position].dcV5

                spininit()
                val constraints=ConstraintSet()
                if (selcdUseName!!.contains("POD", ignoreCase = true)
                    || selcdUseName!!.contains("ASPIRA", ignoreCase = true)
                    || selcdUseName!!.contains("ANKORIS", ignoreCase = true)
                    || selcdUseName!!.contains("ISOPURE123", ignoreCase = true)) {
                    bindingA.bindingOrder.spinSpecPower2.visibility = View.VISIBLE
                    bindingA.bindingOrder.txtFrToPower.visibility = View.VISIBLE
                    constraints.clone(bindingA.bindingOrder.clPower)
                    constraints.connect(bindingA.bindingOrder.spinSpecPower.id, ConstraintSet.END, bindingA.bindingOrder.txtFrToPower.id, ConstraintSet.START, convertDpToPixel(0f,context))
                    constraints.applyTo(bindingA.bindingOrder.clPower)
                } else {
                    bindingA.bindingOrder.spinSpecPower2.visibility = View.INVISIBLE
                    bindingA.bindingOrder.txtFrToPower.visibility = View.INVISIBLE
                    constraints.clone(bindingA.bindingOrder.clPower)
                    constraints.connect(bindingA.bindingOrder.spinSpecPower.id, ConstraintSet.END, bindingA.bindingOrder.clPower.id, ConstraintSet.END, convertDpToPixel(16f,context) )
                    constraints.applyTo(bindingA.bindingOrder.clPower)
                }
                for (idx in 0 until arProductList.size)
                    if (selcdUseName == arProductList[idx].cdUseName) {
                        arSpinNmItem.add(arProductList[idx].nmItem!!)
                    }
                spinItemAdapter.notifyDataSetChanged()
                println("스핀사이즈 : ${arSpinNmItem.size}")
                if (arSpinNmItem.size == 2) {
                    spinItem.setSelection(1)
                    setSpec(1)
                }


                for (idx in 0 until arViewList.size)
                    arViewList[idx].flag = (arViewList[idx].cdUseName == selcdUseName)

                wjOrderAdapter!!.notifyDataSetChanged()

                if (itemList[position].dcV2 == "Y")
                    trV2Dia.visibility = View.VISIBLE
                else
                    trV2Dia.visibility = View.GONE

                if (itemList[position].dcV3 == "Y") {
//                    trV3Bc.visibility = View.VISIBLE
                    bindingA.bindingOrder.txtBc.visibility = View.VISIBLE
                    bindingA.bindingOrder.txtBcUnder.visibility = View.VISIBLE
                    bindingA.bindingOrder.spinSpecBc.visibility = View.VISIBLE
                }
                else {
//                    trV3Bc.visibility = View.GONE
                    bindingA.bindingOrder.txtBc.visibility = View.INVISIBLE
                    bindingA.bindingOrder.txtBcUnder.visibility = View.INVISIBLE
                    bindingA.bindingOrder.spinSpecBc.visibility = View.INVISIBLE
                }

                if (itemList[position].dcV4 == "Y")
                    trV4Power.visibility = View.VISIBLE
                else
                    trV4Power.visibility = View.GONE

                if (itemList[position].dcV5 == "Y")
                    trV5Spec.visibility = View.VISIBLE
                else
                    trV5Spec.visibility = View.GONE

                if (itemList[position].cdItem == "10051" || itemList[position].cdItem == "10055")
                    bindingA.bindingOrder.trDtSurg.visibility = View.VISIBLE
                else
                    bindingA.bindingOrder.trDtSurg.visibility = View.GONE

                if (itemList[position].cdUseName!!.substring(0, 3) == "CRT") {
                    bindingA.bindingOrder.txtPower.text = String.format("RZD")
                    bindingA.bindingOrder.txtSpec.text = String.format("LZA")
                }else {
                    bindingA.bindingOrder.txtPower.text = String.format("Power")
                    bindingA.bindingOrder.txtSpec.text = String.format("Spec")
                }

            }))

            if (itemList[position].flag == true)
                holder.productLayout.setBackgroundResource(R.drawable.product_button_on)
            else
                holder.productLayout.setBackgroundResource(R.drawable.product_button)

        }

        inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            var productLayout : LinearLayout = itemView.findViewById(R.id.layout_Product)
            var textNm : TextView = itemView.findViewById(R.id.text_itemNm)
        }
    }

    inner class WjBasketAdapter(private val context : Context, private val itemList:ArrayList<WjBasketGet>) : RecyclerView.Adapter<WjBasketAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_order_basket, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
            if (position and 1 == 0)
                holder.llayout.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.llayout.setBackgroundResource(R.drawable.list_item_3)

            if (itemList[position].ordergbn=="오더")
                holder.gubunBasket.setTextColor(Color.RED)
            else
                holder.gubunBasket.setTextColor(Color.LTGRAY)

            holder.qtyBasket.text = itemList[position].qty
            holder.gubunBasket.text = itemList[position].ordergbn
            holder.itemBasket.text = String.format(itemList[position].nmItem + " / " + itemList[position].nmSpec)
            holder.patientBasket.text = itemList[position].patient

            if (itemList[position].remark!!.isNotEmpty() &&  itemList[position].addr!!.isNotEmpty())
                holder.remarkBasket.text = String.format(itemList[position].remark + " / " + itemList[position].addr)
            else
                holder.remarkBasket.text = String.format(itemList[position].remark + itemList[position].addr)

            holder.btnDelBasket.setOnClickListener(({
                val cancelDialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage(itemList[position].nmItem + " 을 취소하시겠습니까? (" + itemList[position].noReq + ")")
                    .setIcon(R.drawable.wjicon).setTitle("주문 취소확인")
                    .setPositiveButton("네") { _, _ ->
                        deleteBasket(itemList[position].noReq, custCd)
                        delrow = position
                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
            }))
        }

        inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            var itemBasket : TextView = itemView.findViewById(R.id.txt_itembasket)
            var qtyBasket : TextView = itemView.findViewById(R.id.txt_qtybasket)
            var gubunBasket : TextView = itemView.findViewById(R.id.txt_gbnbasket)
            var patientBasket : TextView = itemView.findViewById(R.id.txt_patientbasket)
            var remarkBasket : TextView = itemView.findViewById(R.id.txt_remarkbasket)

            var btnDelBasket : Button = itemView.findViewById(R.id.btn_delbasket)
            var llayout : LinearLayout = itemView.findViewById(R.id.ll_row_basket)
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
            tv.setBackgroundColor(ContextCompat.getColor(applicationContext,R.color.gradation3_start))
            tv.setTextColor(Color.WHITE)
            tv.textSize = 15f
            return cView
        }

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
            tv.setTextColor(Color.BLACK)
            tv.textSize = 20f
            return cView
        }
    }

    private fun spininit() {
        arSpinNmItem.clear()
        arSpinNmItem.add("선택하세요")
        spinItem.setSelection(0)
        selcdItem = "0"
        selordergbn = ""
        bindingA.bindingOrder.txtDtSurg.text = ""
        specinit()
    }

    private fun specinit() {

        arSpinDia.clear()
        arSpinBc.clear()
        arSpinPower.clear()
        arSpinSpec.clear()
        spinSpecDiaAdapter.notifyDataSetChanged()
        spinSpecBcAdapter.notifyDataSetChanged()
        spinSpecPowerAdapter.notifyDataSetChanged()
        spinSpecSpecAdapter.notifyDataSetChanged()
        bindingA.bindingOrder.txtJaegoQty.text = ""
    }

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            setSpec(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private fun setSpec(position: Int) {
        for (idx in 0 until arProductList.size) {
            if (arSpinNmItem[position] == arProductList[idx].nmItem)
                selcdItem = arProductList[idx].cdItem
        }
        if (selv2Dia == "Y") {
            getSpecList("dia")
        }
        if (selv3Bc == "Y") {
            getSpecList("bc")
        }
        if (selv4Power == "Y") {
            getSpecList("power")
        }
        if (selv5Spec == "Y") {
            getSpecList("spec")
        }

        if (selv2Dia == "N" && selv3Bc == "N" && selv4Power == "N" && selv5Spec == "N")
            specConfirm()
    }

    private val speconItemSelectedListener = object : AdapterView.OnItemSelectedListener{
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            specConfirm()
            bindingA.bindingOrder.txtJaegoQty.text = ""
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }
    private fun dipToPixels(dipValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue,
            resources.displayMetrics
        )
    }
    private fun convertDpToPixel(dp: Float, context: Context): Int {
        return (dp * (context.resources
            .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.toolbar_cust_select -> customerDialog(layoutInflater, mHandler, this, "").show()
        }
        return super.onOptionsItemSelected(item)
    }
}
