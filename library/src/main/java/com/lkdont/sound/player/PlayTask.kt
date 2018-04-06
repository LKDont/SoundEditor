package com.lkdont.sound.player

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.util.Log
import com.lkdont.sound.edit.Decoder
import com.lkdont.sound.edit.Info
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 播放线程
 *
 * Created by kidonliang on 2018/4/6.
 */
internal class PlayTask(val url: String, private val handler: Handler) : Runnable {

    companion object {
        private const val TAG = "PlayTask"
        private const val DOWNLOAD_TIME_OUT = 5000

        const val MSG_WHAT_TIME = 1
        const val MSG_WHAT_STATUS = 2
        const val MSG_WHAT_ERROR = 3

        const val MSG_KEY_CURTIME = "cur_time"
        const val MSG_KEY_TOTAL_TIME = "total_time"
        const val MSG_KEY_ERROR = "error"
    }

    private fun postTimeEvent(curTimeMs: Long, totalTimeMs: Long) {
        val msg = handler.obtainMessage(MSG_WHAT_TIME)
        val data = Bundle()
        data.putLong(MSG_KEY_CURTIME, curTimeMs)
        data.putLong(MSG_KEY_TOTAL_TIME, totalTimeMs)
        msg.data = data
        handler.sendMessage(msg)
    }

    private fun postStatusEvent(status: Status) {
        val msg = handler.obtainMessage(MSG_WHAT_STATUS)
        msg.obj = status
        handler.sendMessage(msg)
    }

    private fun postErrorEvent(errCode: Int, err: String) {
        val msg = handler.obtainMessage(MSG_WHAT_ERROR)
        msg.arg1 = errCode
        val data = Bundle()
        data.putString(MSG_KEY_ERROR, err)
        msg.data = data
        handler.sendMessage(msg)
    }

    @Volatile
    private var mStatus: Status = Status.UNDEFINED

    fun getStatus(): Status = mStatus

    fun setStatus(status: Status) {
        mStatus = status
        postStatusEvent(status)
    }

    override fun run() {
        // 设置线程权限
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var decoder: Decoder? = null
        var audioTrack: AudioTrack? = null

        var ret: Int

        try {

            // 获取声音信息
            val info = Info(url)
            ret = info.load()
            if (ret != 0) {
                Log.e(TAG, "$url : load info fail.")
                postErrorEvent(SoundPlayer.ERROR_CODE_GET_INFO_FAIL, "SoundPlayer : load info fail.")
                return
            }

            if (mStatus == Status.STOPPED) {
                Log.e(TAG, "$url : illegal status($mStatus) when preparing.")
                return
            }

            // 初始化输入流
            if (info.isNetwork) {
                connection = URL(info.url).openConnection() as HttpURLConnection
                connection.readTimeout = DOWNLOAD_TIME_OUT
                connection.connectTimeout = DOWNLOAD_TIME_OUT
                connection.connect()
                inputStream = connection.inputStream
            } else {
                inputStream = FileInputStream(info.url)
            }

            // 初始化解码器
            decoder = Decoder()
            ret = decoder.init(info.codec)
            if (ret != 0) {
                Log.e(TAG, "$url : init decoder fail.")
                postErrorEvent(SoundPlayer.ERROR_CODE_INIT_FAIL, "SoundPlayer : init decoder fail.")
                return
            }
            Log.i(TAG, "info = $info")

            // 初始化播放器
            val channels = when (info.channels) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> {
                    Log.e(TAG, "$url : unsupported channels : " + info.channels)
                    postErrorEvent(SoundPlayer.ERROR_CODE_INIT_FAIL, "SoundPlayer : unsupported channels.")
                    return
                }
            }
            audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    info.sampleRate,
                    channels,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(info.sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT),
                    AudioTrack.MODE_STREAM
            )
            audioTrack.play()

            // 将状态设置为播放中
            if (mStatus != Status.PAUSING) {
                mStatus = Status.PLAYING
                postStatusEvent(Status.PLAYING)
            }

            var read: Int
            val inBuf = ByteArray(2048)

            var decodedSize: Int
            var outBuf = ByteArray(0)
            var nbSamples = 0

            val durationInMs = info.duration / 1000
            var timeInMs = 0L
            var lastTimeInMs = 0L

            while (mStatus != Status.STOPPED) {

                // 暂停
                if (mStatus == Status.PAUSING) continue

                // 读取数据
                read = inputStream!!.read(inBuf, 0, 2048)
                if (read >= 0) {
                    ret = decoder.feedData(inBuf, read)
                    if (ret != 0) {
                        Log.e(TAG, "$url : error while feeding data : ret = $ret")
                        postErrorEvent(SoundPlayer.ERROR_CODE_DECODING, "SoundPlayer : error while feeding data.")
                        return
                    }
                    decodedSize = decoder.decodedSize
                    if (decodedSize < 0) {
                        Log.e(TAG, "$url : error while decoding : decodedSize = $decodedSize")
                        postErrorEvent(SoundPlayer.ERROR_CODE_DECODING, "SoundPlayer : error while decoding.")
                        return
                    }
                    if (decodedSize > outBuf.size) {
                        // 重新分配outBuf
                        outBuf = ByteArray(decodedSize)
                    }
                    decodedSize = decoder.receiveDecodedData(outBuf)
                    audioTrack.write(outBuf, 0, decodedSize)

                    // 更新时间
                    nbSamples += (decodedSize / 2) / info.channels
                    timeInMs = (nbSamples * 1000L) / info.sampleRate
                    if (Math.abs(timeInMs - lastTimeInMs) > 500) {
                        postTimeEvent(timeInMs, durationInMs)
                        lastTimeInMs = timeInMs
                    }

                } else {
                    // 没有更多数据
                    return
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            postErrorEvent(SoundPlayer.ERROR_CODE_OTHER, "SoundPlayer : " + e.message)
        } finally {
            Log.d(TAG, "stopped.")
            // 将状态设置成停止
            mStatus = Status.STOPPED
            postStatusEvent(Status.STOPPED)

            // 关闭播放器
            try {
                // 立即停止播放器
                audioTrack?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioTrack?.flush()
            try {
                audioTrack?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioTrack?.release()

            // 关闭解码器
            decoder?.close()

            // 关闭连接
            connection?.disconnect()

            // 关闭输入流
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}