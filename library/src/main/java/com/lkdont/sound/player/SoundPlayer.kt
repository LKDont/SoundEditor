package com.lkdont.sound.player

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.ref.WeakReference

/**
 * 声音播放器
 *
 * Created by kidonliang on 2018/4/4.
 */
class SoundPlayer(val url: String) {

    companion object {

        private const val TAG = "SoundPlayer"

        const val ERROR_CODE_OTHER = -1
        const val ERROR_CODE_GET_INFO_FAIL = -2
        const val ERROR_CODE_INIT_FAIL = -3
        const val ERROR_CODE_DECODING = -4

    }

    private val handler = EventHandler(this)
    private val playTask = PlayTask(url, handler)

    fun getStatus(): Status {
        return playTask.getStatus()
    }

    fun start() {
        if (getStatus() == Status.UNDEFINED) {
            Thread(playTask).start()
        } else {
            Log.w(TAG, "start at illegal status : " + getStatus())
        }
    }

    fun pause() {
        if (getStatus() == Status.PLAYING) {
            playTask.setStatus(Status.PAUSING)
        } else {
            Log.w(TAG, "pause at illegal status : " + getStatus())
        }
    }

    fun resume() {
        if (getStatus() == Status.PAUSING) {
            playTask.setStatus(Status.PLAYING)
        } else {
            Log.w(TAG, "resume at illegal status : " + getStatus())
        }
    }

    fun stop() {
        playTask.setStatus(Status.STOPPED)
    }

    fun release() {
        handler.stop()
    }

    private var mPlayEventListener: PlayEventListener? = null

    fun setPlayEventListener(listener: PlayEventListener?) {
        mPlayEventListener = listener
    }

    internal class EventHandler(player: SoundPlayer) : Handler(Looper.getMainLooper()) {

        private val mReference = WeakReference<SoundPlayer>(player)

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val player = mReference.get() ?: return
            when(msg?.what) {

                PlayTask.MSG_WHAT_TIME -> {
                    val data = msg.data ?: return
                    val curTime = data.getLong(PlayTask.MSG_KEY_CURTIME, 0)
                    val totalTime = data.getLong(PlayTask.MSG_KEY_TOTAL_TIME, 0)
                    player.mPlayEventListener?.onUpdatingTime(curTime, totalTime)
                }

                PlayTask.MSG_WHAT_STATUS -> {
                    val status = (msg.obj as? Status?) ?: return
                    player.mPlayEventListener?.onUpdatingStatus(status)
                }

                PlayTask.MSG_WHAT_ERROR -> {
                    val err = msg.data?.getString(PlayTask.MSG_KEY_ERROR, null) ?: return
                    player.mPlayEventListener?.onError(msg.arg1, err)
                }
            }
        }

        fun stop() {
            mReference.clear()
        }
    }
}
