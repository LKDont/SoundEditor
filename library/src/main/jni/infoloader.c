//
// 声音信息加载器
//
// Created by Kidon Liang on 2018/4/5.
//

#include "infoloader.h"
#include "android_log.h"

int infoloader_load(struct Info* info, int network) {
    if (!info) return -1;
    if (network) {
        // 网络初始化
        avformat_network_init();
    }
    av_register_all();
    AVFormatContext *fmt_ctx = NULL;
    int ret = avformat_open_input(&fmt_ctx, info->url, NULL, NULL);
    if (ret) {
        char *buf[1024];
        av_strerror(ret, buf, 1024);
        LOGE("%s", buf);
        return ret;
    }
    ret = avformat_find_stream_info(fmt_ctx, NULL);
    if (ret < 0) {
        return -3;
    }
    if (fmt_ctx->nb_streams <= 0) {
        // 不是有效的音频文件
        return -4;
    }

    info->durarion = fmt_ctx->duration;
    info->codec_id = fmt_ctx->streams[0]->codecpar->codec_id;
    info->channels = fmt_ctx->streams[0]->codecpar->channels;
    info->sample_rate = fmt_ctx->streams[0]->codecpar->sample_rate;
    info->bit_rate = fmt_ctx->streams[0]->codecpar->bit_rate;

    // close
    avformat_close_input(&fmt_ctx);
    return 0;
}