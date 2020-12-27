package com.example.avd.screen.server;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.screen.server
 *  @文件名:   CodecTask
 *  @创建者:   bloodsoul
 *  @创建时间:  2020/12/27 8:57
 *  @描述：    TODO
 */

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CodecLiveTask extends Thread {

    private final ServerSocketLive mSocketLive;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec mMediaCodec;
    private boolean mIsRunning;

    public CodecLiveTask(ServerSocketLive socketLive) {
        mSocketLive = socketLive;
    }

    public void startLive(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
        int width = 720;
        int height = 1280;
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface inputSurface = mMediaCodec.createInputSurface();

            mVirtualDisplay = mediaProjection.createVirtualDisplay("screen-live", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, inputSurface, null, null);

            mIsRunning = true;
            start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopLive() {
        mIsRunning = false;
    }

    @Override
    public void run() {

        mMediaCodec.start();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (mIsRunning && !isInterrupted()) {
            int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
            if (index > -1) {
                ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(index);
                dealFrame(byteBuffer, bufferInfo.size);
                mMediaCodec.releaseOutputBuffer(index, false);
            }
        }

        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;

        mVirtualDisplay.release();
        mVirtualDisplay = null;

        mMediaProjection.stop();
        mMediaProjection = null;

    }

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;

    private void dealFrame(ByteBuffer byteBuffer, int size) {
        // 分隔符
        int offset = 4;
        if (byteBuffer.get(2) == 0x01) {
            offset = 3;
        }
        // nalu头
        int type = (byteBuffer.get(offset) & 0x7e) >> 1;
        switch (type) {
            case NAL_VPS:
                vps_sps_pps_buf = new byte[size];
                byteBuffer.get(vps_sps_pps_buf);
                break;
            case NAL_I:
                byte[] idrBytes = new byte[size];
                byteBuffer.get(idrBytes);
                byte[] newBuf = new byte[vps_sps_pps_buf.length + idrBytes.length];
                System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
                System.arraycopy(idrBytes, 0, newBuf, vps_sps_pps_buf.length, idrBytes.length);
                mSocketLive.sendData(newBuf);
                break;
            default:
                byte[] bytes = new byte[size];
                byteBuffer.get(bytes);
                mSocketLive.sendData(bytes);
                break;
        }
    }
}
