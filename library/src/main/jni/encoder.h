//
// Created by Kidon Liang on 2018/4/16.
//

#ifndef SOUNDEDITOR_ENCODER_H
#define SOUNDEDITOR_ENCODER_H

#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"

struct Encoder {
    AVCodec *codec;
    AVCodecContext *codec_ctx;
    AVFrame *frame;
    AVPacket *pkt;

    // 输入参数
    uint64_t src_ch_layout;
    int src_sample_rate;
    enum AVSampleFormat src_sample_fmt;

    // 转换
    struct SwrContext *swr_ctx;
    AVFrame *converted_frame;

    // 输入
    int in_buffer_size;
    int in_buffer_end;
    uint8_t *in_buffer;

    // 输出
    int out_buffer_size;
    uint8_t *out_buffer;
    int encoded_data_size;
};

struct Encoder* encoder_init(char *codec_name,
                             uint64_t in_ch_layout, int in_sp_rate, enum AVSampleFormat in_sp_fmt);

//int encoder_alloc_input_buffer(struct Encoder* encoder, uint8_t **buffer);

int encoder_feed_data(struct Encoder* encoder, uint8_t *data, int len);

int encoder_get_encoded_size(struct Encoder* encoder);

int encoder_receive_encoded_data(struct Encoder* encoder, uint8_t *data);

int encoder_flush(struct Encoder* encoder);

void encoder_close(struct Encoder* encoder);

#endif //SOUNDEDITOR_ENCODER_H
