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

    private File mDirFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_a_c);

        mDirFile = new File(Environment.getExternalStorageDirectory(), "aac_clip");
        if (!mDirFile.exists()) {
            boolean mkdir = mDirFile.mkdir();
            Log.i(TAG, "mkdir " + mkdir);
        }

        FileUtil.copyAssetsFile(this, "music2.mp3", mDirFile, "music_copy.mp3");
    }

    public void clickBtn1(View view) {
        new Thread(() -> {
            String aacPath = new File(mDirFile, "music_copy.mp3").getAbsolutePath();
            String wavPath = new File(mDirFile, "music_clip.mp3").getAbsolutePath();
            String tempPcmPath = new File(mDirFile, "temp_pcm.pcm").getAbsolutePath();
            MusicProcess.clip(aacPath, wavPath, tempPcmPath, 15_000_000, 25_000_000);
        }).start();
    }

}