//
// Created by Kidon Liang on 2018/4/16.
//

#include "encoder.h"
#include "android_log.h"

/**
 * 初始化转换器
 **/
static int init_convertor(struct Encoder *encoder) {
    encoder->swr_ctx = swr_alloc_set_opts(NULL,
                                          encoder->codec_ctx->channel_layout,
                                          encoder->codec_ctx->sample_fmt,
                                          encoder->codec_ctx->sample_rate,
                                          encoder->src_ch_layout,
                                          encoder->src_sample_fmt,
                                          encoder->src_sample_rate, 0, NULL);
    if (!encoder->swr_ctx) {
        LOGE("初始化swr_ctx失败\n");
        return -1;
    }
    int ret = swr_init(encoder->swr_ctx);
    if (ret < 0) {
        LOGE("初始化swr_ctx失败 ret:%d\n", ret);
        return -2;
    }
    encoder->converted_frame = av_frame_alloc();
    if (!encoder->converted_frame) {
        return -3;
    }
    encoder->converted_frame->channel_layout = encoder->codec_ctx->channel_layout;
    encoder->converted_frame->channels = av_get_channel_layout_nb_channels(encoder->codec_ctx->channel_layout);
    encoder->converted_frame->sample_rate = encoder->codec_ctx->sample_rate;
    encoder->converted_frame->format = encoder->codec_ctx->sample_fmt;

    return 0;
}

static AVFrame *convert_frame(struct Encoder *encoder) {
    if (!encoder->swr_ctx) {
        // 还没有初始化convertor
        return encoder->frame;
    }
    // 转换格式
//    LOGE("src sample fmt = %d\n", encoder->frame->format);
//    LOGE("src sample rate = %d\n", encoder->frame->sample_rate);
//    LOGE("src sample channels = %d\n", encoder->frame->channels);
//    LOGE("src sample channel_layout = %lld\n", encoder->frame->channel_layout);
//
//    LOGE("dst sample fmt = %d\n", encoder->converted_frame->format);
//    LOGE("dst sample rate = %d\n", encoder->converted_frame->sample_rate);
//    LOGE("dst sample channels = %d\n", encoder->converted_frame->channels);
//    LOGE("dst sample channel_layout = %lld\n", encoder->converted_frame->channel_layout);

    int ret = swr_convert_frame(encoder->swr_ctx, encoder->converted_frame, encoder->frame);
    if (ret >= 0) {
        return encoder->converted_frame;
    } else {
        LOGE("error in swr_convert_frame. %d\n", ret);
//        char buf[128];
//        av_strerror(ret, buf, 128);
//        LOGE("%s\n", buf);
        return NULL;
    }
}


struct Encoder* encoder_init(char *codec_name,
                             uint64_t in_ch_layout, int in_sp_rate, enum AVSampleFormat in_sp_fmt) {
    struct Encoder* encoder = malloc(sizeof(struct Encoder));
    encoder->codec = NULL;
    encoder->codec_ctx = NULL;
    encoder->frame = NULL;
    encoder->pkt = NULL;

    // 输入参数
    if (in_sp_fmt != AV_SAMPLE_FMT_S16) {
        encoder_close(encoder);
        LOGE("暂时还不支持非AV_SAMPLE_FMT_S16输入格式\n");
        return NULL;
    }
    encoder->src_ch_layout = in_ch_layout;
    encoder->src_sample_rate = in_sp_rate;
    encoder->src_sample_fmt = in_sp_fmt;

    // 转换
    encoder->swr_ctx = NULL;
    encoder->converted_frame = NULL;

    // 输入
    encoder->in_buffer = NULL;
    encoder->in_buffer_size = 0;
    encoder->in_buffer_end = 0;

    // 输出
    encoder->out_buffer_size = 0;
    encoder->out_buffer = NULL;
    encoder->encoded_data_size = 0;

    // find encoder
    encoder->codec = avcodec_find_encoder_by_name(codec_name);
    if (!encoder->codec) {
        // 注册所有可用解码器
        avcodec_register_all();
        encoder->codec = avcodec_find_encoder_by_name(codec_name);
        if (!encoder->codec) {
            encoder_close(encoder);
            LOGE("找不到可用解码器:%s\n", codec_name);
            return NULL;
        }
    }

    // init context
    encoder->codec_ctx = avcodec_alloc_context3(encoder->codec);
    if (!encoder->codec_ctx) {
        encoder_close(encoder);
        LOGE("初始化context失败\n");
        return NULL;
    }
    encoder->codec_ctx->channel_layout = in_ch_layout;
    encoder->codec_ctx->channels = av_get_channel_layout_nb_channels(in_ch_layout);
    encoder->codec_ctx->sample_rate = in_sp_rate;
    if (strcmp(codec_name, "libmp3lame") == 0) {
        // mp3不支持AV_SAMPLE_FMT_S16
        encoder->codec_ctx->sample_fmt = AV_SAMPLE_FMT_FLTP;
        init_convertor(encoder);
    } else {
        encoder->codec_ctx->sample_fmt = in_sp_fmt;
    }

    // open
    if (avcodec_open2(encoder->codec_ctx, encoder->codec, NULL) < 0) {
        encoder_close(encoder);
        LOGE("avcodec_open2失败\n");
        return NULL;
    }

    // alloc packet
    encoder->pkt = av_packet_alloc();
    if (!encoder->pkt) {
        encoder_close(encoder);
        LOGE("分配packet失败\n");
        return NULL;
    }

    // alloc frame
    encoder->frame = av_frame_alloc();
    if (!encoder->frame) {
        encoder_close(encoder);
        LOGE("分配frame失败\n");
        return NULL;
    }
    encoder->frame->nb_samples     = encoder->codec_ctx->frame_size;
    encoder->frame->format         = encoder->src_sample_fmt;
    encoder->frame->channel_layout = encoder->src_ch_layout;
    encoder->frame->channels = av_get_channel_layout_nb_channels(encoder->src_ch_layout);
    encoder->frame->sample_rate = encoder->src_sample_rate;

    // alloc frame data buffers
    if (av_frame_get_buffer(encoder->frame, 0) < 0) {
        encoder_close(encoder);
        LOGE("av_frame_get_buffer失败\n");
        return NULL;
    }

    return encoder;
}

static int encode(struct Encoder* encoder,
                  AVCodecContext *ctx,
                  AVFrame *frame,
                  AVPacket *pkt) {
    int ret;

    /* send the frame for encoding */
    ret = avcodec_send_frame(ctx, frame);
    if (ret < 0) {
        LOGE("Error sending the frame to the encoder\n");
        return -1;
    }

    /* read all the available output packets (in general there may be any
     * number of them */
    while (ret >= 0) {
        ret = avcodec_receive_packet(ctx, pkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
            return 0;
        else if (ret < 0) {
            LOGE("Error encoding audio frame\n");
            return -2;
        }
        // 写入输出数组
        if (encoder->out_buffer_size < (encoder->encoded_data_size + pkt->size)) {
            // 重新分配输出buffer数组长度
            size_t new_buf_size = (size_t) (encoder->encoded_data_size + 2 * pkt->size);
            uint8_t *tmp_buf = malloc(new_buf_size);
            // 将旧数据迁移到新数组中
            if (encoder->out_buffer) {
                memcpy(tmp_buf, encoder->out_buffer, (size_t) encoder->encoded_data_size);
            }
            free(encoder->out_buffer);
            encoder->out_buffer = tmp_buf;
            encoder->out_buffer_size = (int) new_buf_size;
        }
        memcpy(encoder->out_buffer + encoder->encoded_data_size, pkt->data, pkt->size);
        encoder->encoded_data_size += pkt->size;

        av_packet_unref(pkt);
    }
    return 0;
}

int encoder_feed_data(struct Encoder* encoder, uint8_t *data, int len) {
    if (!encoder) return -1;
    if (encoder->in_buffer_size < encoder->in_buffer_end + len) {
        // 需要重新分配buffer
        encoder->in_buffer_size = encoder->in_buffer_end + len;
        uint8_t* tmp_buf = malloc(encoder->in_buffer_size);
        if (encoder->in_buffer) {
            memcpy(tmp_buf, encoder->in_buffer, encoder->in_buffer_end);
            free(encoder->in_buffer);
        }
        encoder->in_buffer = tmp_buf;
    }
    memcpy(encoder->in_buffer + encoder->in_buffer_end, data, len);
    encoder->in_buffer_end += len;

    int frame_size = encoder->codec_ctx->channels * sizeof(uint16_t) * encoder->codec_ctx->frame_size;
    int ret;
    AVFrame* frame;
    while (encoder->in_buffer_end >= frame_size) {
        ret = av_frame_make_writable(encoder->frame);
        if (ret < 0) {
            LOGE("av_frame_make_writable fail.\n");
            return -2;
        }
        // 每次写入需要确保数据长度为一帧
        memcpy(encoder->frame->data[0], encoder->in_buffer, frame_size);
        frame = convert_frame(encoder);
        if (!frame) {
            LOGE("convert_frame fail.\n");
            return -3;
        }

        // 将已写入的数据移除
        memcpy(encoder->in_buffer, encoder->in_buffer + frame_size, encoder->in_buffer_end - frame_size);
        encoder->in_buffer_end -= frame_size;
        // 编码
        ret = encode(encoder, encoder->codec_ctx, frame, encoder->pkt);
        if (ret < 0) {
            LOGE("encode fail.\n");
            return -3;
        }
    }
    return 0;
}

int encoder_get_encoded_size(struct Encoder* encoder) {
    if (!encoder) return -1;
    return encoder->encoded_data_size;
}

int encoder_receive_encoded_data(struct Encoder* encoder, uint8_t *data) {
    if (!encoder) return -1;
    int tmp = encoder->encoded_data_size;
    memcpy(data, encoder->out_buffer, (size_t) encoder->encoded_data_size);
    encoder->encoded_data_size = 0;
    return tmp;
}

int encoder_flush(struct Encoder* encoder) {
    if (!encoder) return -1;
    if (encoder->swr_ctx) {
        LOGE("flush remaining = %d\n", (int) swr_get_delay(encoder->swr_ctx, encoder->codec_ctx->sample_rate));
    }
    /* flush the encoder */
    return encode(encoder, encoder->codec_ctx, NULL, encoder->pkt);
}

void encoder_close(struct Encoder* encoder) {
    if (!encoder) return;
    av_frame_free(&encoder->frame);
    av_packet_free(&encoder->pkt);
    avcodec_free_context(&encoder->codec_ctx);
}