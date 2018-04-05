//
// 解码器-java接口
//
// Created by Kidon Liang on 2018/3/14.
//

#include <jni.h>
#include "decoder.h"
#include "android_log.h"

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder__1init(JNIEnv *env, jobject instance, jstring codecName_) {

    const char *codecName = (*env)->GetStringUTFChars(env, codecName_, 0);

    jclass thclass = (*env)->GetObjectClass(env, instance);
    jfieldID fieldId = (*env)->GetFieldID(env, thclass, "nativeDecoderId", "J");

    struct Decoder *decoder = decoder_init((char *) codecName);
    (*env)->ReleaseStringUTFChars(env, codecName_, codecName);

    if (decoder) {
        // 初始化成功
        (*env)->SetLongField(env, instance, fieldId, (jlong) decoder);
        return 0;
    } else {
        // 初始化失败
        (*env)->SetLongField(env, instance, fieldId, (jlong) 0);
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder__1feed_1data(JNIEnv *env, jobject instance, jlong decoder_id,
                                                jbyteArray data_, jint len) {
    if (decoder_id == 0) {
        LOGE("jni_decoder : Java_com_lkdont_sound_edit_Decoder__1feed_1data : decoder_id == 0");
        return -1;
    }
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    struct Decoder *decoder = (struct Decoder *) decoder_id;
    int ret = decoder_feed_data(decoder, (uint8_t *) data, len);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder__1get_1decoded_1size(JNIEnv *env, jobject instance,
                                                        jlong decoder_id) {
    if (decoder_id == 0) {
        LOGE("jni_decoder : Java_com_lkdont_sound_edit_Decoder__1get_1decoded_1size : decoder_id == 0");
        return -1;
    }
    struct Decoder *decoder = (struct Decoder *) decoder_id;
    return decoder_get_decoded_size(decoder);
}

JNIEXPORT jint JNICALL
Java_com_lkdont_sound_edit_Decoder__1receive_1decoded_1data(JNIEnv *env, jobject instance,
                                                            jlong decoder_id, jbyteArray data_) {
    if (decoder_id == 0) {
        LOGE("jni_decoder : Java_com_lkdont_sound_edit_Decoder__1receive_1decoded_1data : decoder_id == 0");
        return -1;
    }

    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    struct Decoder *decoder = (struct Decoder *) decoder_id;
    int ret = decoder_receive_decoded_data(decoder, (uint8_t *) data);

    (*env)->ReleaseByteArrayElements(env, data_, data, 0);

    return ret;
}

JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_Decoder__1flush(JNIEnv *env, jobject instance, jlong decoder_id) {
    if (decoder_id == 0)
        return;
    struct Decoder *decoder = (struct Decoder *) decoder_id;
    decoder_flush(decoder);
}

JNIEXPORT void JNICALL
Java_com_lkdont_sound_edit_Decoder__1close(JNIEnv *env, jobject instance, jlong decoder_id) {
    if (decoder_id == 0)
        return;
    // 将nativeDecoderId设置为0，防止重复调用close导致崩溃
    jclass thclass = (*env)->GetObjectClass(env, instance);
    jfieldID fieldId = (*env)->GetFieldID(env, thclass, "nativeDecoderId", "J");
    (*env)->SetLongField(env, instance, fieldId, (jlong) 0);

    struct Decoder *decoder = (struct Decoder *) decoder_id;
    decoder_close(decoder);
}