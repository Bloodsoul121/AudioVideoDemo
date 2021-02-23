package com.blood.x264_rtmp.push;

import android.os.Environment;
import android.util.Log;

import com.blood.x264_rtmp.channel.AudioChannel;
import com.blood.x264_rtmp.channel.VideoChannel;
import com.blood.x264_rtmp.util.FileUtil;

import java.io.File;

public class LivePusher {

    static {
        System.loadLibrary("x264rtmp-lib");
    }

    private final VideoChannel mVideoChannel;
    private final AudioChannel mAudioChannel;

    public LivePusher(int width, int height, int bitrate, int fps) {
        native_init();
        mVideoChannel = new VideoChannel(this, width, height, bitrate, fps);
        mAudioChannel = new AudioChannel();
    }

    public VideoChannel getVideoChannel() {
        return mVideoChannel;
    }

    public void startLive(String path) {
        native_start(path);
        mVideoChannel.startLive();
        mAudioChannel.startLive();
    }

    public void stopLive(){
        mVideoChannel.stopLive();
        mAudioChannel.stopLive();
        native_stop();
    }

    // jni回调java层的方法  byte[] data    char *data
    private void postData(byte[] data) {
        Log.i("rtmp", "postData: "+data.length);
        FileUtil.writeContent(data, new File(Environment.getExternalStorageDirectory(), "x264codec.txt"));
        FileUtil.writeBytes(data, new File(Environment.getExternalStorageDirectory(), "x264codec.h264"));
    }

    // 初始化
    public native void native_init();

    // 绑定真实的宽高
    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void native_start(String path);

    public native void native_pushVideo(byte[] data);

    public native void native_stop();

    public native void native_release();
}
