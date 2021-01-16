package com.blood.bilibili;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ScreenBilibiliActivity extends AppCompatActivity {

    private static final String LIVE_URL = "rtmp://live-push.bilivideo.com/live-bvc/";

    private MediaProjectionManager mMediaProjectionManager;

    private final ScreenLive mScreenLive = new ScreenLive();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_bilibili);
    }

    public void startLive(View view) {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 100) {
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            mScreenLive.startLive(LIVE_URL, mediaProjection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScreenLive.stopLive();
    }
}