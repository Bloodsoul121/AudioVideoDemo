package com.blood.giflib;

import android.graphics.Bitmap;

public class GifHandler {

    static {
        System.loadLibrary("native-lib");
    }

    private final long mFilePointer;

    public GifHandler(long filePointer) {
        mFilePointer = filePointer;
    }

    public static GifHandler loadGif(String path) {
        long filePointer = native_loadGif(path);
        return new GifHandler(filePointer);
    }

    public int getWidth() {
        return native_getGifWidth(mFilePointer);
    }

    public int getHeight() {
        return native_getGifHeight(mFilePointer);
    }

    public int updateFrame(Bitmap bitmap) {
        return native_updateFrame(mFilePointer, bitmap);
    }

    private static native long native_loadGif(String path);

    private static native int native_getGifWidth(long filePointer);

    private static native int native_getGifHeight(long filePointer);

    private static native int native_updateFrame(long filePointer, Bitmap bitmap);
}
