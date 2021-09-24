package wjm.co.kr.wjapp_new

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.CheckedTextView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWjBoardWriteBinding
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class WjBoardWriteActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWjBoardWriteBinding
    private var arPerson : ArrayList<PersonVO> = ArrayList()
    private var personAdapter : PersonAdapter? = null
    private var noMsg : String = ""
    private var sendGbn : String = ""
    private var arSendPerson : ArrayList<String> = ArrayList()
    private var notice : String = "N"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj_board_write)
        bindingA = ActivityWjBoardWriteBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        boardWriteInit()
        val rvBoard : RecyclerView = findViewById(R.id.rvBoardWrite)
        rvBoard.animate().setListener(object  : AnimatorListenerAdapter(){
            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
                if (bindingA.bindingBoardWrite.chkTxtAll.isChecked || bindingA.bindingBoardWrite.chkTxtAlm.isChecked)
                    bindingA.bindingBoardWrite.rvBoardWrite.visibility = View.GONE
                else
                    bindingA.bindingBoardWrite.rvBoardWrite.visibility = View.VISIBLE
            }
        })

        bindingA.bindingBoardWrite.chkTxtAll.setOnClickListener(({
            if (bindingA.bindingBoardWrite.chkTxtAll.isChecked) {
                bindingA.bindingBoardWrite.chkTxtAll.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                bindingA.bindingBoardWrite.chkTxtAll.isChecked = false
                if (!bindingA.bindingBoardWrite.chkTxtAlm.isChecked) {
                    bindingA.bindingBoardWrite.chkTxtNotice.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                    bindingA.bindingBoardWrite.chkTxtNotice.isChecked = false
                }
            } else {
                bindingA.bindingBoardWrite.chkTxtAll.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                bindingA.bindingBoardWrite.chkTxtAll.isChecked = true
            }

            if (bindingA.bindingBoardWrite.chkTxtAll.isChecked || bindingA.bindingBoardWrite.chkTxtAlm.isChecked || bindingA.bindingBoardWrite.chkTxtNotice.isChecked)
                rvBoard.animate().alpha(0.0f)
            else
                bindingA.bindingBoardWrite.rvBoardWrite.animate().alpha(1.0f)
        }))

        bindingA.bindingBoardWrite.chkTxtAlm.setOnClickListener(({
            if (bindingA.bindingBoardWrite.chkTxtAlm.isChecked) {
                bindingA.bindingBoardWrite.chkTxtAlm.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                bindingA.bindingBoardWrite.chkTxtAlm.isChecked = false
                if (!bindingA.bindingBoardWrite.chkTxtAll.isChecked) {
                    bindingA.bindingBoardWrite.chkTxtNotice.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                    bindingA.bindingBoardWrite.chkTxtNotice.isChecked = false
                }
            } else {
                bindingA.bindingBoardWrite.chkTxtAlm.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                bindingA.bindingBoardWrite.chkTxtAlm.isChecked = true
            }

            if (bindingA.bindingBoardWrite.chkTxtAll.isChecked || bindingA.bindingBoardWrite.chkTxtAlm.isChecked || bindingA.bindingBoardWrite.chkTxtNotice.isChecked)
                rvBoard.animate().alpha(0.0f)
            else
                bindingA.bindingBoardWrite.rvBoardWrite.animate().alpha(1.0f)
        }))

        bindingA.bindingBoardWrite.chkTxtNotice.setOnClickListener(({
            if (bindingA.bindingBoardWrite.chkTxtNotice.isChecked) {
                bindingA.bindingBoardWrite.chkTxtNotice.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                bindingA.bindingBoardWrite.chkTxtNotice.isChecked = false
            } else {
                if (!bindingA.bindingBoardWrite.chkTxtAll.isChecked && !bindingA.bindingBoardWrite.chkTxtAlm.isChecked) {
                    bindingA.bindingBoardWrite.chkTxtAll.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                    bindingA.bindingBoardWrite.chkTxtAll.isChecked = true
                }
                bindingA.bindingBoardWrite.chkTxtNotice.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                bindingA.bindingBoardWrite.chkTxtNotice.isChecked = true
            }

            if (bindingA.bindingBoardWrite.chkTxtAll.isChecked || bindingA.bindingBoardWrite.chkTxtAlm.isChecked || bindingA.bindingBoardWrite.chkTxtNotice.isChecked)
                rvBoard.animate().alpha(0.0f)
            else
                bindingA.bindingBoardWrite.rvBoardWrite.animate().alpha(1.0f)
        }))

        bindingA.bindingBoardWrite.btnBoardWriteSend.setOnClickListener(({
            sendGbn = if (bindingA.bindingBoardWrite.chkTxtAlm.isChecked) "ALM"
            else if (bindingA.bindingBoardWrite.chkTxtAll.isChecked) "ALL"
            else "Person"

            notice = if(bindingA.bindingBoardWrite.chkTxtNotice.isChecked) "Y" else "N"

            if (arSendPerson.size == 0 && sendGbn == "Person") {
                Toast.makeText(this, "전체 혹은 받을사람을 선택하세요", Toast.LENGTH_LONG).show()
            } else {
                if (notice == "Y") {
                    val cancelDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                    cancelDialog.setMessage("전체 공지사항을 등록 하시겠습니까?")
                        .setIcon(R.drawable.wjicon).setTitle("전체공지 작성")
                        .setPositiveButton("확인") { _, _ ->
                            setNewMssg()
                        }
                        .setNegativeButton("취소", null)
                        .setCancelable(false)
                        .show()
                } else {
                    val cancelDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                    cancelDialog.setMessage("작성한 글을 등록 하시겠습니까?")
                        .setIcon(R.drawable.wjicon).setTitle("게시글 작성")
                        .setPositiveButton("확인") { _, _ ->
                            setNewMssg()
                        }
                        .setNegativeButton("취소", null)
                        .setCancelable(false)
                        .show()
                }
            }

        }))

        bindingA.bindingBoardWrite.clBoardWrite.setOnClickListener(({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(bindingA.bindingBoardWrite.etxtMultiContent.windowToken, 0)
        }))
    }

    private fun boardWriteInit() {
        val intent = intent
        noMsg = intent.getStringExtra("noMsg").toString()

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        personAdapter = PersonAdapter(this, arPerson)
        bindingA.bindingBoardWrite.rvBoardWrite.layoutManager = layoutManager
        bindingA.bindingBoardWrite.rvBoardWrite.adapter = personAdapter

        getPersonList()
    }

    private fun getPersonList() {
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        val url = URL("http://iclkorea.com/android/WJBoardPersonList.asp")
        val body = FormBody.Builder().build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBPersonList = gson.fromJson(body1, DBPersonList::class.java)

                arPerson.addAll(dBPersonList.results)

                runOnUiThread {
                    personAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setNewMssg() {
        var temp = ""
        if (arSendPerson.size > 0) {
            for (idx in 0 until arSendPerson.size)
                temp = temp + arSendPerson[idx] + ","
            temp = temp.substring(0, temp.length - 1)
        }
        val url = URL("http://iclkorea.com/android/WJBoardWrite.asp")
        val body = FormBody.Builder().add("fromsno", WjmMain.LoginUser.sno).add("noMsg", noMsg).add("content", bindingA.bindingBoardWrite.etxtMultiContent.text.toString()).add("gbn", sendGbn).add("tosno", temp).add("notice", notice).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body1 = response.body?.string()
                println("Success to execute request! : $body1")
                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBBoardWrite = gson.fromJson(body1, DBBoardWrite::class.java)

                if (dBBoardWrite.results == "OK") {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@WjBoardWriteActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("게시글 등록 하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("게시글 작성")
                            .setPositiveButton("확인") {_,_ ->
                                setResult(1)
                                finish()
                                //return true
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    runOnUiThread {
                        val cancelDialog = AlertDialog.Builder(this@WjBoardWriteActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                        cancelDialog.setMessage("새글쓰기가 실패하였습니다")
                            .setIcon(R.drawable.wjicon).setTitle("게시판 작성")
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

    data class DBPersonList(var results:List<PersonVO>)
    data class PersonVO(var nmCompany:String?, var name01:String?, var name02:String?, var name03:String?, var name04:String?, var name05:String?,
                        var sno01:String?, var sno02:String?, var sno03:String?, var sno04:String?, var sno05:String?,
                        var chk01:Boolean, var chk02:Boolean, var chk03:Boolean, var chk04:Boolean, var chk05:Boolean)
    data class DBBoardWrite(var results: String?)

    inner class PersonAdapter(private val context: Context, private val itemList: ArrayList<PersonVO>) : RecyclerView.Adapter<PersonAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_board_person , parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val company = itemList[position].nmCompany

            holder.company.text = company
            holder.chkTxtName01.text = itemList[position].name01
            holder.chkTxtName02.text = itemList[position].name02
            holder.chkTxtName03.text = itemList[position].name03
            holder.chkTxtName04.text = itemList[position].name04
            holder.chkTxtName05.text = itemList[position].name05

            if (position != 0 && company == itemList[position-1].nmCompany)
                holder.company.visibility = View.GONE
            else
                holder.company.visibility = View.VISIBLE

            if (itemList[position].name01 == "") holder.chkTxtName01.checkMarkDrawable = null
            else holder.chkTxtName01.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
            if (itemList[position].name02 == "") holder.chkTxtName02.checkMarkDrawable = null
            else holder.chkTxtName02.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
            if (itemList[position].name03 == "") holder.chkTxtName03.checkMarkDrawable = null
            else holder.chkTxtName03.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
            if (itemList[position].name04 == "") holder.chkTxtName04.checkMarkDrawable = null
            else holder.chkTxtName04.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
            if (itemList[position].name05 == "") holder.chkTxtName05.checkMarkDrawable = null
            else holder.chkTxtName05.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)

            if (itemList[position].chk01) {
                holder.chkTxtName01.isChecked = true
                holder.chkTxtName01.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            }
            if (itemList[position].chk02) {
                holder.chkTxtName02.isChecked = true
                holder.chkTxtName02.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            }
            if (itemList[position].chk03) {
                holder.chkTxtName03.isChecked = true
                holder.chkTxtName03.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            }
            if (itemList[position].chk04) {
                holder.chkTxtName04.isChecked = true
                holder.chkTxtName04.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            }
            if (itemList[position].chk05) {
                holder.chkTxtName05.isChecked = true
                holder.chkTxtName05.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
            }

            holder.chkTxtName01.setOnClickListener(({
                if (holder.chkTxtName01.text != "") {
                    if (holder.chkTxtName01.isChecked) {
                        arSendPerson.remove(itemList[position].sno01!!)
                        holder.chkTxtName01.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.chkTxtName01.isChecked = false
                        itemList[position].chk01 = false
                    } else {
                        arSendPerson.add(itemList[position].sno01!!)
                        holder.chkTxtName01.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.chkTxtName01.isChecked = true
                        itemList[position].chk01 = true
                    }
                }
            }))
            holder.chkTxtName02.setOnClickListener(({
                if (holder.chkTxtName02.text != "") {
                    if (holder.chkTxtName02.isChecked) {
                        arSendPerson.remove(itemList[position].sno02!!)
                        holder.chkTxtName02.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.chkTxtName02.isChecked = false
                        itemList[position].chk02 = false
                    } else {
                        arSendPerson.add(itemList[position].sno02!!)
                        holder.chkTxtName02.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.chkTxtName02.isChecked = true
                        itemList[position].chk02 = true
                    }
                }
            }))
            holder.chkTxtName03.setOnClickListener(({
                if (holder.chkTxtName03.text != "") {
                    if (holder.chkTxtName03.isChecked) {
                        arSendPerson.remove(itemList[position].sno03!!)
                        holder.chkTxtName03.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.chkTxtName03.isChecked = false
                        itemList[position].chk03 = false
                    } else {
                        arSendPerson.add(itemList[position].sno03!!)
                        holder.chkTxtName03.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.chkTxtName03.isChecked = true
                        itemList[position].chk03 = true
                    }
                }
            }))
            holder.chkTxtName04.setOnClickListener(({
                if (holder.chkTxtName04.text != "") {
                    if (holder.chkTxtName04.isChecked) {
                        arSendPerson.remove(itemList[position].sno04!!)
                        holder.chkTxtName04.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.chkTxtName04.isChecked = false
                        itemList[position].chk04 = false
                    } else {
                        arSendPerson.add(itemList[position].sno04!!)
                        holder.chkTxtName04.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.chkTxtName04.isChecked = true
                        itemList[position].chk04 = true
                    }
                }
            }))
            holder.chkTxtName05.setOnClickListener(({
                if (holder.chkTxtName05.text != "") {
                    if (holder.chkTxtName05.isChecked) {
                        arSendPerson.remove(itemList[position].sno05!!)
                        holder.chkTxtName05.setCheckMarkDrawable(android.R.drawable.checkbox_off_background)
                        holder.chkTxtName05.isChecked = false
                        itemList[position].chk05 = false
                    } else {
                        arSendPerson.add(itemList[position].sno05!!)
                        holder.chkTxtName05.setCheckMarkDrawable(android.R.drawable.checkbox_on_background)
                        holder.chkTxtName05.isChecked = true
                        itemList[position].chk05 = true
                    }
                }
            }))
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val company : TextView = itemView.findViewById(R.id.txtCompanyBoard)
            val chkTxtName01 : CheckedTextView = itemView.findViewById(R.id.checkTxtName01)
            val chkTxtName02 : CheckedTextView = itemView.findViewById(R.id.checkTxtName02)
            val chkTxtName03 : CheckedTextView = itemView.findViewById(R.id.checkTxtName03)
            val chkTxtName04 : CheckedTextView = itemView.findViewById(R.id.checkTxtName04)
            val chkTxtName05 : CheckedTextView = itemView.findViewById(R.id.checkTxtName05)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val menuSearch = menu.findItem(R.id.app_bar_search)
        menuSearch.isVisible = false
        val menuCustomer = menu.findItem(R.id.toolbar_cust_select)
        menuCustomer.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
        }
        return super.onOptionsItemSelected(item)
    }
}
