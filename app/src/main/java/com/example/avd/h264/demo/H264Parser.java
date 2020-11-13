package com.example.avd.h264.demo;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class H264Parser implements Runnable {

    private static final String TAG = H264Parser.class.getSimpleName();

    private static final long TIMEOUTUS = 10000;

    private File mFile;
    private Context mContext;
    private Surface mSurface;
    private MediaCodec mMediaCodec;

    private int mWidth;
    private int mHeight;
    private int mStartFrameIndex;
    private boolean mIsRunning;

    H264Parser(Context context, Surface surface, int width, int height) {
        mContext = context;
        mSurface = surface;
        mWidth = width;
        mHeight = height;
        initMediaCodec(surface, width, height);
    }

    private void initMediaCodec(Surface surface, int width, int height) {
        try {
            if (mMediaCodec == null) {
                mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                mMediaCodec.configure(mediaFormat, surface, null, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "MediaCodec init failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void setFilePath(File file) {
        mFile = file;
    }

    public void start() {
        start(true);
    }

    public void start(boolean restart) {
        if (mFile == null) {
            Toast.makeText(mContext, "file is null", Toast.LENGTH_SHORT).show();
            return;
        }
        initMediaCodec(mSurface, mWidth, mHeight);
        if (mMediaCodec == null) {
            return;
        }
        if (mIsRunning) {
            Toast.makeText(mContext, "running", Toast.LENGTH_SHORT).show();
            return;
        }
        if (restart) {
            mStartFrameIndex = 0;
        }
        mMediaCodec.start();
        new Thread(this).start();
    }

    public void stop() {
        mIsRunning = false;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    // 如果播放片段没有I帧，则报错

    @Override
    public void run() {
        mIsRunning = true;

        byte[] bytes = getBytes();
        if (bytes == null) {
            mIsRunning = false;
            Log.e(TAG, "getBytes failed");
            return;
        }

        int[] spsSize = H264SpsParser.getSizeFromSps(bytes);
        if (spsSize != null) {
            Log.d(TAG, "Sps=(" + spsSize[0] + ", " + spsSize[1] + ")");
        }

        while (mIsRunning) {
            // 放数据 to dsp芯片
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUTUS);
            if (inputBufferIndex > -1) {
                ByteBuffer byteBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                byteBuffer.clear();

                // startIndex + 1，不然一直返回原地踏步
                int nextFrameIndex = findByFrame(bytes, mStartFrameIndex + 1);
                if (nextFrameIndex == -1) {
                    Log.e(TAG, "findByFrame failed");
                    break;
                }

                Log.d(TAG, "findByFrame : " + nextFrameIndex);

                byteBuffer.put(bytes, mStartFrameIndex, nextFrameIndex - mStartFrameIndex);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, nextFrameIndex - mStartFrameIndex, 0, 0);

                mStartFrameIndex = nextFrameIndex;
            } else {
                continue;
            }

            try {
                Thread.sleep(33);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 取数据 from dsp芯片
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUTUS);
            if (outputBufferIndex > -1) {
                ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                byteBuffer.position(bufferInfo.offset);
                byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] remain = new byte[byteBuffer.remaining()];
                byteBuffer.get(remain);
                byteBuffer.clear();
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            }

        }

        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
    }

    private int findByFrame(byte[] bytes, int start) {
        int length = bytes.length;
        for (int i = start; i < length; i++) {
            if (bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00 && bytes[i + 3] == 0x01) {
                return i;
            }
        }
        Log.i(TAG, "findByFrame length " + length + " , " + start);
        return -1;  // Not found
    }

    private byte[] getBytes() {
        if (mFile == null) {
            Log.e(TAG, "File is null");
            return null;
        }
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(mFile));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            byte[] arr = new byte[1024];
            while ((len = dis.read(arr)) != -1) {
                bos.write(arr, 0, len);
            }
            Log.d(TAG, "getBytes size : " + bos.size());
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
