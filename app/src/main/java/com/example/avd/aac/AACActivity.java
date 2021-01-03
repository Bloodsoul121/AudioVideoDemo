package com.example.avd.aac;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.example.avd.R;
import com.example.avd.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import androidx.appcompat.app.AppCompatActivity;

public class AACActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_a_c);
        FileUtil.copyAssetsFile(this, "music2.mp3", Environment.getExternalStorageDirectory(), "music_copy.mp3");
    }

    public void clickBtn1(View view) {
        final String aacPath = new File(Environment.getExternalStorageDirectory(), "music_copy.mp3").getAbsolutePath();
        final String outPath = new File(Environment.getExternalStorageDirectory(), "music_clip.mp3").getAbsolutePath();
        clip(aacPath, outPath, 10_000_000, 15_000_000);
    }

    private void clip(String aacPath, String outPath, int startTime, int endTime) {
        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(aacPath);

            int trackIndex = -1;
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String type = trackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    trackIndex = i;
                    break;
                }
            }

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

            File outFile = new File(Environment.getExternalStorageDirectory(), "music_temp.pcm");
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

            File wavFile = new File(outPath);
            new PcmToWavUtil(44100,
                             AudioFormat.CHANNEL_IN_STEREO,
                             2,
                             AudioFormat.ENCODING_PCM_16BIT)
                    .pcmToWav(outFile.getAbsolutePath(), wavFile.getAbsolutePath());

            Log.i("AACActivity", "mixAudioTrack: 转换完毕");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}