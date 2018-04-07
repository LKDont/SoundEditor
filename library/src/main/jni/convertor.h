//
// Created by Kidon Liang on 2018/4/7.
//

#ifndef convertor_h
#define convertor_h

#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>

struct Convertor {
    struct SwrContext *swr_ctx;
    uint8_t **src_buffers;
    uint8_t **dst_buffers;

    enum AVSampleFormat src_sample_fmt;
    enum AVSampleFormat dst_sample_fmt;

    int src_sample_rate;
    int dst_sample_rate;

    int src_nb_samples;         // ffmpeg默认每次采样数为1024
    int dst_nb_samples;
    int max_dst_nb_samples;     // 用于记录最大的输出采样数，防止数组越界

    int src_linesize;
    int dst_linesize;

    int src_nb_channels;
    int src_nb_buffers;

    int dst_nb_channels;
    int dst_nb_buffers;

    int converted_size;
};

/**
 * 初始化
 *
 * @param src_ch_layout    输入声道类型
 * @param src_sample_fmt   输入采样格式
 * @param src_sample_rate  输入采样率
 * @param dst_ch_layout    输出声道类型
 * @param dst_sample_fmt   输出采样格式
 * @param dst_sample_rate  输出采样率
 **/
struct Convertor* convertor_init(int64_t src_ch_layout, enum AVSampleFormat src_sample_fmt, int src_sample_rate,
                   int64_t dst_ch_layout, enum AVSampleFormat dst_sample_fmt, int dst_sample_rate);

/**
 * 输入数据
 *
 * @param data 音频数据
 * @param len  数据长度
 **/
int convertor_feed_data(struct Convertor* convertor, uint8_t **data, int len);

/**
 * 获取已转换数据长度
 **/
int convertor_get_converted_size(struct Convertor* convertor);

/**
 * 接收已转换数据
 *
 * @param data 接收数据的数组
 **/
int convertor_receive_converted_data(struct Convertor* convertor, uint8_t **data);

/**
 * 排空所有数据
 **/
void convertor_flush(struct Convertor* convertor);

/**
 * 关闭转换器
 **/
void convertor_close(struct Convertor* convertor);

#endif /* convertor_h */