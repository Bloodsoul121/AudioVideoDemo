package com.example.avd.mv_encode_decode.encode;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.example.avd.ActivityLifeManager;
import com.example.avd.util.BitmapUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 其他补充：
 * MediaCodec是Android在4.1中加入的新的API，目前也有很多文章介绍MediaCodec的用法，但是很多时候很多手机都失败，
 * 主要问题出现在调用dequeueOutputBuffer的时候总是返回-1，让你以为No buffer available !
 * 这里介绍一个开源项目libstreaming,我们借助此项目中封装的一个工具类EncoderDebugger，来初始化MediaCodec会很好的解决此问题，
 * 目前为止测试了几个手机都可以成功，包括小米华为Moto。
 * 跳转地址：http://kidloserme.github.io/2016/09/24/2016-03-09-mediacodec/
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class AvcEncoder {

    private static final String TAG = "AvcEncoder";

    private int TIMEOUT_USEC = 12000;
    private int mYuvQueueSize = 10;
    private int mWidth;
    private int mHeight;
    private int mFrameRate;

    // true--Camera的预览数据编码
    // false--Camera2的预览数据编码
    private boolean mIsCamera;
    private boolean isRunning = false;

    private byte[] mConfigByte;
    private File mOutFile;
    private Callback mCallback;
    private Thread mEncoderThread;
    private MediaCodec mMediaCodec;
    private BufferedOutputStream outputStream;
    private ArrayBlockingQueue<byte[]> mYuvQueue = new ArrayBlockingQueue<>(mYuvQueueSize);

    public AvcEncoder(int width, int height, int frameRate, File outFile, boolean isCamera) {
        mIsCamera = isCamera;
        mWidth = width;
        mHeight = height;
        mFrameRate = frameRate;
        mOutFile = outFile;

        MediaFormat mediaFormat;
        if (getDgree() == 0) {
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width);
        } else {
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        }
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        createFile();
    }

    private void createFile() {
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mOutFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stopEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    public void putYUVData(byte[] buffer) {
        if (mYuvQueue.size() >= 10) {
            mYuvQueue.poll();
        }
        mYuvQueue.add(buffer);
    }

    public void stopThread() {
        if (!isRunning) return;
        isRunning = false;
        if (mEncoderThread != null) {
            mEncoderThread.interrupt();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startEncoderThread() {
        mEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRunning = true;
                byte[] input = null;
                long pts = 0;
                long generateIndex = 0;

                while (isRunning) {
                    try {
                        if (mYuvQueue.size() > 0) {
                            input = mYuvQueue.poll();
                            if (mIsCamera) {//Camera  NV21
                                //NV12数据所需空间为如下，所以建立如下缓冲区
                                //y=W*h;u=W*H/4;v=W*H/4,so total add is W*H*3/2 (1 + 1/4 + 1/4 = 3/2)
                                byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                                NV21ToNV12(input, yuv420sp, mWidth, mHeight);
                                input = yuv420sp;
                            } else {//Camera2  YV12
                                byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                                YV12toNV12(input, yuv420sp, mWidth, mHeight);
                                input = yuv420sp;
                            }
                        }

                        // 回调，测试图片是否正常
                        if (input != null) {
                            if (mCallback != null) {
                                mCallback.onCaptureBitmap(BitmapUtil.getBitmapImageFromYUV(input, mWidth, mHeight));
                            }
                            // 尝试旋转90度
                            input = RotateYuvUtil.rotateYUV420SP3(input, mWidth, mHeight);
                            if (mCallback != null) {
                                mCallback.onCaptureRotateBitmap(BitmapUtil.getBitmapImageFromYUV(input, mHeight, mWidth));
                            }
                        }

                        if (input != null) {
                            // 获取输入队列空闲数组下标
                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                            Log.i(TAG, "inputBufferIndex " + inputBufferIndex);
                            if (inputBufferIndex >= 0) {
                                pts = computePresentationTime(generateIndex);
                                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                                generateIndex += 1;
                            }

                            // 写入到本地文件
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            Log.i(TAG, "outputBufferIndex " + outputBufferIndex);
                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);
                                if (bufferInfo.flags == 2) {
                                    mConfigByte = new byte[bufferInfo.size];
                                    mConfigByte = outData;
                                } else if (bufferInfo.flags == 1) {
                                    byte[] keyframe = new byte[bufferInfo.size + mConfigByte.length];
                                    System.arraycopy(mConfigByte, 0, keyframe, 0, mConfigByte.length);
                                    System.arraycopy(outData, 0, keyframe, mConfigByte.length, outData.length);
                                    try {
                                        outputStream.write(keyframe, 0, keyframe.length);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        outputStream.write(outData, 0, outData.length);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }
                        } else {
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                try {
                    stopEncoder();
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                        outputStream = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        mEncoderThread.start();
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    private void YV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {

        int nLenY = width * height;
        int nLenU = nLenY / 4;


        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + nLenU + i];
        }
    }

    private void swapYV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;

        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + nLenU + i];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {

        void onCaptureBitmap(Bitmap bitmap);

        void onCaptureRotateBitmap(Bitmap bitmap);
    }

    private int getDgree() {
        int rotation = ActivityLifeManager.getInstance().getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }

}
