package wjm.co.kr.wjapp_new

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ContentTopMainBinding
import java.io.IOException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TopMainActivity : AppCompatActivity() {
    private lateinit var binding: ContentTopMainBinding
    private var jaegocdialogadapter : CDialogAdapter? = null
    private var cdialogList : ArrayList<ScheduleCDialog> = ArrayList()
    private var etxtSchedule : TextView? = null
    private var cdSD = ""
    private val dbName = "WJDB2"
    private var db : SQLiteDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_top_main)
        binding = ContentTopMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)

        binding.clJaego.setOnClickListener(({
            val intent = Intent(baseContext, InventoryMenuActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
        }))

        binding.clCompany.setOnClickListener(({
            if (WjmMain.LoginUser.grade == "대리점") {
                Toast.makeText(this, "추후 오픈합니다.", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(baseContext, CompanyMenuActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
            }
        }))

        binding.txtCompanySchedule.setOnClickListener(({
            cDialogBoard()
            getCompanySchedules()
        }))

        binding.txtLogout.setOnClickListener(({
            db!!.execSQL("update user_info set passWD = '' where sno = '" + WjmMain.LoginUser.sno + "';")
            //finish()
            val intent = Intent(baseContext, WjmMain::class.java)
            startActivity(intent)
        }))

        binding.txtLoginName.text =  WjmMain.LoginUser.name

        if (dbgetReadYN())
        else{
            cDialogBoard()
            getCompanySchedules()
        }

        if (WjmMain.LoginUser.screenWidth >2000) {
            binding.txtTM01.textSize = 34f
            binding.txtTM02.textSize = 34f
            binding.txtTM03.textSize = 34f
            binding.txtTM04.textSize = 34f
            binding.txtTM05.textSize = 34f
        } else {
            binding.txtTM01.textSize = 20f
            binding.txtTM02.textSize = 20f
            binding.txtTM03.textSize = 20f
            binding.txtTM04.textSize = 20f
            binding.txtTM05.textSize = 20f
        }

    }

    private fun getCompanySchedules() {
        cdialogList.clear()
        val url = URL("http://iclkorea.com/android/WJCScheduleList.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBCSchedule = gson.fromJson(body1, DBCSchedule::class.java)

                cdialogList.addAll(dBCSchedule.results)
                runOnUiThread {
                    jaegocdialogadapter!!.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setSaveSchedule(frDt:String, toDt:String, title:String, content:String, view:View) {
        val url = URL("http://iclkorea.com/android/WJCScheduleWrite.asp")
        val body = FormBody.Builder().add("sno", WjmMain.LoginUser.sno).add("frDt", frDt).add("toDt", toDt).add("title", title).add("content", content).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBCScheduleWrite = gson.fromJson(body1, DBCScheduleWrite::class.java)

                if (dBCScheduleWrite.results == "OK") {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@TopMainActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("신규 회사 일정을 등록하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("회사일정 등록")
                            .setPositiveButton("확인") {_,_ ->

                                setResult(1)
                                val txtSchedule : TextView = view.findViewById(R.id.etxtSchedule)
                                val btnCTWrite : Button = view.findViewById(R.id.btnCTWrite)
                                val btnCTSave : Button = view.findViewById(R.id.btnCTSave)
                                val llDateSelect : LinearLayout = view.findViewById(R.id.llDateSelect)
                                val llTitle : LinearLayout = view.findViewById(R.id.llTitle)
                                val rvCDialog : RecyclerView = view.findViewById(R.id.rvSchedule)

                                btnCTSave.text = "삭제"
                                txtSchedule.setText("")
                                txtSchedule.setHint("위의 일정을 누르면 세부내용이 나타납니다")
                                txtSchedule.isEnabled = false
                                txtSchedule.isFocusableInTouchMode = false
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(txtSchedule.windowToken, 0)
                                btnCTWrite.setText("작성")
                                llDateSelect.visibility = View.GONE
                                llTitle.visibility = View.GONE
                                rvCDialog.visibility = View.VISIBLE
                                getCompanySchedules()
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@TopMainActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("일정등록을 실패하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("회사일정 작성")
                            .setPositiveButton("확인", null)
                            .setCancelable(false)
                            .show()
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setDelete(noSch: String, view: View) {
        val url = URL("http://iclkorea.com/android/WJCScheduleDelete.asp")
        val body = FormBody.Builder().add("noSch", noSch).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBCScheduleWrite = gson.fromJson(body1, DBCScheduleWrite::class.java)

                if (dBCScheduleWrite.results == "OK") {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@TopMainActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("선택한 회사 일정을 삭제하였습니다1")
                            .setIcon(R.drawable.wjicon).setTitle("회사일정 삭제")
                            .setPositiveButton("확인") {_,_ ->

                                setResult(1)
                                val txtSchedule : TextView = view.findViewById(R.id.etxtSchedule)
                                txtSchedule.setText("")
                                txtSchedule.setHint("위의 일정을 누르면 세부내용이 나타납니다")
                                getCompanySchedules()
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@TopMainActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("선택한 회사 일정 삭제를 실패하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("회사일정 삭제")
                            .setPositiveButton("확인", null)
                            .setCancelable(false)
                            .show()
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBCSchedule(var results: List<ScheduleCDialog>)
    data class ScheduleCDialog(var noSch:String?, var frDate:String?, var toDate:String?, var title:String?, var remark:String?)
    data class DBCScheduleWrite(var results: String?)

    private fun cDialogBoard() {
        jaegocdialogadapter = null
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.cdialog_company_timetable, nullParent)
        val builder = AlertDialog.Builder(this)

        builder.setView(view)

        val rvCDialog : RecyclerView = view.findViewById(R.id.rvSchedule)
        val btnClose: ImageView = view.findViewById(R.id.imgClose)
        val txtSchedule : TextView = view.findViewById(R.id.etxtSchedule)
        val txtTitle : TextView = view.findViewById(R.id.etxtTitle)
        val txtFrDt : TextView = view.findViewById(R.id.etxtFrDt)
        val txtToDt : TextView = view.findViewById(R.id.etxtToDt)
        etxtSchedule = view.findViewById(R.id.etxtSchedule)

        // date init setting
        val calendarNow = Calendar.getInstance()
        txtFrDt.setText(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time))
        txtToDt.setText(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time))

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        btnClose.setOnClickListener(({
            dbsaveReadInfo()
            dialog.dismiss()
        }))

        val btnCTWrite : Button = view.findViewById(R.id.btnCTWrite)
        val btnCTSave : Button = view.findViewById(R.id.btnCTSave)
        val llDateSelect : LinearLayout = view.findViewById(R.id.llDateSelect)
        val llTitle : LinearLayout = view.findViewById(R.id.llTitle)

        llDateSelect.visibility = View.GONE
        if (WjmMain.LoginUser.name == "박상용" ||WjmMain.LoginUser.name == "조원철" ||WjmMain.LoginUser.name == "최인범" ||WjmMain.LoginUser.name == "최인영" ||WjmMain.LoginUser.name == "장성훈" ||WjmMain.LoginUser.name == "강형선" ||WjmMain.LoginUser.name == "최한아" ||WjmMain.LoginUser.name == "김성대" ||WjmMain.LoginUser.name == "김동관" ) {
            btnCTSave.visibility = View.VISIBLE
            btnCTWrite.visibility = View.VISIBLE
        } else {
            btnCTSave.visibility = View.GONE
            btnCTWrite.visibility = View.GONE
        }

        btnCTWrite.setOnClickListener(({
            if (btnCTWrite.text == "작성") {
                btnCTWrite.text = "취소"
                btnCTSave.text = "저장"
                llDateSelect.visibility = View.VISIBLE
                llTitle.visibility = View.VISIBLE
                rvCDialog.visibility = View.GONE
                txtTitle.text = ""
                txtSchedule.text = ""
                txtSchedule.hint = "새로운 일정을 입력해주세요"
                txtSchedule.isEnabled = true
                txtSchedule.isFocusableInTouchMode = true
            } else {
                btnCTSave.text = "삭제"
                txtSchedule.text = ""
                txtSchedule.hint = "위의 일정을 누르면 세부내용이 나타납니다"
                txtSchedule.isEnabled = false
                txtSchedule.isFocusableInTouchMode = false
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(txtSchedule.windowToken, 0)
                btnCTWrite.text = "작성"
                llDateSelect.visibility = View.GONE
                llTitle.visibility = View.GONE
                rvCDialog.visibility = View.VISIBLE
            }
        }))

        btnCTSave.setOnClickListener(({
            if (btnCTSave.text == "저장") {
                if (checkDate(txtFrDt.text.toString(), txtToDt.text.toString())) {
                    if (txtTitle.text != "" && txtSchedule.text != "")
                        setSaveSchedule(
                            txtFrDt.text.toString(),
                            txtToDt.text.toString(),
                            txtTitle.text.toString(),
                            txtSchedule.text.toString(), view
                        )

                }
            } else {
                if(cdSD != "") {
                    val cancelDialog = AlertDialog.Builder(
                        this@TopMainActivity,
                        android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
                    )
                    cancelDialog.setMessage("선택한 회사 일정을 삭제하시겠습니까?")
                        .setIcon(R.drawable.wjicon).setTitle("회사일정 삭제")
                        .setPositiveButton("확인") { _, _ ->
                            setDelete(cdSD, view)
                        }
                        .setNegativeButton("취소") { _, _ ->

                        }
                        .show()
                }
            }
        }))

        val jaegoListManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvCDialog.layoutManager = jaegoListManager
        // custom dialog lsit view Setting
        jaegocdialogadapter = CDialogAdapter(this, cdialogList)
        rvCDialog.adapter = jaegocdialogadapter
    }

    inner class CDialogAdapter(private var context: Context, private var itemList:ArrayList<ScheduleCDialog>) : RecyclerView.Adapter<CDialogAdapter.ViewHolder>(){

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_top_menu_schedule, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position and 1 == 0)
                holder.clayout.setBackgroundResource(R.drawable.list_item_2)
            else
                holder.clayout.setBackgroundResource(R.drawable.list_item_3)

            holder.txtDays.text = "${itemList[position].frDate} ~ ${itemList[position].toDate}"
            holder.txtSchedule.text = itemList[position].title

            holder.clayout.setOnClickListener(({
                etxtSchedule!!.setText(itemList[position].remark)
                cdSD = itemList[position].noSch!!
            }))
        }


        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val txtDays : TextView = itemView.findViewById(R.id.txtDays)
            val txtSchedule : TextView = itemView.findViewById(R.id.txtSchedule)
            val clayout : LinearLayout = itemView.findViewById(R.id.llRowCompanySchedule)
        }
    }

    private fun checkDate(frDt : String, toDt : String) : Boolean {
        val dateFr =frDt
        val dateTo = toDt
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

        return true
    }

    private fun dbgetReadYN() : Boolean {
        val cur1 = db!!.rawQuery("select DT_READ from SCHEDULE_READ where sno = '" + WjmMain.LoginUser.sno + "' ;", null)
        if (cur1.count == 0) {
            cur1.close()
            return false
        } else {
            cur1.moveToNext()
            val date = cur1.getString(0)
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            sdf.isLenient = false
            val now = Calendar.getInstance()

            cur1.close()
            return (date == sdf.format(now.time))
        }
    }

    private fun dbsaveReadInfo() {
        val cur1 = db!!.rawQuery("select DT_READ from SCHEDULE_READ where sno = '" + WjmMain.LoginUser.sno + "' ;", null)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.isLenient = false
        val now = Calendar.getInstance()

        if (cur1.count == 0)
            db!!.execSQL("insert into SCHEDULE_READ(sno, DT_READ) values('" + WjmMain.LoginUser.sno + "', '" + sdf.format(now.time) + "');")
        else {
            cur1.moveToNext()
            db!!.execSQL("update SCHEDULE_READ set DT_READ = '" + sdf.format(now.time) + "' where sno = '" + WjmMain.LoginUser.sno + "';")
        }
        cur1.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        db!!.close()
    }
}
