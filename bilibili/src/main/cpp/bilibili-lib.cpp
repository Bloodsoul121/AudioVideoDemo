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

int sendVideo(const int8_t *buf, int len, long tms);

int sendAudio(const int8_t *buf, int len, long tms, int type);

int sendPacket(RTMPPacket *packet);

void prepareSpsPps(const int8_t *buf, int len, LiveData *data);

RTMPPacket *createSpsPpsPackage(LiveData *data);

RTMPPacket *createVideoPackage(const int8_t *buf, int len, long tms, LiveData *data);

RTMPPacket *createAudioPacket(const int8_t *buf, int len, long tms, int type, LiveData *data);

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
                                            jlong tms, jint type) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    switch (type) {
        case 0:
            ret = sendVideo(data, len, tms); // video
            break;
        default:
            ret = sendAudio(data, len, tms, type); // audio
            break;
    }
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}

int sendAudio(const int8_t *buf, int len, long tms, int type) {
    RTMPPacket *packet = createAudioPacket(buf, len, tms, type, liveData);
    int ret = sendPacket(packet);
    return ret;
}

int sendVideo(const int8_t *buf, int len, long tms) {
    int ret = 0;
    // sps
    if (buf[4] == 0x67) {
        if (liveData && (!liveData->sps || !liveData->pps)) {
            // 解析sps，pps数据
            prepareSpsPps(buf, len, liveData);
            return ret;
        }
    }
    // 关键帧，先发送sps，pps包
    if (buf[4] == 0x65) {
        RTMPPacket *packet = createSpsPpsPackage(liveData);
        sendPacket(packet);
    }
    // 发送关键帧和非关键帧包
    RTMPPacket *packet = createVideoPackage(buf, len, tms, liveData);
    ret = sendPacket(packet);
    return ret;
}

int sendPacket(RTMPPacket *packet) {
    int ret = RTMP_SendPacket(liveData->rtmp, packet, 1);
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

RTMPPacket *createVideoPackage(const int8_t *buf, int len, long tms, LiveData *data) {
    buf += 4;
    len -= 4;
    // 长度
    int body_size = len + 9;
    // 初始化RTMP内部的body数组
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);

    if (buf[0] == 0x65) {
        packet->m_body[0] = 0x17;
        LOGI("发送关键帧 data");
    } else {
        packet->m_body[0] = 0x27;
        LOGI("发送非关键帧 data");
    }

    //    固定的大小
    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    //长度
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;

    //数据
    memcpy(&packet->m_body[9], buf, len);
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = data->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createSpsPpsPackage(LiveData *data) {
    // sps  pps 的 packaet
    int body_size = 16 + data->sps_len + data->pps_len;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    // 实例化数据包
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    packet->m_body[i++] = 0x17;
    //AVC sequence header 设置为0x00
    packet->m_body[i++] = 0x00;
    //CompositionTime
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //AVC sequence header
    packet->m_body[i++] = 0x01;
    //    原始 操作
    packet->m_body[i++] = data->sps[1]; //profile 如baseline、main、 high
    packet->m_body[i++] = data->sps[2]; //profile_compatibility 兼容性
    packet->m_body[i++] = data->sps[3]; //profile level
    packet->m_body[i++] = 0xFF;//已经给你规定好了
    packet->m_body[i++] = 0xE1; //reserved（111） + lengthSizeMinusOne（5位 sps 个数） 总是0xe1
    // 高八位
    packet->m_body[i++] = (data->sps_len >> 8) & 0xFF;
    // 低八位
    packet->m_body[i++] = data->sps_len & 0xff;
    // 拷贝sps的内容
    memcpy(&packet->m_body[i], data->sps, data->sps_len);
    i += data->sps_len;
    // pps
    packet->m_body[i++] = 0x01; //pps number
    // rtmp 协议
    //pps length
    packet->m_body[i++] = (data->pps_len >> 8) & 0xff;
    packet->m_body[i++] = data->pps_len & 0xff;
    // 拷贝pps内容
    memcpy(&packet->m_body[i], data->pps, data->pps_len);
    // packaet
    // 视频类型
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    // 视频 04
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = data->rtmp->m_stream_id;
    return packet;
}

void prepareSpsPps(const int8_t *buf, int len, LiveData *data) {
    for (int i = 0; i < len; i++) {
        if (i + 4 < len) {
            if (buf[i] == 0x00 && buf[i + 1] == 0x00 && buf[i + 2] == 0x00 && buf[i + 3] == 0x01) {
                // pps
                if (buf[i + 4] == 0x68) {
                    data->sps_len = i - 4;
                    data->sps = (int8_t *) malloc(data->sps_len);
                    memcpy(data->sps, buf + 4, data->sps_len);
                    data->pps_len = len - data->sps_len - 4 - 4;
                    data->pps = (int8_t *) malloc(data->pps_len);
                    memcpy(data->pps, buf + 4 + data->sps_len + 4, data->sps_len);
                    LOGI("sps:%d pps:%d", data->sps_len, data->pps_len);
                    break;
                }
            }
        }
    }
}

RTMPPacket *createAudioPacket(const int8_t *buf, int len, long tms, int type, LiveData *data) {
    // 组装音频包  两个字节   是固定的   af    如果是第一次发  你就是 01   如果后面  00  或者是 01  aac
    int body_size = len + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    // 音频头
    packet->m_body[0] = 0xAF;
    // 头
    if (type == 1) {
        packet->m_body[1] = 0x00;
    } else {
        packet->m_body[1] = 0x01;
    }
    memcpy(&packet->m_body[2], buf, len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x05;
    packet->m_nBodySize = body_size;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = data->rtmp->m_stream_id;
    return packet;
}
