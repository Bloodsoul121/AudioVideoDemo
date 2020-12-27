package com.example.avd.screen.server;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.avd.BaseActivity;
import com.example.avd.R;

import androidx.annotation.Nullable;

public class ServerScreenActivity extends BaseActivity {

    private static final int REQUEST_CODE_SCREEN = 0x01;
    public static final int SOCKET_SERVER_PORT = 10000;

    private Button mBtn1;

    private MediaProjectionManager mMediaProjectionManager;
    private ServerSocketLive mSocketLive;
    private MediaProjection mMediaProjection;

    private boolean mIsStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_server);
        mBtn1 = findViewById(R.id.start_media_projection);
    }

    public void clickBtn1(View view) {
        if (mIsStart) { return; }
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN);
    }

    public void clickBtn2(View view) {
        if (!mIsStart) {
            toast("请先开启录屏");
            return;
        }
        if (mSocketLive == null) {
            mSocketLive = new ServerSocketLive(SOCKET_SERVER_PORT);
        }
        if (mSocketLive.isOpen()) {
            toast("已开启 push");
            return;
        }
        if (mMediaProjection == null) {
            return;
        }
        mSocketLive.start(mMediaProjection);
        toast("开启 push");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) { return; }
        if (requestCode == REQUEST_CODE_SCREEN) {
            if (data == null) { return; }
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            mIsStart = true;
            mBtn1.setText("录屏中");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocketLive != null) {
            mSocketLive.stop();
        }
    }
}