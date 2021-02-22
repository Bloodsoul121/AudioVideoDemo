//
// Created by 蔡光祖 on 2/4/21.
//

#ifndef AUDIOVIDEODEMO_VIDEOCHANNEL_H
#define AUDIOVIDEODEMO_VIDEOCHANNEL_H

#include <inttypes.h>
#include <jni.h>
#include <x264.h>
#include "util/JavaCallHelper.h"

class VideoChannel {
public:
    // 构造函数
    VideoChannel();

    // 析构函数
    ~VideoChannel();

    // 将配置信息设置进去，创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);

    // 编码一帧数据
    void encodeframe(int8_t *data);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;

    int ySize;
    int uvSize;

    // 编码器
    x264_t *videoCodec = 0;

    // yuv-->h264 平台 容器 x264_picture_t=bytebuffer
    x264_picture_t *pic_in = 0;

public:
    JavaCallHelper *javaCallHelper;
};

#endif //AUDIOVIDEODEMO_VIDEOCHANNEL_H
