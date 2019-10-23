package com.example.avd.audio_encode_decode;


import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecodeRunnable implements Runnable {

    private static final String TAG = "AudioDecodeRunnable";
    private static final long TIMEOUT_USEC = 0;
    private final MediaExtractor mMediaExtractor;
    private final int mAudioTrack;
    private final String mAudioSavePath;
    private final AudioEncodeDecodeActivity.DecodeOverListener mListener;

    public AudioDecodeRunnable(MediaExtractor mediaExtractor, int audioTrack, String audioSavePath, AudioEncodeDecodeActivity.DecodeOverListener listener) {
        this.mMediaExtractor = mediaExtractor;
        this.mAudioTrack = audioTrack;
        this.mAudioSavePath = audioSavePath;
        this.mListener = listener;
    }

    @Override
    public void run() {
        try {
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(mAudioTrack);
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(trackFormat, null, null,0);
            // 启动MediaCodec，等待传入数据
            mediaCodec.start();

            //用于描述解码得到的byte[]数据的相关信息
            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo inputInfo = new MediaCodec.BufferInfo();
            boolean codeOver = false;
            boolean inputDone = false;

            FileOutputStream fos = new FileOutputStream(mAudioSavePath);

            while (!codeOver) {
                if (!inputDone) {
                    //将数据传入之后，再去输出端取出数据
                    int inputIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputIndex > 0) {
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                        assert inputBuffer != null;
                        inputBuffer.clear();

                        int readSampleDataSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                        if (readSampleDataSize < 0) {
                            mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            inputInfo.offset = 0;
                            inputInfo.size = readSampleDataSize;
                            inputInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                            inputInfo.presentationTimeUs = mMediaExtractor.getSampleTime();
                            Log.e(TAG,"往解码器写入数据，当前时间戳：" + inputInfo.presentationTimeUs);

                            mediaCodec.queueInputBuffer(inputIndex, inputInfo.offset, inputInfo.size, inputInfo.presentationTimeUs, 0);
                            mMediaExtractor.advance();
                        }
                    }
                }

                boolean decodeOutputDone = false;
                while (!decodeOutputDone) {
                    int outputIndex = mediaCodec.dequeueOutputBuffer(decodeBufferInfo, TIMEOUT_USEC);
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        //没有可用的解码器
                        decodeOutputDone = true;
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat outputFormat = mediaCodec.getOutputFormat();
                    } else if (outputIndex < 0) {
                        // nothing
                    } else {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputIndex);
                        byte[] chunckPcm = new byte[decodeBufferInfo.size];
                        assert outputBuffer != null;
                        outputBuffer.get(chunckPcm);
                        outputBuffer.clear();

                        fos.write(chunckPcm);
                        fos.flush();
                        Log.i(TAG, "释放输出流缓冲区");
                        mediaCodec.releaseOutputBuffer(outputIndex, false);

                        if ((decodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){//编解码结束
                            mMediaExtractor.release();
                            mediaCodec.stop();
                            mediaCodec.release();
                            codeOver = true;
                            decodeOutputDone = true;
                        }
                    }
                }

            }

            fos.close();
            if (mListener != null){
                mListener.decodeOver();
            }

        } catch (IOException e) {
            e.printStackTrace();
            if (mListener != null){
                mListener.decodeFail();
            }
        }
    }
}
