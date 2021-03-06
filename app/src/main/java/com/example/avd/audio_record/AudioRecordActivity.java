package com.example.avd.audio_record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.avd.BaseActivity;
import com.example.avd.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;

public class AudioRecordActivity extends BaseActivity {

    private static final String TAG = AudioRecordActivity.class.getSimpleName();

    @BindView(R.id.status)
    TextView mStatus;
    @BindView(R.id.dir_path)
    TextView mDirPath;
    @BindView(R.id.ori_path)
    TextView mPlayPath;

    // 初始化配置
    private int mAudioSource;
    private int mSampleRateInHz;
    private int mChannelConfig;
    private int mAudioFormat;
    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    // 其他
    private File mFileDir;
    private String mPcmFileName = "audio.pcm";
    private String mMavFileName = "audio.mav";
    private boolean mIsRecording;
    private boolean mIsPlaying;
    private String mPlayFilePath;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        ButterKnife.bind(this);
        requestPermissions(); // 需要录制权限

        mStatus.setText("等待录制");
        mPlayPath.setText("音频文件：" + Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioRecord.release(); // 回收
    }

    @SuppressLint("CheckResult")
    public void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (aBoolean) {
                    toast("accept");
                    initRecord();
                    initBaseDir();
                } else {
                    toast("deny");
                    finish();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResultSystemSelectedFilePath(String filePath) {
        super.onResultSystemSelectedFilePath(filePath);
        mPlayFilePath = filePath;
        mPlayPath.setText("音频文件：" + filePath);
    }

    private void initRecord() {
        // 指定音频源，设定录音来源为主麦克风
        mAudioSource = MediaRecorder.AudioSource.MIC;
        // 指定采样率(MediaRecoder 的采样率通常是8000Hz CD的通常是44100Hz 不同的Android手机硬件将能够以不同的采样率进行采样。其中11025是一个常见的采样率)
        // 音频采样率，即可每秒中采集多少个音频数据
        mSampleRateInHz = 44100;
        // 指定捕获音频的通道数目.在AudioFormat类中指定用于此的常量
        // 录音通道，MONO单声道，STEREO立体声
//        mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
        mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        // 指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。
        // 因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
        // 音频每个采样点的位数，即音频的精度
        mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        // 音频数据写入缓冲区的总数，通过 AudioRecord.getMinBufferSize 获取最小的缓冲区。
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
        // 对于采样率为16k位深有16bit（2byte）的录制参数，每秒钟的byte数为:16000*2=32000，即每分钟为:32000*60 byte/min= 1.875 MB/min的数据量
    }

    @SuppressLint("SetTextI18n")
    private void initBaseDir() {
        mFileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecord");
        if (!mFileDir.exists()) {
            boolean mkdirs = mFileDir.mkdirs();
            Log.i(TAG, "mkdirs " + mkdirs);
        }
        mDirPath.setText("存储目录：" + mFileDir.getAbsolutePath());
    }

    public void startRecord(View view) {
        mIsRecording = true;
        new Thread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {

                File file = new File(mFileDir, mPcmFileName);
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

                    long startTime = System.currentTimeMillis();

                    // 开始录音
                    mAudioRecord.startRecording();
                    while (mIsRecording) {
                        int result = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);

                        for (int i = 0; i < result; i++) {
                            dos.write(buffer[i]);
                        }

//                        dos.write(buffer);

                        // 更新状态
                        mStatus.setText("录制中 " + (System.currentTimeMillis() - startTime) / 1000 + "s");
                    }

                    mAudioRecord.stop();
                    dos.close();

                    mStatus.setText("录制完成 : " + file.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void stopRecord(View view) {
        mIsRecording = false;
    }

    public void playAudioPcm(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mIsPlaying) {
                    return;
                }
                mIsPlaying = true;
                File file = new File(mFileDir, mPcmFileName);
                try {
                    int bufferSize = AudioTrack.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                    byte[] buffer = new byte[bufferSize];

                    AudioTrack audioTrack = new AudioTrack(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, bufferSize, AudioTrack.MODE_STREAM);
                    audioTrack.play();

                    while (mIsPlaying) {
                        int i = 0;
                        while (dis.available() > 0 && i < bufferSize) {
                            buffer[i] = dis.readByte();
                            i++;
                        }

                        audioTrack.write(buffer, 0, bufferSize);

                        if (i < bufferSize) {
                            audioTrack.stop();
                            audioTrack.release();
                            dis.close();
                            mIsPlaying = false;
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    public void transformPCM2MAV(View view) {
        File pcm = new File(mFileDir, mPcmFileName);
        File mav = new File(mFileDir, mMavFileName);
        long longSampleRate = mSampleRateInHz;
        int channels = mChannelConfig;
        long byteRate = 16 * mSampleRateInHz * channels / 8;
        byte[] data = new byte[mBufferSizeInBytes];
        try {
            FileInputStream in = new FileInputStream(pcm);
            FileOutputStream out = new FileOutputStream(mav);
            // 视频源的总长度
            long totalAudioLen = in.getChannel().size();
            // 由于不包括RIFF和mav
            long totalDataLen = totalAudioLen + 36;

            writeWavFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();

            mStatus.setText("转换成功 : " + mav.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            mStatus.setText("转换失败");
        }
    }

    private void writeWavFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        try {
            out.write(header, 0, 44);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playAudioMav(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = AudioTrack.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
                int streamType = AudioManager.STREAM_SYSTEM;
                AudioTrack audioTrack = new AudioTrack(streamType, mSampleRateInHz, mChannelConfig, mAudioFormat, bufferSize, AudioTrack.MODE_STREAM);
                File mavFile = new File(mFileDir, mMavFileName);
                byte[] buffer = new byte[bufferSize];
                audioTrack.play();

                try {
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(mavFile)));
                    readWavHeader(dis);

                    while (true) {
                        int i = 0;
                        while (dis.available() > 0 && i < bufferSize) {
                            buffer[i] = dis.readByte();
                            i++;
                        }

                        audioTrack.write(buffer, 0, bufferSize);

                        if (i < bufferSize) {
                            audioTrack.stop();
                            audioTrack.release();
                            dis.close();
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void readWavHeader(DataInputStream dis) {
        byte[] byteIntValue = new byte[4];
        byte[] byteShortValue = new byte[2];
        try {
            //读取四个
            String mChunkID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "mChunkID:" + mChunkID);
            dis.read(byteIntValue);
            int chunkSize = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "chunkSize:" + chunkSize);
            String format = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "format:" + format);
            String subchunk1ID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "subchunk1ID:" + subchunk1ID);
            dis.read(byteIntValue);
            int subchunk1Size = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "subchunk1Size:" + subchunk1Size);
            dis.read(byteShortValue);
            short audioFormat = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "audioFormat:" + audioFormat);
            dis.read(byteShortValue);
            short numChannels = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "numChannels:" + numChannels);
            dis.read(byteIntValue);
            int sampleRate = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "sampleRate:" + sampleRate);
            dis.read(byteIntValue);
            int byteRate = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "byteRate:" + byteRate);
            dis.read(byteShortValue);
            short blockAlign = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "blockAlign:" + blockAlign);
            dis.read(byteShortValue);
            short btsPerSample = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "btsPerSample:" + btsPerSample);
            String subchunk2ID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "subchunk2ID:" + subchunk2ID);
            dis.read(byteIntValue);
            int subchunk2Size = byteArrayToInt(byteIntValue);
            Log.e("subchunk2Size", "subchunk2Size:" + subchunk2Size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int byteArrayToInt(byte[] byteIntValue) {
        return ByteBuffer.wrap(byteIntValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private short byteArrayToShort(byte[] byteShortValue) {
        return ByteBuffer.wrap(byteShortValue).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /******************************** 播放指定PCM文件 **********************************/

    public void setOriFilePath(View view) {
        requestSystemFilePath();
    }

    public void playAudioPcm2(View view) {
        String fileName = mPlayFilePath;
        if (TextUtils.isEmpty(fileName)) {
            toast("file name is null");
            return;
        }
        if (mIsPlaying) {
            return;
        }

        mIsPlaying = true;
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int bufferSize = AudioTrack.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
            AudioTrack audioTrack = new AudioTrack(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, bufferSize, AudioTrack.MODE_STREAM);
            byte[] buffer = new byte[bufferSize];
            audioTrack.play();

            while (mIsPlaying) {
                int i = 0;
                while (dis.available() > 0 && i < bufferSize) {
                    buffer[i] = dis.readByte();
                    i++;
                }

                audioTrack.write(buffer, 0, bufferSize);

                if (i < bufferSize) {
                    audioTrack.stop();
                    audioTrack.release();
                    dis.close();
                    mIsPlaying = false;
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
