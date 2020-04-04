package com.lkdont.soundeditor.ui

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.lkdont.sound.edit.Codec
import com.lkdont.sound.edit.Encoder
import com.lkdont.soundeditor.R
import kotlinx.android.synthetic.main.encoder_frag.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * 编码页面
 */
class EncoderFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.encoder_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.pcm")
        in_channel_layouts_sp.setSelection(1)
        in_sample_rates_sp.setSelection(5)
        in_sample_fmts_sp.setSelection(1)
        encoder_sp.setSelection(0)

        output_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-encode.mp3")

        encode_btn.setOnClickListener {

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

            val sampleRate = getSampleRate(in_sample_rates_sp.selectedItemPosition)
            val channel = getChannelLayout(in_channel_layouts_sp.selectedItemPosition)
                    ?: return@setOnClickListener
            val sampleFormat = getSampleFmt(in_sample_fmts_sp.selectedItemPosition)
                    ?: return@setOnClickListener
            val codec = if (encoder_sp.selectedItemPosition == 0) Codec.ENCODER_MP3 else Codec.ENCODER_AAC

            EncodeTask(this, codec,
                    channel, sampleRate, sampleFormat)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputFile, outputFile)
        }
    }

    private var progressDialog: ProgressDialog? = null

    private fun showWaitingDialog() {
        if (progressDialog?.isShowing == true) {
            hideWaitingDialog()
        }
        progressDialog = ProgressDialog.show(context, "请稍候...", "正在编码中")
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

    private class EncodeTask(page: EncoderFrag, val codecName: String,
                             val channel: Codec.ChannelLayout, val sampleRate: Int,
                             val sampleFormat: Codec.SampleFormat) : AsyncTask<File, Void, Boolean>() {

        private val TAG = "EncodeTask"

        private val reference = WeakReference<EncoderFrag>(page)

        override fun onPreExecute() {
            super.onPreExecute()
            val page = reference.get() ?: return
            page.showWaitingDialog()
            Log.d(TAG, "$codecName : encode started.")
        }

        override fun doInBackground(vararg params: File?): Boolean {

            val bufferSize = 2048
            var encodedSize = 0

            val encoder = Encoder()
            var inputStream: FileInputStream? = null
            var outputStream: FileOutputStream? = null

            try {

                // 初始化
                var ret = encoder.init(codecName, channel, sampleRate, sampleFormat)
                if (ret != 0) {
                    Log.e(TAG, "encoder not found.")
                    return false
                }

                inputStream = FileInputStream(params[0])
                outputStream = FileOutputStream(params[1])
                val inBuffer = ByteArray(bufferSize)
                var outBuffer = ByteArray(0)

                // read data
                var read = inputStream.read(inBuffer, 0, bufferSize)
                while (read > 0) {
                    ret = encoder.feedData(inBuffer, read)
                    if (ret < 0) {
                        Log.e(TAG, "error while encoding.")
                        return false
                    }

                    encodedSize = encoder.encodedSize
                    if (encodedSize > 0) {
                        if (outBuffer.size < encodedSize) {
                            // 重新分配输出buffer
                            outBuffer = ByteArray(encodedSize)
                        }
                        encodedSize = encoder.receiveEncodedData(outBuffer)
                        outputStream.write(outBuffer, 0, encodedSize)
                    }
                    // read data
                    read = inputStream.read(inBuffer, 0, bufferSize)
                }

                // flush
                while (encoder.flush() > 0) {
                    encodedSize = encoder.encodedSize
                    if (encodedSize > 0) {
                        if (outBuffer.size < encodedSize) {
                            // 重新分配输出buffer
                            outBuffer = ByteArray(encodedSize)
                        }
                        encodedSize = encoder.receiveEncodedData(outBuffer)
                        outputStream.write(outBuffer, 0, encodedSize)
                    }
                }

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                // close
                try {
                    inputStream?.close()
                } catch (e: Exception) {
                }

                try {
                    outputStream?.close()
                } catch (e: Exception) {
                }
                encoder.close()
            }
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            val page = reference.get() ?: return
            page.hideWaitingDialog()
            if (result == true) {
                Toast.makeText(page.context, "编码成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(page.context, "编码失败", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "$codecName : encode finished.")
        }
    }
}