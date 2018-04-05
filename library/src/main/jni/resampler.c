//
// Created by Kidon Liang on 2018/2/18.
//
#include <jni.h>
#include "android_log.h"

#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>

struct SwrContext *swr_ctx = NULL;
uint8_t **src_data = NULL, **dst_data = NULL;
int src_nb_channels = 0, dst_nb_channels = 0;
int src_linesize, dst_linesize;
int src_nb_samples, dst_nb_samples, max_dst_nb_samples;

int64_t src_ch_layout;
int64_t dst_ch_layout;
int src_rate;
int dst_rate;
enum AVSampleFormat src_sample_fmt;
enum AVSampleFormat dst_sample_fmt;

/**
 * 关闭重采样器
 */
void close() {
    if (src_data)
        av_freep(&src_data[0]);
    av_freep(&src_data);
    if (dst_data)
        av_freep(&dst_data[0]);
    av_freep(&dst_data);
    swr_free(&swr_ctx);
    swr_ctx = NULL;
}

int64_t get_channel_layout(int ch_type) {
    switch (ch_type) {
        case 1:
            return AV_CH_LAYOUT_MONO;

        case 2:
            return AV_CH_LAYOUT_STEREO;

        default:
            return 0;
    }
}

enum AVSampleFormat get_sample_fmt(int fmt_type) {
    switch (fmt_type) {
        case 1:
            return AV_SAMPLE_FMT_U8;
        case 2:
            return AV_SAMPLE_FMT_S16;
        case 3:
            return AV_SAMPLE_FMT_S32;
        case 4:
            return AV_SAMPLE_FMT_FLT;
        case 5:
            return AV_SAMPLE_FMT_DBL;
        default:
            return AV_SAMPLE_FMT_NONE;
    }
}

/**
 * 初始化
 */
JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_JniResampler_initResampler(JNIEnv *env, jclass type, jint in_nb_samples,
                                                   jint in_ch_layout, jint out_ch_layout,
                                                   jint in_rate, jint out_rate,
                                                   jint in_sample_fmt, jint out_sample_fmt) {
    swr_ctx = swr_alloc();
    if (!swr_ctx) {
        LOGE("Could not allocate resampler context\n");
        close();
        return 1;
    }

    src_nb_samples = in_nb_samples;
    src_ch_layout = get_channel_layout(in_ch_layout);
    dst_ch_layout = get_channel_layout(out_ch_layout);
    src_rate = in_rate;
    dst_rate = out_rate;
    src_sample_fmt = get_sample_fmt(in_sample_fmt);
    dst_sample_fmt = get_sample_fmt(out_sample_fmt);

    /* set options */
    av_opt_set_int(swr_ctx, "in_channel_layout", src_ch_layout, 0);
    av_opt_set_int(swr_ctx, "in_sample_rate", src_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt", src_sample_fmt, 0);
    av_opt_set_int(swr_ctx, "out_channel_layout", dst_ch_layout, 0);
    av_opt_set_int(swr_ctx, "out_sample_rate", dst_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt", dst_sample_fmt, 0);

    /* initialize the resampling context */
    if (swr_init(swr_ctx) < 0) {
        LOGE("Failed to initialize the resampling context\n");
        close();
        return 1;
    }

    /* allocate source and destination samples buffers */
    src_nb_channels = av_get_channel_layout_nb_channels(src_ch_layout);
    int ret = av_samples_alloc_array_and_samples(&src_data, &src_linesize, src_nb_channels,
                                                 src_nb_samples, src_sample_fmt, 0);
    if (ret < 0) {
        LOGE("Could not allocate source samples\n");
        close();
        return 1;
    }

    /* compute the number of converted samples: buffering is avoided
     * ensuring that the output buffer will contain at least all the
     * converted input samples */
    max_dst_nb_samples = dst_nb_samples =
            av_rescale_rnd(src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);
    /* buffer is going to be directly written to a rawaudio file, no alignment */
    dst_nb_channels = av_get_channel_layout_nb_channels(dst_ch_layout);
    ret = av_samples_alloc_array_and_samples(&dst_data, &dst_linesize, dst_nb_channels,
                                             dst_nb_samples, dst_sample_fmt, 0);
    if (ret < 0) {
        LOGE("Could not allocate destination samples\n");
        close();
        return 1;
    }

    return 0;
}

int compute_destination_nb_samples() {
    /* compute destination number of samples */
    dst_nb_samples = av_rescale_rnd(swr_get_delay(swr_ctx, src_rate) +
                                    src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);
    if (dst_nb_samples > max_dst_nb_samples) {
        av_freep(&dst_data[0]);
        if (av_samples_alloc(dst_data, &dst_linesize, dst_nb_channels, dst_nb_samples,
                             dst_sample_fmt, 1) < 0) {
            LOGE("resample: Error while av_samples_alloc\n");
            return -1;
        }
        max_dst_nb_samples = dst_nb_samples;
    }
    return dst_nb_samples;
}

/**
 * 重采样
 */
JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_JniResampler_resample(JNIEnv *env, jobject instance, jbyteArray input_,
                                              jint inLen, jbyteArray output_) {

    if (!swr_ctx) {
        LOGE("SwrContext还没有初始化\n");
        return -1;
    }

    jbyte *input = (*env)->GetByteArrayElements(env, input_, NULL);
    jbyte *output = (*env)->GetByteArrayElements(env, output_, NULL);

    // 将输入数据复制到src_data中
    memcpy(src_data[0], input, inLen);

    /* convert to destination format */
    int ret = swr_convert(swr_ctx, dst_data, dst_nb_samples, (const uint8_t **) src_data,
                      src_nb_samples);
    if (ret < 0) {
        LOGE("resample: Error while swr_convert\n");
        return -1;
    }
    ret = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                     ret, dst_sample_fmt, 1);

    // 将结果复制到output中
    memcpy(output, dst_data[0], ret);

    (*env)->ReleaseByteArrayElements(env, input_, input, 0);
    (*env)->ReleaseByteArrayElements(env, output_, output, 0);

    return ret;
}

/**
 * 关闭重采样器
 */
JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_JniResampler_closeResampler(JNIEnv *env, jobject instance) {
    close();
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_JniResampler_computeOutputSamplesNumber(JNIEnv *env, jobject instance) {
    return compute_destination_nb_samples();
}