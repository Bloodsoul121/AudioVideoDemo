package com.blood.bilibili;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.blood.bilibili
 *  @文件名:   AudioCodec
 *  @创建者:   bloodsoul
 *  @创建时间:  2021/1/17 14:45
 *  @描述：    TODO
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.blood.bilibili.LiveConstant.TAG;

public class AudioCodec implements Runnable {

    private final ScreenLive mScreenLive;
    private boolean mIsRecoding;
    private int mMinBufferSize;
    private MediaCodec mMediaCodec;
    private AudioRecord mAudioRecord;
    private long mStartTime;

    public AudioCodec(ScreenLive screenLive) {
        mScreenLive = screenLive;
    }

    public void startLive() {
        try {
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); // 录音质量
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64_000); // 码率
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            //录音工具类  采样位数 通道数 采样频率   固定了   设备没关系  录音 数据一样的
            mMinBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ThreadPoolUtil.getInstance().start(this);
    }

    public void stopLive() {
        mIsRecoding = false;
    }

    @Override
    public void run() {
        mIsRecoding = true;
        mMediaCodec.start();
        mAudioRecord.startRecording();

        // 发送音频之前，空数据头
        RTMPPackage rtmpPackage = new RTMPPackage();
        byte[] audioDecoderSpecificInfo = {0x12, 0x08};
        rtmpPackage.setBuffer(audioDecoderSpecificInfo);
        rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_HEAD);
        mScreenLive.addPackage(rtmpPackage);

        Log.i(TAG, "开始录音  minBufferSize： " + mMinBufferSize);

        byte[] buffer = new byte[mMinBufferSize];
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (mIsRecoding) {

            int len = mAudioRecord.read(buffer, 0, buffer.length);

            if (len <= 0) {
                continue;
            }

            int index = mMediaCodec.dequeueInputBuffer(0);
            if (index >= 0) {
                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                inputBuffer.put(buffer, 0, len);
                //填充数据后再加入队列
                mMediaCodec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
            }
            index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            while (index >= 0 && mIsRecoding) {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData); //编码好的数据
                if (mStartTime == 0) {
                    mStartTime = bufferInfo.presentationTimeUs / 1000;
                }
                rtmpPackage = new RTMPPackage();
                rtmpPackage.setBuffer(outData);
                //                native    音频数据
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA);
                //设置时间戳
                long tms = (bufferInfo.presentationTimeUs / 1000) - mStartTime;
                rtmpPackage.setTms(tms);
                mScreenLive.addPackage(rtmpPackage);
                mMediaCodec.releaseOutputBuffer(index, false);
                index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
        mIsRecoding = false;
        mStartTime = 0;
        Log.i(TAG, "run: -----------> AudioCodec over");
    }
}
