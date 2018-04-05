package com.lkdont.soundeditor.sound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 耳机广播
 *
 * Created by kidonliang on 2018/4/3.
 */
class HeadsetBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.w("HeadSetPlugInTest", "onReceive")
        val action = intent?.action ?: return
        if (Intent.ACTION_HEADSET_PLUG == action) {
            Log.d("HeadSetPlugInTest", "state: " + intent.getIntExtra("state", -1))
            Log.d("HeadSetPlugInTest", "microphone: " + intent.getIntExtra("microphone", -1))
        }
    }
}