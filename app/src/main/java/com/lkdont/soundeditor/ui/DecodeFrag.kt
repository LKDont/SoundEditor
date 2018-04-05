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
import com.lkdont.sound.edit.Codec
import com.lkdont.sound.edit.Decoder
import com.lkdont.soundeditor.R
import kotlinx.android.synthetic.main.decode_audio_frag.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * Decode Audio Page
 *
 * Created by kidonliang on 2018/3/4.
 */
class DecodeFrag : Fragment() {

    private val TAG = "DecodeFrag"

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.decode_audio_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.mp3")
        output_et.setText("/sdcard/sound_editor/SuperMalioRemix-Stereo-Original.pcm")
        decoder_sp.setSelection(0)

        decode_btn.setOnClickListener {
            val inputFile = File(input_et.text.toString())
            if (!inputFile.exists()) {
                Toast.makeText(activity, "找不到输入文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val outputFile = File(output_et.text.toString())
            val decoder = if (decoder_sp.selectedItemPosition == 0) Codec.DECODER_MP3 else Codec.DECODER_AAC
            DecoderTask(this, decoder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputFile, outputFile)

//            val inFile_1 = File("/sdcard/sound_editor/SuperMalioRemix-Stereo.aac")
//            val outFile_1 = File("/sdcard/sound_editor/SuperMalioRemix-Stereo-aac.pcm")
//            DecoderTask(this, Codec.DECODER_AAC).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inFile_1, outFile_1)
//
//            val inFile_2 = File("/sdcard/sound_editor/SuperMalioRemix-Stereo.mp3")
//            val outFile_2 = File("/sdcard/sound_editor/SuperMalioRemix-Stereo-mp3.pcm")
//            DecoderTask(this, Codec.DECODER_MP3).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inFile_2, outFile_2)
        }
    }

    private var progressDialog: ProgressDialog? = null

    private fun showWaitingDialog() {
        if (progressDialog?.isShowing == true) {
            hideWaitingDialog()
        }
        progressDialog = ProgressDialog.show(context, "请稍候...", "正在解码中")
    }

    private fun hideWaitingDialog() {
        progressDialog?.hide()
        progressDialog = null
    }

    private class DecoderTask(page: DecodeFrag, val codecName: String) : AsyncTask<File, Void, Boolean>() {

        private val TAG = "DecoderTask"

        private val reference = WeakReference<DecodeFrag>(page)

        override fun onPreExecute() {
            val page = reference.get() ?: return
            page.showWaitingDialog()
            Log.d(TAG, "$codecName : decode started.")
        }

        override fun doInBackground(vararg params: File): Boolean {

            val bufferSize = 2048
            var ret: Int
            var decodedSize: Int
            var readDecodedSize: Int

            // init
            val decoder = Decoder()
            ret = decoder.init(codecName)
            if (ret != 0) {
                Log.e(TAG, "decoder not found.")
                return false
            }

            var inputStream: FileInputStream? = null
            var outputStream: FileOutputStream? = null

            try {

                inputStream = FileInputStream(params[0])
                outputStream = FileOutputStream(params[1])
                val inBuffer = ByteArray(bufferSize)
                var outBuffer: ByteArray? = null

                // read data
                var read = inputStream.read(inBuffer, 0, bufferSize)

                while (read > 0) {
                    // feed data
                    ret = decoder.feedData(inBuffer, read)
                    if (ret < 0) {
                        Log.e(TAG, "error while decoding.")
                        return false
                    }

                    // receive data
                    decodedSize = decoder.decodedSize
                    if (decodedSize > 0) {
                        if ((outBuffer?.size ?: -1) < decodedSize) {
                            // 重新分配输出buffer
                            Log.i(TAG, "重新分配输出buffer $decodedSize")
                            outBuffer = ByteArray(decodedSize)
                        }
                        readDecodedSize = decoder.receiveDecodedData(outBuffer)
                        // write file
                        outputStream.write(outBuffer, 0, readDecodedSize)
                    }

                    // read new data
                    read = inputStream.read(inBuffer, 0, bufferSize)
                }

                decoder.flush()
                decodedSize = decoder.decodedSize
                if (decodedSize > 0) {
                    Log.i(TAG, "after flush decoder.decodedSize = $decodedSize")
                    readDecodedSize = decoder.receiveDecodedData(outBuffer)
                    // write file
                    outputStream.write(outBuffer, 0, readDecodedSize)
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
                decoder.close()
            }
        }

        override fun onPostExecute(result: Boolean) {
            val page = reference.get() ?: return
            page.hideWaitingDialog()
            if (result) {
                Toast.makeText(page.context, "解码成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(page.context, "解码失败", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "$codecName : decode finished.")
        }
    }
}
