#include <jni.h>
#include <string>

// 引入log头文件
#include <android/log.h>
// log标签
#define TAG "ffmpeg_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

extern "C" {
#include <libavcodec/avcodec.h>
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_cgz_ffmpeg_FfmpegActivity_testFfmpeg(JNIEnv *env, jobject thiz) {
    std::string config = avcodec_configuration();
    return env->NewStringUTF(config.c_str());
}
