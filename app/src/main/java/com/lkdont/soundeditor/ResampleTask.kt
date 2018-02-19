package com.lkdont.soundeditor

import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.lkdont.sound.edit.Resampler
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * 重采样异步线程
 *
 * Created by kidonliang on 2018/2/19.
 */
class ResampleTask(act: MainActivity,
                   val in_ch_layout: Int, val out_ch_layout: Int,
                   val in_rate: Int, val out_rate: Int,
                   val in_sample_fmt: Int, val out_sample_fmt: Int) : AsyncTask<File, Void, Boolean>() {

    private val reference = WeakReference<MainActivity>(act)

    override fun onPreExecute() {
        val act = reference.get() ?: return
        act.showWaitingDialog()
    }

    private fun getBufferSize(sample_fmt: Int,
                              ch_layout: Int,
                              nb_samples: Int): Int {

        val layout = when (ch_layout) {
            Resampler.AV_CH_LAYOUT_MONO -> 1
            Resampler.AV_CH_LAYOUT_STEREO -> 2
            else -> 0
        }

        return when (sample_fmt) {
            Resampler.AV_SAMPLE_FMT_U8 -> layout * nb_samples
            Resampler.AV_SAMPLE_FMT_S16 -> layout * 2 * nb_samples
            Resampler.AV_SAMPLE_FMT_S32 -> layout * 4 * nb_samples
            Resampler.AV_SAMPLE_FMT_FLT -> layout * 4 * nb_samples
            Resampler.AV_SAMPLE_FMT_DBL -> layout * 8 * nb_samples
            else -> 0
        }
    }

    override fun doInBackground(vararg params: File?): Boolean {
        Log.w("MainActivity", "Test Start...")

        val inFile = params[0]
        var inputStream: FileInputStream? = null
        val outFile = params[1]
        var outputStream: FileOutputStream? = null

        var resampler: Resampler? = null

        try {
            inputStream = FileInputStream(inFile)
            outputStream = FileOutputStream(outFile);

            val in_nb_samples = 1024
            val in_buf_size = getBufferSize(in_sample_fmt,
                    in_ch_layout,
                    in_nb_samples)
            val in_buffer = ByteArray(in_buf_size)

            var out_buffer = ByteArray(0)

            resampler = Resampler.createResampler(in_nb_samples,
                    in_ch_layout, out_ch_layout,
                    in_rate, out_rate,
                    in_sample_fmt, out_sample_fmt)

            if (resampler == null) {
                Log.e("MainActivity", "创建Resampler失败")
                return false
            }

            var out_nb_samples: Int
            var output_size: Int
            var read = inputStream.read(in_buffer, 0, in_buf_size)
            while (read > 0) {
                out_nb_samples = resampler.computeOutputSamplesNumber()
                if (out_nb_samples < 0) {
                    Log.e("MainActivity", "computeOutputSamplesNumber失败")
                    return false
                }
                if (getBufferSize(out_sample_fmt,
                                out_ch_layout,
                                out_nb_samples) > out_buffer.size) {
                    // 重新分配buffer
                    out_buffer = ByteArray(getBufferSize(out_sample_fmt,
                            out_ch_layout, out_nb_samples))
                }
                // 重采样
                output_size = resampler.resample(in_buffer, read, out_buffer)
                if (output_size < 0) {
                    Log.e("MainActivity", "resample失败")
                    return false
                }
                // 写入文件
                outputStream.write(out_buffer, 0, output_size)
                // 读取下一个buffer
                read = inputStream.read(in_buffer, 0, in_buf_size)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            // 关闭
            resampler?.close()
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                outputStream?.flush()
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.w("MainActivity", "Test End...")
        }
    }

    override fun onPostExecute(result: Boolean) {
        val act = reference.get() ?: return
        act.hideWaitingDialog()
        if (result) {
            Toast.makeText(act, "转换成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(act, "转换失败", Toast.LENGTH_SHORT).show()
        }
    }
}