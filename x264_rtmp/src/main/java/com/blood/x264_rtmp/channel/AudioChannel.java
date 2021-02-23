package com.blood.x264_rtmp.channel;

public class AudioChannel {

    private boolean mIsLiving;

    public void startLive() {
        mIsLiving = true;
    }

    public void stopLive() {
        mIsLiving = false;
    }

}
