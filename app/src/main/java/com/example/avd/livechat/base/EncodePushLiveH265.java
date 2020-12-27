package com.example.avd.livechat.base;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.example.avd.util.YuvUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class EncodePushLiveH265 {

    private static final String TAG = "David";

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;

    private final int width;
    private final int height;
    private final SocketLive socketLive;

    private int frameIndex;
    private byte[] nv12; // nv21转换成nv12的数据
    private byte[] yuv; // 旋转之后的yuv数据
    private byte[] vps_sps_pps_buf;
    private MediaCodec mediaCodec;

    public EncodePushLiveH265(SocketLive socketLive, int width, int height) {
        this.socketLive = socketLive;
        this.socketLive.start();
        this.width = width;
        this.height = height;
    }

    public void startLive() {
        // 实例化编码器
        // 创建对应编码器
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //IDR帧刷新时间
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            int bufferLength = width * height * 3 / 2;
            nv12 = new byte[bufferLength];
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        socketLive.close();
        mediaCodec.release();
        mediaCodec = null;
    }

    public void encodeFrame(byte[] input) {
        //        旋转
        //        nv21-nv12
        nv12 = YuvUtils.nv21toNV12(input);
        //        旋转
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(yuv);
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / 15;
    }

    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = (bb.get(offset) & 0x7E) >> 1;
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            socketLive.sendData(newBuf);
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            socketLive.sendData(bytes);
            Log.v(TAG, "视频数据  " + Arrays.toString(bytes));
        }
    }
}
