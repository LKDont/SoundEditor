package com.lkdont.soundeditor

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.lkdont.sound.edit.Resampler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

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
            0 -> Resampler.AV_CH_LAYOUT_MONO
            1 -> Resampler.AV_CH_LAYOUT_STEREO
            else -> 0
        }
    }

    private fun getSampleFmt(pos: Int): Int {
        return when (pos) {
            0 -> Resampler.AV_SAMPLE_FMT_U8
            1 -> Resampler.AV_SAMPLE_FMT_S16
            2 -> Resampler.AV_SAMPLE_FMT_S32
            3 -> Resampler.AV_SAMPLE_FMT_FLT
            4 -> Resampler.AV_SAMPLE_FMT_DBL
            else -> 0
        }
    }

    @SuppressLint("SdCardPath", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                Toast.makeText(this, "输入文件不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val outputFile = File(output_et.text.toString())
            if (outputFile.parentFile?.exists() != true) {
                Toast.makeText(this, "输出目录不存在", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ResampleTask(this,

                    getChannelLayout(in_channel_layouts_sp.selectedItemPosition),
                    getChannelLayout(out_channel_layouts_sp.selectedItemPosition),

                    getSampleRate(in_sample_rates_sp.selectedItemPosition),
                    getSampleRate(out_sample_rates_sp.selectedItemPosition),

                    getSampleFmt(in_sample_fmts_sp.selectedItemPosition),
                    getSampleFmt(out_sample_fmts_sp.selectedItemPosition)).execute(inputFile, outputFile)
        }

    }

    private var progressDialog: ProgressDialog? = null

    fun showWaitingDialog() {
        if (progressDialog?.isShowing == true) {
            hideWaitingDialog()
        }
        progressDialog = ProgressDialog.show(this, "请稍候...", "正在转换中")
    }

    fun hideWaitingDialog() {
        progressDialog?.hide()
        progressDialog = null
    }
}
