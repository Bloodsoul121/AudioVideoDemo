package com.example.avd.audio_encode_decode;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AudioEncodeRunnable implements Runnable {

    private static final String TAG = "AudioEncodeRunnable";
    private final String mPcmPath;
    private final String mAudioPath;
    private final AudioEncodeDecodeActivity.AudioDecodeListener mListener;

    public AudioEncodeRunnable(String pcmPath, String audioPath, AudioEncodeDecodeActivity.AudioDecodeListener audioDecodeListener) {
        this.mPcmPath = pcmPath;
        this.mAudioPath = audioPath;
        this.mListener = audioDecodeListener;
    }

    @Override
    public void run() {
        File file = new File(mPcmPath);
        if (!file.exists()) {
            if (mListener != null) {
                mListener.decodeFail();
            }
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(mPcmPath);
            byte[] buffer = new byte[8*1024];
            byte[] allAudioBytes;
            int inputIndex;
            ByteBuffer inputBuffer;
            int outputIndex;
            ByteBuffer outputBuffer;
            byte[] chunkAudio;
            int outBitSize;
            int outPacketSize;

            //初始化编码格式   mimetype  采样率  声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE,96000);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,500 * 1024);

            //初始化编码器
            MediaCodec mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaEncode.configure(encodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaEncode.start();

            ByteBuffer[] encodeInputBuffers = mediaEncode.getInputBuffers();
            ByteBuffer[] encodeOutputBuffers = mediaEncode.getOutputBuffers();
            MediaCodec.BufferInfo encodeBufferInfo = new MediaCodec.BufferInfo();

            //初始化文件写入流
            FileOutputStream fos = new FileOutputStream(new File(mAudioPath));
            BufferedOutputStream bos = new BufferedOutputStream(fos,500 * 1024);
            boolean isReadEnd = false;
            int progress = 0;
            while (!isReadEnd){
                for (int i = 0;i < encodeInputBuffers.length - 1;i++){//减掉1很重要，不要忘记
                    if (fis.read(buffer) != -1){
                        allAudioBytes = Arrays.copyOf(buffer,buffer.length);
                    } else {
                        Log.e(TAG,"文件读取完成");
                        isReadEnd = true;
                        break;
                    }

                    Log.e(TAG,"读取文件并写入编码器" + allAudioBytes.length);
                    inputIndex = mediaEncode.dequeueInputBuffer(-1);
                    inputBuffer = encodeInputBuffers[inputIndex];
                    inputBuffer.clear();
                    inputBuffer.limit(allAudioBytes.length);
                    inputBuffer.put(allAudioBytes);//将pcm数据填充给inputBuffer
                    mediaEncode.queueInputBuffer(inputIndex,0,allAudioBytes.length,0,0);//开始编码
                }
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,10000);
                while (outputIndex >= 0){
                    //从解码器中取出数据
                    outBitSize = encodeBufferInfo.size;
                    outPacketSize = outBitSize + 7;//7为adts头部大小
                    outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出的buffer
                    outputBuffer.position(encodeBufferInfo.offset);
                    outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                    chunkAudio = new byte[outPacketSize];
                    addADTStoPacket(chunkAudio,outPacketSize);//添加ADTS
                    outputBuffer.get(chunkAudio,7,outBitSize);//将编码得到的AAC数据取出到byte[]中，偏移量为7
                    outputBuffer.position(encodeBufferInfo.offset);
                    Log.e(TAG,"编码成功并写入文件" + chunkAudio.length);
                    bos.write(chunkAudio,0,chunkAudio.length);//将文件保存在sdcard中
                    bos.flush();

                    mediaEncode.releaseOutputBuffer(outputIndex,false);
                    outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo,10000);

                    if (mListener != null) {
                        progress += chunkAudio.length;
                        mListener.decodeProgress(progress);
                    }
                }
            }
            mediaEncode.stop();
            mediaEncode.release();
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

    /**
     * 写入ADTS头部数据
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
