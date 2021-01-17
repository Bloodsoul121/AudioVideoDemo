#include <jni.h>
#include <string>

// 引入log头文件
#include <android/log.h>
// log标签
#define TAG "bilibili_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

extern "C" {
#include  "librtmp/rtmp.h"
}

typedef struct {
    RTMP *rtmp;
    int8_t *sps; // 指针，8位，相当一个byte
    int8_t *pps;
    int16_t sps_len; // 长度，用两个字节够了
    int16_t pps_len;
} LiveData;

// 定义一个LiveData指针
LiveData *liveData = nullptr;

int sendVideo(jbyte *data, jint len, jlong tms);

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blood_bilibili_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url) {
    const char *rtmp_url = env->GetStringUTFChars(url, nullptr);
    int ret;
    do {
        liveData = (LiveData *) malloc(sizeof(LiveData));
        memset(liveData, 0, sizeof(LiveData));
        liveData->rtmp = RTMP_Alloc();
        RTMP_Init(liveData->rtmp);
        liveData->rtmp->Link.timeout = 10;
        LOGI("connect %s", rtmp_url);
        if (!(ret = RTMP_SetupURL(liveData->rtmp, (char *) rtmp_url))) break;
        RTMP_EnableWrite(liveData->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(liveData->rtmp, nullptr))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(liveData->rtmp, 0))) break;
        LOGI("connect success");
    } while (0);
    if (!ret && liveData) {
        free(liveData);
        liveData = nullptr;
    }
    env->ReleaseStringUTFChars(url, rtmp_url);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_blood_bilibili_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_, jint len,
                                            jlong tms) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    ret = sendVideo(data, len, tms);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}

int sendVideo(jbyte *data, jint len, jlong tms) {
    return 0;
}