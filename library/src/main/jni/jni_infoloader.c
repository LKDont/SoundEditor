//
// Created by Kidon Liang on 2018/4/5.
//
#include <jni.h>
#include "infoloader.h"
#include "android_log.h"


JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Info__1load(JNIEnv *env, jobject instance, jstring url_, jint network) {
    const char *url = (*env)->GetStringUTFChars(env, url_, 0);

    struct Info* info = malloc(sizeof(struct Info));
    info->url = (char *) url;
    int ret = infoloader_load(info, network);
    if (ret == 0) {

        jstring codec;
        switch (info->codec_id) {

            case AV_CODEC_ID_MP3:
                codec = (*env)->NewStringUTF(env, "mp3");
                break;
            case AV_CODEC_ID_AAC:
                codec = (*env)->NewStringUTF(env, "libfdk_aac");
                break;
            default:
                LOGE("load audio info fail : unsupported codec : %d", info->codec_id);
                return -2;
        }

        // 更新声音信息
        jclass thclass = (*env)->GetObjectClass(env, instance);

        // codec
        jfieldID fieldId = (*env)->GetFieldID(env, thclass, "codec", "Ljava/lang/String;");
        (*env)->SetObjectField(env, instance, fieldId, codec);

        // duration
        fieldId = (*env)->GetFieldID(env, thclass, "duration", "J");
        (*env)->SetLongField(env, instance, fieldId, info->durarion);

        // sampleRate
        fieldId = (*env)->GetFieldID(env, thclass, "sampleRate", "I");
        (*env)->SetIntField(env, instance, fieldId, info->sample_rate);

        // channels
        fieldId = (*env)->GetFieldID(env, thclass, "channels", "I");
        (*env)->SetIntField(env, instance, fieldId, info->channels);

        // bitRate
        fieldId = (*env)->GetFieldID(env, thclass, "bitRate", "J");
        (*env)->SetLongField(env, instance, fieldId, info->bit_rate);
    } else {
        LOGE("load audio info fail : %d : %s", ret, url);
    }

    // free
    free(info);

    (*env)->ReleaseStringUTFChars(env, url_, url);

    return ret;
}
