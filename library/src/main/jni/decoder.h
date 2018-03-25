//
// Created by Kidon Liang on 2018/3/14.
//

#ifndef SOUNDEDITOR_DECODER_H
#define SOUNDEDITOR_DECODER_H

#include "libavcodec/avcodec.h"

/**
 * 初始化
 *
 * @param codecId 解码器id
 * @return 0：操作成功；其它：操作失败。
 **/
int init_decoder(enum AVCodecID codecId);

/**
 * 将原始数据喂给解码器
 *
 * @return 小于0：操作失败。
 **/
int feed_data(uint8_t *data, int len);

/**
 * 获取已解码数据长度
 **/
int get_decoded_size(void);

/**
 * 接收已解码数据
 *
 * @return 读取数据长度
 **/
int receive_decoded_data(uint8_t *data);

/**
 * 释放资源
 **/
void release_decoder(void);

#endif //SOUNDEDITOR_DECODER_H
