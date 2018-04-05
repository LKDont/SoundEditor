//
// Created by Kidon Liang on 2018/3/14.
//

#ifndef SOUNDEDITOR_DECODER_H
#define SOUNDEDITOR_DECODER_H

#include "libavcodec/avcodec.h"

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
 * 解码剩余数据
 */
void decoder_flush(struct Decoder*);

/**
 * 关闭解码器
 */
void decoder_close(struct Decoder*);

#endif //SOUNDEDITOR_DECODER_H
