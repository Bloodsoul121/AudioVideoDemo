package com.example.avd.aac;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MusicProcess {

    private static final String TAG = "MusicProcess";

    public static void mixAudioTrack(String videoInput, // 视频音频
                                     String audioInput, // bgm音频
                                     String output, // 输出音频
                                     String videoInputTemp, // 视频音频，pcm临时文件
                                     String audioInputTemp, // bgm音频，pcm临时文件
                                     String mixTemp, // 混合音频，pcm临时文件
                                     int startTimeUs,
                                     int endTimeUs,
                                     int videoVolume, // 视频声音大小
                                     int aacVolume // 音频声音大小
    ) {
        try {

            // 解码 pcm
            decodeAacToPcm(videoInput, videoInputTemp, startTimeUs, endTimeUs);
            decodeAacToPcm(audioInput, audioInputTemp, startTimeUs, endTimeUs);

            // 混音 pcm
            mixPcm(videoInputTemp, audioInputTemp, mixTemp, videoVolume, aacVolume);

            // pcm 合成 wav
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixTemp, output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // vol1  vol2  0-100  0静音  120
    private static void mixPcm(String pcm1Path, String pcm2Path, String toPath, int volume1, int volume2) throws IOException {

        // 精准化音量值
        float vol1 = normalizeVolume(volume1);
        float vol2 = normalizeVolume(volume2);

        // 一次读取多一点 2kb
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
        // 待输出数据
        byte[] buffer3 = new byte[2048];

        FileInputStream fis1 = new FileInputStream(pcm1Path);
        FileInputStream fis2 = new FileInputStream(pcm2Path);
        FileOutputStream fos = new FileOutputStream(toPath);

        short temp1, temp2; // 两个short变量相加 会大于short 声音 采样值 两个字节 65535 (-32768 - 32767)
        int temp;
        boolean end1 = false, end2 = false;

        while (!end1 || !end2) {

            if (!end1) {

                end1 = fis1.read(buffer1) == -1;

                // 拷贝到buffer3，先拷贝进来，万一 fis2 的数据不够呢
                System.arraycopy(buffer1, 0, buffer3, 0, buffer1.length);
            }

            if (!end2) {

                end2 = fis2.read(buffer2) == -1;

                // 一个声音为两个字节，所以 +2，将两个音频源进行合并
                // 声音格式，低8位 + 高8位
                for (int i = 0; i < buffer2.length; i += 2) {

                    temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                    temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                    // 合并音乐和视频声音，直接两个short值相加，然后再分割为两个byte
                    temp = (int) (temp1 * vol1 + temp2 * vol2);

                    // 考虑越界的问题
                    if (temp > 32767) {
                        temp = 32767;
                    } else if (temp < -32768) {
                        temp = -32768;
                    }

                    buffer3[i] = (byte) (temp & 0xFF); // 低8位
                    buffer3[i + 1] = (byte) ((temp >>> 8) & 0xFF); // 高8位，无符号位移
                }

                fos.write(buffer3);

            }

        }

        fis1.close();
        fis2.close();
        fos.close();

        Log.i(TAG, "mixPcm: 转换完毕");
    }

    // 浮点计算，保持精准度
    private static float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }

    // 剪辑
    public static void clip(String aacPath, String wavPath, String tempPcmPath, int startTime, int endTime) {
        try {

            // 将 mp3 解码为 pcm
            decodeAacToPcm(aacPath, tempPcmPath, startTime, endTime);

            // pcm 合成 wav 文件，没有压缩的，只是加了一个 wav 头信息
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(tempPcmPath, wavPath);

            Log.i(TAG, "clip: 转换完毕");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decodeAacToPcm(String aacPath, String outPath, int startTime, int endTime) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(aacPath);

        int trackIndex = selectTrack(mediaExtractor, true);

        if (trackIndex < 0) {
            return;
        }

        mediaExtractor.selectTrack(trackIndex);
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        MediaFormat trackFormat = mediaExtractor.getTrackFormat(trackIndex);

        int maxInputSize;
        if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxInputSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxInputSize = 100_1000;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxInputSize);

        File outFile = new File(outPath);
        FileChannel channel = new FileOutputStream(outFile).getChannel();

        MediaCodec mediaCodec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME));
        mediaCodec.configure(trackFormat, null, null, 0);
        mediaCodec.start();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        Log.i(TAG, "decodeAacToPcm start");

        while (true) {
            int index = mediaCodec.dequeueInputBuffer(100_000);
            if (index > -1) {

                long sampleTime = mediaExtractor.getSampleTime();
                if (sampleTime == -1) {
                    break;
                } else if (sampleTime < startTime) {
                    mediaExtractor.advance();
                    continue;
                } else if (sampleTime > endTime) {
                    break;
                }

                info.size = mediaExtractor.readSampleData(byteBuffer, 0);
                info.presentationTimeUs = sampleTime;
                info.flags = mediaExtractor.getSampleFlags();

                byte[] content = new byte[byteBuffer.remaining()];
                byteBuffer.get(content);

                // 临时输出日志
                FileUtil.writeContent(content, new File(Environment.getExternalStorageDirectory(), "aac_codec.txt"));

                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(index, 0, info.size, info.presentationTimeUs, info.flags);

                mediaExtractor.advance();
            }

            int outIndex = mediaCodec.dequeueOutputBuffer(info, 10_000);
            while (outIndex > -1) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outIndex);
                channel.write(outputBuffer);
                mediaCodec.releaseOutputBuffer(outIndex, false);
                outIndex = mediaCodec.dequeueOutputBuffer(info, 10_000);
            }
        }

        channel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();

        Log.i(TAG, "decodeAacToPcm end");
    }

    private static int selectTrack(MediaExtractor mediaExtractor, boolean isAudio) {
        // 获取每条轨道
        int numTracks = mediaExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (isAudio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -1;
    }

}
