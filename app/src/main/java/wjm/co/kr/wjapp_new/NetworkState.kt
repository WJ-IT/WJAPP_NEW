package wjm.co.kr.wjapp_new

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class NetworkState internal constructor(context: Context) {
    var boolean = false
    init {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo : NetworkInfo? = manager.activeNetworkInfo
        boolean = activeNetworkInfo?.isConnected ?: false
    }

    fun isConnected() : Boolean {
        return boolean
    }
}
