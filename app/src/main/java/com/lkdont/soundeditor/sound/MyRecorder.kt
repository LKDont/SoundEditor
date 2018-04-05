package com.lkdont.soundeditor.sound

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.FileOutputStream

/**
 * 录音器
 *
 * Created by kidonliang on 2018/4/3.
 */
class MyRecorder : AsyncTask<String, Long, Boolean>() {

    companion object {

        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        /**
         * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
         */
        private const val DEFAULT_SAMPLING_RATE = 44100// 模拟器仅支持从麦克风输入8kHz采样率
        private const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // 单声道
        private const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    }

    private var mBufferSize: Int
    private var mAudioRecord: AudioRecord?

    private val mFramePeriod = 160
    private val mPCMBuffer: ByteArray
    private val mHandler = Handler(Looper.getMainLooper())

    init {

        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT)
        Log.d("MyRecorder", "getMinBufferSize = $mBufferSize")

        var frameSize = mBufferSize
        if (frameSize % mFramePeriod != 0) {
            frameSize += mFramePeriod - frameSize % mFramePeriod
            mBufferSize = frameSize
        }
        Log.d("MyRecorder", "frameSize = $frameSize, mBufferSize = $mBufferSize")

        // 初始化recorder
        mAudioRecord = AudioRecord(AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT, mBufferSize)
        mPCMBuffer = ByteArray(mBufferSize)

        mAudioRecord?.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {

            override fun onMarkerReached(recorder: AudioRecord?) {
                Log.i("MyRecorder", "onMarkerReached")
            }

            override fun onPeriodicNotification(recorder: AudioRecord?) {
                Log.w("MyRecorder", "onPeriodicNotification")
            }

        }, mHandler)
        mAudioRecord?.positionNotificationPeriod = mFramePeriod
    }

    private var mStop = false

    fun stopRecord() {
        mStop = true
    }

    override fun doInBackground(vararg params: String?): Boolean {
        Log.d("MyRecorder", "mBufferSize = $mBufferSize, mFramePeriod = $mFramePeriod")
        var outStream: FileOutputStream? = null
        try {
            outStream = try {
                FileOutputStream(params[0])
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            mAudioRecord?.startRecording()

            while (!mStop) {
                val readSize = mAudioRecord?.read(mPCMBuffer, 0, mBufferSize) ?: 0
                if (readSize > 0) {
                    // 写入文件
                    outStream.write(mPCMBuffer, 0, readSize)
                }
            }

            return true
        } finally {
            // end
            try {
                mAudioRecord?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mAudioRecord?.release()
            mAudioRecord = null
            try {
                outStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        Log.i("MyRecorder", "onPostExecute $result")
    }
}
