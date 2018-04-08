//
// Created by Kidon Liang on 2018/3/14.
//

#ifndef SOUNDEDITOR_DECODER_H
#define SOUNDEDITOR_DECODER_H

#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"

struct Decoder {
    AVCodec *codec;
    AVCodecContext *codec_ctx;
    AVCodecParserContext *parser;
    AVPacket *pkt;
    AVFrame *decoded_frame;

    // 输出buffer数组大小
    int out_buffer_size;
    // 输出buffer数组
    uint8_t *out_buffer;

    int decoded_data_size;

    // 转换
    struct SwrContext *swr_ctx;
    AVFrame *converted_frame;
    uint64_t dst_ch_layout;
    int dst_sample_rate;
    enum AVSampleFormat dst_sample_fmt;
    int converted_remaining_data_size;

    // 有一些mp3文件带有ID3头部，需要把头部这部分数据跳过
    int header_size;
    uint8_t *header_buffer;
    int header_buffer_size;
    int header_buffer_end;
};

/**
 * 初始化
 *
 * @param codec_name 解码器名字
 * @return 解码器结构体
 **/
struct Decoder* decoder_init(char *codec_name);

/**
 * 初始化
 *
 * @param codec_name 解码器名字
 * @param out_ch_layout 输出声道
 * @param out_sp_rate 输出采样率
 * @param out_sp_fmt 输出采样格式
 * @return 解码器结构体
 */
struct Decoder* decoder_init_2(char *codec_name,
                               uint64_t out_ch_layout, int out_sp_rate, enum AVSampleFormat out_sp_fmt);

/**
 * 将原始数据喂给解码器
 *
 * @return 小于0：操作失败。
 **/
int decoder_feed_data(struct Decoder*, uint8_t *data, int len);

/**
 * 获取已解码数据长度
 **/
int decoder_get_decoded_size(struct Decoder*);

/**
 * 接收已解码数据
 *
 * @return 读取数据长度
 **/
int decoder_receive_decoded_data(struct Decoder*, uint8_t *data);

/**
 * 解码剩余数据，可能需要多次调用才能排空所有数据。
 *
 * @return 剩余数据大小
 */
int decoder_flush(struct Decoder*);

/**
 * 关闭解码器
 */
void decoder_close(struct Decoder*);

#endif //SOUNDEDITOR_DECODER_H
