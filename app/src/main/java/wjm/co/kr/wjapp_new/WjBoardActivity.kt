package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.widget.Toast
import okhttp3.OkHttpClient
import wjm.co.kr.wjapp_new.databinding.ActivityWjBoardBinding
import java.io.*

class WjBoardActivity : AppCompatActivity() {
     private lateinit var bindingA: ActivityWjBoardBinding
    private var selDateFr : String = ""
    private var selDateTo : String = ""
    private var searchView: SearchView? = null
    var originList : ArrayList<BoardList> = ArrayList()
    var arBoardList : ArrayList<BoardList> = ArrayList()
    private var boardListAdapter : BoardListAdapter? = null

    private var attachFilesAdapter : AttachFilesAdapter? = null

    // SQLite DB관련
    private var db: SQLiteDatabase? = null
    private var dbName = "WJDB2"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_board)
        bindingA = ActivityWjBoardBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        wjBoardInit()

        bindingA.bindingBoardContent.btnSearchBoard.setOnClickListener(({
            if (checkDate()) {
                arBoardList.clear()
                originList.clear()
                boardListAdapter!!.notifyDataSetChanged()
                if (!searchView!!.isIconified) {
                    searchView!!.isIconified = true
                    searchView!!.clearFocus()
                    searchView!!.onActionViewCollapsed()
                }
                getBoardList()
            }
        }))

        bindingA.bindingBoardContent.btnWrithBoard.setOnClickListener(({
            val intent = Intent(this, WjBoardWriteActivity::class.java)
            intent.putExtra("noMsg", "NEW")
            startActivityForResult(intent, 0)
            overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
        }))

        bindingA.bindingBoardContent.rvWjBoard.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    private fun wjBoardInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        boardListAdapter = BoardListAdapter(this, arBoardList)
        bindingA.bindingBoardContent.rvWjBoard.layoutManager = layoutManager
        bindingA.bindingBoardContent.rvWjBoard.adapter = boardListAdapter

        val calendarNow = Calendar.getInstance()
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        calendarNow.add(Calendar.DATE, -20)
        selDateFr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        bindingA.bindingBoardContent.etxtDtfrBoard.setText(selDateFr)
        bindingA.bindingBoardContent.etxtDttoBoard.setText(selDateTo)

        db = openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)

        if (checkDate()) {
            getBoardList()
        }
    }

    private fun checkDate() : Boolean {
        val dateFr = bindingA.bindingBoardContent.etxtDtfrBoard.text.toString()
        val dateTo = bindingA.bindingBoardContent.etxtDttoBoard.text.toString()
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

        selDateFr = dateFr
        selDateTo = dateTo
        return true
    }

    private fun getBoardList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJBoardList.asp")
        val body = FormBody.Builder().add("sno", WjmMain.LoginUser.sno).add("frdt", selDateFr).add("todt", selDateTo).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBBoardList = gson.fromJson(body1, DBBoardList::class.java)

                var cnt = 0
                arBoardList.addAll(dBBoardList.results)
                for (idx in 0 until arBoardList.size) {
                    if (idx != 0 && arBoardList[idx].noMsg == arBoardList[idx-1].noMsg && arBoardList[idx].nmTo != arBoardList[idx-1].nmTo)
                        cnt += 1
                    else {
                      //  println("${idx} : ${cnt}")
                        if (cnt > 0) {
                            arBoardList[idx-(cnt+1)].nmTo = arBoardList[idx-cnt].nmTo + "외${cnt+1}명"
                        }
                        cnt = 0
                    }
                }
                originList.addAll(arBoardList)

                runOnUiThread {
                    boardListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()

                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setReadOK(noMsg: String?, gbn: String?, position: Int) {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJBoardConfirm.asp")
        val body = FormBody.Builder().add("sno", WjmMain.LoginUser.sno).add("noMsg", noMsg!!).add("gbn", gbn!!).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBBoardConfirm = gson.fromJson(body1, DBBoardConfirm::class.java)

                if (dBBoardConfirm.results == "OK") {
                    val cal = Calendar.getInstance()
                    val nowDt = SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault()).format(cal.time)
                    arBoardList[position].dtAns = nowDt
                } else {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@WjBoardActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("확인이 실패하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("게시판 확인")
                            .setPositiveButton("확인", null)
                            .setCancelable(false)
                            .show()
                    }
                }

                runOnUiThread {
                    boardListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBBoardList(var results : List<BoardList>)
    data class BoardList(var noMsg:String?, var nmFrom:String?, var nmTo:String?, var snoFrom:String?, var snoTo:String?,
                         var dtTime:String?, var dcMsg:String?, var dtAns:String?, var gbn:String?, var confirm:String?,
                         var nmFile:String?, var locFile:String?)
    data class DBBoardConfirm (var results : String?)
    data class AttachFileList(var nmFile:String?, var locFile:String?)

    inner class BoardListAdapter(private val context: Context, private val itemList:ArrayList<BoardList>) : RecyclerView.Adapter<BoardListAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_board, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val noMsg = itemList[position].noMsg
            var nmTo = itemList[position].nmTo
            val gbn = itemList[position].gbn
            val arFileList : ArrayList<AttachFileList> = ArrayList()

            attachFilesAdapter = AttachFilesAdapter(context, arFileList)
            holder.rvAttachFiles.adapter = attachFilesAdapter

            if (position == 0 || noMsg != itemList[position-1].noMsg) {

                holder.lltemp.visibility = View.VISIBLE
                holder.txttemp.visibility = View.VISIBLE
                holder.dcMsg.visibility = View.VISIBLE

                holder.nmFrom.text = itemList[position].nmFrom

                if (gbn == "공지")
                    nmTo = "전체(공지)"
                else if (nmTo == null)
                    nmTo = "전체"
                holder.nmTo.text = nmTo

                holder.dtDate.text = itemList[position].dtTime

                if (nmTo == "전체" || itemList[position].dtAns != null) {
                    holder.dtAns.visibility = View.VISIBLE
                    holder.btnConfirm.visibility = View.GONE
                } else if (WjmMain.LoginUser.sno == itemList[position].snoTo) {
                    holder.dtAns.visibility = View.GONE
                    holder.btnConfirm.visibility = View.VISIBLE
                } else {
                    holder.dtAns.visibility = View.VISIBLE
                    holder.btnConfirm.visibility = View.GONE
                }

                if (gbn == "공지" && itemList[position].confirm.isNullOrEmpty()) {
                    holder.dtAns.visibility = View.GONE
                    holder.btnConfirm.visibility = View.VISIBLE
                } else if (gbn == "공지" && itemList[position].confirm!!.isNotEmpty()){
                    holder.dtAns.visibility = View.GONE
                    holder.btnConfirm.visibility = View.INVISIBLE
                }

                holder.dtAns.text = itemList[position].dtAns
                holder.dcMsg.text = itemList[position].dcMsg

                for (idx in 0 until itemList.size)
                    if (itemList[idx].noMsg == noMsg && !itemList[idx].nmFile.isNullOrEmpty())
                        arFileList.add(AttachFileList(itemList[idx].nmFile, itemList[idx].locFile))

                if (arFileList.size > 0)
                    holder.llAttchFiles.visibility = View.VISIBLE
                else
                    holder.llAttchFiles.visibility = View.GONE
            } else {
                holder.llAttchFiles.visibility = View.GONE
                holder.lltemp.visibility = View.GONE
                holder.txttemp.visibility = View.GONE
                holder.dcMsg.visibility = View.GONE
            }

            holder.btnConfirm.setOnClickListener(({
                val gbn2 = if (gbn == "공지") "Notice" else "Person"
                setReadOK(itemList[position].noMsg, gbn2, position)
                if (gbn2 == "Notice")
                    holder.btnConfirm.visibility = View.GONE
            }))

            holder.dcMsg.tag = holder.dcMsg.text
            holder.dcMsg.setOnLongClickListener(onLongClick)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nmFrom : TextView = itemView.findViewById(R.id.txtNmFrom)
            val nmTo : TextView = itemView.findViewById(R.id.txtNmTo)
            val dtDate : TextView = itemView.findViewById(R.id.txtDtDate)
            val dtAns : TextView = itemView.findViewById(R.id.txtDtAns)
            val dcMsg : TextView = itemView.findViewById(R.id.txtMsg)
            val btnConfirm : Button = itemView.findViewById(R.id.btn_confirm_board)
            val rvAttachFiles : RecyclerView = itemView.findViewById(R.id.rvAttatchFiles)
            val llAttchFiles : LinearLayout = itemView.findViewById(R.id.llAtachFiles)
            val lltemp : LinearLayout= itemView.findViewById(R.id.linearLayout27)
            val txttemp : TextView = itemView.findViewById(R.id.textView113)

            init {
                val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                rvAttachFiles.layoutManager = layoutManager
            }
        }

        fun setFilter(items : ArrayList<BoardList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }
    }

    private val onLongClick = View.OnLongClickListener { v->
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("라벨", v.tag.toString())
        clipboardManager.setPrimaryClip(clipData)
       // clipboardManager.primaryClip = clipData

        Toast.makeText(applicationContext, "복사완료", Toast.LENGTH_LONG).show()
        true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem1 = menu.findItem(R.id.toolbar_cust_select)
        searchItem1.isVisible = false
        val searchItem2 = menu.findItem(R.id.toolbar_publicAccount)
        searchItem2.isVisible = true

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
            R.id.app_bar_search -> Toast.makeText(applicationContext, "검색눌럿네", Toast.LENGTH_LONG).show()
            R.id.toolbar_publicAccount -> setPublicAccount()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setPublicAccount() {
       cDialogBoard()
    }

    private fun cDialogBoard() {
        val layout = layoutInflater
        val nullParent : ViewGroup? = null
        val view = layout.inflate(R.layout.cdialog_board_layout, nullParent)
        val builder = AlertDialog.Builder(this)

        builder.setView(view)

        val btnCancle: Button = view.findViewById(R.id.btnBoardPublicCancle)
        val btnSave: Button = view.findViewById(R.id.btnBoardPublicSave)
        val publicID: TextView = view.findViewById(R.id.etxtPublicID)
        val publicPW: TextView = view.findViewById(R.id.etxtPublicPW)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.show()

        if (WjmMain.LoginUser.publicId.isNotEmpty()) {
            publicID.text = WjmMain.LoginUser.publicId
            publicPW.text = WjmMain.LoginUser.publicPw
        }

        btnCancle.setOnClickListener(({
            dialog.dismiss()
        }))

        btnSave.setOnClickListener(({
            if (publicID.text.isNullOrEmpty() || publicPW.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "아이디 또는 비밀번호를 입력하세요", Toast.LENGTH_LONG).show()
            } else {
                val cancelDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                cancelDialog.setMessage("공용 서버 ID/PW를 저장하시곗습니까?")
                    .setIcon(R.drawable.wjicon).setTitle("공용 계정 저장")
                    .setPositiveButton("확인") {_,_ ->
                        WjmMain.LoginUser.publicId = publicID.text.toString()
                        WjmMain.LoginUser.publicPw = publicPW.text.toString()
                        setDBPublicAccount(publicID.text.toString(), publicPW.text.toString())

                        dialog.dismiss()
                        Toast.makeText(baseContext, "저장하였습니다.", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("취소", null)
                    .setCancelable(false)
                    .show()

            }
        }))
    }

    private fun setDBPublicAccount(publicID: String, publicPW: String) {
        db!!.execSQL("update user_info set PUBLIC_ID = '${publicID}', PUBLIC_PASSWD = '${publicPW}';")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == 1) {
            bindingA.bindingBoardContent.btnSearchBoard.performClick()
        }
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String): Boolean {
            val filterList : ArrayList<BoardList> = ArrayList()
            var textdcMsg : String?
            var textnmFrom : String?
            var textnmTo : String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until originList.size){
                textdcMsg = nullChk(originList[idx].dcMsg)
                textnmFrom = nullChk(originList[idx].nmFrom)
                textnmTo = nullChk(originList[idx].nmTo)
                if (textdcMsg.toLowerCase(Locale.ROOT).contains(searchText) ||
                    textnmFrom.toLowerCase(Locale.ROOT).contains(searchText) || textnmTo.toLowerCase(
                        Locale.ROOT
                    ).contains(searchText))
                    filterList.add(originList[idx])
            }
            boardListAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }

        private fun nullChk(originval:String?) : String {
            return if (originval.isNullOrEmpty()) "" else originval
        }
    }

    inner class AttachFilesAdapter(private val context: Context, private val itemList:ArrayList<AttachFileList>) : RecyclerView.Adapter<AttachFilesAdapter.ViewHolder> () {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_board_files, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txtNmFile.text = itemList[position].nmFile
            holder.btnOpenFile.setOnClickListener(({
                if (WjmMain.LoginUser.publicId.isEmpty()) {
                    setPublicAccount()
                    Toast.makeText(context, "공용 접속계정 설정 후 다시 열어주세요", Toast.LENGTH_LONG).show()
                } else {
                    val down = DownloadHelper
                    down.downloadFile(
                        itemList[position].locFile!!,
                        null,
                        this@WjBoardActivity,
                        itemList[position].nmFile!!,
                        WjmMain.LoginUser.publicId,
                        WjmMain.LoginUser.publicPw
                    )
                }
            }))
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtNmFile : TextView = itemView.findViewById(R.id.txtNmFileBoard)
            val btnOpenFile : Button = itemView.findViewById(R.id.btnOpenFileBoard)
        }
    }
}
