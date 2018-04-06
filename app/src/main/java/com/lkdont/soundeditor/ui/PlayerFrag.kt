package com.lkdont.soundeditor.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lkdont.sound.player.PlayEventListener
import com.lkdont.sound.player.SoundPlayer
import com.lkdont.sound.player.Status
import com.lkdont.soundeditor.R
import kotlinx.android.synthetic.main.player_frag.*

/**
 * 播放器测试页面
 *
 * Created by kidonliang on 2018/4/5.
 */
class PlayerFrag : Fragment() {

    private val TAG = "PlayerFrag"

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.player_frag, container, false)
        return view
    }

    private var mPlayer: SoundPlayer? = null
    private val mPlayEventListener = object : PlayEventListener {

        override fun onUpdatingTime(curTime: Long, totalTime: Long) {
            Log.i(TAG, "onUpdatingTime curTime=$curTime, totalTime=$totalTime")
            time_pb.progress = (100 * curTime / totalTime).toInt()
        }

        override fun onUpdatingStatus(status: Status) {
            Log.i(TAG, "onUpdatingStatus status=$status")
            updatePlayUi(status)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.mp3")
        play_btn.setOnClickListener {
            when (mPlayer?.getStatus()) {

                null -> {
                    if (mPlayer != null) {
                        mPlayer?.stop()
                        mPlayer?.release()
                    }
                    mPlayer = SoundPlayer(input_et.text.toString())
                    mPlayer?.setPlayEventListener(mPlayEventListener)
                    mPlayer?.start()
                    updatePlayUi(Status.PLAYING)
                }

                Status.PLAYING -> {
                    mPlayer?.pause()
                }

                Status.PAUSING -> {
                    mPlayer?.resume()
                }

                else -> {
                    Log.w(TAG, "do nothing at status : " + mPlayer?.getStatus())
                }
            }
        }
        stop_btn.setOnClickListener {
            mPlayer?.stop()
            mPlayer = null
        }
    }

    private fun updatePlayUi(status: Status?) {
        when (status) {

            Status.PLAYING -> {
                play_btn.text = "暂停"
                stop_btn.visibility = View.VISIBLE
            }

            Status.PAUSING -> {
                play_btn.text = "继续"
                stop_btn.visibility = View.VISIBLE
            }

            else -> {
                play_btn.text = "播放"
                stop_btn.visibility = View.GONE
                time_pb.progress = 0
            }
        }
    }
}