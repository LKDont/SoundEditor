package com.lkdont.soundeditor.ui

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lkdont.sound.edit.Codec
import com.lkdont.sound.edit.Convertor
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
class ConvertorFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.resample_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // init ui
        input_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.pcm")
        in_channel_layouts_sp.setSelection(1)
        in_sample_rates_sp.setSelection(5)
        in_sample_fmts_sp.setSelection(1)

        output_et.setText("/sdcard/sound_editor/SuperMalioRemix-Mono-Original-32000.pcm")
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
            ConvertorTask(this,
                    getChannelLayout(in_channel_layouts_sp.selectedItemPosition)
                            ?: return@setOnClickListener,
                    getChannelLayout(out_channel_layouts_sp.selectedItemPosition)
                            ?: return@setOnClickListener,

                    getSampleRate(in_sample_rates_sp.selectedItemPosition),
                    getSampleRate(out_sample_rates_sp.selectedItemPosition),

                    getSampleFmt(in_sample_fmts_sp.selectedItemPosition)
                            ?: return@setOnClickListener,
                    getSampleFmt(out_sample_fmts_sp.selectedItemPosition)
                            ?: return@setOnClickListener)
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

    private fun getChannelLayout(pos: Int): Codec.ChannelLayout? {
        return when (pos) {
            0 -> Codec.ChannelLayout.MONO
            1 -> Codec.ChannelLayout.STEREO
            else -> null
        }
    }

    private fun getSampleFmt(pos: Int): Codec.SampleFormat? {
        return when (pos) {
            0 -> Codec.SampleFormat.AV_SAMPLE_FMT_U8
            1 -> Codec.SampleFormat.AV_SAMPLE_FMT_S16
            2 -> Codec.SampleFormat.AV_SAMPLE_FMT_S32
            3 -> Codec.SampleFormat.AV_SAMPLE_FMT_FLT
            4 -> Codec.SampleFormat.AV_SAMPLE_FMT_DBL
            else -> null
        }
    }

    /**
     * 重采样异步线程
     *
     * Created by kidonliang on 2018/2/19.
     */
    class ConvertorTask(page: ConvertorFrag,
                        val in_ch_layout: Codec.ChannelLayout, val out_ch_layout: Codec.ChannelLayout,
                        val in_rate: Int, val out_rate: Int,
                        val in_sample_fmt: Codec.SampleFormat, val out_sample_fmt: Codec.SampleFormat)
        : AsyncTask<File, Void, Boolean>() {

        private val TAG = "ConvertorTask"

        private val reference = WeakReference<ConvertorFrag>(page)

        override fun onPreExecute() {
            val page = reference.get() ?: return
            page.showWaitingDialog()
        }

        private fun getBufferSize(sample_fmt: Codec.SampleFormat,
                                  ch_layout: Codec.ChannelLayout,
                                  nb_samples: Int): Int {

            val layout = when (ch_layout) {
                Codec.ChannelLayout.MONO -> 1
                Codec.ChannelLayout.STEREO -> 2
                else -> 0
            }

            return when (sample_fmt) {
                Codec.SampleFormat.AV_SAMPLE_FMT_U8 -> layout * nb_samples
                Codec.SampleFormat.AV_SAMPLE_FMT_S16 -> layout * 2 * nb_samples
                Codec.SampleFormat.AV_SAMPLE_FMT_S32 -> layout * 4 * nb_samples
                Codec.SampleFormat.AV_SAMPLE_FMT_FLT -> layout * 4 * nb_samples
                Codec.SampleFormat.AV_SAMPLE_FMT_DBL -> layout * 8 * nb_samples
                else -> 0
            }
        }

        override fun doInBackground(vararg params: File?): Boolean {
            Log.w(TAG, "Test Start...")

            val inFile = params[0]
            var inputStream: FileInputStream? = null
            val outFile = params[1]
            var outputStream: FileOutputStream? = null

            var convertor = Convertor()

            try {
                inputStream = FileInputStream(inFile)
                outputStream = FileOutputStream(outFile);

                val in_nb_samples = 1024
                val in_buf_size = getBufferSize(in_sample_fmt,
                        in_ch_layout,
                        in_nb_samples)
                val in_buffer = ByteArray(in_buf_size)

                var out_buffer = ByteArray(0)

                var ret = convertor.init(in_ch_layout, in_sample_fmt, in_rate,
                        out_ch_layout, out_sample_fmt, out_rate);

                if (ret != 0) {
                    Log.e(TAG, "创建Convertor失败")
                    return false
                }

                var convertedSize = 0
                var read = inputStream.read(in_buffer, 0, in_buf_size)
                while (read > 0) {

                    ret = convertor.feedData(in_buffer, read)
                    if (ret < 0) {
                        Log.e(TAG, "feedData error")
                        return false
                    }

                    convertedSize = convertor.convertedSize
                    if (convertedSize > 0) {
                        if ((out_buffer.size) < convertedSize) {
                            // 重新分配输出buffer
                            Log.i(TAG, "重新分配输出buffer $convertedSize")
                            out_buffer = ByteArray(convertedSize)
                        }
                        convertedSize = convertor.receiveConvertedData(out_buffer)
                        // write file
                        outputStream.write(out_buffer, 0, convertedSize)
                    }
                    // 读取下一个buffer
                    read = inputStream.read(in_buffer, 0, in_buf_size)
                }
                convertor.flush()
                convertedSize = convertor.convertedSize
                if (convertedSize > 0) {
                    Log.i(TAG, "after flush convertor.convertedSize = $convertedSize")
                    convertedSize = convertor.receiveConvertedData(out_buffer)
                    // write file
                    outputStream.write(out_buffer, 0, convertedSize)
                }

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                // 关闭
                convertor.close()

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
                Log.w(TAG, "Test End...")
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
