package com.blood.x264_rtmp.channel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.blood.x264_rtmp.push.LivePusher;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.CHANNEL_IN_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

public class AudioChannel {

    private final LivePusher mLivePusher;
    private final int mSampleRate;
    private final int mChannels;
    private final int mChannelConfig;
    private int mMinBufferSize;
    private final byte[] mBuffer;
    private AudioRecord mAudioRecord;
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private boolean mIsLiving;

    public AudioChannel(LivePusher livePusher, int sampleRate, int channels) {
        mLivePusher = livePusher;
        mSampleRate = sampleRate;
        mChannels = channels;

        // 双通道应该传的值   一律用单通道
        mChannelConfig = channels == 2 ? CHANNEL_IN_STEREO : CHANNEL_IN_MONO;
        // 数据大小 是根据mediacodec来的数据 怎么不准确 minBufferSize 参考值 软编 肯定返回 硬编
        // 硬编   不可以    minBufferSize  -1
        mMinBufferSize = AudioRecord.getMinBufferSize(sampleRate, mChannelConfig, ENCODING_PCM_16BIT);
        // 初始化faac软编  inputByteNum  最小容器
        int inputByteNum = livePusher.nativeInitAudioEnc(sampleRate, channels);
        mMinBufferSize = Math.max(mMinBufferSize, inputByteNum);
        mBuffer = new byte[mMinBufferSize];

        mHandlerThread = new HandlerThread("Audio-Record");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void startLive() {
        if (mIsLiving) {
            return;
        }
        mIsLiving = true;
        mHandler.post(() -> {
            mAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    mSampleRate, mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);
            mAudioRecord.startRecording();
            while (mIsLiving && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                // len实际长度len 打印下这个值  录音不成功
                int len = mAudioRecord.read(mBuffer, 0, mBuffer.length);
                Log.i("AudioChannel", "AudioRecord read len: " + len);
                if (len > 0) {
                    mLivePusher.nativeSendAudio(mBuffer, len);
                }
            }
            Log.i("AudioChannel", "AudioRecord release");
            mAudioRecord.release();
            mAudioRecord = null;
        });
    }

    public void stopLive() {
        mIsLiving = false;
    }

}
