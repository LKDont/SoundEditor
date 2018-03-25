//
// 解码器-java接口
//
// Created by Kidon Liang on 2018/3/14.
//

#include <jni.h>
#include "android_log.h"
#include "decoder.h"

//#include <stdio.h>
//#include <stdlib.h>
//#include <string.h>
//#include <libavutil/frame.h>
//#include <libavutil/mem.h>
//#include <libavcodec/avcodec.h>
//#include <libavformat/avformat.h>
//
//#define AUDIO_INBUF_SIZE 20480
//#define AUDIO_REFILL_THRESH 4096
//
//void decode_test(AVCodecContext *dec_ctx, AVPacket *pkt, AVFrame *frame,
//                 FILE *outfile) {
//    int i, ch;
//    int ret, data_size;
//    /* send the packet with the compressed data to the decoder */
//    ret = avcodec_send_packet(dec_ctx, pkt);
//    if (ret < 0) {
//        LOGD("Error submitting the packet to the decoder\n");
//        return;
//    }
//    /* read all the output frames (in general there may be any number of them */
//    while (ret >= 0) {
//        ret = avcodec_receive_frame(dec_ctx, frame);
//        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
//            return;
//        else if (ret < 0) {
//            LOGD("Error during decoding\n");
//            return;
//        }
//        data_size = av_get_bytes_per_sample(dec_ctx->sample_fmt);
//        if (data_size < 0) {
//            /* This should not occur, checking just for paranoia */
//            LOGD("Failed to calculate data size\n");
//            return;
//        }
//        for (i = 0; i < frame->nb_samples; i++)
//            for (ch = 0; ch < dec_ctx->channels; ch++)
//                fwrite(frame->data[ch] + data_size * i, 1, data_size, outfile);
//    }
//}
//
//JNIEXPORT jint JNICALL
//Java_com_lkdont_sound_edit_Decoder_test(JNIEnv *env, jclass type) {
//    avcodec_register_all();
//
//    const char *outfilename, *filename;
//    const AVCodec *codec;
//    AVCodecContext *c = NULL;
//    AVCodecParserContext *parser = NULL;
//    int len, ret;
//    FILE *f, *outfile;
//    uint8_t inbuf[AUDIO_INBUF_SIZE + AV_INPUT_BUFFER_PADDING_SIZE];
//    uint8_t *data;
//    size_t data_size;
//    AVPacket *pkt;
//    AVFrame *decoded_frame = NULL;
//
//    filename = "/sdcard/sound_editor/SuperMalioRemix-44100.mp3";
//    outfilename = "/sdcard/sound_editor/SuperMalioRemix-44100.pcm";
//
//    pkt = av_packet_alloc();
//    /* find the MPEG audio decoder */
//    codec = avcodec_find_decoder(AV_CODEC_ID_MP3);
//    if (!codec) {
//        LOGD("Codec not found\n");
//        return -1;
//    }
//    parser = av_parser_init(codec->id);
//    if (!parser) {
//        LOGD("Parser not found\n");
//        return -2;
//    }
//    c = avcodec_alloc_context3(codec);
//    if (!c) {
//        LOGD("Could not allocate audio codec context\n");
//        return -3;
//    }
//    /* open it */
//    if (avcodec_open2(c, codec, NULL) < 0) {
//        LOGD("Could not open codec\n");
//        return -4;
//    }
//    f = fopen(filename, "rb");
//    if (!f) {
//        LOGD("Could not open %s\n", filename);
//        return -5;
//    }
//    outfile = fopen(outfilename, "wb");
//    if (!outfile) {
//        av_free(c);
//        return -6;
//    }
//    /* decode until eof */
//    data = inbuf;
//    data_size = fread(inbuf, 1, AUDIO_INBUF_SIZE, f);
//
//    if (!(decoded_frame = av_frame_alloc())) {
//        LOGD("Could not allocate audio frame\n");
//        return -7;
//    }
//
//    while (data_size > 0) {
////        if (!decoded_frame) {
////            if (!(decoded_frame = av_frame_alloc())) {
////                LOGD("Could not allocate audio frame\n");
////                exit(1);
////            }
////        }
//        ret = av_parser_parse2(parser, c, &pkt->data, &pkt->size,
//                               data, data_size,
//                               AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
//        printf("ret=%d, data_size=%d\n", ret, data_size);
//        if (ret < 0) {
//            LOGD("Error while parsing\n");
//            return -8;
//        }
//        data += ret;
//        data_size -= ret;
//        if (pkt->size)
//            decode_test(c, pkt, decoded_frame, outfile);
//        if (data_size < AUDIO_REFILL_THRESH) {
//            memmove(inbuf, data, data_size);
//            data = inbuf;
//            len = fread(data + data_size, 1,
//                        AUDIO_INBUF_SIZE - data_size, f);
//            if (len > 0)
//                data_size += len;
//        }
//    }
//    /* flush the decoder */
//    pkt->data = NULL;
//    pkt->size = 0;
//    decode_test(c, pkt, decoded_frame, outfile);
//    fclose(outfile);
//    fclose(f);
//    avcodec_free_context(&c);
//    av_parser_close(parser);
//    av_frame_free(&decoded_frame);
//    av_packet_free(&pkt);
//
//    return 0;
//}


/**
 * 初始化解码器
 *
 * @param env
 * @param type
 * @param codec
 * @return
 */
JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder_initDecoder(JNIEnv *env, jclass type,
                                               jint codec) {
    enum AVCodecID codecID;
    switch (codec) {
        case 1:
            codecID = AV_CODEC_ID_MP3;
            break;

        case 2:
            codecID = AV_CODEC_ID_AAC;
            break;

        default:
            LOGE("unsupported codec : %d", codec);
            return -1;
    }

    return init_decoder(codecID);
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder_feedData(JNIEnv *env, jobject instance,
                                            jbyteArray data_, jint len) {

    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    int ret = feed_data((uint8_t *) data, len);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder_getDecodedSize(JNIEnv *env, jobject instance) {
    return get_decoded_size();
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder_receiveDecodedData(JNIEnv *env, jobject instance,
                                                      jbyteArray data_) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    int ret = receive_decoded_data((uint8_t *) data);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

/**
 * 关闭解码器
 *
 * @param env
 * @param type
 */
JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_Decoder_closeDecoder(JNIEnv *env, jclass type) {
    release_decoder();
}
