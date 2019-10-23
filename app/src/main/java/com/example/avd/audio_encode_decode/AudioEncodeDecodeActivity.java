package com.example.avd.audio_encode_decode;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.avd.BaseActivity;
import com.example.avd.R;

import java.io.File;
import java.io.IOException;

public class AudioEncodeDecodeActivity extends BaseActivity {

    private String mOriFilePath; // 原始文件

    private TextView mPathTv;
    private EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_encode_decode);

        mEdit = findViewById(R.id.edit);
        mPathTv = findViewById(R.id.path);
        mPathTv.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public void setOriFilePath(View view) {
        File oriFile = new File(Environment.getExternalStorageDirectory(), mEdit.getText().toString());
        mOriFilePath = oriFile.getAbsolutePath();
        mPathTv.setText(mOriFilePath);
    }

    public void decode(View view) {
        String audioPath = mOriFilePath;
        String audioSavePath = Environment.getExternalStorageDirectory() + "/aaa.pcm";
        getPCMFromAudio(audioPath, audioSavePath);
    }

    public void encode(View view) {
        String pcmPath = Environment.getExternalStorageDirectory() + "/aaa.pcm";
        String audioPath = Environment.getExternalStorageDirectory() + "/aaa.m4a";
        pcmToAudio(pcmPath, audioPath);
    }

    /**
     * 将音频文件解码成原始的PCM数据
     * @param audioPath         MP3文件目录
     * @param audioSavePath     pcm文件保存位置
     */
    public void getPCMFromAudio(String audioPath, final String audioSavePath){
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
                        mPathTv.setText(audioSavePath);
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
                mPathTv.setText(audioPath);
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
    public interface AudioDecodeListener{
        void decodeOver();
        void decodeFail();
    }

    public interface DecodeOverListener{
        void decodeOver();
        void decodeFail();
    }

}
