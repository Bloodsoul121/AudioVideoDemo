package com.example.avd.h264.encode;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.avd.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class H264EncodeActivity extends AppCompatActivity {

    private static final String TAG = "H264EncodeActivity";
    private static final int REQUEST_CODE = 1;

    private MediaProjectionManager mMediaProjectionManager;
    private VideoCodec mVideoCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_encode);
    }

    public void clickBtn1(View view) {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: " + requestCode + " , " + resultCode + " , " + data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (data == null) {
            return;
        }
        if (mVideoCodec != null && mVideoCodec.isLiving()) {
            return;
        }
        Log.i(TAG, "startLive");
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        mVideoCodec = new VideoCodec();
        mVideoCodec.startLive(mediaProjection);
    }

    public void clickBtn2(View view) {
        Log.i(TAG, "stopLive");
        if (mVideoCodec != null) {
            mVideoCodec.stopLive();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy stopLive");
        if (mVideoCodec != null) {
            mVideoCodec.stopLive();
        }
    }
}
