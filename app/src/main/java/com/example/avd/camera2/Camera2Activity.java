package com.example.avd.camera2;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.avd.R;
import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Camera2Activity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera2Listener {

    private TextureView mTextureView;
    private Camera2Helper mCamera2Helper;
    private MediaCodec mMediaCodec;

    private final File saveCodecFile = new File(Environment.getExternalStorageDirectory(), "camera2codec.h264");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        mTextureView = findViewById(R.id.texture);
        init();
    }

    private void init() {
        if (saveCodecFile.exists()) {
            saveCodecFile.delete();
        }
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera2Helper = new Camera2Helper(this, this);
        mCamera2Helper.start(mTextureView);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera2Helper.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private byte[] nv21; // 原相机合成格式 yuv -> nv21
    private byte[] nv21_rotated; // 将数据旋转90度
    private byte[] nv12; // 输出数据格式

    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride) {
        // 先转成nv21 , n21 横着 -> 竖着 , 再转成 yuv420
        if (nv21 == null) {
            nv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
            nv21_rotated = new byte[stride * previewSize.getHeight() * 3 / 2];
        }

        ImageUtil.yuvToNv21(y, u, v, nv21, stride, previewSize.getHeight());
        // 对数据进行旋转 90度
        ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, stride, previewSize.getHeight());
        // Nv12  yuv420
        nv12 = ImageUtil.nv21toNV12(nv21_rotated);

        initMediaCodec(previewSize);

        //输出成H264的码流
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inIndex = mMediaCodec.dequeueInputBuffer(100000);
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = mMediaCodec.getInputBuffer(inIndex);
            byteBuffer.clear();
            byteBuffer.put(nv12, 0, nv12.length);
            mMediaCodec.queueInputBuffer(inIndex, 0, nv12.length, 0, 0);
        }
        int outIndex = mMediaCodec.dequeueOutputBuffer(info, 100000);
        if (outIndex >= 0) {
            ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outIndex);
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
            FileUtil.writeBytes(data, saveCodecFile);
            mMediaCodec.releaseOutputBuffer(outIndex, false);
        }
    }

    private void initMediaCodec(Size size) {
        if (mMediaCodec != null) {
            return;
        }
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            // 注意这里的宽高，跟相机是反的
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, size.getHeight(), size.getWidth());
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);//15*2 =30帧
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}