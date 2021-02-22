//
// Created by 蔡光祖 on 2/4/21.
//

#include <cstring>
#include "VideoChannel.h"
#include "util/logutil.h"

VideoChannel::VideoChannel() {

}

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    // 实例化X264
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;

    ySize = width * height;
    uvSize = ySize / 4;

    // 如果已经开启，则先释放掉
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }

    // 定义参数
    x264_param_t param;
    // 参数赋值   x264  麻烦  编码器 速度   直播  越快 1  越慢2
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    // 编码等级
    param.i_level_idc = 32;
    // 选取显示格式
    param.i_csp = X264_CSP_I420;
    // 宽高必须指定大小
    param.i_width = width;
    param.i_height = height;
    // B帧
    param.i_bframe = 0;
    // 折中    cpu   突发情况   ABR 平均
    param.rc.i_rc_method = X264_RC_ABR;
    // k为单位
    param.rc.i_bitrate = bitrate / 1024;
    // 帧率   1s/25帧     40ms  视频 编码      帧时间 ms存储  us   s
    // 帧率 时间  分子  分母
    param.i_fps_den = 1;
    param.i_fps_num = fps;
    // 分子
    param.i_timebase_num = param.i_fps_den;
    // 分母
    param.i_timebase_den = param.i_fps_num;
    //单位 分子/分母    发热  --
    //用fps而不是时间戳来计算帧间距离
    param.b_vfr_input = 0;
    //I帧间隔（单位：帧） 2s 相当于 15*2 帧
    param.i_keyint_max = fps * 2;

    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;
    // sps  pps  赋值及裙楼
    // 多线程
    param.i_threads = 1;
    x264_param_apply_profile(&param, "baseline");

    // 打开编码器
    videoCodec = x264_encoder_open(&param);
    // 容器
    pic_in = new x264_picture_t;
    // 设置初始化大小  容器大小就确定的
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);

    LOGI("setVideoEncInfo %p", videoCodec);
}

// 拿到yuv数据（nv12 YYYYYYYYUVUV），然后x264编码，生成h264数据
void VideoChannel::encodeframe(int8_t *data) {
    LOGI("encodeframe");
    // 容器 y的数据
    memcpy(pic_in->img.plane[0], data, ySize);
    // uv
    for (int i = 0; i < uvSize; ++i) {
        //间隔1个字节取一个数据
        //u数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2);
        //v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2 + 1);
    }

    //编码成H264码流
    //编码出了几个 nalu （暂时理解为帧）  1   pi_nal  1  永远是1
    int pi_nal;
    //编码出的数据
    x264_nal_t *pp_nal;
    //编码出的参数  BufferInfo
    x264_picture_t pic_out;

    LOGI("encodeframe encode %p %p", videoCodec, pic_in);

    //关键的一句话，编码
    x264_encoder_encode(videoCodec, &pp_nal, &pi_nal, pic_in, &pic_out);

    LOGI("编码帧数 %d", pi_nal);

    if (pi_nal > 0) {
        for (int i = 0; i < pi_nal; i++) {
            javaCallHelper->postH264(reinterpret_cast<char *>(pp_nal[i].p_payload), pp_nal[i].i_payload, THREAD_CHILD);
        }
    }
}


