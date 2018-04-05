package com.lkdont.sound.edit;

import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 重采样处理器
 * <p>
 * Created by kidonliang on 2018/2/18.
 */

public class JniResampler {

    /**
     * FFmpeg中支持很多格式参数，这里只接受常用的格式参数。
     */

    public static final int AV_SAMPLE_FMT_U8 = 1;          ///< unsigned 8 bits
    public static final int AV_SAMPLE_FMT_S16 = 2;         ///< signed 16 bits
    public static final int AV_SAMPLE_FMT_S32 = 3;         ///< signed 32 bits
    public static final int AV_SAMPLE_FMT_FLT = 4;         ///< float
    public static final int AV_SAMPLE_FMT_DBL = 5;         ///< double

    public static final int AV_CH_LAYOUT_MONO = 1;      // 单声道
    public static final int AV_CH_LAYOUT_STEREO = 2;    // 立体声

    private static JniResampler mResampler;

    private JniResampler() {
    }

    private static native int initResampler(int in_nb_samples,
                                            int in_ch_layout, int out_ch_layout,
                                            int in_rate, int out_rate,
                                            int in_sample_fmt, int out_sample_fmt);

    /**
     * 创建重采样器
     *
     * @param in_nb_samples  每次输入声音样本数
     * @param in_ch_layout   输入声道
     * @param out_ch_layout  输出声道
     * @param in_rate        输入采样率
     * @param out_rate       输出采样率
     * @param in_sample_fmt  输入采样格式
     * @param out_sample_fmt 输出采样格式
     * @return 若返回结果为null，则表示创建失败。
     */
    @Nullable
    public static JniResampler createResampler(int in_nb_samples,
                                               int in_ch_layout, int out_ch_layout,
                                               int in_rate, int out_rate,
                                               int in_sample_fmt, int out_sample_fmt) {
        if (mResampler != null) {
            Log.e("JniResampler", "创建失败，已有一个重采样器在运行。");
            return null;
        }
        // 创建
        if (initResampler(in_nb_samples, in_ch_layout, out_ch_layout, in_rate, out_rate, in_sample_fmt, out_sample_fmt) != 0) {
            Log.e("JniResampler", "初始化重采样器失败。");
            return null;
        }
        mResampler = new JniResampler();
        return mResampler;
    }

    /**
     * @return 正在运行的重采样器，若返回结果为null，则表示没有。
     */
    public static JniResampler getRunningResampler() {
        return mResampler;
    }

    /**
     * @return 输出音频采样数，根据这个值分配输出buffer。
     */
    public native int computeOutputSamplesNumber();

    /**
     * 重采样
     *
     * @param input  输入数据
     * @param inLen  输入数据长度
     * @param output 输出数据
     * @return 输出数据长度
     */
    public native int resample(byte[] input, int inLen,
                               byte[] output);

    private native void closeResampler();

    /**
     * 关闭重采样器
     */
    public void close() {
        closeResampler();
        mResampler = null;
    }
}
