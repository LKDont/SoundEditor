package com.lkdont.sound.edit;

import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 解码器
 * <p>
 * Created by kidonliang on 2018/3/4.
 */

public class Decoder {

    public static native void test();

    private static Decoder mDecoder;

    private Decoder() {
    }

    @Nullable
    public static Decoder createDecoder() {
        if (mDecoder != null) {
            Log.e("Decoder", "创建失败，已有一个解码器在运行。");
            return null;
        }
        if (initDecoder() != 0) {
            Log.e("Decoder", "初始化解码器失败。");
            return null;
        }
        mDecoder = new Decoder();
        return mDecoder;
    }

    private static native int initDecoder();

    @Nullable
    public static Decoder getRunningDecoder() {
        return mDecoder;
    }

    private static native void closeDecoder();

    public void close() {
        closeDecoder();
        mDecoder = null;
    }
}
