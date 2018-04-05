package com.lkdont.soundeditor.sound

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.AsyncTask
import android.util.Log
import java.io.FileInputStream

/**
 * 播放器
 *
 * Created by kidonliang on 2018/4/3.
 */
class MyPlayer : AsyncTask<String, Long, Boolean>() {

    private var mAudioTrack: AudioTrack? = null

    init {
        val sampleRate = 44100
        val channel = 2

        mAudioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channel,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM)
    }

    override fun doInBackground(vararg params: String?): Boolean {
        try {
            val inStream = try {
                FileInputStream(params[0])
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            mAudioTrack?.play()

            val buffer = ByteArray(1024)
            var read = inStream.read(buffer, 0, 1024)
            while (read > 0) {
                mAudioTrack?.write(buffer, 0, read)
                read = inStream.read(buffer, 0, 1024)
            }

            return true
        } finally {
            mAudioTrack?.stop()
            mAudioTrack?.release()
            mAudioTrack = null
        }
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        Log.i("MyPlayer", "onPostExecute $result")
    }
}