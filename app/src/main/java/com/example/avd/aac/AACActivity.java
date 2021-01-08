package com.example.avd.aac;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.avd.R;
import com.example.avd.util.FileUtil;

import java.io.File;

public class AACActivity extends AppCompatActivity {

    private static final String TAG = "AACActivity";

    private final File mDirFile = new File(Environment.getExternalStorageDirectory(), "aac_clip");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_a_c);
        init();
    }

    private void init() {
        new Thread(() -> {
            if (mDirFile.exists()) {
                FileUtil.deleteDir(mDirFile.getAbsolutePath());
            }
            boolean mkdir = mDirFile.mkdir();
            Log.i(TAG, "mkdir " + mkdir);

            FileUtil.copyAssetsFile(AACActivity.this, "input2.mp4", mDirFile, "video_copy.mp4");
            FileUtil.copyAssetsFile(AACActivity.this, "music2.mp3", mDirFile, "music_copy.mp3");
            Log.i(TAG, "copyAssetsFile over");
        }).start();
    }

    public void clickBtn1(View view) {
        new Thread(() -> {
            String aacPath = new File(mDirFile, "music_copy.mp3").getAbsolutePath();
            String wavPath = new File(mDirFile, "music_clip.mp3").getAbsolutePath();
            String tempPcmPath = new File(mDirFile, "temp_pcm.pcm").getAbsolutePath();
            MusicProcess.clip(aacPath, wavPath, tempPcmPath, 15_000_000, 25_000_000);
        }).start();
    }

    public void clickBtn2(View view) {
        new Thread(() -> {
            String aacPath = new File(mDirFile, "video_copy.mp4").getAbsolutePath();
            String wavPath = new File(mDirFile, "video_clip.mp3").getAbsolutePath();
            String tempPcmPath = new File(mDirFile, "temp_pcm.pcm").getAbsolutePath();
            MusicProcess.clip(aacPath, wavPath, tempPcmPath, 25_000_000, 35_000_000);
        }).start();
    }

    public void clickBtn3(View view) {
        new Thread(() -> {
            String videoInput = new File(mDirFile, "video_copy.mp4").getAbsolutePath();
            String audioInput = new File(mDirFile, "music_copy.mp3").getAbsolutePath();
            String output = new File(mDirFile, "mix.mp3").getAbsolutePath();
            String videoInputTemp = new File(mDirFile, "video_temp_pcm.pcm").getAbsolutePath();
            String audioInputTemp = new File(mDirFile, "audio_temp_pcm.pcm").getAbsolutePath();
            String mixTemp = new File(mDirFile, "mix_temp_pcm.pcm").getAbsolutePath();
            MusicProcess.mixAudioTrack(
                    videoInput,
                    audioInput,
                    output,
                    videoInputTemp,
                    audioInputTemp,
                    mixTemp,
                    25_000_000,
                    35_000_000,
                    100,
                    100
            );
        }).start();
    }
}