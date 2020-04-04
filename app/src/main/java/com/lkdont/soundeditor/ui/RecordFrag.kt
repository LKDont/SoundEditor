package com.lkdont.soundeditor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.lkdont.soundeditor.R
import com.lkdont.soundeditor.sound.MyPlayer
import com.lkdont.soundeditor.sound.MyRecorder
import kotlinx.android.synthetic.main.record_frag.*
import java.io.File

/**
 * 录音分页
 *
 * Created by kidonliang on 2018/4/3.
 */
class RecordFrag : Fragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.record_frag, container, false)
        return view
    }

    private var recordPcmPath: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dir = context?.externalCacheDir
        if (dir != null) {
            recordPcmPath = File(dir, "test_record.pcm").absolutePath
        }

        record_btn.setOnClickListener(this)
        play_btn.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("RecordFrag", "onDestroy")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("RecordFrag", "onRequestPermissionsResult")
    }

    private var myRecorder: MyRecorder? = null

    override fun onClick(v: View?) {

        when (v) {
            record_btn -> {
                if (myRecorder != null) {
                    // stop
                    record_btn.text = "开始录音"
                    myRecorder?.stopRecord()
                    myRecorder = null
                } else {
                    // start
                    if (PackageManager.PERMISSION_GRANTED !=
                            ContextCompat.checkSelfPermission(context
                                    ?: return, Manifest.permission.RECORD_AUDIO)) {
                        // request permission
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                        return
                    }
                    if (recordPcmPath == null) return
                    record_btn.text = "停止录音"
                    myRecorder = MyRecorder()
                    myRecorder?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recordPcmPath)
                }
            }

            play_btn -> {
                if (recordPcmPath == null) return
                MyPlayer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recordPcmPath)
            }
        }

    }

}
