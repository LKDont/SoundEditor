//
//  convertor.c
//  SoundEditor
//
//  Created by Kidon Liang on 2018/4/1.
//  Copyright © 2018年 Kidon Liang. All rights reserved.
//

#include "convertor.h"
#include "android_log.h"

struct Convertor* convertor_init(int64_t src_ch_layout, enum AVSampleFormat src_sp_fmt, int src_sp_rate,
                   int64_t dst_ch_layout, enum AVSampleFormat dst_sp_fmt, int dst_sp_rate) {

    // malloc convertor
    struct Convertor *convertor = malloc(sizeof(struct Convertor));
    convertor->swr_ctx = NULL;
    convertor->src_buffers = NULL;
    convertor->dst_buffers = NULL;
    convertor->src_nb_samples = 1024;

    convertor->src_sample_fmt = src_sp_fmt;
    convertor->dst_sample_fmt = dst_sp_fmt;

    convertor->src_sample_rate = src_sp_rate;
    convertor->dst_sample_rate = dst_sp_rate;

    convertor->src_nb_channels = av_get_channel_layout_nb_channels(src_ch_layout);
    convertor->src_nb_buffers = av_sample_fmt_is_planar(convertor->src_sample_fmt) ? convertor->src_nb_channels : 1;

    convertor->dst_nb_channels = av_get_channel_layout_nb_channels(dst_ch_layout);
    convertor->dst_nb_buffers = av_sample_fmt_is_planar(convertor->dst_sample_fmt) ? convertor->dst_nb_channels : 1;

    // init
    convertor->swr_ctx = swr_alloc();
    if (!convertor->swr_ctx) {
        LOGE("can not alloc SwrContext.");
        convertor_close(convertor);
        return NULL;
    }

    /* set options */
    av_opt_set_int(convertor->swr_ctx, "in_channel_layout", src_ch_layout, 0);
    av_opt_set_int(convertor->swr_ctx, "in_sample_rate", convertor->src_sample_rate, 0);
    av_opt_set_sample_fmt(convertor->swr_ctx, "in_sample_fmt", convertor->src_sample_fmt, 0);

    av_opt_set_int(convertor->swr_ctx, "out_channel_layout", dst_ch_layout, 0);
    av_opt_set_int(convertor->swr_ctx, "out_sample_rate", convertor->dst_sample_rate, 0);
    av_opt_set_sample_fmt(convertor->swr_ctx, "out_sample_fmt", convertor->dst_sample_fmt, 0);

    /* initialize the resampling context */
    if (swr_init(convertor->swr_ctx) < 0) {
        LOGE("Failed to initialize the resampling context\n");
        convertor_close(convertor);
        return NULL;
    }

    // allocate samples buffers
    int tmp_ret = av_samples_alloc_array_and_samples(&convertor->src_buffers, &convertor->src_linesize, convertor->src_nb_channels,
                                                     convertor->src_nb_samples, convertor->src_sample_fmt, 0);
    if (tmp_ret < 0) {
        LOGE("Could not allocate source samples\n");
        convertor_close(convertor);
        return NULL;
    }
    convertor->max_dst_nb_samples = convertor->dst_nb_samples =
            (int) av_rescale_rnd(convertor->src_nb_samples, convertor->dst_sample_rate, convertor->src_sample_rate, AV_ROUND_UP);

    tmp_ret = av_samples_alloc_array_and_samples(&convertor->dst_buffers, &convertor->dst_linesize, convertor->dst_nb_channels,
                                                 convertor->dst_nb_samples, convertor->dst_sample_fmt, 0);
    if (tmp_ret < 0) {
        LOGE("Could not allocate destination samples\n");
        convertor_close(convertor);
        return NULL;
    }
    return convertor;
}

/**
 * 输入数据，需要确保每次输入1024个样本数据。
 **/
int convertor_feed_data(struct Convertor *convertor, uint8_t **data, int len) {
    int tmp_ret;
    // 填充数据
    for (int i = 0; i < convertor->src_nb_buffers; i++) {
        memcpy(convertor->src_buffers[i], data[i], len);
    }
    /* compute destination number of samples */
    convertor->dst_nb_samples = (int) av_rescale_rnd(
            swr_get_delay(convertor->swr_ctx, convertor->src_sample_rate) +
            convertor->src_nb_samples, convertor->dst_sample_rate, convertor->src_sample_rate,
            AV_ROUND_UP);
    if (convertor->dst_nb_samples > convertor->max_dst_nb_samples) {
        // 重新分配输出buffer
        if (convertor->dst_buffers) {
            av_freep(&convertor->dst_buffers[0]);
        }
        tmp_ret = av_samples_alloc(convertor->dst_buffers, &convertor->dst_linesize,
                                   convertor->dst_nb_channels,
                                   convertor->dst_nb_samples, convertor->dst_sample_fmt, 1);
        if (tmp_ret < 0) {
            LOGE("重新分配输出buffer失败\n");
            return -1;
        }
        convertor->max_dst_nb_samples = convertor->dst_nb_samples;
    }

    /* convert to destination format */
    tmp_ret = swr_convert(convertor->swr_ctx, convertor->dst_buffers, convertor->dst_nb_samples,
                          (const uint8_t **) convertor->src_buffers, convertor->src_nb_samples);
    if (tmp_ret < 0) {
        LOGE("Error while converting\n");
        return -2;
    }
    convertor->converted_size = av_samples_get_buffer_size(&convertor->dst_linesize,
                                                           convertor->dst_nb_channels,
                                                           tmp_ret, convertor->dst_sample_fmt, 1);
    if (convertor->converted_size < 0) {
        LOGE("Could not get sample buffer size\n");
        return -3;
    }
    return 0;
}

int convertor_get_converted_size(struct Convertor *convertor) {
    return convertor->converted_size;
}

int convertor_receive_converted_data(struct Convertor *convertor, uint8_t **data) {
    int tmp_ret = convertor->converted_size;
    for (int i = 0; i < convertor->dst_nb_buffers; i++) {
        memcpy(data[i], convertor->dst_buffers[i], convertor->converted_size);
    }
    convertor->converted_size = 0;
    return tmp_ret;
}

void convertor_flush(struct Convertor *convertor) {
    int tmp_ret = swr_convert(convertor->swr_ctx, convertor->dst_buffers, convertor->dst_nb_samples,
                              NULL, 0);
    if (tmp_ret < 0) {
        LOGE("Error while converting\n");
        return;
    }
    convertor->converted_size = av_samples_get_buffer_size(&convertor->dst_linesize,
                                                           convertor->dst_nb_channels,
                                                           tmp_ret, convertor->dst_sample_fmt, 1);
    if (convertor->converted_size < 0) {
        LOGE("Could not get sample buffer size\n");
    }
}

void convertor_close(struct Convertor *convertor) {
    if (convertor->src_buffers) {
        av_freep(&convertor->src_buffers[0]);
    }
    av_freep(&convertor->src_buffers);

    if (convertor->dst_buffers) {
        av_freep(&convertor->dst_buffers[0]);
    }
    av_freep(&convertor->dst_buffers);

    swr_free(&convertor->swr_ctx);
}
