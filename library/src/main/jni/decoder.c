//
// Created by Kidon Liang on 2018/3/4.
//

#include "decoder.h"
#include "android_log.h"

AVCodec *decoder = NULL;
AVCodecContext *decoder_ctx = NULL;
AVCodecParserContext *parser = NULL;

AVPacket *pkt = NULL;
AVFrame *decoded_frame = NULL;

int out_buffer_size;        // 输出buffer数组大小
uint8_t *out_buffer = NULL;

int init_decoder(enum AVCodecID codecId) {

    // find decoder
    decoder = avcodec_find_decoder(codecId);
    if (!decoder) {
        // 注册所有可用解码器
        avcodec_register_all();
        decoder = avcodec_find_decoder(codecId);
        if (!decoder) {
            release_decoder();
            return -1;
        }
    }

    // init context
    parser = av_parser_init(decoder->id);
    if (!parser) {
        release_decoder();
        return -2;
    }

    decoder_ctx = avcodec_alloc_context3(decoder);
    if (!decoder_ctx) {
        release_decoder();
        return -3;
    }

    // open it
    if (avcodec_open2(decoder_ctx, decoder, NULL) < 0) {
        release_decoder();
        return -4;
    }

    // alloc packet
    pkt = av_packet_alloc();
    if (!pkt) {
        release_decoder();
        return -5;
    }

    // alloc frame
    decoded_frame = av_frame_alloc();
    if (!decoded_frame) {
        release_decoder();
        return -6;
    }

    return 0;
}

int decoded_data_size = 0;  // 已解码数据大小

/**
 * 解码操作
 **/
int decode() {
    int ret = avcodec_send_packet(decoder_ctx, pkt);
    if (ret < 0) {
        LOGE("decode error in avcodec_send_packet : ret = %d", ret);
        return -1;
    }

    int sample_size = 0;
    int d_size = 0;
    int i, ch;

    while (ret >= 0) {
        ret = avcodec_receive_frame(decoder_ctx, decoded_frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
            // 数据暂时不可用
            return 1;
        else if (ret < 0) {
            LOGE("decode error in avcodec_receive_frame\n");
            return -2;
        }

        sample_size = av_get_bytes_per_sample(decoder_ctx->sample_fmt);
        if (sample_size < 0) {
            // 计算数据长度失败
            LOGE("decode 计算数据长度失败\n");
            return -3;
        }
        d_size = sample_size * decoded_frame->nb_samples * decoded_frame->channels;
        if (out_buffer_size < (decoded_data_size + d_size)) {
            // 重新分配输出buffer数组长度
            size_t new_buf_size = (size_t) (decoded_data_size + 2 * d_size);
            uint8_t *tmp_buf = malloc(new_buf_size);
            // 将旧数据迁移到新数组中
            if (out_buffer) {
                memmove(tmp_buf, out_buffer, (size_t) decoded_data_size);
            }
            free(out_buffer);
            out_buffer = tmp_buf;
            out_buffer_size = new_buf_size;
        }
        // 将已解码数据移至输出buffer中
        for (i = 0; i < decoded_frame->nb_samples; i++) {
            for (ch = 0; ch < decoded_frame->channels; ch++) {
                memmove(out_buffer + decoded_data_size + sample_size * i,
                        decoded_frame->data[ch] + sample_size * i,
                        (size_t) sample_size);
            }
        }
        decoded_data_size += d_size;
    }
    return 0;
}

int feed_data(uint8_t *data, int len) {

    int parse_ret;
    while (len > 0) {
        parse_ret = av_parser_parse2(parser, decoder_ctx,
                                     &pkt->data, &pkt->size, data, len,
                                     AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
        if (parse_ret < 0) {
            LOGE("av_parser_parse2 error : parse_ret = %d", parse_ret);
            release_decoder();
            return -1;
        }

        if (pkt->size) {
            if (decode() < 0) {
                release_decoder();
                return -2;
            }
        }

        data += parse_ret;
        len -= parse_ret;
    }

    return 0;
}

int get_decoded_size() {
    return decoded_data_size;
}

int receive_decoded_data(uint8_t *data) {
    int tmp = decoded_data_size;
    memmove(data, out_buffer, (size_t) decoded_data_size);
    decoded_data_size = 0;
    return tmp;
}

void release_decoder() {

    avcodec_free_context(&decoder_ctx);
    decoder_ctx = NULL;

    av_parser_close(parser);
    parser = NULL;

    av_frame_free(&decoded_frame);
    av_packet_free(&pkt);

    out_buffer_size = 0;
    free(out_buffer);
    out_buffer = NULL;

    decoded_data_size = 0;
}
