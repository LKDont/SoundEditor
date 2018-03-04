package com.lkdont.soundeditor.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lkdont.sound.edit.Decoder
import com.lkdont.soundeditor.R
import kotlinx.android.synthetic.main.decode_audio_frag.*

/**
 * Decode Audio Page
 *
 * Created by kidonliang on 2018/3/4.
 */
class DecodeFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.decode_audio_frag, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        test_btn.setOnClickListener {
            Decoder.test()
        }
    }
}
