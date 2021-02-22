//
// Created by Administrator on 2021/1/18.
//

#ifndef CAMERA1PUSH_MANIULOG_H
#define CAMERA1PUSH_MANIULOG_H

// 引入log头文件
#include <android/log.h>
// log标签
#define TAG "x264rtmp_native"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif //CAMERA1PUSH_MANIULOG_H
