package com.example.avd.livechat.server;

import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.avd.R;
import com.example.avd.livechat.base.DecodePlayerLiveH265;
import com.example.avd.livechat.base.LocalSurfaceView;
import com.example.avd.livechat.base.SocketCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ServerLiveChatActivity extends AppCompatActivity implements SocketCallback {

    private SurfaceView remoteSurfaceView;
    private LocalSurfaceView localSurfaceView;
    private DecodePlayerLiveH265 mDecodePlayerLiveH265;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_live_chat);
        initView();
    }

    private void initView() {
        localSurfaceView = findViewById(R.id.localSurfaceView);
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
        remoteSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                mDecodePlayerLiveH265 = new DecodePlayerLiveH265();
                mDecodePlayerLiveH265.initDecoder(surface);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }

    public void connect(View view) {
        localSurfaceView.startCapture(new ServerSocketLive(this));
    }

    @Override
    public void callBack(byte[] data) {
        if (mDecodePlayerLiveH265 != null) {
            mDecodePlayerLiveH265.decode(data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localSurfaceView.stop();
        if (mDecodePlayerLiveH265 != null) {
            mDecodePlayerLiveH265.stop();
        }
    }
}