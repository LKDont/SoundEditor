//
// Created by Kidon Liang on 2018/3/4.
//

#include "decoder.h"
#include "android_log.h"

struct Decoder *decoder_init(char *codec_name) {

    // malloc Decoder
    struct Decoder *decoder = malloc(sizeof(struct Decoder));
    decoder->codec = NULL;
    decoder->codec_ctx = NULL;
    decoder->parser = NULL;
    decoder->pkt = NULL;
    decoder->decoded_frame = NULL;
    decoder->out_buffer_size = 0;        // 输出buffer数组大小
    decoder->out_buffer = NULL;
    decoder->decoded_data_size = 0;

    // find decoder
    decoder->codec = avcodec_find_decoder_by_name(codec_name);
    if (!decoder->codec) {
        // 注册所有可用解码器
        avcodec_register_all();
        decoder->codec = avcodec_find_decoder_by_name(codec_name);
        if (!decoder->codec) {
            decoder_close(decoder);
            return NULL;
        }
    }

    if (strcmp(codec_name, "mp3") == 0) {
        // 需要检查头部
        decoder->header_size = -1;
        LOGD("需要检查头部, codec_name == mp3");
    } else {
        decoder->header_size = 0;
    }
    decoder->header_buffer = NULL;
    decoder->header_buffer_size = 0;
    decoder->header_buffer_end = 0;

    // init context
    decoder->parser = av_parser_init(decoder->codec->id);
    if (!decoder->parser) {
        decoder_close(decoder);
        return NULL;
    }

    decoder->codec_ctx = avcodec_alloc_context3(decoder->codec);
    if (!decoder->codec_ctx) {
        decoder_close(decoder);
        return NULL;
    }

    // open it
    if (avcodec_open2(decoder->codec_ctx, decoder->codec, NULL) < 0) {
        decoder_close(decoder);
        return NULL;
    }

    // alloc packet
    decoder->pkt = av_packet_alloc();
    if (!decoder->pkt) {
        decoder_close(decoder);
        return NULL;
    }

    // alloc frame
    decoder->decoded_frame = av_frame_alloc();
    if (!decoder->decoded_frame) {
        decoder_close(decoder);
        return NULL;
    }

    return decoder;
}

/**
 * 解码操作
 **/
static int decode(struct Decoder *decoder) {

    int ret = avcodec_send_packet(decoder->codec_ctx, decoder->pkt);
    if (ret < 0) {
        LOGE("decode error in avcodec_send_packet : ret = %d", ret);
        return -1;
    }

    int sample_size = 0;    // 每个样本大小
    int d_size = 0;         // 已解码数据长度
    int i, ch, out_index;

    while (ret >= 0) {
        ret = avcodec_receive_frame(decoder->codec_ctx, decoder->decoded_frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
            // 数据暂时不可用
            return 1;
        else if (ret < 0) {
            LOGE("decode error in avcodec_receive_frame\n");
            return -2;
        }

        sample_size = av_get_bytes_per_sample(decoder->codec_ctx->sample_fmt);
        if (sample_size < 0) {
            // 计算数据长度失败
            LOGE("decode 计算数据长度失败\n");
            return -3;
        }
        d_size =
                sample_size * decoder->decoded_frame->nb_samples * decoder->decoded_frame->channels;
        if (decoder->out_buffer_size < (decoder->decoded_data_size + d_size)) {
            // 重新分配输出buffer数组长度
            size_t new_buf_size = (size_t) (decoder->decoded_data_size + 2 * d_size);
            uint8_t *tmp_buf = malloc(new_buf_size);
            // 将旧数据迁移到新数组中
            if (decoder->out_buffer) {
                memcpy(tmp_buf, decoder->out_buffer, (size_t) decoder->decoded_data_size);
            }
            free(decoder->out_buffer);
            decoder->out_buffer = tmp_buf;
            decoder->out_buffer_size = (int) new_buf_size;
        }
        // 将已解码数据移至输出buffer中
        if (av_sample_fmt_is_planar(decoder->codec_ctx->sample_fmt)) {
            // 样本是平坦的，将其变成非平坦输出
            out_index = decoder->decoded_data_size;
            for (i = 0; i < decoder->decoded_frame->nb_samples; i++) {
                for (ch = 0; ch < decoder->decoded_frame->channels; ch++) {
                    memcpy(decoder->out_buffer + out_index,
                           decoder->decoded_frame->data[ch] + sample_size * i,
                           (size_t) sample_size);
                    out_index += sample_size;
                }
            }
        } else {
            // 非平坦
            out_index = decoder->decoded_data_size;
            memcpy(decoder->out_buffer + out_index, decoder->decoded_frame->data[0],
                   (size_t) d_size);
        }
        decoder->decoded_data_size += d_size;
    }
    return 0;
}

/**
 * 获取mp3头部长度
 *
 * @param buffer 头部buffer，长度最少为10
 * @return
 */
static int get_mp3_header_size(uint8_t *buffer) {
    if (buffer[0] == 73 && buffer[1] == 68 && buffer[2] == 51) {
        return (buffer[6] << 21) + (buffer[7] << 14) + (buffer[8] << 7) + buffer[9] + 10;
    }
    return 0;
}

/**
 * 将原始数据喂给解码器
 *
 * @return 小于0：操作失败。
 **/
int decoder_feed_data(struct Decoder *decoder, uint8_t *data, int len) {
    if (!decoder) return -1;
    if (decoder->header_size < 0 || decoder->header_buffer_end < decoder->header_size) {
        if (decoder->header_buffer_size < decoder->header_buffer_end + len) {
            // 重新分配buffer空间
            size_t new_buf_size = (size_t) (decoder->header_buffer_end + 2 * len);
            uint8_t *tmp_buf = malloc(new_buf_size);
            if (decoder->header_buffer) {
                memcpy(tmp_buf, decoder->header_buffer, (size_t) decoder->header_buffer_end);
            }
            free(decoder->header_buffer);
            decoder->header_buffer = tmp_buf;
            decoder->header_buffer_size = new_buf_size;
        }
        memcpy(decoder->header_buffer + decoder->header_buffer_end, data, (size_t) len);
        decoder->header_buffer_end += len;

        if (decoder->header_size < 0 && decoder->header_buffer_end >= 10) {
            // 计算头部大小
            decoder->header_size = get_mp3_header_size(decoder->header_buffer);
            LOGI("Decoder : header size = %d", decoder->header_size);
        }
        if (decoder->header_size < 0 || decoder->header_buffer_end < decoder->header_size) {
            // 继续接收数据
            return 0;
        }
    }

    if (decoder->header_size > 0) {
        // skip header
        len = decoder->header_buffer_end - decoder->header_size;
        memcpy(decoder->header_buffer, decoder->header_buffer + decoder->header_size,
               (size_t) len);
        data = decoder->header_buffer;
        decoder->header_size = 0;
    }

    int parse_ret;
    while (len > 0) {
        parse_ret = av_parser_parse2(decoder->parser, decoder->codec_ctx,
                                     &decoder->pkt->data, &decoder->pkt->size, data, len,
                                     AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
        if (parse_ret < 0) {
            LOGE("av_parser_parse2 error : parse_ret = %d", parse_ret);
            return -2;
        }

        if (decoder->pkt->size) {
            if (decode(decoder) < 0) {
                return -3;
            }
        }

        data += parse_ret;
        len -= parse_ret;
    }

    return 0;
}

/**
 * 获取已解码数据长度
 **/
int decoder_get_decoded_size(struct Decoder *decoder) {
    if (!decoder) return -1;
    return decoder->decoded_data_size;
}

/**
 * 接收已解码数据
 *
 * @return 读取数据长度
 **/
int decoder_receive_decoded_data(struct Decoder *decoder, uint8_t *data) {
    if (!decoder) return -1;
    int tmp = decoder->decoded_data_size;
    memcpy(data, decoder->out_buffer, (size_t) decoder->decoded_data_size);
    decoder->decoded_data_size = 0;
    return tmp;
}

/**
 * 解码剩余数据
 */
void decoder_flush(struct Decoder *decoder) {
    if (!decoder) return;
    decoder->pkt->data = NULL;
    decoder->pkt->size = 0;
    decode(decoder);
}

void decoder_close(struct Decoder *decoder) {
    if (!decoder) return;
    avcodec_free_context(&decoder->codec_ctx);
    decoder->codec_ctx = NULL;

    av_parser_close(decoder->parser);
    decoder->parser = NULL;

    av_frame_free(&decoder->decoded_frame);
    av_packet_free(&decoder->pkt);

    // header buffer
    decoder->header_size = 0;
    free(decoder->header_buffer);
    decoder->header_buffer = NULL;
    decoder->header_buffer_size = 0;
    decoder->header_buffer_end = 0;

    // output buffer
    decoder->out_buffer_size = 0;
    free(decoder->out_buffer);
    decoder->out_buffer = NULL;

    decoder->decoded_data_size = 0;

    free(decoder);
}