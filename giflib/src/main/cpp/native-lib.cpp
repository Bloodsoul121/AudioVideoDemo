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
#define  dispose(ext) (((ext)->Bytes[0] & 0x1c) >> 2)
#define  trans_index(ext) ((ext)->Bytes[3])
#define  transparency(ext) ((ext)->Bytes[0] & 1)

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

int drawFrame(GifFileType* gif,AndroidBitmapInfo  info,   void* pixels,  bool force_dispose_1) {
    GifColorType *bg;

    GifColorType *color;

    SavedImage * frame;

    ExtensionBlock * ext = 0;

    GifImageDesc * frameInfo;

    ColorMapObject * colorMap;

    int *line;

    int width, height,x,y,j,loc,n,inc,p;

    void* px;

    GifBean *gifBean = static_cast<GifBean *>(gif->UserData);

    width = gif->SWidth;

    height = gif->SHeight;
    frame = &(gif->SavedImages[gifBean->current_frame]);

    frameInfo = &(frame->ImageDesc);

    if (frameInfo->ColorMap) {

        colorMap = frameInfo->ColorMap;

    } else {

        colorMap = gif->SColorMap;

    }



    bg = &colorMap->Colors[gif->SBackGroundColor];



    for (j=0; j<frame->ExtensionBlockCount; j++) {

        if (frame->ExtensionBlocks[j].Function == GRAPHICS_EXT_FUNC_CODE) {

            ext = &(frame->ExtensionBlocks[j]);

            break;

        }

    }
    // For dispose = 1, we assume its been drawn
    px = pixels;
    if (ext && dispose(ext) == 1 && force_dispose_1 && gifBean->current_frame > 0) {
        gifBean->current_frame=gifBean->current_frame-1,
                drawFrame(gif , info, pixels,  true);
    }

    else if (ext && dispose(ext) == 2 && bg) {

        for (y=0; y<height; y++) {

            line = (int*) px;

            for (x=0; x<width; x++) {

                line[x] = argb(255, bg->Red, bg->Green, bg->Blue);

            }

            px = (int *) ((char*)px + info.stride);

        }

    } else if (ext && dispose(ext) == 3 && gifBean->current_frame > 1) {
        gifBean->current_frame=gifBean->current_frame-2,
                drawFrame(gif,  info, pixels,  true);

    }
    px = pixels;
    if (frameInfo->Interlace) {

        n = 0;

        inc = 8;

        p = 0;

        px = (int *) ((char*)px + info.stride * frameInfo->Top);

        for (y=frameInfo->Top; y<frameInfo->Top+frameInfo->Height; y++) {

            for (x=frameInfo->Left; x<frameInfo->Left+frameInfo->Width; x++) {

                loc = (y - frameInfo->Top)*frameInfo->Width + (x - frameInfo->Left);

                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {

                    continue;

                }



                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg : &colorMap->Colors[frame->RasterBits[loc]];

                if (color)

                    line[x] = argb(255, color->Red, color->Green, color->Blue);

            }

            px = (int *) ((char*)px + info.stride * inc);

            n += inc;

            if (n >= frameInfo->Height) {

                n = 0;

                switch(p) {

                    case 0:

                        px = (int *) ((char *)pixels + info.stride * (4 + frameInfo->Top));

                        inc = 8;

                        p++;

                        break;

                    case 1:

                        px = (int *) ((char *)pixels + info.stride * (2 + frameInfo->Top));

                        inc = 4;

                        p++;

                        break;

                    case 2:

                        px = (int *) ((char *)pixels + info.stride * (1 + frameInfo->Top));

                        inc = 2;

                        p++;

                }

            }

        }

    }

    else {

        px = (int *) ((char*)px + info.stride * frameInfo->Top);

        for (y=frameInfo->Top; y<frameInfo->Top+frameInfo->Height; y++) {

            line = (int*) px;

            for (x=frameInfo->Left; x<frameInfo->Left+frameInfo->Width; x++) {

                loc = (y - frameInfo->Top)*frameInfo->Width + (x - frameInfo->Left);

                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {

                    continue;

                }

                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg : &colorMap->Colors[frame->RasterBits[loc]];

                if (color)

                    line[x] = argb(255, color->Red, color->Green, color->Blue);

            }

            px = (int *) ((char*)px + info.stride);

        }
    }
    GraphicsControlBlock gcb;//获取控制信息
    DGifSavedExtensionToGCB(gif,gifBean->current_frame,&gcb);
    int delay=gcb.DelayTime * 10; // gcb.DelayTime 单位10ms，转为ms，就要 *10
    LOGE("delay %d",delay);
    return delay; // ms
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
    int delay = drawFrame(gifFileType, bitmapInfo, pixels, false);
    // 释放
    AndroidBitmap_unlockPixels(env, bitmap);

    // 记录当前帧数
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    gifBean->current_frame++;
    if (gifBean->current_frame >= gifBean->total_frame) {
        gifBean->current_frame = 0;
    }

    return delay;
}