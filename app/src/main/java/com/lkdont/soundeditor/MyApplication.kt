package com.lkdont.soundeditor

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.lkdont.soundeditor.sound.HeadsetBroadcastReceiver

class MyApplication : Application() {

    private val headsetBroadcastReceiver = HeadsetBroadcastReceiver()

    override fun onCreate() {
        super.onCreate()
        Log.i("MyApplication", "onCreate")
        // init lib
        System.loadLibrary("soundeditor")
        registerReceiver(headsetBroadcastReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }
}