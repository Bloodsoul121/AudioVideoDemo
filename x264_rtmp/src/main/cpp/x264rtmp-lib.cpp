#include <jni.h>
#include <string>

// 引入log头文件
#include <android/log.h>
// log标签
#define TAG "x264rtmp_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

extern "C" {
#include  "librtmp/rtmp.h"
}

#include "safe_queue.h"
#include "VideoChannel.h"

VideoChannel *videoChannel = nullptr;
int isStart = 0;//是否已开播
pthread_t pid;//记录子线程的对象
int readyPushing = 0;//推流标志位
SafeQueue<RTMPPacket *> packets;//阻塞式队列
uint32_t start_time;

void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp = nullptr;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp创建失败");
            break;
        }
        RTMP_Init(rtmp);
        //设置超时时间 5s
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        //开启输出模式
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, nullptr);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }
        LOGE("rtmp连接成功----------->:%s", url);

        //准备好了 可以开始推流了
        readyPushing = 1;
        //记录一个开始推流的时间
        start_time = RTMP_GetTime();
        packets.setWork(1);
        RTMPPacket *packet = nullptr;
        //循环从队列取包 然后发送
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    isStart = 0;
    return nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    // 实例化编码层
    videoChannel = new VideoChannel;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                  jint width, jint height, jint fps,
                                                                  jint bitrate) {
    // 配置信息
    videoChannel->setVideoEncInfo(width, height, fps, bitrate);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path_) {
    // 链接rtmp服务器   子线程
    if (isStart) {
        return;
    }
    const char *path = env->GetStringUTFChars(path_, nullptr);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);

    // 开始直播
    isStart = 1;
    // 开子线程链接B站服务器
    pthread_create(&pid, nullptr, start, url);
    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                            jbyteArray data_) {
    // data yuv nv12
    // 没有链接 成功
    if (!videoChannel || !readyPushing) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    videoChannel->encodeframe(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blood_x264_1rtmp_push_LivePusher_native_1stop(JNIEnv *env, jobject thiz) {

}