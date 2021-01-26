package com.blood.x264_rtmp.channel;

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
        if (mIsLiving) {
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
