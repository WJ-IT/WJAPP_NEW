package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityJegoInJosaBinding
import wjm.co.kr.wjapp_new.databinding.ActivityJegoJosaBinding
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

class JegoInJosaActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityJegoInJosaBinding
    private var nmloc = ""
    private lateinit var mTimeHandler : Handler
    private var batteryCount = 10
    private var tagBefSize = 0
    private var readCnt = 0

    //위치 리스트
    private var inJosaLocList : MutableList<String> = ArrayList()

    private lateinit var spinAdapter : ArrayAdapter<String>

    //리스트 어뎁터와 리스트
    private var inJosaAdapter: InJosaAdapter? = null
    private var inJosalist : ArrayList<InJosaList> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingA = ActivityJegoInJosaBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        jegoInJosaInit()

        bindingA.bindingJInjosa.btnInJosaReaderInit.setOnClickListener(({
            if(InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
                InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
            }
        }))

        bindingA.bindingJInjosa.btnInJosaSendResult.setOnClickListener(({
            AlertDialog.Builder(this)
                .setTitle("결과전송")
                .setMessage("스캔한 데이타를 전송하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("확인") { _, _ ->
                    sendTagReadingResult(makeSendTagList())
                }
                .setNegativeButton("취소", null).show()
        }))
    }

    private fun jegoInJosaInit() {
        // 리딩결과 값을 받을 화면을 알려준다
        InventoryMenuActivity.reading_loc.topPage = "JEGOINJOSA"
        spinAdapter = ArrayAdapter(this, R.layout.spinnerlayout)
        getLocation(this)
        bindingA.bindingJInjosa.spinInJosaLoc.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                nmloc = bindingA.bindingJInjosa.spinInJosaLoc.selectedItem.toString()
                // 위치가 바뀌면 리스트 재조회
                inJosalist.clear()
                tagBefSize = 0
                inJosaAdapter!!.notifyDataSetChanged()
                getInJosaList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // product list setting
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bindingA.bindingJInjosa.rvInjosalist.layoutManager = layoutManager
        inJosaAdapter = InJosaAdapter(this,inJosalist)
        bindingA.bindingJInjosa.rvInjosalist.adapter = inJosaAdapter

        //리더기가 연결되어 있으면 타이머 시작
        if (swingConnChk()) {
            mTimeHandler = object : Handler(Looper.getMainLooper()) { // 3초 간격 무한 실행
                override fun handleMessage(msg: Message) {
                    setReadTagList()
                    if (InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true) && batteryCount == 20) { //프리즈마 리더기일경우 배터리 상태는 1분마다 체크
                        batteryCount = 0
                        InventoryMenuActivity.SwingInfo.mSwing!!.prisma_getBattery()
                        setBatteryImg(ReadingInJosaRFID.batteryStatus)
                    }
                    batteryCount ++
                    this.sendEmptyMessageDelayed(0, 3000)
                }
            }
            mTimeHandler.sendEmptyMessage(0)
        }

        //프리즈마면 화면 집입시 초기화
        if(InventoryMenuActivity.SwingInfo.swing_NAME.contains("RFPrisma", true)) {
            InventoryMenuActivity.SwingInfo.mSwing!!.prisma_tagDel()
        }

        //최초진입시 조회
        getInJosaList()

    }

    private fun makeSendTagList() : String {
        var  sendTags = ""
        for (idx in inJosalist)
            if (idx.chk == "Y")
                sendTags = sendTags + "'${idx.noRfid}',"

        sendTags = sendTags.substring(0, sendTags.length-1)

        return sendTags
    }

    private fun setBatteryImg(status: String) {
        when (status) {
            "0" -> bindingA.bindingJInjosa.imgInJosaBattery.setImageResource(R.drawable.battery00)
            "1" -> bindingA.bindingJInjosa.imgInJosaBattery.setImageResource(R.drawable.battery20)
            "2" -> bindingA.bindingJInjosa.imgInJosaBattery.setImageResource(R.drawable.battery40)
            "3" -> bindingA.bindingJInjosa.imgInJosaBattery.setImageResource(R.drawable.battery60)
            "4" -> bindingA.bindingJInjosa.imgInJosaBattery.setImageResource(R.drawable.battery80)
        }
    }

    private fun swingConnChk() : Boolean {
        //블루투스 장비 연결 체크
        if (InventoryMenuActivity.SwingInfo.swing_ADDRESS.equals("")) {
            Toast.makeText(this, "리더기를 연결해주세요", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun setReadTagList() {
        val tagNowSize = ReadingInJosaRFID.mTagItem.size
        if (inJosalist.size > 0) {
            for (i in tagBefSize until tagNowSize) {
                for (j in 0 until inJosalist.size) {
                    if (inJosalist[j].noRfid == ReadingInJosaRFID.mTagItem[i].epcID_Ascii) {
                        inJosalist[j].chk = "Y"
                        readCnt ++
                    }
                }
            }

            tagBefSize = tagNowSize
            bindingA.bindingJInjosa.txtInJosaReadCnt.text = readCnt.toString()
            inJosaAdapter!!.notifyDataSetChanged()
        }
    }

    private fun getLocation(context: Context) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoInJosa_LocList.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBInJosaLocation = gson.fromJson(body1, DBInJosaLocation::class.java)

                for (idx in dBInJosaLocation.results.indices) {
                    inJosaLocList.add(dBInJosaLocation.results[idx].nmloc.toString())
                }

                runOnUiThread { //UI에 알려줌
                    //location setting
                    spinAdapter = ArrayAdapter(context, R.layout.spinnerlayout, inJosaLocList)
                    bindingA.bindingJInjosa.spinInJosaLoc.adapter = spinAdapter
                    bindingA.bindingJInjosa.spinInJosaLoc.setSelection(0)
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getInJosaList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoInJosa_List.asp")
        val body = FormBody.Builder().add("nmloc", nmloc).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBInJosa = gson.fromJson(body1, DBInJosa::class.java)

                inJosalist.addAll(dBInJosa.results)

                runOnUiThread { //UI에 알려줌
                    inJosaAdapter!!.notifyDataSetChanged()
                    readCnt = 0
                    bindingA.bindingJInjosa.txtInJosaReadCnt.text = "0"
                    bindingA.bindingJInjosa.txtInJosaTotCnt.text = inJosalist.size.toString()
                    loadingDialog.dismiss()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun sendTagReadingResult(tagLists: String) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/JegoInJosa_TagResultSend.asp")
        println("josa result : $tagLists")
        val body = FormBody.Builder().add("tags", tagLists).build()
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
                    val dBSendTagList = gson.fromJson(body1, DBSendTagList::class.java)

                    if (dBSendTagList.results == "OK") {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(baseContext,"전송이 완료되엇습니다.", Toast.LENGTH_LONG).show()
                            loadingDialog.dismiss()
                        }
                    }
                    else {
                        runOnUiThread { //UI에 알려줌
                            Toast.makeText(baseContext,"전송이 실패되엇습니다.("+dBSendTagList.status+")", Toast.LENGTH_LONG).show()
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

    data class DBInJosaLocation(val results: List<InJosaLocList>)
    data class InJosaLocList(var nmloc: String?)
    data class DBInJosa(val results: List<InJosaList>)
    data class InJosaList(var nmRes:String?, var noRfid:String, var nmLoc:String?, var mng:String?, var spec:String?, var chk:String?)
    data class DBSendTagList(val results:String, val status:String)

    inner class InJosaAdapter(private val context: Context, private val itemList: ArrayList<InJosaList>) : RecyclerView.Adapter<InJosaAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // context 와 parent.getContext() 는 같다.
            val view = LayoutInflater.from(context).inflate(R.layout.row_in_josa, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txtNmRes.text = itemList[position].nmRes
            holder.txtSpec.text = itemList[position].spec
            holder.txtLoc.text = itemList[position].nmLoc
            holder.txtMng.text = itemList[position].mng

            if (itemList[position].chk == "N")
                holder.txtChk.visibility = View.INVISIBLE
            else
                holder.txtChk.visibility = View.VISIBLE
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var txtNmRes : TextView = itemView.findViewById(R.id.txt_in_josa_nmRes)
            var txtSpec : TextView = itemView.findViewById(R.id.txt_in_josa_Spec)
            var txtLoc : TextView = itemView.findViewById(R.id.txt_in_josa_Loc)
            var txtMng : TextView = itemView.findViewById(R.id.txt_in_josa_mng)
            var txtChk : TextView = itemView.findViewById(R.id.txt_in_josa_chk)
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

    class ReadingInJosaRFID {
        companion object {
            var mTagItem: ArrayList<TagItem> = ArrayList()//스윙기로 읽은 데이타를 담을 그릇
            var batteryStatus : String = ""

            fun showDataJepum(Item: TagItem) {
                val pos = getPosition(Item)
                if (pos < 0) {              //읽은 태그가 중복이 아니면 변수 등록
                    mTagItem.add(Item)
                }
            }

            //검색된것의 중복체크를 위한 함수 START : -1이면 신규 아니면 읽은것
            private fun getPosition(item: TagItem): Int {
                var position = -1

                var titem: TagItem?
                var oldID: String?

                for (i in 0 until mTagItem.size) {
                    titem = mTagItem[i]
                    oldID = titem.getEpcID()
                    if (oldID == item.getEpcID()) {
                        position = i
                        break
                    }
                    titem = null
                }
                return position
            }
            //검색된것의 중복체크를 위한 함수 END

            fun setBattery(status:String) {
                batteryStatus = status
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadingInJosaRFID.mTagItem.clear()
        if (swingConnChk())
            mTimeHandler.removeMessages(0)
    }
}