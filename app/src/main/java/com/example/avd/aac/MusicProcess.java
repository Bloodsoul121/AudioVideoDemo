package com.example.avd.aac;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MusicProcess {

    private static final String TAG = "MusicProcess";

    public static void clip(String aacPath, String wavPath, String tempPcmPath, int startTime, int endTime) {
        try {
            // 将 mp3 解码为 pcm
            decodeAacToPcm(aacPath, tempPcmPath, startTime, endTime);

            // pcm 合成 wav 文件，没有压缩的，只是加了一个 wav 头信息
            PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT);
            pcmToWavUtil.pcmToWav(tempPcmPath, wavPath);

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
    }

    private static int selectTrack(MediaExtractor mediaExtractor, boolean isAudio) {
        // 获取每条轨道
        int numTracks = mediaExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (isAudio && mime.startsWith("audio/")) {
                return i;
            } else if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

}
