#include <jni.h>
#include <string>

// 引入log头文件
#include  <android/log.h>
// log标签
#define  TAG    "bilibili_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blood_bilibili_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data, jint len,jlong tms) {
    return 1;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blood_bilibili_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url) {
    return 1;
}






