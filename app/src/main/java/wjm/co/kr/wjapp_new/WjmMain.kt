package wjm.co.kr.wjapp_new

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Point
import android.os.*
import android.provider.Settings
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ContentWjmMain2Binding
import java.io.IOException
import java.net.URL


class WjmMain : AppCompatActivity(), AdapterView.OnItemSelectedListener{
    private lateinit var binding: ContentWjmMain2Binding
    private val knames = ArrayList<String>()
    private val snos = ArrayList<String>()
    private var pw = ""
    lateinit var spinadapter : ArrayAdapter<String>

    private var isLogin = false
    private var isFinish = false
    private var finishToast:Toast?=null

    private val dbName = "WJDB2"
    private var db : SQLiteDatabase? = null

    private var cntSave = 0
    private var firstSpin = true
    private var secondSpin = false

    class LoginUser {
        companion object {
            var sno = ""
            var name = ""
            var dept = ""
            var grade = ""
            var ccode = ""
            var publicId = ""
            var publicPw = ""
            var screenWidth = 0

            fun init() {
                sno = ""
                name = ""
                dept = ""
                grade = ""
                ccode = ""
                publicId = ""
                publicPw = ""
                screenWidth = 0
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_wjm_main)
//        setContentView(R.layout.activity_wjm_main2)
        binding = ContentWjmMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //network connecting
        val networkState = NetworkState(this)
        if (!networkState.isConnected())
            myToast( "네트워크에 연결되지 않았습니다.")
//
//
//        Log.i("ANDROID ID", "##### READ Android ID ######")
//
//        try {
//            val SSAID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
//            //Log.i("ANDROID ID", my_android_id)
//            val deviceName = Build.MANUFACTURER + " " + Build.MODEL
////to add to textview
//            //to add to textview
//
//            //Log.i("ANDROID NAME",  deviceName)
//        }
//        catch (e: SecurityException){
//            Log.i("ANDROID ID", "Secure Exception!")
//        }


        finishToast= Toast.makeText(applicationContext, "\'뒤로가기\'를 한번 더 눌러 종료하십시오.", Toast.LENGTH_LONG)
        binding.txtPass.hint = "게시판 비밀번호"
        loginData()

        spinadapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, knames)
        binding.spinner.adapter=spinadapter
        binding.spinner.onItemSelectedListener = this

        binding.btnLogin.setOnClickListener(({
            isLogin = false
            pw = binding.txtPass.text.toString()
            loginOk()
        }))

        db = openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        createTable()

        val mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val constraintSet1 = ConstraintSet()
                constraintSet1.clone(binding.clLogin)

                val constraintSet2 = ConstraintSet()
                constraintSet2.clone(this@WjmMain, R.layout.ani_login)

                TransitionManager.beginDelayedTransition(binding.clLogin)
                constraintSet2.applyTo(binding.clLogin)
            }
        }
        mHandler.sendEmptyMessageDelayed(0, 500)

        val display = getWindowManager().getDefaultDisplay()
        val size = Point()
        display.getSize(size)
        LoginUser.screenWidth = size.x


    }

    // 로그인 할 이름, 사번을 가져와서 리스트에 세팅함
    private fun loginData(){
        val url = URL("http://iclkorea.com/android/WjmMain_list.asp")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback{
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dbListsno = gson.fromJson(body, DBListsno::class.java)

                for (idx in 0 until dbListsno.results.size -1 ) {
                    knames.add(dbListsno.results[idx].kname)
                    snos.add(dbListsno.results[idx].sno)
                }

                runOnUiThread {
                    spinadapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    // 로그인 버튼 누를때  비번 맞는지 확인
    private fun loginOk() {
        if (!loginChk()) {
            runOnUiThread {
                myToast("로그인 정보를 입력해주세요")
            }
        } else {

            val url = URL("http://iclkorea.com/android/WjmMain_login.asp")
            val body = FormBody.Builder().add("sno", LoginUser.sno).add("pw", pw).build()
            val request = Request.Builder().url(url).post(body).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body1 = response.body?.string()
                    //println("Success to execute request! : $body1")
                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dbLoginAC = gson.fromJson(body1, DBLoginAC::class.java)

                    if (dbLoginAC.results.size == 1) {
                        isLogin = true
                        LoginUser.sno = dbLoginAC.results[0].sno
                        LoginUser.name = dbLoginAC.results[0].kname
                        LoginUser.dept = dbLoginAC.results[0].deptCode
                        LoginUser.grade = dbLoginAC.results[0].grade
                        LoginUser.ccode = dbLoginAC.results[0].cCode
                    }

                    runOnUiThread {
                        if (isLogin) {
                            saveUserInfo()
                            secondLogin()
//                            val intent = Intent(baseContext, TopMainActivity::class.java)
//                            startActivity(intent)
//                            overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
                            //화면 이동 스크립트 작성
                        }
                        else
                            myToast(resources.getString(R.string.login_fail))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }

            })
        }
    }

    private fun loginChk() : Boolean {
        if (LoginUser.sno == "default")
            return false

        if (pw == "")
            return false

        return true
    }

    private fun secondLogin() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WjmMain_2nd_login.asp")
        val body = FormBody.Builder().add("uuid", Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)).add("sno", LoginUser.sno).add("name", LoginUser.name).add("deviceName", Build.MANUFACTURER + " " + Build.MODEL).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBSecondLogin = gson.fromJson(body1, DBSecondLogin::class.java)

                if (dBSecondLogin.results == "NEW") {
                    runOnUiThread {
                        myToast(dBSecondLogin.status)
                        loadingDialog.dismiss()
                    }
                } else {
                    val intent = Intent(baseContext, TopMainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }
//    class MyAsyncTask : AsyncTask<String, String, String>() {
//        override fun doInBackground(vararg params: String?): String {
//            val url = URL("http://iclkorea.com/android/WjmMain_list.asp")
//            val request = Request.Builder().url(url).build()
//            val client = OkHttpClient()
//            try {
//                client.newCall(request).enqueue(object : Callback{
//                    override fun onResponse(call: Call?, response: Response?) {
//                        val body = response?.body()?.string()
//                        println("Success to execute request! : $body")
//
//                        //Gson으로 파싱
//                        val gson = Gson()
//                        val list = gson.fromJson(body, DBListsno::class.java)
//                        Log.d("Json Parsing", list.results[1].kname)
//                    }
//
//                    override fun onFailure(call: Call?, e: IOException) {
//                        println("Failed to execute request!")
//                        println(e.message)
//                    }
//                })
//
//            } catch (e : IOException) {
//                Log.d("FetchPostsTask", e.message)
//            }
//           return "test"
//        }
//
//    }

    private fun myToast(msg:String) {
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    // db테이블 생성
    private fun createTable() {
        db!!.execSQL("CREATE TABLE IF NOT EXISTS USER_INFO (SNO VARCHAR, PASSWD VARCHAR, PUBLIC_ID VARCHAR, PUBLIC_PASSWD VARCHAR);")
        db!!.execSQL("CREATE TABLE IF NOT EXISTS READER_INFO (_ID INTEGER PRIMARY KEY AUTOINCREMENT, DT_DAY VARCHAR, CD_CUST VARCHAR, NO_TAG VARCHAR, YN VARCHAR);")
        db!!.execSQL("CREATE TABLE IF NOT EXISTS SCHEDULE_READ (SNO VARCHAR, DT_READ VARCHAR);")
        db!!.execSQL("CREATE TABLE IF NOT EXISTS JOSA_INFO (CD_PLN VARCHAR, NO_TAG VARCHAR)")
    }

    // 로그인성공시 유저 로그인 정보 저장
    private fun saveUserInfo() {
        val cur1 = db!!.rawQuery("select sno, passWD from user_info ;", null)

        if (cur1.count == 0)
            db!!.execSQL("insert into user_info(sno, passWD) values('" + LoginUser.sno + "', '" + binding.txtPass.text.toString() + "');")
        else {
            cur1.moveToNext()
            if (cur1.getString(0) == LoginUser.sno)
                db!!.execSQL("update user_info set sno = '" + LoginUser.sno + "', passWD = '" + binding.txtPass.text.toString() + "';")
            else {
                db!!.execSQL("update user_info set sno = '" + LoginUser.sno + "', passWD = '" + binding.txtPass.text.toString() + "', public_id='', public_passwd='';")
                LoginUser.publicId = ""
                LoginUser.publicPw = ""
            }
        }
        cur1.close()
    }

    // 화면진입시 로그인정보 있으면 화면에 세팅
    private fun setLogin() {
        val cur2 = db!!.rawQuery("select sno, passWD, public_id, public_passwd from user_info ;", null)
        var x = 0

        if (cur2.count > 0) {
            cntSave = cur2.count
            cur2.moveToNext()

            LoginUser.sno = cur2.getString(0)
            pw = cur2.getString(1)
            println(pw)
            if (cur2.getString(2).isNullOrEmpty())
                LoginUser.publicId = ""
            else
                LoginUser.publicId = cur2.getString(2)

            if (cur2.getString(3).isNullOrEmpty())
                LoginUser.publicPw = ""
            else
                LoginUser.publicPw = cur2.getString(3)
            for (i in 0 until snos.size) {
                if (snos[i] == LoginUser.sno) {
                    x = i
                    break
                }
            }
            binding.spinner.setSelection(x)
        }
        cur2.close()

        //아이디 비번세팅이 다되어있으면 자동 로그인
        if (LoginUser.sno != "" && pw != "") {
            isLogin = false
            loginOk()
        }
    }

    data class DBListsno(val results : List<Selsno>)
    data class Selsno (val kname : String, val sno: String)
    data class DBLoginAC(val results : List<MyAC>)
    data class MyAC(val sno: String, val kname : String, val deptCode : String, val cCode : String, val grade : String )
    data class DBSecondLogin(val results : String, val status : String)

    //spinner 선택 이벤트
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if(firstSpin) {
            setLogin()
            firstSpin = false
            if (cntSave == 1) secondSpin = true
        }else if (secondSpin) {
            binding.txtPass.setText(pw)
            secondSpin = false
        }else {
            LoginUser.sno = snos[position]
            binding.txtPass.setText("")
        }
    }

    //spinner 무선택 이벤트
    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

    //백버튼 2번 눌러야 종료되도록
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (!isFinish) {
                    isFinish = true
                    finishToast?.show()
                    return true
                }
            }
            else -> {

            }
        }
        return super.onKeyUp(keyCode, event)
    }

    //앱 터치시 이벤트[종료 취소]
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        finishToast?.cancel()
        isFinish = false
        return super.dispatchTouchEvent(ev)
    }

    //앱 종료시 이벤트
    override fun onDestroy() {
        super.onDestroy()
        finishToast?.cancel()
        isLogin = false
        LoginUser.init()
        db!!.close()
        Process.killProcess(Process.myPid())
    }
}