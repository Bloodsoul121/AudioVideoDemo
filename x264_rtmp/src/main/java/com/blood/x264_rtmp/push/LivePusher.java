package com.blood.x264_rtmp.push;

import com.blood.x264_rtmp.channel.VideoChannel;

public class LivePusher {

    static {
        System.loadLibrary("x264rtmp-lib");
    }

    private final VideoChannel mVideoChannel;

    public LivePusher(int width, int height, int bitrate, int fps) {
        native_init();
        mVideoChannel = new VideoChannel(this, width, height, bitrate, fps);
    }

    public VideoChannel getVideoChannel() {
        return mVideoChannel;
    }

    public void startLive(String path) {
        native_start(path);
        mVideoChannel.startLive();
    }

    public void stopLive(){
        mVideoChannel.stopLive();
        native_stop();
    }

    // 初始化
    public native void native_init();

    // 绑定真实的宽高
    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void native_start(String path);

    public native void native_pushVideo(byte[] data);

    public native void native_stop();
}
