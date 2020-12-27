package com.example.avd.livechat.base;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import androidx.annotation.NonNull;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera.Size size;
    private Camera mCamera;

    private EncodePushLiveH265 mEncodePushLiveH265;

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        startPreview();
    }


    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        //        流程
        Camera.Parameters parameters = mCamera.getParameters();
        //尺寸
        size = parameters.getPreviewSize();

        try {
            mCamera.setPreviewDisplay(getHolder());
            //            横着
            mCamera.setDisplayOrientation(90);
            byte[] buffer = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            //            输出数据怎么办
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    public void startCapture(SocketLive socketLive) {
        mEncodePushLiveH265 = new EncodePushLiveH265(socketLive, size.width, size.height);
        mEncodePushLiveH265.startLive();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        //        获取到摄像头的原始数据yuv
        //        开始    视频通话
        if (mEncodePushLiveH265 != null) {
            mEncodePushLiveH265.encodeFrame(bytes);
        }

        mCamera.addCallbackBuffer(bytes);
    }

    public void stop() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mEncodePushLiveH265 != null) {
            mEncodePushLiveH265.stop();
        }
    }
}
