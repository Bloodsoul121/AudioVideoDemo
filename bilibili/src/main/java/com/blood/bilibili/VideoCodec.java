package com.blood.bilibili;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.blood.bilibili
 *  @文件名:   VideoCodec
 *  @创建者:   bloodsoul
 *  @创建时间:  2021/1/16 19:06
 *  @描述：    TODO
 */

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.blood.bilibili.LiveConstant.TAG;

public class VideoCodec implements Runnable {

    private final ScreenLive mScreenLive;
    private MediaProjection mMediaProjection;
    private MediaCodec mMediaCodec;
    private VirtualDisplay mVirtualDisplay;

    private boolean mIsLiving;
    private long mStartTime;
    private long mTimeStamp;

    public VideoCodec(ScreenLive screenLive) {
        mScreenLive = screenLive;
    }

    public void startLive(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mMediaCodec.createInputSurface();
            mVirtualDisplay = mediaProjection.createVirtualDisplay("screen-codec", 720, 1280, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ThreadPoolUtil.getInstance().start(this);
    }

    public void stopLive() {
        mIsLiving = false;
    }

    @Override
    public void run() {
        mIsLiving = true;
        mMediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (mIsLiving) {
            if (System.currentTimeMillis() - mTimeStamp >= 2000) {
                // dsp 芯片触发I帧
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mMediaCodec.setParameters(params);
                mTimeStamp = System.currentTimeMillis();
            }
            int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >= 0) {
                if (mStartTime == 0) {
                    mStartTime = bufferInfo.presentationTimeUs / 1000; // 毫秒
                }
                ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
                // 发包
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs / 1000) - mStartTime);
                mScreenLive.addPackage(rtmpPackage);
                mMediaCodec.releaseOutputBuffer(index, false);
            }
        }
        mIsLiving = false;
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mMediaProjection.stop();
        mMediaProjection = null;
        mStartTime = 0;
        Log.i(TAG, "run: -----------> VideoCodec over");
    }
}
