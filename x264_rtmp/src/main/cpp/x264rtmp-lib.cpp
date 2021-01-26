#include <jni.h>
#include <string>

// 引入log头文件
#include <android/log.h>
// log标签
#define TAG "x264rtmp_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

extern "C" {
#include  "librtmp/rtmp.h"
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    // TODO: implement native_init()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                  jint width, jint height, jint fps,
                                                                  jint bitrate) {
    // TODO: implement native_setVideoEncInfo()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path) {
    // TODO: implement native_start()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                            jbyteArray data) {
    // TODO: implement native_pushVideo()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1stop(JNIEnv *env, jobject thiz) {
    // TODO: implement native_stop()
}