//
// Created by Administrator on 2020/11/26.
//
#include <jni.h>
#include <string>
#include "android/bitmap.h"

extern "C" {
#include "gif_lib.h"
}

// 引入log头文件
#include  <android/log.h>
// log标签
#define  TAG    "gif_native"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#define  argb(a, r, g, b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

struct GifBean {
    int current_frame; // 当前帧
    int total_frame;
    int *delays;
};

void drawFrame(GifFileType *gifFileType, AndroidBitmapInfo info, void *pixels);

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blood_giflib_GifHandler_native_1loadGif(JNIEnv *env, jclass clazz, jstring path) {
    // 将path转为C的字符串
    const char *gifPath = env->GetStringUTFChars(path, 0);

    // 错误码
    int error;
    // 打开一个新的GIF文件供读取，根据它的名字给出。返回动态分配的GifFileType指针，作为GIF信息记录。
    // 分配内存
    GifFileType *gifFileType = DGifOpenFileName(gifPath, &error);

    // 初始化缓冲区，对属性赋值
    DGifSlurp(gifFileType); // 將整個GIF文件讀入gifFile結構

    GifBean *gifBean = static_cast<GifBean *>(malloc(sizeof(GifBean)));
    memset(gifBean, 0, sizeof(gifBean));
    gifFileType->UserData = gifBean; // 用来存储开发者的数据，类似view设置tag

    gifBean->current_frame = 0;
    gifBean->total_frame = gifFileType->ImageCount; // 总帧数

    // 释放回收字符串
    env->ReleaseStringUTFChars(path, gifPath);

    // 返回指针
    return reinterpret_cast<jlong>(gifFileType);
}

//    typedef struct GifFileType {
//    GifWord SWidth, SHeight;         /* gif的逻辑宽高 */
//    GifWord SColorResolution;        /*  gif文件需要生成的多少种颜色，最大不会超过255 */
//    GifWord SBackGroundColor;        /* 画布的背景颜色 */
//    GifByteType AspectByte;	     /*  用来计算宽高比 */
//    ColorMapObject *SColorMap;       /* 颜色列表，如果局部颜色列表为空，这边表示的是全局颜色列表，否则是局部颜色列表 */
//    int ImageCount;                  /*  gif的图像帧数 */
//    GifImageDesc Image;              /* 当前帧图像 */
//    SavedImage *SavedImages;         /*用来存储已经读取过得图像数据 */
//    int ExtensionBlockCount;         /* 表示图像扩展数据块数量 */
//    ExtensionBlock *ExtensionBlocks; /* 用来存储图像的数据块 */
//    int Error;			     /* 错误码 */
//    void *UserData;                  /* 用来存储开发者的数据，类似view设置tag */
//    void *Private;                   /* Don't mess with this! */
//    } GifFileType;

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1getGifWidth(JNIEnv *env, jclass clazz,
                                                     jlong file_pointer) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(file_pointer);
    return gifFileType->SWidth;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1getGifHeight(JNIEnv *env, jclass clazz,
                                                      jlong file_pointer) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(file_pointer);
    return gifFileType->SHeight;
}

void drawFrame(GifFileType *gifFileType, AndroidBitmapInfo bitmapInfo, void *pixels) {

    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);

    // 当前帧（与理解的帧不是一个东西，相当于压缩后的帧，存着索引）
    SavedImage savedImage = gifFileType->SavedImages[gifBean->current_frame];

    GifImageDesc gifImageDesc = savedImage.ImageDesc; // 描述
    GifByteType *bits = savedImage.RasterBits; // 像素集
    ColorMapObject *colorMapObject = gifImageDesc.ColorMap; // 颜色字典，索引字典

    int *px = (int *) pixels; // 强转为 int * ， 操作二维数组，px 相当于第一个一维数组的第一个元素的起始指针
//    int *line;
    for (int y = gifImageDesc.Top; y < gifImageDesc.Top + gifImageDesc.Height; ++y) { // 列
        // 重新赋值，这里相当于把数组的索引切到下一行了
//        line = px;
        for (int x = gifImageDesc.Left; x < gifImageDesc.Left + gifImageDesc.Width; ++x) { // 行
            int pointerPixel = (y - gifImageDesc.Top) * gifImageDesc.Width + (x - gifImageDesc.Left);
            GifByteType gifByteType = bits[pointerPixel]; // 1个字节
            GifColorType gifColorType = colorMapObject->Colors[gifByteType];
            px[x] = argb(255, gifColorType.Red, gifColorType.Green, gifColorType.Blue); // 4个字节，为什么是从x索引开始赋值
//            px[x + gifImageDesc.Width * (y - gifImageDesc.Top)] = argb(255, gifColorType.Red, gifColorType.Green, gifColorType.Blue); // 这个也是可行的
        }
        // 切换到下一行
//        px = (int *) ((char *) px + bitmapInfo.stride);
        px = px + gifImageDesc.Width;
    }

    LOGI("drawFrame : %d , %d", gifImageDesc.Width, bitmapInfo.stride); // drawFrame : 813 , 3252
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1updateFrame(JNIEnv *env, jclass clazz,
                                                     jlong file_pointer, jobject bitmap) {
    // 获取宽高，这里用bitmap来获取
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(file_pointer);
//    int width = gifFileType->SWidth;
//    int height = gifFileType->SHeight;

    AndroidBitmapInfo bitmapInfo;
    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);

//    int width = bitmapInfo.width;
//    int height = bitmapInfo.height;

    void *pixels;

    // 锁住线程
    AndroidBitmap_lockPixels(env, bitmap, &pixels); // 像素二维数组
    // 绘制
    drawFrame(gifFileType, bitmapInfo, pixels);
    // 释放
    AndroidBitmap_unlockPixels(env, bitmap);

    // 记录当前帧数
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    gifBean->current_frame++;
    if (gifBean->current_frame >= gifBean->total_frame) {
        gifBean->current_frame = 0;
    }

    return 100;
}