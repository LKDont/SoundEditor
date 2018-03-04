//
// Created by Kidon Liang on 2018/3/4.
//
#include <jni.h>
#include "android_log.h"

#include <libavutil/frame.h>
#include <libavutil/mem.h>
#include <libavcodec/avcodec.h>

JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_Decoder_test(JNIEnv *env, jclass type) {
    avcodec_register_all();
    char *names[] = {"AV_CODEC_ID_AMR_NB", "AV_CODEC_ID_AMR_WB", "AV_CODEC_ID_MP2",
                     "AV_CODEC_ID_MP3", "AV_CODEC_ID_AAC"};
    enum AVCodecID arr[] = {AV_CODEC_ID_AMR_NB, AV_CODEC_ID_AMR_WB, AV_CODEC_ID_MP2,
                            AV_CODEC_ID_MP3, AV_CODEC_ID_AAC};

    for (int i = 0; i < 5; i++) {
        AVCodec *codec = avcodec_find_decoder(arr[i]);
        if (!codec) {
            LOGE("%s not found!", names[i]);
        } else {
            LOGI("%s found!", names[i]);
        }
    }

//    AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_MP2);
//    if (!codec) {
//        LOGE("AV_CODEC_ID_MP2 not found!");
//    } else {
//        LOGI("AV_CODEC_ID_MP2 found!");
//    }
}