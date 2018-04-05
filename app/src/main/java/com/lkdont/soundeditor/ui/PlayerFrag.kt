package com.lkdont.soundeditor.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lkdont.sound.edit.Info
import com.lkdont.soundeditor.R

/**
 * 播放器测试页面
 *
 * Created by kidonliang on 2018/4/5.
 */
class PlayerFrag: Fragment() {

    private val TAG = "PlayerFrag"

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.player_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // test
        Log.d(TAG, "start loading info...")

        val info = Info("http://88.88.88.253:8081/IXC43ab253f175e21c746bacda242273c27/deduct_attachment/program/428/685/80d252ecfd392d02.mp3")
//        val info = Info("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.mp3")
        val ret = info.load()
        if (ret == 0) {
            Log.d(TAG, info.toString())
        } else {
            Log.e(TAG, "load info fail : $ret")
        }

        Log.d(TAG, "finish loading info...")
    }
}