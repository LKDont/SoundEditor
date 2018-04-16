//
// Created by Kidon Liang on 2018/4/16.
//
#include <jni.h>
#include "encoder.h"
#include "android_log.h"

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Encoder__1init(JNIEnv *env, jobject instance, jstring codecName_,
                                          jint channels, jint sample_rate, jint sample_fmt) {

    uint64_t dst_ch_layout;
    if (channels == 1) {
        dst_ch_layout = AV_CH_LAYOUT_MONO;
    } else if (channels == 2) {
        dst_ch_layout = AV_CH_LAYOUT_STEREO;
    } else {
        LOGE("不支持声道类型:%d", channels);
        return -1;
    }

    enum AVSampleFormat dst_sp_fmt = (enum AVSampleFormat) sample_fmt;

    const char *codecName = (*env)->GetStringUTFChars(env, codecName_, 0);

    jclass thclass = (*env)->GetObjectClass(env, instance);
    jfieldID fieldId = (*env)->GetFieldID(env, thclass, "nativeEncoderId", "J");

    struct Encoder *encoder = encoder_init((char *) codecName, dst_ch_layout, sample_rate,
                                           dst_sp_fmt);
    (*env)->ReleaseStringUTFChars(env, codecName_, codecName);

    if (encoder) {
        // 初始化成功
        (*env)->SetLongField(env, instance, fieldId, (jlong) encoder);
        return 0;
    } else {
        // 初始化失败
        (*env)->SetLongField(env, instance, fieldId, (jlong) 0);
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Encoder__1feed_1data(JNIEnv *env, jobject instance, jlong encoder_id,
                                                jbyteArray data_, jint len) {
    if (encoder_id == 0) {
        LOGE("Java_com_lkdont_sound_edit_Encoder__1feed_1data : encoder_id == 0");
        return -1;
    }
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    struct Encoder *encoder = (struct Encoder *) encoder_id;
    int ret = encoder_feed_data(encoder, (uint8_t *) data, len);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Encoder__1get_1encoded_1size(JNIEnv *env, jobject instance,
                                                        jlong encoder_id) {
    if (encoder_id == 0) {
        LOGE("Java_com_lkdont_sound_edit_Encoder__1get_1encoded_1size : encoder_id == 0");
        return -1;
    }

    struct Encoder *encoder = (struct Encoder *) encoder_id;
    return encoder_get_encoded_size(encoder);
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Encoder__1receive_1encoded_1data(JNIEnv *env, jobject instance,
                                                            jlong encoder_id, jbyteArray data_) {
    if (encoder_id == 0) {
        LOGE("Java_com_lkdont_sound_edit_Encoder__1receive_1encoded_1data : encoder_id == 0");
        return -1;
    }
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    struct Encoder *encoder = (struct Encoder *) encoder_id;
    int ret = encoder_receive_encoded_data(encoder, (uint8_t *) data);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Encoder__1flush(JNIEnv *env, jobject instance, jlong encoder_id) {
    if (encoder_id == 0) {
        LOGE("Java_com_lkdont_sound_edit_Encoder__1flush : encoder_id == 0");
        return -1;
    }
    struct Encoder *encoder = (struct Encoder *) encoder_id;
    return encoder_flush(encoder);
}

JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_Encoder__1close(JNIEnv *env, jobject instance, jlong encoder_id) {
    if (encoder_id == 0) {
        LOGE("Java_com_lkdont_sound_edit_Encoder__1close : encoder_id == 0");
        return;
    }
    jclass thclass = (*env)->GetObjectClass(env, instance);
    jfieldID fieldId = (*env)->GetFieldID(env, thclass, "nativeEncoderId", "J");
    (*env)->SetLongField(env, instance, fieldId, (jlong) 0);

    struct Encoder *encoder = (struct Encoder *) encoder_id;
    encoder_close(encoder);
}