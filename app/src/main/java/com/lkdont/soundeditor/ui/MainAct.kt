package com.lkdont.soundeditor.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.lkdont.soundeditor.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Main Page
 *
 * Created by kidonliang on 2018/3/4.
 */
class MainAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        setContentView(R.layout.main_act)
        supportFragmentManager?.beginTransaction()
                ?.replace(R.id.container, ExamplesListFrag())
                ?.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    class FragmentEvent(val fragment: Fragment)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFragmentEvent(event: FragmentEvent) {
        supportFragmentManager?.beginTransaction()
                ?.replace(R.id.container, event.fragment)
                ?.addToBackStack(null)
                ?.commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragments = supportFragmentManager.fragments
        if (fragments != null) {
            for (fragment in fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

}
