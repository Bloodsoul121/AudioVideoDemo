package com.blood.bilibili;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.blood.bilibili
 *  @文件名:   LiveScreen
 *  @创建者:   bloodsoul
 *  @创建时间:  2021/1/16 18:52
 *  @描述：    TODO
 */

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

import static com.blood.bilibili.LiveConstant.TAG;

// 传输层，通过 rtmp 传输数据
public class ScreenLive extends Thread {

    static {
        System.loadLibrary("bilibili-lib");
    }

    private String mLiveUrl;
    private MediaProjection mMediaProjection;
    private VideoCodec mVideoCodec;

    private boolean mIsLiving;
    private final LinkedBlockingQueue<RTMPPackage> mQueue = new LinkedBlockingQueue<>();

    public void startLive(String liveUrl, MediaProjection mediaProjection) {
        mLiveUrl = liveUrl;
        mMediaProjection = mediaProjection;
        mVideoCodec = new VideoCodec(this);
        start();
    }

    public void stopLive() {
        mIsLiving = false;
        mVideoCodec.stopLive();
        interrupt();
    }

    public void addPackage(RTMPPackage rtmpPackage) {
        if (mIsLiving) {
            mQueue.add(rtmpPackage);
        }
    }

    @Override
    public void run() {
        if (!connect(mLiveUrl)) {
            Log.i(TAG, "run: ----------->推送失败");
            return;
        }

        mIsLiving = true;

        mVideoCodec.startLive(mMediaProjection);

        while (mIsLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = mQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (rtmpPackage != null && rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(TAG, "run: ----------->推送 " + rtmpPackage.getBuffer().length);
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer().length, rtmpPackage.getTms());
            }
        }
        Log.i(TAG, "run: -----------> ScreenLive over");
    }

    private native boolean connect(String url);

    private native boolean sendData(byte[] data, int len, long tms);

}
