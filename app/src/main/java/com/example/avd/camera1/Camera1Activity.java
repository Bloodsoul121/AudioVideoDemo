package com.example.avd.camera1;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.avd.BaseActivity;
import com.example.avd.R;
import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Camera1Activity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private byte[] mBuffer;
    private boolean mIsCapture;
    private int mImageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        mPreviewSize = parameters.getPreviewSize();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mBuffer = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2];
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);

            List<String> focusModeList = parameters.getSupportedFocusModes();
            for (String focusMode : focusModeList) {//检查支持的对焦
                if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
            }

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mIsCapture) {
            mIsCapture = false;
            byte[] rotateByes = portraitData2Raw(data, mPreviewSize.width, mPreviewSize.height);
            capture(rotateByes);
        }
        mCamera.addCallbackBuffer(mBuffer);
    }

    public void startCapture(View view) {
        mIsCapture = true;
    }

    private void capture(byte[] data) {
        //保存一张照片
        String fileName = "IMG_capture_" + mImageIndex++ + ".jpg";  //jpeg文件名定义
        File pictureFile = new File(FileUtil.getParentDir(), fileName);
        if (pictureFile.exists()) {
            pictureFile.delete();
            try {
                pictureFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream filecon = new FileOutputStream(pictureFile);
            //ImageFormat.NV21 and ImageFormat.YUY2 for now
//            YuvImage image = new YuvImage(data, ImageFormat.NV21, mPreviewSize.width, mPreviewSize.height, null);   //将NV21 data保存成YuvImage
            // 旋转90之后，宽高互换
            YuvImage image = new YuvImage(data, ImageFormat.NV21, mPreviewSize.height, mPreviewSize.width, null);   //将NV21 data保存成YuvImage
            //图像压缩
            image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, filecon);   // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
            toast("生成图片：" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    画面旋转90度  rtmp 推流  软解推流
    private byte[] portraitData2Raw(byte[] data, int width, int height) {
        byte[] rotateBytes = new byte[data.length];
        //        旋转y
        int y_len = width * height;
        //u   y/4   v  y/4
        int uvHeight = height / 2;

        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                //                存值  k++  0          取值  width * i + j
                rotateBytes[k++] = data[width * i + j];
            }
        }

        //        旋转uv
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                rotateBytes[k++] = data[y_len + width * i + j];
                rotateBytes[k++] = data[y_len + width * i + j + 1];
            }
        }

        return rotateBytes;
    }

}