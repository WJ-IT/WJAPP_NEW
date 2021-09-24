package wjm.co.kr.wjapp_new

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException
import java.net.URL

class CustomerAdapter(val context: Context, var arItem:ArrayList<CustomerA>): BaseAdapter(),
    Filterable {

    internal var mOriginalValues:ArrayList<CustomerA>? = null // Original Values
    var selectItem : Int = -1

    override fun getCount():Int {
        return arItem.size
    }

    override fun getItem(position:Int): CustomerA {
        return arItem[position]
    }

    override fun getItemId(position:Int):Long {
        return position.toLong()
    }

    fun getSelectRow() : Int {
        return selectItem
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val layoutInflater = LayoutInflater.from(context)
        val res = R.layout.list_item_selectbutton
        val convertView = layoutInflater.inflate(res, parent, false)

        if (position and 1 == 0) {
            convertView.setBackgroundResource(R.drawable.list_item_2)
        } else {
            convertView.setBackgroundResource(R.drawable.list_item_3)
        }

        val col1 : TextView = convertView.findViewById(android.R.id.text1)
        col1.text = arItem[position].custNm

        val col2 : TextView = convertView.findViewById(android.R.id.text2)
        col2.text = arItem[position].custCd + " / Team : " + arItem[position].team + " / FAX : " + arItem[position].faxNo

        if (selectItem == -1 || selectItem != position)
            col1.setTextColor(Color.BLACK)
        else
            col1.setTextColor(Color.RED)

        val btnselect : Button = convertView.findViewById(R.id.btnCustSelect)
        btnselect.setOnClickListener {
            selectItem = position
            this.notifyDataSetChanged()
        }

        return convertView
    }

    override fun getFilter(): Filter {
        return  object : Filter() {
            override fun publishResults(constraint: CharSequence, results: FilterResults) {

                //myData = (List<MyDataType>) results.values;
                //MyCustomAdapter.this.notifyDataSetChanged();
                arItem = results.values as ArrayList<CustomerA>
                notifyDataSetChanged()
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                var const = constraint
                val results = FilterResults()        // Holds the results of a filtering operation in values
                val filteredArrList = ArrayList<CustomerA>()

                if (mOriginalValues == null) {
                    mOriginalValues = ArrayList(arItem) // saves the original data in mOriginalValues
                }

                if (const == null || const.isEmpty()) {

                    // set the Original result to return
                    results.count = mOriginalValues!!.size
                    results.values = mOriginalValues
                } else {
                    const = const.toString().toLowerCase()
                    for (i in 0 until mOriginalValues!!.size) {
                        val data : String? = mOriginalValues!![i].custNm
                        if (data!!.toLowerCase().startsWith(const.toString())) {
                            filteredArrList.add(mOriginalValues!![i])
                        }
                    }
                    // set the Filtered result to return
                    results.count = filteredArrList.size
                    results.values = filteredArrList
                }
                return results
            }
        }
        //  return filter
    }
}

data class DBCustomerList(val results : List<CustomerA>)
data class CustomerA (var custNm:String?, var custCd:String?, var faxNo:String?, var team:String?)

// 거래처 다이얼로그 Start
fun customerDialog(layoutInflater: LayoutInflater, mHandler : Handler, activity: Activity, gbn:String): Dialog {
    val layout = layoutInflater
    val nullParent : ViewGroup? = null
    val customerview = layout.inflate(R.layout.select_customer, nullParent)
    val builder = AlertDialog.Builder(activity)
    val arCustomerList : ArrayList<CustomerA> = ArrayList()
    val myCustomerList : ListView = customerview.findViewById(R.id.listCustomer)
    val customerAdapter = CustomerAdapter(activity, arCustomerList)
    val loadingDialog  = LodingDialog(activity)


    myCustomerList.adapter = customerAdapter
    myCustomerList.itemsCanFocus = false
    myCustomerList.choiceMode = ListView.CHOICE_MODE_SINGLE

    builder.setIcon(R.drawable.wjicon)
    builder.setTitle(R.string.dia_T_cust)
    builder.setCancelable(false)
    builder.setPositiveButton(R.string.dia_confirm) { _, _ ->
        //확인버튼 터치시
        loadingDialog.dismiss()
        val selectRow = customerAdapter.getSelectRow()
        if (selectRow != -1){
            val bundle = Bundle()
            bundle.putString("custCd", customerAdapter.getItem(selectRow).custCd)
            bundle.putString("custNm", customerAdapter.getItem(selectRow).custNm)
            bundle.putString("faxNo", customerAdapter.getItem(selectRow).faxNo)
            bundle.putString("team", customerAdapter.getItem(selectRow).team)
            val msg = mHandler.obtainMessage(0)

            msg.data = bundle
            mHandler.sendMessage(msg)
        } else {
            Toast.makeText(activity, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show()
            customerDialog(layoutInflater, mHandler, activity, gbn).show()
        }
//        val chkArr = myCustomerList.checkedItemPositions
//        if (chkArr.size() != 0) {
//            for (i in myCustomerList.count - 1 downTo -1 + 1) {
//                if (chkArr.get(i)) {
//
//                    val bundle = Bundle()
//                    bundle.putString("custCd", customerAdapter.getItem(i)?.custCd)
//                    bundle.putString("custNm", customerAdapter.getItem(i)?.custNm)
//                    bundle.putString("faxNo", customerAdapter.getItem(i)?.faxNo)
//                    bundle.putString("team", customerAdapter.getItem(i)?.team)
//                    val msg = mHandler.obtainMessage(0)
//
//                    msg.data = bundle
//                    mHandler.sendMessage(msg)
//                }
//            }
//        } else {
//            Toast.makeText(activity, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show()
//            customerDialog(layoutInflater, mHandler, activity, gbn).show()
//        }
    }
    builder.setNegativeButton(R.string.dia_cancle) { _, _ ->
    //취소버튼 터치시.
}
    builder.setView(customerview)

    //검색버튼 액션 실행
    val edCustomer : EditText = customerview.findViewById(R.id.edCustomer)
    val btnCustomerSch : Button = customerview.findViewById(R.id.btnCustomerSch)
    btnCustomerSch.setOnClickListener(({
        val str = edCustomer.text.toString().trim()
        arCustomerList.clear()
        customerAdapter.notifyDataSetChanged()

        if (str != "") {
            loadingDialog.show()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(edCustomer.windowToken, 0)
            val url : URL
            if (gbn=="NF") url = URL("http://iclkorea.com/android/Common_Cust_NF.asp")
            else url = URL("http://iclkorea.com/android/Common_Cust.asp")
            val body = FormBody.Builder().add("search_word", str).build()
            val request = Request.Builder().url(url).post(body).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseMsg = response.body?.string()
                    //println("Success to execute request! : $body")

                    //Gson으로 파싱
                    val gson = GsonBuilder().create()
                    val dbCustomerList = gson.fromJson(responseMsg, DBCustomerList::class.java)

                    for (idx in 0 until dbCustomerList.results.size  ) {
                        arCustomerList.add(
                            CustomerA(
                                dbCustomerList.results[idx].custNm,
                                dbCustomerList.results[idx].custCd,
                                dbCustomerList.results[idx].faxNo,
                                dbCustomerList.results[idx].team
                            )
                        )
                    }

                    activity.runOnUiThread {
                        customerAdapter.notifyDataSetChanged()
                        loadingDialog.hide()
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e.message)
                }
            })
        } else {

        }


    }))

    edCustomer.hint = "거래처명 입력부분"
   // edCustomer.imeOptions = EditorInfo.IME_ACTION_SEARCH
    edCustomer.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
    edCustomer.setOnEditorActionListener(object  : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> btnCustomerSch.performClick()
            }
            return false
        }
    })

    val dialog = builder.create()
    dialog.window?.setBackgroundDrawableResource(R.drawable.cdialog_outline)

//    edCustomer.setOnEditorActionListener{ v, actionId, event ->
//        when(actionId) {
//            EditorInfo.IME_ACTION_SEARCH -> btnCustomerSch.performClick()
//            else -> false
//        }
//    }
    return dialog
}
// 거래처 다이얼로그 End