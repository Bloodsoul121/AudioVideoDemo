package com.example.avd.h264.encode;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

class VideoCodec extends Thread {

    private static final String TAG = "VideoCodec";

    private MediaProjection mMediaProjection;
    private MediaCodec mMediaCodec;
    private VirtualDisplay mVirtualDisplay;
    private boolean mIsLiving;
    private long mStartTime;

    public VideoCodec() {
    }

    public boolean isLiving() {
        return mIsLiving;
    }

    public void startLive(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
        initMediaCodec();
        start();
    }

    public void stopLive() {
        mIsLiving = false;
    }

    private void initMediaCodec() {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 540, 960);
        // 编码器一定需要这些参数！！！
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 提供一个surface，与MediaProjection进行连接，MediaProjection录制的每一帧数据就会填充到surface，然后传入dso芯片编码
            Surface surface = mMediaCodec.createInputSurface();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-codec", 540, 960, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        mIsLiving = true;
        mMediaCodec.start();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        long timeStamp = 0;
        while (mIsLiving) {
            //2000毫秒 手动触发输出关键帧
            if (System.currentTimeMillis() - timeStamp >= 2_000) {
                Bundle params = new Bundle();
                //立即刷新 让下一帧是关键帧
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mMediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }

            int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100_000);
            if (index > -1) {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(index);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] outData = new byte[outputBuffer.remaining()];
                outputBuffer.get(outData);
                outputBuffer.clear();

                if (mStartTime == 0) {
                    // 微妙转为毫秒
                    mStartTime = bufferInfo.presentationTimeUs / 1000;
                }

                // 现在可以处理 outData ，输出的已经编码好的 h264 码流
                // byte文本，供分析测试
                FileUtil.writeContent(outData, new File(Environment.getExternalStorageDirectory(), "codec.txt"));
                // byte字节，可播放文件
                FileUtil.writeBytes(outData, new File(Environment.getExternalStorageDirectory(), "codec.h264"));

                // 释放 buffer
                mMediaCodec.releaseOutputBuffer(index, false);
            }
        }

        Log.i(TAG, "thread end");

        mIsLiving = false;
        mStartTime = 0;

        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;

        mVirtualDisplay.release();
        mVirtualDisplay = null;

        mMediaProjection.stop();
        mMediaProjection = null;
    }

}
