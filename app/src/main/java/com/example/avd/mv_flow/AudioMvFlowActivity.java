package com.example.avd.mv_flow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.avd.BaseActivity;
import com.example.avd.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;

public class AudioMvFlowActivity extends BaseActivity {

    private static final String TAG = AudioMvFlowActivity.class.getSimpleName();

    @BindView(R.id.camera_preview)
    AudioMvFlowCameraView mCameraPreview;
    @BindView(R.id.record_red_icon)
    ImageView mRecordRedIcon;
    @BindView(R.id.start_record)
    Button mStartRecord;
    @BindView(R.id.stop_record)
    Button mStopRecord;

    // 初始化配置
    private int mAudioSource;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    // 其他
    private static final String mPcmFileName = "audio.pcm";
    private static final String mH264FileName = "media.h264";
    private static final String mCombineFileName = "mv.mp4";
    private File mFlowFileDir;
    private boolean mIsRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_mv_flow);
        ButterKnife.bind(this);
        mCameraPreview.bindActivity(this);
        mCameraPreview.setCallback(null);
        mRecordRedIcon.setActivated(false);
        mStopRecord.setEnabled(false);
        requestPermissions();
    }

    @SuppressLint("CheckResult")
    public void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (aBoolean) {
                    Toast.makeText(AudioMvFlowActivity.this, "accept", Toast.LENGTH_SHORT).show();
                    initRecord();
                    initBaseDir();
                } else {
                    Toast.makeText(AudioMvFlowActivity.this, "deny", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void initRecord() {
        // 指定音频源
        mAudioSource = MediaRecorder.AudioSource.MIC;
        // 指定采样率(MediaRecoder 的采样率通常是8000Hz CD的通常是44100Hz 不同的Android手机硬件将能够以不同的采样率进行采样。其中11025是一个常见的采样率)
        mSampleRateInHz = 44100;
        // 指定捕获音频的通道数目.在AudioFormat类中指定用于此的常量
        mChannelConfig = AudioFormat.ENCODING_PCM_16BIT;
        // 指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。
        // 因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
        mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
    }

    @SuppressLint("SetTextI18n")
    private void initBaseDir() {
        mFlowFileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio_mv_flow");
        if (!mFlowFileDir.exists()) {
            boolean mkdirs = mFlowFileDir.mkdirs();
            Log.i(TAG, "AudioFileDir mkdirs " + mkdirs);
        }
        File file = new File(mFlowFileDir, mH264FileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCameraPreview.setOutputFile(file);
    }

    public void startRecord(View view) {
        mIsRecording = true;
        mCameraPreview.toggleVideo();
        startRecordAudio();
        mStartRecord.setEnabled(false);
        mStopRecord.setEnabled(true);
    }

    public void stopRecord(View view) {
        mIsRecording = false;
        mCameraPreview.closeVideo();
        mStopRecord.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                combineVideo();
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraPreview.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioRecord.release(); // 回收
        mCameraPreview.closeVideo();
    }

    private void startRecordAudio() {
        mIsRecording = true;
        new Thread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {

                File file = new File(mFlowFileDir, mPcmFileName);
                if (file.exists()) {
                    file.delete();
                }
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    // 保持数据的原样性读出来写出去
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    byte[] buffer = new byte[mBufferSizeInBytes];

                    // 开始录音
                    mAudioRecord.startRecording();
                    while (mIsRecording) {
                        mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
                        dos.write(buffer);
                        // 更新状态
                        mRecordRedIcon.setActivated(true);
                    }

                    mAudioRecord.stop();
                    dos.close();
                    mRecordRedIcon.setActivated(false);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void combineVideo() {
        File media = new File(mFlowFileDir, mH264FileName);
        if (!media.exists()) {
            toast("media is not exist");
            return;
        }

        File audio = new File(mFlowFileDir, mPcmFileName);
        if (!audio.exists()) {
            toast("audio is not exist");
            return;
        }

        File mv = new File(mFlowFileDir, mCombineFileName);
        if (mv.exists()) {
            mv.delete();
        }
        try {
            mv.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            toast("mv create failed");
            return;
        }

        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(media.getAbsolutePath());
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            MediaFormat mediaTrackFormat = null;
            for (int i = 0; i < videoTrackCount; i++) {
                mediaTrackFormat = videoExtractor.getTrackFormat(i);
                String type = mediaTrackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audio.getAbsolutePath());
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            MediaFormat audioTrackFormat = null;
            for (int i = 0; i < audioTrackCount; i++) {
                audioTrackFormat = audioExtractor.getTrackFormat(i);
                String type = audioTrackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            MediaMuxer mediaMuxer = new MediaMuxer(mv.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            assert mediaTrackFormat != null;
            int writeVideoTrackIndex = mediaMuxer.addTrack(mediaTrackFormat);
            assert audioTrackFormat != null;
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioTrackFormat);

            mediaMuxer.start();

            ByteBuffer byteBufferMedia = ByteBuffer.allocate(1024 * 500);
            videoExtractor.readSampleData(byteBufferMedia, 0);
            if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoExtractor.advance();
            }
            videoExtractor.readSampleData(byteBufferMedia, 0);
            long secondTime = videoExtractor.getSampleTime();
            videoExtractor.advance();
            long thirdTime = videoExtractor.getSampleTime();
            long videoSampleTime = Math.abs(thirdTime - secondTime);
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);

            ByteBuffer byteBufferAudio = ByteBuffer.allocate(500 * 1024);
            //获取帧之间的间隔时间
            audioExtractor.readSampleData(byteBufferAudio, 0);
            if (audioExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                audioExtractor.advance();
            }
            audioExtractor.readSampleData(byteBufferAudio, 0);
            long secondTime2 = audioExtractor.getSampleTime();
            audioExtractor.advance();
            long thirdTime2 = audioExtractor.getSampleTime();
            long audioSampleTime = Math.abs(thirdTime2 - secondTime2);
            audioExtractor.unselectTrack(audioTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBufferMedia, 0);
                if (readVideoSampleSize < 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += videoSampleTime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBufferMedia, videoBufferInfo);
                videoExtractor.advance();
            }

            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBufferAudio, 0);
                if (readAudioSampleSize < 0) {
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs += audioSampleTime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = audioExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBufferAudio, audioBufferInfo);
                audioExtractor.advance();
            }

            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();

            toast("combine video done");

        } catch (IOException e) {
            e.printStackTrace();
            toast("combine video failed");
        }
    }

}
