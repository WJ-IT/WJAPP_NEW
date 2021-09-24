package wjm.co.kr.wjapp_new

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder

//import kotlinx.android.synthetic.main.content_company_menu.*
import okhttp3.*
import wjm.co.kr.wjapp_new.databinding.ActivityCompanyMenuBinding
import wjm.co.kr.wjapp_new.databinding.ContentCompanyMenuBinding
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class CompanyMenuActivity : AppCompatActivity() {
    private lateinit var binding: ContentCompanyMenuBinding
    private var companyMenuList : ArrayList<CompanyMenuList> = ArrayList()
    private var companyMenuAdapter : CompanyMenuAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_company_menu)
        binding = ContentCompanyMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        companyMenuInit()
    }

    private fun companyMenuInit() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo)
        binding.imgLogoCompany.animation = anim

        val listManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        companyMenuAdapter = CompanyMenuAdapter(this, companyMenuList)
        binding.rvCompanyMenuList.layoutManager = listManager
        binding.rvCompanyMenuList.adapter = companyMenuAdapter

        val const:LinearLayout= findViewById(R.id.llCMTop)
        val param:ConstraintLayout.LayoutParams = const.layoutParams as ConstraintLayout.LayoutParams

        if (WjmMain.LoginUser.screenWidth > 2000)
            param.matchConstraintPercentWidth = 0.5f
        else
            param.matchConstraintPercentWidth = 0.9f
        const.layoutParams = param
        getCMenuList()
    }

    private fun getCMenuList() {
        val url = URL("http://iclkorea.com/android/WJMenu_list.asp")
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
                val dBCompanyMenuList = gson.fromJson(responsebody, DBCompanyMenuList::class.java)

                for (idx in 0 until dBCompanyMenuList.results.size ) {
                    companyMenuList.add(
                        CompanyMenuList(
                            dBCompanyMenuList.results[idx].idMenu,
                            dBCompanyMenuList.results[idx].nmMenu,
                            dBCompanyMenuList.results[idx].bgColorSt,
                            dBCompanyMenuList.results[idx].bgColorEnd,
                            dBCompanyMenuList.results[idx].bookmarkYN
                        )
                    )
                }

                runOnUiThread {
                    companyMenuAdapter!!.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to execute request!")
                println(e.message)
            }
        })
    }

    private fun setBookmark(item:CompanyMenuList) {
        val url = URL("http://iclkorea.com/android/WJMenu_bookmark.asp")
        val body = FormBody.Builder().add("sno", WjmMain.LoginUser.sno).add("id_menu", item.idMenu!!).build()
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
                val dbSetBookmark = gson.fromJson(responsebody, DBCompanyMenuList::class.java)

                if (dbSetBookmark.status == "NO") {
                    runOnUiThread {
                        Toast.makeText(baseContext, "즐겨찾기 설정에 실패하였습니다.", Toast.LENGTH_LONG).show()
                        loadingDialog.dismiss()
                    }
                }else {
                    runOnUiThread {
                        val comp : Comparator<CompanyMenuList> = object : Comparator<CompanyMenuList> {
                            override fun compare(o1: CompanyMenuList, o2: CompanyMenuList): Int {
                                var temp1 = ""
                                var temp2 = ""
                                if (o1.bookmarkYN=="Y") temp1 = "0"
                                if (o2.bookmarkYN=="Y") temp2 = "0"
                                val v1 = temp1 + o1.idMenu
                                val v2 = temp2 + o2.idMenu
                                return v1.compareTo(v2)
                            }
                        }
                        Collections.sort(companyMenuList, comp)

                        Toast.makeText(baseContext, "즐겨찾기 ON/OFF 설정 하였습니다.", Toast.LENGTH_LONG).show()
                        companyMenuAdapter!!.notifyDataSetChanged()
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

    data class DBCompanyMenuList(var results : List<CompanyMenuList>, var status:String?)
    data class CompanyMenuList(var idMenu:String?, var nmMenu:String?, var bgColorSt:String?, var bgColorEnd:String?, var bookmarkYN:String?)

    inner class CompanyMenuAdapter(private val context: Context,private val itemList: ArrayList<CompanyMenuList>) : RecyclerView.Adapter<CompanyMenuAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // context 와 parent.getContext() 는 같다.
            val view = LayoutInflater.from(context).inflate(R.layout.row_wj_menu, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val param:ConstraintLayout.LayoutParams = holder.llCompanyRow.layoutParams as ConstraintLayout.LayoutParams
            if (WjmMain.LoginUser.screenWidth > 2000)
                param.matchConstraintPercentWidth = 0.5f
            else
                param.matchConstraintPercentWidth = 0.9f
            holder.llCompanyRow.layoutParams = param

            val stColor = Color.parseColor("#" + itemList[position].bgColorSt)
            val endColor = Color.parseColor("#" + itemList[position].bgColorEnd)
            val color = intArrayOf(stColor, endColor)
            val gradient = GradientDrawable(GradientDrawable.Orientation.BL_TR, color)
            gradient.shape = GradientDrawable.RECTANGLE
            gradient.cornerRadius = 50.toFloat()

            holder.llCompanyRow.background = gradient
            holder.txtNmMenu.text = companyMenuList[position].nmMenu
            if (itemList[position].bookmarkYN == "Y") holder.imgBookmark.setImageDrawable(resources.getDrawable(android.R.drawable.btn_star_big_on, null))
            else holder.imgBookmark.setImageDrawable(resources.getDrawable(android.R.drawable.btn_star_big_off, null))

            holder.txtNmMenu.setOnClickListener(({
                var intent: Intent? = null
                when (companyMenuList[position].idMenu) {
                    "app0100001" -> intent = Intent(context, WjSalesReportActivity::class.java)
                    "app0100002" -> intent = Intent(context, WjJaegoReportActivity::class.java)
                    "app0100003" -> intent = Intent(context, WjLmReportActivity::class.java)
                    "app0100004" -> intent = Intent(context, WjOrderReportActivity::class.java)
                    "app0100005" -> intent = Intent(context, WjOrderActivity::class.java)
                    "app0100006" -> intent = Intent(context, WjBalanceManageActivity::class.java)
                    "app0100007" -> intent = Intent(context, WjSalesTeamReportActivity::class.java)
                    "app0100008" -> intent = Intent(context, WjBoardActivity::class.java)
                    "app0100009" -> intent = Intent(context, WjPodftJisiActivity::class.java)
                    "app0100010" -> intent = Intent(context, WjCustJournalActivity::class.java)
                    "app0100011" -> intent = Intent(context, WjCustStockActivity::class.java)

                    "app0101000" -> intent = Intent(context, Wj2stLoginManageActivity::class.java) //2차 로그인 설정화면(관리자용)
                }

                if (intent != null) {
                    startActivity(intent)
                    overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout)
                }

            }))

            holder.imgBookmark.setOnClickListener(({
                if (itemList[position].bookmarkYN == "Y") itemList[position].bookmarkYN = "N"
                else itemList[position].bookmarkYN = "Y"
                setBookmark(itemList[position])
            }))
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtNmMenu : TextView = itemView.findViewById(R.id.txt_company_menu)
            val llCompanyRow: LinearLayout = itemView.findViewById(R.id.ll_company_row)
            val imgBookmark : ImageView = itemView.findViewById(R.id.img_wj_bookmark)
            init {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
