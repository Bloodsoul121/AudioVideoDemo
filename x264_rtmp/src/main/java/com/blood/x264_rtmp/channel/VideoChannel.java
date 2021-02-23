package com.blood.x264_rtmp.channel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.blood.x264_rtmp.push.LivePusher;
import com.blood.x264_rtmp.util.ImageUtil;

public class VideoChannel {

    private final LivePusher mLivePusher;
    private final int mWidth;
    private final int mHeight;
    private final int mBitrate;
    private final int mFps;

    private boolean mIsLiving;
    private byte[] nv21;
    private byte[] nv21_rotated;
    private byte[] nv12;

    private boolean isFirstInit = true;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsTest = false;

    public VideoChannel(LivePusher livePusher, int width, int height, int bitrate, int fps) {
        mLivePusher = livePusher;
        mWidth = width;
        mHeight = height;
        mBitrate = bitrate;
        mFps = fps;
    }

    public void startLive() {
        mIsLiving = true;
    }

    public void stopLive() {
        mIsLiving = false;
    }

    public void onSizeChanged(int width, int height) {
        // 初始化编码器，宽高互换
        mLivePusher.native_setVideoEncInfo(height, width, mFps, mBitrate);
    }

    public void analyzeFrameData(byte[] y, byte[] u, byte[] v, int width, int height, int rowStride) {
        if (mIsTest) {
            mHandler.post(() -> analyze(y, u, v, width, height, rowStride));
        } else {
            analyze(y, u, v, width, height, rowStride);
        }
    }

    private void analyze(byte[] y, byte[] u, byte[] v, int width, int height, int rowStride) {
        if (mIsLiving) {
            if (isFirstInit) {
                isFirstInit = false;
                // 配置真实宽高，以最终传到编码器的为准
                mLivePusher.native_setVideoEncInfo(height, width, mFps, mBitrate);
            }
            if (nv21 == null) {
                nv21 = new byte[rowStride * height * 3 / 2];
                nv21_rotated = new byte[rowStride * height * 3 / 2];
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, rowStride, height);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, rowStride, height);
            nv12 = ImageUtil.nv21toNV12(nv21_rotated);
            mLivePusher.native_pushVideo(nv12);
        }
    }

}
