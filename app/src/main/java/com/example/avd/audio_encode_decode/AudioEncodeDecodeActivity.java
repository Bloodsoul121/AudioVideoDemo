package com.example.avd.audio_encode_decode;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.avd.BaseActivity;
import com.example.avd.R;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AudioEncodeDecodeActivity extends BaseActivity {

    @BindView(R.id.ori_path)
    TextView mOriPath;
    @BindView(R.id.path)
    TextView mPath;

    private String mOriFilePath; // 原始文件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_encode_decode);
        ButterKnife.bind(this);

        mOriPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        mPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    @Override
    protected void onResultSystemSelectedFilePath(String filePath) {
        super.onResultSystemSelectedFilePath(filePath);
        mOriFilePath = filePath;
        mOriPath.setText(filePath);
    }

    public void setOriFilePath(View view) {
        requestSystemFilePath();
    }

    public void decode(View view) {
        String audioPath = mOriFilePath;
        String audioSavePath = Environment.getExternalStorageDirectory() + "/audio_decode.pcm";
        getPCMFromAudio(audioPath, audioSavePath);
    }

    public void encode(View view) {
        String pcmPath = Environment.getExternalStorageDirectory() + "/audio_decode.pcm";
        String audioPath = Environment.getExternalStorageDirectory() + "/audio_encode.m4a";
        pcmToAudio(pcmPath, audioPath);
    }

    /**
     * 将音频文件解码成原始的PCM数据
     *
     * @param audioPath     MP3文件目录
     * @param audioSavePath pcm文件保存位置
     */
    public void getPCMFromAudio(String audioPath, final String audioSavePath) {
        if (TextUtils.isEmpty(audioPath)) {
            Toast.makeText(AudioEncodeDecodeActivity.this, "audio path is null", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(audioPath);

            int audioTrack = -1;
            boolean hasAudio = false;

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String type = trackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    break;
                }
            }

            if (hasAudio) {
                mediaExtractor.selectTrack(audioTrack);

                new Thread(new AudioDecodeRunnable(mediaExtractor, audioTrack, audioSavePath, new DecodeOverListener() {
                    @Override
                    public void decodeOver() {
                        mPath.setText(audioSavePath);
                        toast("decode over");
                    }

                    @Override
                    public void decodeFail() {
                        toast("decode fail");
                    }
                })).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pcmToAudio(String pcmPath, final String audioPath) {
        if (TextUtils.isEmpty(pcmPath)) {
            Toast.makeText(AudioEncodeDecodeActivity.this, "pcm path is null", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new AudioEncodeRunnable(pcmPath, audioPath, new AudioDecodeListener() {
            @Override
            public void decodeOver() {
                toast("encode over");
                mPath.setText(audioPath);
            }

            @Override
            public void decodeFail() {
                toast("encode fail");
            }
        })).start();
    }

    /**
     * 音频解码监听器：监听是否解码成功
     */
    public interface AudioDecodeListener {
        void decodeOver();

        void decodeFail();
    }

    public interface DecodeOverListener {
        void decodeOver();

        void decodeFail();
    }

}
