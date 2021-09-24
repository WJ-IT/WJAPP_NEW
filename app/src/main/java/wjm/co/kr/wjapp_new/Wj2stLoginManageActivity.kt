package wjm.co.kr.wjapp_new

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder

import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityWj2stLoginManageBinding
import java.io.IOException
import java.net.URL

class Wj2stLoginManageActivity : AppCompatActivity() {
    private lateinit var bindingA: ActivityWj2stLoginManageBinding
    private var secondLoginUserList : ArrayList<LoginUserList> = ArrayList()
    private var secondLoginAdapter : SecondLoginAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wj2st_login_manage)
        bindingA = ActivityWj2stLoginManageBinding.inflate(layoutInflater)
        setContentView(bindingA.root)
        setSupportActionBar(bindingA.toolbar)

        secondLoginManageInit()
    }

    private fun secondLoginManageInit() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        if (WjmMain.LoginUser.sno == "") {
            Toast.makeText(baseContext, "로그인 정보가 사라졌어요!", Toast.LENGTH_LONG).show()
            val intent = Intent(baseContext, WjmMain::class.java)
            startActivity(intent)
        }

        val listManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        secondLoginAdapter = SecondLoginAdapter(this, secondLoginUserList)
        bindingA.binding2stLogin.rv2stLogin.layoutManager = listManager
        bindingA.binding2stLogin.rv2stLogin.adapter = secondLoginAdapter

        get2stUserList()
    }

    private fun get2stUserList() {
        secondLoginUserList.clear()
        val url = URL("http://iclkorea.com/android/WJ2stLoginList.asp")
        val body = FormBody.Builder().add("sno", WjmMain.LoginUser.sno).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responsebody = response.body?.string()
                println("Success to execute request! : $responsebody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBLoginUserList = gson.fromJson(responsebody, DBLoginUserList::class.java)

                for (idx in dBLoginUserList.results.indices) {
                    secondLoginUserList.add(
                        LoginUserList(
                            dBLoginUserList.results[idx].uuid,
                            dBLoginUserList.results[idx].sno,
                            dBLoginUserList.results[idx].permission,
                            dBLoginUserList.results[idx].loginDate,
                            dBLoginUserList.results[idx].name,
                            dBLoginUserList.results[idx].device
                        )
                    )
                }

                runOnUiThread {
                    secondLoginAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setTogglePermission(uuid:String, sno:String, permission:String) {
        val url = URL("http://iclkorea.com/android/WJ2stChangePermission.asp")
        val body = FormBody.Builder().add("uuid", uuid).add("sno", sno).add("permission", permission).build()
        val request = Request.Builder().url(url).post(body).build()
        val client = OkHttpClient()
        val loadingDialog = LodingDialog(this)
        loadingDialog.show()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responsebody = response.body?.string()
                println("Success to execute request! : $responsebody")

                //Gson으로 파싱
                val gson = GsonBuilder().create()
                val dBChangePermission = gson.fromJson(responsebody, DBChangePermission::class.java)
                if (dBChangePermission.results == "NO")
                    runOnUiThread {
                        loadingDialog.dismiss()
                        Toast.makeText(parent, "변경실패", Toast.LENGTH_SHORT).show()
                    }
                else
                    runOnUiThread {
                        loadingDialog.dismiss()
                        get2stUserList()
                    }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    data class DBLoginUserList(var results : List<LoginUserList>, var status:String?)
    data class LoginUserList(var uuid:String?, var sno:String?, var permission:String?, var loginDate:String?, var name:String?, var device:String?)
    data class DBChangePermission(var results: String)

    inner class SecondLoginAdapter(private val context: Context, private val itemList: ArrayList<LoginUserList>) : RecyclerView.Adapter<SecondLoginAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup?,
            viewType: Int
        ): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj2st_user, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txt2stDevice.text = itemList[position].uuid
            holder.txt2stName.text = "${itemList[position].name} / ${itemList[position].device} / ${itemList[position].loginDate}"
            holder.btn2stPermission.text = itemList[position].permission

            holder.btn2stPermission.setOnClickListener(({
                setTogglePermission(itemList[position].uuid!!, itemList[position].sno!!, itemList[position].permission!!)
            }))
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txt2stName : TextView = itemView.findViewById(R.id.txt2stName)
            val txt2stDevice : TextView = itemView.findViewById(R.id.txt2stDevice)
            val btn2stPermission : Button = itemView.findViewById(R.id.btn2stPermission)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {finish() ; return true}
        }
        return super.onOptionsItemSelected(item)
    }
}
