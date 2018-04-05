//
// 声音信息加载器
//
// Created by Kidon Liang on 2018/4/5.
//

#ifndef SOUNDEDITOR_INFOLOADER_H
#define SOUNDEDITOR_INFOLOADER_H

#include "libavformat/avformat.h"

struct Info {
    char *url;
    int codec_id;
    int64_t durarion;
    int sample_rate;
    int channels;
    int64_t bit_rate;
};

/**
 * 加载声音信息
 *
 * @param info 声音信息
 * @param network 0：本地文件；1：网络文件
 * @return 0：操作成功；其它：失败
 */
int infoloader_load(struct Info* info, int network);

#endif //SOUNDEDITOR_INFOLOADER_H
