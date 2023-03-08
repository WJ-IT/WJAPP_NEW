package wjm.co.kr.wjapp_new

import android.content.Context
import android.widget.Toast


class Gen_fun {
    fun ssToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    fun slToast(context: Context, msg:String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}