package wjm.co.kr.wjapp_new

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.view.*
import android.widget.*
import android.widget.SearchView
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjCustJournalBinding
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class WjCustJournalActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjCustJournalBinding
    private var cdTeam = ""
    private var arKname : ArrayList<String> = ArrayList()
    private var selDateFr = ""
    private var selDateTo = ""
    private var selWriter = ""
    private var writerList : ArrayList<WriterList> = ArrayList()
    private var arJournalList : ArrayList<JournalList> = ArrayList()
    private var originList : ArrayList<JournalList> = ArrayList()
    private var journalListAdapter :JournalListAdapter? = null
    private var searchView: SearchView? = null
    private val snapHelper = MyLinearSnapHelper() //PagerSnapHelper()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_cust_journal)
        bindingA = ActivityWjCustJournalBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

       custJournalInit()

        bindingA.bindingCustJournal.btnSearchJournalSearch.setOnClickListener(({
            arJournalList.clear()
            originList.clear()
            getJournalList()
        }))

        bindingA.bindingCustJournal.btnJournalRight.setOnClickListener(({
            if (snapHelper.getSnappedPosition() < arJournalList.size)
                bindingA.bindingCustJournal.rvJournalList.smoothScrollToPosition(snapHelper.getSnappedPosition()+1)

        }))
        bindingA.bindingCustJournal.btnJournalLeft.setOnClickListener(({
            if (snapHelper.getSnappedPosition() != 0)
                bindingA.bindingCustJournal.rvJournalList.smoothScrollToPosition(snapHelper.getSnappedPosition()-1)
        }))

        bindingA.bindingCustJournal.rvJournalList.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v!!.performClick()
                    searchView!!.clearFocus()
                }
            }
            v?.onTouchEvent(event) ?: true
        }

    }

    private fun custJournalInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        layoutManager.isSmoothScrollbarEnabled
        journalListAdapter = JournalListAdapter(this, arJournalList)
        bindingA.bindingCustJournal.rvJournalList.layoutManager = layoutManager
        bindingA.bindingCustJournal.rvJournalList.adapter = journalListAdapter

        snapHelper.attachToRecyclerView(bindingA.bindingCustJournal.rvJournalList)

        //rvJournalList.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING)

        // date init setting
        val calendarNow = Calendar.getInstance()
        selDateTo = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        calendarNow.add(Calendar.DATE, -4)
        selDateFr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarNow.time)
        bindingA.bindingCustJournal.etxtSearchJournalFrdt.setText(selDateFr)
        bindingA.bindingCustJournal.etxtSearchJournalTodt.setText(selDateTo)

        //team spin setting
        val teamSpinAdapter : ArrayAdapter<String> = ArrayAdapter(this, R.layout.spinnerlayout, resources.getStringArray(R.array.team_list))
        bindingA.bindingCustJournal.spinSearchJournalTeam.adapter = teamSpinAdapter
        bindingA.bindingCustJournal.spinSearchJournalTeam.setSelection(0)
        bindingA.bindingCustJournal.spinSearchJournalTeam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position) {
                    0 -> cdTeam = "%"
                    1, 2, 3, 4 -> cdTeam = resources.getStringArray(R.array.team_list)[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        //writer list setting
        // 1. db 가져오기 2. spinner stting
        getWriterList()
        getJournalList()

    }

    private fun getWriterList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJJournal_writer_list.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                //println("Success to execute request! : $body")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBWriterList = gson.fromJson(body1, DBWriterList::class.java)

                for (idx in dBWriterList.results.indices) {
                    writerList.add(
                        WriterList(
                            dBWriterList.results[idx].sno,
                            dBWriterList.results[idx].kname
                        )
                    )
                    arKname.add(dBWriterList.results[idx].kname!!)
                }
                runOnUiThread { //UI에 알려줌
                    loadingDialog.dismiss()
                    val writerSpinAdapter : ArrayAdapter<String> = ArrayAdapter(baseContext, R.layout.spinnerlayout, arKname)
                    bindingA.bindingCustJournal.spinSearchJournalWriter.adapter = writerSpinAdapter
                    bindingA.bindingCustJournal.spinSearchJournalWriter.setSelection(0)
                    bindingA.bindingCustJournal.spinSearchJournalWriter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selWriter = writerList[position].sno!!
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun getJournalList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJJournal_list.asp")

        val body = FormBody.Builder().add("cCode", WjmMain.LoginUser.ccode ).add("frDt", bindingA.bindingCustJournal.etxtSearchJournalFrdt.text.toString() ).add("toDt", bindingA.bindingCustJournal.etxtSearchJournalTodt.text.toString() ).add("sno", WjmMain.LoginUser.sno ).add("writer", selWriter ).add("nmCust", bindingA.bindingCustJournal.etxtSearchJournalCust.text.toString() ).add("team",cdTeam ).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBJournalList = gson.fromJson(body1, DBJournalList::class.java)

                for (idx in dBJournalList.results.indices) {
                    arJournalList.add(
                        JournalList(
                            dBJournalList.results[idx].sortN,
                            dBJournalList.results[idx].dtVisit,
                            dBJournalList.results[idx].cdCust,
                            dBJournalList.results[idx].dcDoctor,
                            dBJournalList.results[idx].sno,
                            dBJournalList.results[idx].kname,
                            dBJournalList.results[idx].nmCust,
                            dBJournalList.results[idx].dcContent,
                            dBJournalList.results[idx].remark
                        )
                    )
                }
                originList.addAll(arJournalList)

                runOnUiThread { //UI에 알려줌
                    journalListAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                   // txtCount.text = "1 / ${arJournalList.size}"
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBWriterList (var results:List<WriterList>)
    data class WriterList (var sno:String?, var kname:String?)
    data class DBJournalList (var results : List<JournalList>)
    data class JournalList (var sortN:String?, var dtVisit:String?, var cdCust:String?, var dcDoctor:String?, var sno:String?, var kname:String?, var nmCust:String?, var dcContent:String?, var remark:String?)

    inner class JournalListAdapter(private val context: Context, private val itemList:ArrayList<JournalList>) : RecyclerView.Adapter<JournalListAdapter.ViewHolder>(){
        override fun getItemCount(): Int {
            //txtCount.text = "1 / ${itemList.size}"
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_cust_journal, parent, false)

            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txtNmCust.text = itemList[position].nmCust
            holder.txtDtVisit.text = itemList[position].dtVisit
            holder.txtVisiter.text = itemList[position].kname
            holder.txtRemark.text = itemList[position].remark
            holder.txtDoctor.text = itemList[position].dcDoctor
            holder.etxtContent.setText(itemList[position].dcContent)
            holder.txtRowNum.text = "${itemList[position].sortN} / ${itemList.size}"
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setFilter(items : ArrayList<JournalList>) {
            itemList.clear()
            itemList.addAll(items)
            notifyDataSetChanged()
        }


        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var txtDtVisit : TextView = itemView.findViewById(R.id.txt_journal_date)
            var txtNmCust : TextView = itemView.findViewById(R.id.txt_journal_custname)
            var txtVisiter : TextView = itemView.findViewById(R.id.txt_journal_visiter)
            var txtDoctor : TextView = itemView.findViewById(R.id.txt_journal_doctor)
            var txtRemark : TextView = itemView.findViewById(R.id.txt_journal_remark)
            var etxtContent : EditText = itemView.findViewById(R.id.etxt_journal_content)
            var txtRowNum : TextView = itemView.findViewById(R.id.txt_journal_rownum)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem1 = menu.findItem(R.id.toolbar_cust_select)
        searchItem1.isVisible = false

        val searchItem = menu.findItem(R.id.app_bar_search)
        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = "내용 검색어 입력"
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
            val filterList : ArrayList<JournalList> = ArrayList()
            var textContent: String?
            val searchText = newText.toLowerCase(Locale.ROOT)

            for (idx in 0 until originList.size){
                textContent = nullChk(originList[idx].dcContent)
                if (textContent.toLowerCase(Locale.ROOT).contains(searchText))
                    filterList.add(originList[idx])
            }
            journalListAdapter!!.setFilter(filterList)
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }

        private fun nullChk(originval:String?) : String {
            return if (originval.isNullOrEmpty()) "" else originval
        }
    }

    inner class MyLinearSnapHelper : LinearSnapHelper() {

        /**
         * add a listener to get notified when a filter is snapped
         */
        override fun attachToRecyclerView(recyclerView: RecyclerView?) {
            var viewCache: View? = null
            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val view = findSnapView(bindingA.bindingCustJournal.rvJournalList.layoutManager!!)
                    if (viewCache != view) {
                        viewCache = view
                        //                  snapped.value = layoutManager!!.getPosition(view!!)
                    }
                }
            })
            super.attachToRecyclerView(recyclerView)
        }

        /**
         * correctly calculate the center of each filter, including first and last ones
         */
        override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
            val helper = OrientationHelper.createHorizontalHelper(layoutManager)

            // those are only the visible children!
            val totalChildren = layoutManager.childCount
            if (totalChildren == 0)
                return null
            // center position of parent
            val center = if (layoutManager.clipToPadding) {
                helper.startAfterPadding + helper.totalSpace / 2
            } else {
                helper.end / 2
            }
            // var for child closest to the center of display
            var closestChild: View? = null

            var absClosest = Integer.MAX_VALUE
            for (i in 0 until totalChildren) {
                val child = layoutManager.getChildAt(i)
                // if child center is closer than previous closest, set it as closest
                val childCenter = helper.getTransformedStartWithDecoration(child) + child!!.width / 2
                val distToCenter = abs(childCenter - center)
                if (distToCenter < absClosest) {
                    absClosest = distToCenter
                    closestChild = child
                }
            }
            return closestChild
        }

        fun getSnappedPosition(): Int {
            val view = findSnapView(bindingA.bindingCustJournal.rvJournalList.layoutManager!!)
            return if (view != null) bindingA.bindingCustJournal.rvJournalList.layoutManager!!.getPosition(view) else -1
        }

    }
}
