package com.lkdont.soundeditor

import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // init lib
        System.loadLibrary("soundeditor")
    }

}