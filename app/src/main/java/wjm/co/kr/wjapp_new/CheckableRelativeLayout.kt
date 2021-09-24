package wjm.co.kr.wjapp_new

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.RelativeLayout

//import android.content.Context
//import android.util.AttributeSet
//import android.view.LayoutInflater
//import android.widget.CheckBox
//import android.widget.Checkable
//import android.widget.RelativeLayout
//import wjm.co.kr.wjapp_new.databinding.ListItemRadiobuttonBinding
//
//class CheckableRelativeLayout : RelativeLayout,Checkable{
//    private var checked = false
//    //val viewRoot = LayoutInflater.from(context).inflate(R.layout.list_item_radiobutton, null, false)
//    //private lateinit var binding : ListItemRadiobuttonBinding// DataBindingUtil.setContentView(R.layout.list_item_radiobutton)
//    constructor(context: Context?) : super(context, null) {}
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
//    //private val binding: ListItemRadiobuttonBinding = ListItemRadiobuttonBinding.inflate(LayoutInflater.from(context), this, false)
//    override fun isChecked(): Boolean{
//        //binding = ListItemRadiobuttonBinding.bind(viewRoot)
//        return checked
//    }
//
//    override fun toggle() {
//        //binding = ListItemRadiobuttonBinding.bind(viewRoot)
//        checked = !checked
//    }
//
//    override fun setChecked(check: Boolean) {
//        //binding = ListItemRadiobuttonBinding.bind(viewRoot)
//        if(isChecked != check ) checked = check
//        forceLayout()
//        refreshDrawableState()
//    }
//
//    //checkBox.isChecked 는 레이아웃 안에 존재하는 체크박스의 체크여부를 말하고
//    //isChecked 홀로 있는 것은 체크박스를 담고 있는 뷰그룹 전체를 가리킴
//}


//import android.R
//import android.content.Context
//import android.util.AttributeSet
//import android.widget.Checkable
//import android.widget.RelativeLayout
//
//
//class CheckableRelativeLayout: RelativeLayout, Checkable {
//    private var checked = false
//
//    constructor(context: Context?) : super(context, null) {}
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
//
//    override fun isChecked(): Boolean {
//        return checked
//    }
//
//    override fun setChecked(b: Boolean) {
//        checked = b
//        refreshDrawableState()
//        forceLayout()
//    }
//
//    override fun toggle() {
//        checked = !checked
//    }
//
//    override fun onCreateDrawableState(extraSpace: Int): IntArray {
//        val drawableState = super.onCreateDrawableState(extraSpace + 1)
//        if (isChecked) {
//            mergeDrawableStates(drawableState, CheckedStateSet)
//        }
//        return drawableState
//    }
//
//    companion object {
//        private val CheckedStateSet = intArrayOf(
//            R.attr.state_checked
//        )
//    }
//}
class CheckableRelativeLayout : RelativeLayout, Checkable {
    private var mChecked = false
    //val checkable : Checkable
    constructor(context: Context?) : super(context) {}

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshDrawableState()
        }
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(
            R.attr.state_checked
        )
    }
}