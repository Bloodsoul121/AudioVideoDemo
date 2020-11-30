#include <jni.h>
#include <string>

// log标签
#define  TAG    "native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

void toast(JNIEnv *env, jobject thiz) {
    jclass jclazz = env->GetObjectClass(thiz);
    jmethodID id = env->GetMethodID(jclazz, "toast", "()V");
    env->CallVoidMethod(thiz, id);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_jni_JniActivity_changeField(JNIEnv *env, jobject thiz, jstring msg) {
    jclass jclazz = env->GetObjectClass(thiz);
    jfieldID id = env->GetFieldID(jclazz, "mField", "Ljava/lang/String;");
    env->SetObjectField(thiz, id, msg);

    // 回调java层
    toast(env, thiz);
}
