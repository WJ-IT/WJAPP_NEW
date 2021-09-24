package wjm.co.kr.wjapp_new

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.animation.AnimationUtils
import wjm.co.kr.wjapp_new.databinding.CustomDialogBinding

class LodingDialog(private val c: Context) : Dialog(c) {
//    private var imgLogo: ImageView? = nul;
    private lateinit var binding: CustomDialogBinding

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        setCanceledOnTouchOutside(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.custom_dialog)
        binding = CustomDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val anim = AnimationUtils.loadAnimation(c, R.anim.loading)
        binding.imgAndroid.animation = anim
    }

//    override fun show() {
//        super.show()
//    }
//    override fun dismiss() {
//        super.dismiss()
//    }
}
