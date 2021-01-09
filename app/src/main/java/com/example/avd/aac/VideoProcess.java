package com.example.avd.aac;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.aac
 *  @文件名:   VideoProcess
 *  @创建者:   bloodsoul
 *  @创建时间:  2021/1/9 12:52
 *  @描述：    TODO
 */

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import com.example.avd.util.FileUtil;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoProcess {

    private static final String TAG = "VideoProcess";

    public static void appendVideo(String videoInput1, // 视频1
                                   String videoInput2, // 视频2
                                   String outputMp4 // 输出合成视频
    ) {
        try {
            MediaMuxer mediaMuxer = new MediaMuxer(outputMp4, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int videoTrackIndex1 = -1;
            int audioTrackIndex1 = -1;
            int videoTrackIndex2 = -1;
            int audioTrackIndex2 = -1;

            MediaExtractor mediaExtractor1 = new MediaExtractor();
            mediaExtractor1.setDataSource(videoInput1);

            MediaExtractor mediaExtractor2 = new MediaExtractor();
            mediaExtractor2.setDataSource(videoInput2);

            videoTrackIndex1 = selectTrack(mediaExtractor1, false);
            MediaFormat videoTrackFormat1 = mediaExtractor1.getTrackFormat(videoTrackIndex1);
            mediaMuxer.addTrack(videoTrackFormat1);

            audioTrackIndex1 = selectTrack(mediaExtractor1, true);
            MediaFormat audioTrackFormat1 = mediaExtractor1.getTrackFormat(audioTrackIndex1);
            mediaMuxer.addTrack(audioTrackFormat1);

            mediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(500 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            mediaExtractor1.selectTrack(videoTrackIndex1);

            // 视频1 - 视频通道
            while (true) {
                int size = mediaExtractor1.readSampleData(byteBuffer, 0);
                if (size < 0) {
                    break;
                }

                // 仅供测试
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                FileUtil.writeContent(data, new File(Environment.getExternalStorageDirectory(), "aac_codec.txt"));

                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs = mediaExtractor1.getSampleTime();
                bufferInfo.flags = mediaExtractor1.getSampleFlags();
                bufferInfo.size = size;
                mediaMuxer.writeSampleData(videoTrackIndex1, byteBuffer, bufferInfo);

                mediaExtractor1.advance();
            }

            bufferInfo.presentationTimeUs = 0;

            mediaExtractor1.unselectTrack(videoTrackIndex1);
            mediaExtractor1.selectTrack(audioTrackIndex1);

            // 视频1 - 音频通道
            while (true) {
                int size = mediaExtractor1.readSampleData(byteBuffer, 0);
                if (size < 0) {
                    break;
                }
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs = mediaExtractor1.getSampleTime();
                bufferInfo.flags = mediaExtractor1.getSampleFlags();
                bufferInfo.size = size;
                mediaMuxer.writeSampleData(audioTrackIndex1, byteBuffer, bufferInfo);
                mediaExtractor1.advance();
            }

            mediaExtractor1.release();
            mediaExtractor2.release();
            mediaMuxer.stop();
            mediaMuxer.release();

            Log.i(TAG, "appendVideo: 转换完毕");

        } catch (Exception e) {
            e.printStackTrace();
        }
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
