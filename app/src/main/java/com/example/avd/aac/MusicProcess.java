package com.example.avd.aac;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
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

    // 视频 + BGM ，封装新的视频mp4
    public static void mixVideoAndAudioToMp4(String videoInput, // 视频音频
                                             String audioInput, // bgm音频
                                             String outputMp3, // 输出音频
                                             String outputMp4, // 输出音频
                                             String videoInputTemp, // 视频音频，pcm临时文件
                                             String audioInputTemp, // bgm音频，pcm临时文件
                                             String mixTemp, // 混合音频，pcm临时文件
                                             int startTimeUs, int endTimeUs, // 时间段
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
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixTemp, outputMp3);

            mixVideoAndAudioToMp4(videoInput, outputMp3, outputMp4, startTimeUs, endTimeUs);

            Log.i(TAG, "mixVideoAndAudioToMp4: 转换完毕");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 抽离视频，与单独音频混合，封装新的视频mp4
    private static void mixVideoAndAudioToMp4(String mp4Input, // 视频文件（包含视频和音频）
                                              String wavInput, // 单独音频，wav文件
                                              String output, // 输出视频，mp4
                                              int startTimeUs, int endTimeUs // 时间段
    ) {
        try {
            // 封装容器
            MediaMuxer mediaMuxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // 抽离视频
            MediaExtractor mp4Extractor = new MediaExtractor();
            mp4Extractor.setDataSource(mp4Input);
            // 视频轨道索引
            int videoTrackIndex = selectTrack(mp4Extractor, false);
            // 音频轨道索引
            int audioTrackIndex = selectTrack(mp4Extractor, true);

            // 往容器添加轨道信息
            MediaFormat videoTrackFormat = mp4Extractor.getTrackFormat(videoTrackIndex);
            // 空轨道，没有数据的
            mediaMuxer.addTrack(videoTrackFormat);

            MediaFormat audioTrackFormat = mp4Extractor.getTrackFormat(audioTrackIndex);
            // 配置信息，后面添加新的音频时需要用到
            int audioBitRate = audioTrackFormat.getInteger(MediaFormat.KEY_BIT_RATE);
            // 后面需要添加aac音频
            audioTrackFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            int muxerAudioIndex = mediaMuxer.addTrack(audioTrackFormat);

            // 配置完成，开始运行工作
            mediaMuxer.start();

            // 正片开始啦！！！

            // 将单独音频数据添加到轨道中
            MediaExtractor wavExtractor = new MediaExtractor();
            wavExtractor.setDataSource(wavInput);
            int wavTrackIndex = selectTrack(wavExtractor, true);
            wavExtractor.selectTrack(wavTrackIndex);

            MediaFormat wavMediaFormat = wavExtractor.getTrackFormat(wavTrackIndex);

            int maxBufferSize;
            if (wavMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize = wavMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            } else {
                maxBufferSize = 100_000;
            }

            MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            // 参数对应-> mime type、采样率、声道数
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
            // 比特率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate);
            // 音质等级
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // 解码  那段
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(maxBufferSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean encodeDone = false;
            while (!encodeDone) {
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(10_000);
                if (inputBufferIndex > -1) {
                    long sampleTime = wavExtractor.getSampleTime();
                    if (sampleTime < 0) {
                        // pts小于0  来到了文件末尾 通知编码器  不用编码了
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        int flags = wavExtractor.getSampleFlags();
                        int size = wavExtractor.readSampleData(byteBuffer, 0);
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(byteBuffer);
                        inputBuffer.position(0);
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);

                        // 下一帧
                        wavExtractor.advance();
                    }
                }

                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
                while (outputBufferIndex > -1) {
                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        encodeDone = true;
                        break;
                    }

                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                    mediaMuxer.writeSampleData(muxerAudioIndex, outputBuffer, bufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                    // 循环
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
                }
            }

            // 音频端就结束了

            if (audioTrackIndex > -1) {
                mp4Extractor.unselectTrack(audioTrackIndex);
            }

            // 操作视频

            mp4Extractor.selectTrack(videoTrackIndex);

            mp4Extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            maxBufferSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            byteBuffer = ByteBuffer.allocateDirect(maxBufferSize);
            //封装容器添加视频轨道信息
            while (true) {
                long sampleTimeUs = mp4Extractor.getSampleTime();
                if (sampleTimeUs == -1) {
                    break;
                }
                if (sampleTimeUs < startTimeUs) {
                    mp4Extractor.advance();
                    continue;
                }
                if (sampleTimeUs > endTimeUs) {
                    break;
                }
                // pts  0
                bufferInfo.presentationTimeUs = sampleTimeUs - startTimeUs + 600;
                bufferInfo.flags = mp4Extractor.getSampleFlags();
                // 读取视频文件的数据  画面 数据   压缩1  未压缩2
                bufferInfo.size = mp4Extractor.readSampleData(byteBuffer, 0);
                if (bufferInfo.size < 0) {
                    break;
                }
                // 视频轨道  画面写完了
                mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
                mp4Extractor.advance();
            }

            wavExtractor.release();
            mp4Extractor.release();
            mediaCodec.stop();
            mediaCodec.release();
            mediaMuxer.release();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 混合多个音频
    public static void mixAudioTrack(String videoInput, // 视频音频
                                     String audioInput, // bgm音频
                                     String output, // 输出音频
                                     String videoInputTemp, // 视频音频，pcm临时文件
                                     String audioInputTemp, // bgm音频，pcm临时文件
                                     String mixTemp, // 混合音频，pcm临时文件
                                     int startTimeUs, int endTimeUs, int videoVolume, // 视频声音大小
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

    // 剪辑音频
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
