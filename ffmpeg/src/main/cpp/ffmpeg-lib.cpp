#include <jni.h>
#include <string>

#include <android/log.h>
#include <android/native_window_jni.h>

// log标签
#define TAG "ffmpeg_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <libavutil/time.h>
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_cgz_ffmpeg_FfmpegActivity_testFfmpeg(JNIEnv *env, jobject thiz) {
    std::string config = avcodec_configuration();
    return env->NewStringUTF(config.c_str());
}

static AVFormatContext *avFormatContext;
static AVCodecContext *avCodecContext;
AVCodec *videoCodec;

ANativeWindow *nativeWindow;
ANativeWindow_Buffer nativeWindowBuffer;

static AVPacket *avPacket;
static AVFrame *avFrame, *rgbFrame;

uint8_t *outBuffer;
static SwsContext *swsContext;

extern "C"
JNIEXPORT jint JNICALL
Java_com_cgz_ffmpeg_FfmpegActivity_play(JNIEnv *env, jobject thiz, jstring url_, jobject surface) {
    const char *url = env->GetStringUTFChars(url_, nullptr);

    // 注册所有组件
    avcodec_register_all();

    // 实例化上下文
    avFormatContext = avformat_alloc_context();

    // 打开文件
    if (avformat_open_input(&avFormatContext, url, nullptr, nullptr) < 0) {
        LOGE("打开文件失败");
        return -1;
    }
    LOGI("打开文件成功");

    // 找到视频流索引
    int videoIndex = -1;
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }
    if (videoIndex == -1) {
        LOGE("没有找到视频流");
        return -1;
    }
    LOGI("找到视频流 index %d", videoIndex);

    // h264 h265 实例化解码器
    avCodecContext = avFormatContext->streams[videoIndex]->codec;
//    AVCodecID avCodecId = avFormatContext->streams[videoIndex]->codecpar->codec_id;
    videoCodec = avcodec_find_decoder(avCodecContext->codec_id);

    // 打开解码器
    if (avcodec_open2(avCodecContext, videoCodec, nullptr) < 0) {
        LOGE("打开解码器失败");
        return -1;
    }
    LOGI("打开解码器成功");

    // 获取上层传下来的surface
    nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (nativeWindow == nullptr) {
        LOGE("获取Surface异常");
        return -1;
    }
    LOGI("获取Surface成功");

    // 三个容器 : 原始数据、解码数据、渲染到surface数据（适配宽高大小）
    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();
    // 跟输入（avFrame）和输出（surface）有关
    rgbFrame = av_frame_alloc();

    // avFrame（解码数据）-> 缓冲区 -> avFrame（渲染数据rgb）-> surface

    int inputWidth = avCodecContext->width;
    int inputHeight = avCodecContext->height;
    // 跟输入（avFrame）
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, inputWidth, inputHeight, 1);
    LOGI("解码后大小：%d", numBytes);
    // 实例化一个缓冲区
    outBuffer = static_cast<uint8_t *>(malloc(numBytes * sizeof(uint8_t)));
    // 填充，将缓冲区设置给rgbFrame
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, outBuffer, AV_PIX_FMT_RGBA, inputWidth,
                         inputHeight, 1);
    LOGI("将缓冲区设置给rgbFrame");
    // 跟输出（surface）有关
    // 转换器上下文、转换器对象（就是一个函数）
    AVPixelFormat avPixelFormat = avCodecContext->pix_fmt;
    if (avPixelFormat == AV_PIX_FMT_NONE) {
        LOGE("不支持AV_PIX_FMT_NONE");
        return -1;
    }
    LOGI("avPixelFormat : %d", avPixelFormat);
    swsContext = sws_getContext(inputWidth, inputHeight, avPixelFormat, inputWidth,
                                inputHeight,
                                AV_PIX_FMT_RGBA, SWS_BICUBIC,
                                nullptr, nullptr, nullptr);
    LOGI("初始化转换器");

    // window 转为 buffer
    if (ANativeWindow_setBuffersGeometry(nativeWindow, inputWidth, inputHeight,
                                         WINDOW_FORMAT_RGBA_8888) < 0) {
        LOGE("window 转为 buffer 失败");
        ANativeWindow_release(nativeWindow);
        return -1;
    }
    LOGI("window 转为 buffer 成功");

    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        // 读出来的是视频数据就处理，音频数据就不管
        if (avPacket->stream_index == videoIndex) {
            int ret = avcodec_send_packet(avCodecContext, avPacket);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR(AVERROR_EOF)) {
                LOGE("解码异常");
                return -1;
            }
            // 取出解压的数据
            ret = avcodec_receive_frame(avCodecContext, avFrame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }
            // 解压缩
            sws_scale(swsContext, avFrame->data, avFrame->linesize, 0, inputHeight, rgbFrame->data,
                      rgbFrame->linesize);
            if (ANativeWindow_lock(nativeWindow, &nativeWindowBuffer, nullptr) < 0) {
                LOGE("can not lock window");
                // 释放
                ANativeWindow_release(nativeWindow);
                nativeWindow = nullptr;
            } else {
                auto *dst = (uint8_t *) nativeWindowBuffer.bits;
                for (int h = 0; h < inputHeight; ++h) {
                    memcpy(dst + h * nativeWindowBuffer.stride * 4,
                           outBuffer + h * rgbFrame->linesize[0], rgbFrame->linesize[0]);
                }
            }
            av_usleep(33);
            ANativeWindow_unlockAndPost(nativeWindow);
        }
    }

    avformat_free_context(avFormatContext);

    env->ReleaseStringUTFChars(url_, url);

    return 0;
}