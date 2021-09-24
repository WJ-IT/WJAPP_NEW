package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.SearchManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjPodftJisiBinding
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WjPodftJisiActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjPodftJisiBinding
    private var selDateFr = ""
    private var selDateTo = ""
    private var selCancelYN = "N"
    private var selOutYN = "N"
    private var selNoReqs = ""
    private var selInjecotrs = ""
    private var selDtOrds = ""
    private var totChkCount = 0
    private var layoutManager : LinearLayoutManager? = null
    private var podftJisiAdapter : PodFTJisiAdapter? = null
    private var arPodFTJisiList : ArrayList<PodFTJisiList> = ArrayList()
    private var arOriginList : ArrayList<PodFTJisiList> = ArrayList()

    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_podft_jisi)
        bindingA = ActivityWjPodftJisiBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        podftJisiInit()

        bindingA.bindingPodftJisi.btnJisiSearch.setOnClickListener(({
            if (checkDate()) {
                totChkCount = 0
                bindingA.bindingPodftJisi.llJisiButtons.visibility = View.GONE
                getPodFTJisiData()
            }
        }))

        bindingA.bindingPodftJisi.btnJisiSeoul.setOnClickListener(({
            val jisiDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
            jisiDialog.setMessage(" (서울)체크한 주문을 출고지시 하시겠습니까? [${totChkCount}건] ")
                .setIcon(R.drawable.wjicon).setTitle("POD FT/ANKORIS 출고지시")
                .setPositiveButton("네") { _, _ ->
                    setJisiConfirm("SEOUL")
                }
                .setNegativeButton("아니오", null)
                .setCancelable(false)
                .show()
        }))

        bindingA.bindingPodftJisi.btnJisiBusan.setOnClickListener(({
            val jisiDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
            jisiDialog.setMessage(" (부산)체크한 주문을 출고지시 하시겠습니까? [${totChkCount}건]")
                .setIcon(R.drawable.wjicon).setTitle("POD FT/ANKORIS 출고지시")
                .setPositiveButton("네") { _, _ ->
                    setJisiConfirm("BUSAN")
                }
                .setNegativeButton("아니오", null)
                .setCancelable(false)
                .show()
        }))

        bindingA.bindingPodftJisi.btnJisiDaegu.setOnClickListener(({
            val jisiDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
            jisiDialog.setMessage(" (대구)체크한 주문을 출고지시 하시겠습니까? [${totChkCount}건]")
                .setIcon(R.drawable.wjicon).setTitle("POD FT/ANKORIS 출고지시")
                .setPositiveButton("네") { _, _ ->
                    setJisiConfirm("DAEGU")
                }
                .setNegativeButton("아니오", null)
                .setCancelable(false)
                .show()
        }))

        bindingA.bindingPodftJisi.chkJisiCancel.setOnClickListener(({
            if (bindingA.bindingPodftJisi.chkJisiCancel.isChecked) {
                selCancelYN = "N"
                bindingA.bindingPodftJisi.chkJisiCancel.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                bindingA.bindingPodftJisi.chkJisiCancel.isChecked = false
            } else {
                selCancelYN = "Y"
                bindingA.bindingPodftJisi.chkJisiCancel.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                bindingA.bindingPodftJisi.chkJisiCancel.isChecked = true
            }
        }))

        bindingA.bindingPodftJisi.chkJisiOut.setOnClickListener(({
            if (bindingA.bindingPodftJisi.chkJisiOut.isChecked) {
                selOutYN = "N"
                bindingA.bindingPodftJisi.chkJisiOut.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                bindingA.bindingPodftJisi.chkJisiOut.isChecked = false
            } else {
                selOutYN = "Y"
                bindingA.bindingPodftJisi.chkJisiOut.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                bindingA.bindingPodftJisi.chkJisiOut.isChecked = true
            }
        }))

        bindingA.bindingPodftJisi.clPodFTJisi.setOnClickListener(({
            closeKeyboard()
        }))

        bindingA.bindingPodftJisi.llJisiButtons.visibility = View.GONE
    }

    fun closeKeyboard() {
        val view = this.currentFocus

        if(view != null)
        {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        //키보드 관련은 아니지만..
        if (totChkCount > 0) bindingA.bindingPodftJisi.llJisiButtons.visibility = View.VISIBLE
        else bindingA.bindingPodftJisi.llJisiButtons.visibility = View.GONE
    }

    private fun podftJisiInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        //network connecting
        val networkState = NetworkState(this)
        if (networkState.isConnected())
        else myToast( "네트워크에 연결되지 않았습니다.")

        // date init setting
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
        selDateFr = selDateTo.substring(0,6) + "01"
        bindingA.bindingPodftJisi.etxtJisiDateFr.setText(selDateFr)
        //etxtJisiDateTo.setText(selDateTo)

        // recyclerView init setting
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingPodftJisi.rvPodftJisi.layoutManager = layoutManager
        podftJisiAdapter = PodFTJisiAdapter(this, arPodFTJisiList)
        bindingA.bindingPodftJisi.rvPodftJisi.adapter = podftJisiAdapter

        totChkCount = 0
        bindingA.bindingPodftJisi.llJisiButtons.visibility = View.GONE
        getPodFTJisiData()
    }

    private fun checkDate() : Boolean {
        val dateFr = bindingA.bindingPodftJisi.etxtJisiDateFr.text.toString()
        val dateTo = bindingA.bindingPodftJisi.etxtJisiDateTo.text.toString()
        if (dateFr.length != 8 || (dateTo.isNotEmpty() && dateTo.length != 8)) {
            Toast.makeText(this, "날짜는 8자리로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.isLenient = false
        try {
            sdf.parse(dateFr)
            if (dateTo.isNotEmpty())
                sdf.parse(dateTo)
        }catch (e: ParseException){
            Toast.makeText(this, "정확한 날짜로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        selDateFr = dateFr
        selDateTo = dateTo
        return true
    }

    private fun checkDate2(dt: String?) : Boolean {
        if (dt.isNullOrEmpty() || dt.length != 8 ) {
            Toast.makeText(this, "출고지시일자는 8자리로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.isLenient = false
        try {
            sdf.parse(dt)
        }catch (e: ParseException){
            Toast.makeText(this, "출고지시일자를 정확한 날짜로 입력해주세요.", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun checkedItem() : Boolean {
        var i = 0
        for (obj in arPodFTJisiList) {
            if (!obj.chk.isNullOrEmpty() && obj.chk!! == "Y") {
                if (i == 0) {
                    selNoReqs = obj.noReq!!
                    selInjecotrs = if (obj.injector.isNullOrEmpty()) "0"
                    else obj.injector!!
                    if (checkDate2(obj.dtOrd)) selDtOrds = obj.dtOrd!!
                    else {
                        selNoReqs = ""
                        selInjecotrs = ""
                        selDtOrds = ""
                        return false
                    }
                    i += 1
                }
                else {
                    selNoReqs = selNoReqs + "," + obj.noReq!!
                    selInjecotrs += if (obj.injector.isNullOrEmpty()) ",0"
                    else "," + obj.injector!!
                    if (checkDate2(obj.dtOrd)) selDtOrds += "," + obj.dtOrd!!
                    else {
                        selNoReqs = ""
                        selInjecotrs = ""
                        selDtOrds = ""
                        return false
                    }
                    i += 1
                }
            }
        }
        return true
    }

    private fun getPodFTJisiData() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val temp: String = if (bindingA.bindingPodftJisi.rbtnPodFT.isChecked) "10051"
        else "10055"

        val url = URL("http://iclkorea.com/android/WJPodFTJisi_list.asp")
        val body = FormBody.Builder().add("cditem", temp).add("frdt", selDateFr).add("todt", selDateTo).add("cancelyn", selCancelYN).add("outyn", selOutYN).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        arPodFTJisiList.clear()
        arOriginList.clear()

        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                  //  println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBPodFTJisiList = gson.fromJson(body1, DBPodFTJisiList::class.java)

                    arPodFTJisiList.addAll(dBPodFTJisiList.results)
                    arOriginList.addAll(arPodFTJisiList)
                    runOnUiThread {
                        podftJisiAdapter!!.notifyDataSetChanged()
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }

            })
    }

    private fun setCancelOrder(noReq: String, position: Int) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJPodFTJisi_cancel.asp")
        val body = FormBody.Builder().add("noReq", noReq).add("kname", WjmMain.LoginUser.name).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()

        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
//                    println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBPodRTJisiResult = gson.fromJson(body1, DBPodRTJisiResult::class.java)

                    if (dBPodRTJisiResult.results == "OK") {
                        arPodFTJisiList.removeAt(position)
                        arOriginList.removeAt(position)
                        runOnUiThread {
                            podftJisiAdapter!!.notifyDataSetChanged()
                            loadingDialog.dismiss()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(baseContext, "주문취소를 실패하였습니다.", Toast.LENGTH_LONG).show()
                            loadingDialog.dismiss()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }

            })
    }

    private fun setSaveRmk(noReq: String, patient: String, rmk: String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJPodFTJisi_save.asp")
        val body = FormBody.Builder().add("noReq", noReq).add("patient", patient).add("rmk", rmk).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()

        client.newBuilder().readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES).build()
            .newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
//                    println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dBPodRTJisiResult = gson.fromJson(body1, DBPodRTJisiResult::class.java)

                    if (dBPodRTJisiResult.results == "OK") {
                        for ((index, value) in arPodFTJisiList.withIndex()) {
                            if (value.noReq == noReq) {
                                val temp = PodFTJisiList(value.noReq, value.cdItem, value.cdSpec, value.nmHosp, value.power, value.specAdd, value.qty, value.fgConf, rmk, patient, value.dtOrd, value.dtSurg, value.dtOut, value.dtCancel, value.nmCancel, value.nmUser, value.waCnt, value.wdCnt, value.stanby, value.chk, value.injector, value.dtReg)
                                arPodFTJisiList[index] = temp
                            }
                        }
                        for ((index, value) in arOriginList.withIndex()) {
                            if (value.noReq == noReq) {
                                val temp = PodFTJisiList(value.noReq, value.cdItem, value.cdSpec, value.nmHosp, value.power, value.specAdd, value.qty, value.fgConf, rmk, patient, value.dtOrd, value.dtSurg, value.dtOut, value.dtCancel, value.nmCancel, value.nmUser, value.waCnt, value.wdCnt, value.stanby, value.chk, value.injector, value.dtReg)
                                arOriginList[index] = temp
                            }
                        }
                        runOnUiThread {
                            podftJisiAdapter!!.notifyDataSetChanged()
                            loadingDialog.dismiss()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(baseContext, "환자명/비고 변경에 실패하였습니다.", Toast.LENGTH_LONG).show()
                            loadingDialog.dismiss()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }

            })
    }

    private fun setJisiConfirm(gbn: String) {
        if (checkedItem()) {
            println(selNoReqs)
            println(selInjecotrs)
            println(selDtOrds)
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJPodFTJisi_Confirm.asp")
        val body = FormBody.Builder().add("gbn", gbn).add("noReqs", selNoReqs).add("injs", selInjecotrs).add("dtOrds", selDtOrds).build()
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
                    val dBPodRTJisiResult = gson.fromJson(body1, DBPodRTJisiResult::class.java)

                    if (dBPodRTJisiResult.results == "OK") {
                        runOnUiThread {
                            podftJisiAdapter!!.notifyDataSetChanged()
                            loadingDialog.dismiss()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(baseContext, "출고지시를 실패하였습니다.", Toast.LENGTH_LONG).show()
                            loadingDialog.dismiss()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }

            })
        }

    }

    data class DBPodRTJisiResult(var results:String?, var status:String?)
    data class DBPodFTJisiList(var results:List<PodFTJisiList>)
    data class PodFTJisiList(var noReq:String?, var cdItem:String?, var cdSpec:String?, var nmHosp:String?, var power:String?, var specAdd:String?, var qty:String?, var fgConf:String?, var dcRmk:String?, var hospRmk:String?, var dtOrd:String?,
                             var dtSurg:String?, var dtOut:String?, var dtCancel:String?, var nmCancel:String?, var nmUser:String?, var waCnt:String?, var wdCnt:String?, var stanby:String?, var chk:String?, var injector:String?, var dtReg:String?)

    inner class PodFTJisiAdapter(var context : Context, private var itemList:ArrayList<PodFTJisiList>) : RecyclerView.Adapter<PodFTJisiAdapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return arPodFTJisiList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_podft_jisi, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == 0) holder.trJisiNmHosp.visibility = View.VISIBLE
            else
                if ((itemList[position].nmHosp != itemList[position - 1].nmHosp) || (itemList[position].dtReg != itemList[position - 1].dtReg))
                    holder.trJisiNmHosp.visibility = View.VISIBLE
                else
                    holder.trJisiNmHosp.visibility = View.GONE

            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.DATE, 0)
            if (SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time) < itemList[position].dtSurg!!) {
                holder.txtJisiDtSurg.background.setTint(Color.TRANSPARENT)
                holder.txtJisiDtSurg.setTextColor(Color.BLACK)
            } else {
              //  holder.txtJisiDtSurg.background = resources.getDrawable(R.drawable.bg_round_red, null)
                holder.txtJisiDtSurg.background.setTint(Color.RED)
                holder.txtJisiDtSurg.setTextColor(Color.WHITE)
            }

            if (itemList[position].chk == "Y")
                holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            else
                holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)

            holder.txtDtReg.text = itemList[position].dtReg
            holder.txtJisiNmHosp.text = itemList[position].nmHosp
            holder.txtJisiPower.text = itemList[position].power
            holder.txtJisiSpecAdd.text = itemList[position].specAdd
            holder.txtjisiTagcnt.text = ("${itemList[position].waCnt}(${itemList[position].wdCnt})")
            holder.txtJisiQty.text = itemList[position].qty
            holder.txtJisiSB.text = itemList[position].stanby
            holder.txtJisiDtSurg.text = itemList[position].dtSurg
            holder.txtJisiPatient.setText(itemList[position].hospRmk)
            holder.txtJisiDtOut.text = itemList[position].dtOut
            holder.txtJisiRmk.setText(itemList[position].dcRmk)
            holder.txtJisiDtCancel.text = itemList[position].dtCancel
            //holder.txtJisiRmk.text = itemList[position].dcRmk
            holder.txtJisiDtJisi.text = itemList[position].dtOrd
            holder.txtJisiInj.setText(itemList[position].injector)

            when (itemList[position].fgConf) {
                "1" -> {
                    holder.txtJisiGbn.text = "오더"
                    holder.txtJisiGbn.setTextColor(Color.RED)
                    holder.txtJisiPower.setTextColor(Color.RED)
                    holder.txtJisiSpecAdd.setTextColor(Color.RED)
                    holder.txtJisiPatient.setTextColor(Color.RED)
                }
                "0" -> {
                    holder.txtJisiGbn.text = "일반"
                    holder.txtJisiGbn.setTextColor(Color.LTGRAY)
                    holder.txtJisiPower.setTextColor(Color.BLACK)
                    holder.txtJisiSpecAdd.setTextColor(Color.BLACK)
                    holder.txtJisiPatient.setTextColor(Color.BLACK)
                }
                "C" -> {
                    holder.txtJisiGbn.text = "취소"
                    holder.txtJisiGbn.setTextColor(Color.LTGRAY)
                    holder.txtJisiPower.setTextColor(Color.LTGRAY)
                    holder.txtJisiSpecAdd.setTextColor(Color.LTGRAY)
                    holder.txtJisiPatient.setTextColor(Color.LTGRAY)
                }
            }

            if (itemList[position].dtCancel.isNullOrEmpty() && itemList[position].dtOut.isNullOrEmpty() ) {
                holder.btnCancel.visibility = View.VISIBLE
                holder.txtJisiDtCancel.visibility = View.GONE
            } else {
                holder.btnCancel.visibility = View.GONE
                holder.txtJisiDtCancel.visibility = View.VISIBLE
            }
            holder.trJisiUp.setOnClickListener(({
                if (itemList[position].dtCancel.isNullOrEmpty() && itemList[position].dtOut.isNullOrEmpty() ) {
                    if (holder.txtJisiChk.isChecked) {
                        holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.txtJisiChk.isChecked = false
                        itemList[position].chk = "N"
                        totChkCount --
                    } else {
                        holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.txtJisiChk.isChecked = true
                        itemList[position].chk = "Y"
                        totChkCount ++
                    }
                }
                closeKeyboard()
            }))
            holder.trJisiDown.setOnClickListener(({

                if (itemList[position].dtCancel.isNullOrEmpty() && itemList[position].dtOut.isNullOrEmpty() ) {
                    if (holder.txtJisiChk.isChecked) {
                        holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.txtJisiChk.isChecked = false
                        itemList[position].chk = "N"
                        totChkCount --
                    } else {
                        holder.txtJisiChk.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.txtJisiChk.isChecked = true
                        itemList[position].chk = "Y"
                        totChkCount ++
                    }
                }
                closeKeyboard()
            }))

            holder.trJisiNmHosp.setOnClickListener(({
                closeKeyboard()
            }))

            holder.txtJisiInj.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        val req = arPodFTJisiList[holder.adapterPosition].noReq
                        for ((index, value) in arPodFTJisiList.withIndex()) {
                            if (req == value.noReq)
                                arPodFTJisiList[index].injector = s.toString()

                        }
                        for ((index, value) in arOriginList.withIndex()) {
                            if (req == value.noReq)
                                arOriginList[index].injector = s.toString()

                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }
            })

            holder.btnCancel.setOnClickListener(({
                val cancelDialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage(itemList[position].nmHosp + " : " + itemList[position].power + "/"+ itemList[position].specAdd + "제품을 취소하시겠습니까?")
                    .setIcon(R.drawable.wjicon).setTitle("POD FT주문 취소 확인")
                    .setPositiveButton("네") { _, _ ->
                        setCancelOrder(itemList[position].noReq!!, position)
                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
                notifyDataSetChanged()

            }))

            holder.txtJisiDtJisi.setOnClickListener(({
                //val cal = Calendar.getInstance()
                val datePicker = DatePickerDialog(context, { _, year, month, date ->
                    //val seldate = String.format("%d%02d%02d", year, month+1, date)
                    holder.txtJisiDtJisi.text = String.format("%d%02d%02d", year, month+1, date)
                    val req = arPodFTJisiList[holder.adapterPosition].noReq
                    for ((index, value) in arPodFTJisiList.withIndex()) {
                        if (req == value.noReq)
                            arPodFTJisiList[index].dtOrd = String.format("%d%02d%02d", year, month+1, date)
                    }
                    for ((index, value) in arOriginList.withIndex()) {
                        if (req == value.noReq)
                            arOriginList[index].dtOrd = String.format("%d%02d%02d", year, month+1, date)
                    }
                    for ((index, value) in itemList.withIndex()) {
                        if (req == value.noReq)
                            itemList[index].dtOrd = String.format("%d%02d%02d", year, month+1, date)
                    }

                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)
                )
                datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis
                datePicker.show()
            }))

            holder.btnSave.setOnClickListener(({
                val cancelDialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage(itemList[position].nmHosp + " : " + itemList[position].power + "/"+ itemList[position].specAdd + "환자명/비고를 변경하시겠습니까?")
                    .setIcon(R.drawable.wjicon).setTitle("환자명/비고 변경")
                    .setPositiveButton("네") { _, _ ->
                        setSaveRmk(itemList[position].noReq!!, holder.txtJisiPatient.text.toString(), holder.txtJisiRmk.text.toString())
                    }
                    .setNegativeButton("아니오", null)
                    .setCancelable(false)
                    .show()
                notifyDataSetChanged()
            }))
        }

        inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            val txtJisiNmHosp : TextView = itemView.findViewById(R.id.txtJisiNmHosp)
            val txtJisiChk : CheckedTextView = itemView.findViewById(R.id.chkJisi)
            val txtJisiPower : TextView = itemView.findViewById(R.id.txtJisiPower)
            val txtJisiSpecAdd : TextView = itemView.findViewById(R.id.txtJisiSpecAdd)
            val txtjisiTagcnt : TextView = itemView.findViewById(R.id.txtJisiTagcnt)
            val txtJisiQty : TextView = itemView.findViewById(R.id.txtJisiQty)
            val txtJisiSB : TextView = itemView.findViewById(R.id.txtJisiSB)
            val txtJisiInj : EditText = itemView.findViewById(R.id.etxtJisiInj)
            val txtJisiDtSurg : TextView = itemView.findViewById(R.id.txtJisiDtSurg)
            val txtJisiDtOut : TextView = itemView.findViewById(R.id.txtJisiDtOut)
            val txtJisiGbn : TextView = itemView.findViewById(R.id.txtJisiGbn)
            val txtJisiPatient : EditText = itemView.findViewById(R.id.etxtJisiPatient)
            val txtJisiRmk : EditText = itemView.findViewById(R.id.etxtJisiRmk)
            val txtJisiDtJisi : TextView = itemView.findViewById(R.id.txtJisiDtJisi)
            val txtJisiDtCancel : TextView = itemView.findViewById(R.id.txtJisiDtCancel)
            val txtDtReg : TextView = itemView.findViewById(R.id.txtJisiDtReg)
            val btnCancel : Button = itemView.findViewById(R.id.btnJisiCancel)
            val btnSave : ImageButton = itemView.findViewById(R.id.btnJisiSave)

            val trJisiNmHosp : TableRow = itemView.findViewById(R.id.trJisiNmHosp)
            var trJisiUp : TableRow = itemView.findViewById(R.id.trJisiUp)
            var trJisiDown : TableRow = itemView.findViewById(R.id.trJisiDown)

        //    var trJisiTable : TableLayout = itemView.findViewById(R.id.tlPodFTJisi)
        }

        fun setFilter(items : ArrayList<PodFTJisiList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuFax = menu.findItem(R.id.toolbar_cust_select)
        menuFax.isVisible = false

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.app_bar_search -> Toast.makeText(applicationContext, "검색눌럿네", Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            val filterList : ArrayList<PodFTJisiList> = ArrayList()
            var textPower : String?
            var textSpecAdd : String?
            var textRemark : String?
            var texthospRmk : String?
            var textchk : String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until arOriginList.size){
                textRemark = nullChk(arOriginList[idx].dcRmk)
                texthospRmk = nullChk(arOriginList[idx].hospRmk)
                textPower = nullChk(arOriginList[idx].power)
                textSpecAdd = nullChk(arOriginList[idx].specAdd)
                textchk = nullChk(arOriginList[idx].chk)

                if (textRemark.toLowerCase(Locale.ROOT).contains(searchText) || arOriginList[idx].nmHosp!!.toLowerCase(Locale.ROOT).contains(searchText) || texthospRmk.toLowerCase(Locale.ROOT).contains(searchText) ||
                    textPower.toLowerCase(Locale.ROOT).contains(searchText) || textSpecAdd.toLowerCase(Locale.ROOT).contains(searchText) || arOriginList[idx].dtSurg!!.toLowerCase(Locale.ROOT).contains(searchText) ||
                    textchk.toLowerCase(Locale.ROOT).contains(searchText))
                    filterList.add(arOriginList[idx])
            }
            podftJisiAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }
    }

    private fun nullChk(originval:String?) : String {
        return if (originval.isNullOrEmpty()) "" else originval
    }

    private fun myToast(msg:String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

}
