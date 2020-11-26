//
// Created by Administrator on 2020/11/26.
//
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blood_giflib_GifHandler_native_1loadGif(JNIEnv *env, jclass clazz, jstring path) {

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1getGifWidth(JNIEnv *env, jclass clazz,
                                                     jlong file_pointer) {

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1getGifHeight(JNIEnv *env, jclass clazz,
                                                      jlong file_pointer) {

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blood_giflib_GifHandler_native_1updateFrame(JNIEnv *env, jclass clazz,
                                                     jlong file_pointer, jobject bitmap) {

}