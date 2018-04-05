package com.lkdont.soundeditor.ui

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.lkdont.sound.edit.JniResampler
import com.lkdont.soundeditor.R
import kotlinx.android.synthetic.main.resample_frag.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Audio Resampling Page
 *
 * Created by kidonliang on 2018/3/4.
 */
class ResampleFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.resample_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // init ui
        input_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-44100.pcm")
        in_channel_layouts_sp.setSelection(1)
        in_sample_rates_sp.setSelection(5)
        in_sample_fmts_sp.setSelection(1)

        output_et.setText("/sdcard/sound_editor/out-SuperMalioRemix-Mono-32000.pcm")
        out_channel_layouts_sp.setSelection(0)
        out_sample_rates_sp.setSelection(4)
        out_sample_fmts_sp.setSelection(1)

        // event
        convert_btn.setOnClickListener {
            val inputFile = File(input_et.text.toString())
            if (!inputFile.exists()) {
                Toast.makeText(context, "输入文件不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val outputFile = File(output_et.text.toString())
            if (outputFile.parentFile?.exists() != true) {
                Toast.makeText(context, "输出目录不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ResampleTask(this,
                    getChannelLayout(in_channel_layouts_sp.selectedItemPosition),
                    getChannelLayout(out_channel_layouts_sp.selectedItemPosition),

                    getSampleRate(in_sample_rates_sp.selectedItemPosition),
                    getSampleRate(out_sample_rates_sp.selectedItemPosition),

                    getSampleFmt(in_sample_fmts_sp.selectedItemPosition),
                    getSampleFmt(out_sample_fmts_sp.selectedItemPosition))
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputFile, outputFile)
        }
    }

    private var progressDialog: ProgressDialog? = null

    private fun showWaitingDialog() {
        if (progressDialog?.isShowing == true) {
            hideWaitingDialog()
        }
        progressDialog = ProgressDialog.show(context, "请稍候...", "正在转换中")
    }

    private fun hideWaitingDialog() {
        progressDialog?.hide()
        progressDialog = null
    }

    //******************************** Data ********************************//

    private fun getSampleRate(pos: Int): Int {
        return when (pos) {
            0 -> 8000
            1 -> 11025
            2 -> 16000
            3 -> 22050
            4 -> 32000
            5 -> 44100
            6 -> 48000
            7 -> 88200
            8 -> 96000
            else -> 0
        }
    }

    private fun getChannelLayout(pos: Int): Int {
        return when (pos) {
            0 -> JniResampler.AV_CH_LAYOUT_MONO
            1 -> JniResampler.AV_CH_LAYOUT_STEREO
            else -> 0
        }
    }

    private fun getSampleFmt(pos: Int): Int {
        return when (pos) {
            0 -> JniResampler.AV_SAMPLE_FMT_U8
            1 -> JniResampler.AV_SAMPLE_FMT_S16
            2 -> JniResampler.AV_SAMPLE_FMT_S32
            3 -> JniResampler.AV_SAMPLE_FMT_FLT
            4 -> JniResampler.AV_SAMPLE_FMT_DBL
            else -> 0
        }
    }

    /**
     * 重采样异步线程
     *
     * Created by kidonliang on 2018/2/19.
     */
    class ResampleTask(page: ResampleFrag,
                       val in_ch_layout: Int, val out_ch_layout: Int,
                       val in_rate: Int, val out_rate: Int,
                       val in_sample_fmt: Int, val out_sample_fmt: Int) : AsyncTask<File, Void, Boolean>() {

        private val reference = WeakReference<ResampleFrag>(page)

        override fun onPreExecute() {
            val page = reference.get() ?: return
            page.showWaitingDialog()
        }

        private fun getBufferSize(sample_fmt: Int,
                                  ch_layout: Int,
                                  nb_samples: Int): Int {

            val layout = when (ch_layout) {
                JniResampler.AV_CH_LAYOUT_MONO -> 1
                JniResampler.AV_CH_LAYOUT_STEREO -> 2
                else -> 0
            }

            return when (sample_fmt) {
                JniResampler.AV_SAMPLE_FMT_U8 -> layout * nb_samples
                JniResampler.AV_SAMPLE_FMT_S16 -> layout * 2 * nb_samples
                JniResampler.AV_SAMPLE_FMT_S32 -> layout * 4 * nb_samples
                JniResampler.AV_SAMPLE_FMT_FLT -> layout * 4 * nb_samples
                JniResampler.AV_SAMPLE_FMT_DBL -> layout * 8 * nb_samples
                else -> 0
            }
        }

        override fun doInBackground(vararg params: File?): Boolean {
            Log.w("MainActivity", "Test Start...")

            val inFile = params[0]
            var inputStream: FileInputStream? = null
            val outFile = params[1]
            var outputStream: FileOutputStream? = null

            var resampler: JniResampler? = null

            try {
                inputStream = FileInputStream(inFile)
                outputStream = FileOutputStream(outFile);

                val in_nb_samples = 1024
                val in_buf_size = getBufferSize(in_sample_fmt,
                        in_ch_layout,
                        in_nb_samples)
                val in_buffer = ByteArray(in_buf_size)

                var out_buffer = ByteArray(0)

                resampler = JniResampler.createResampler(in_nb_samples,
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
            val page = reference.get() ?: return
            page.hideWaitingDialog()
            if (result) {
                Toast.makeText(page.context, "转换成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(page.context, "转换失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
