package com.example.avd.mv_split_compose;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.avd.BaseActivity;
import com.example.avd.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MvSplitComposeActivity extends BaseActivity {

    private String mOriFilePath; // 原始文件
    private String mMediaFilePath; // 分离后的视频
    private String mAudioFilePath; // 分离后的音频
    private String mCombineFilePath; // 重新合成的音视频

    private TextView mPathTv;
    private EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mv_split_compose);

        File oriFile = new File(Environment.getExternalStorageDirectory(), "aaa.3gp");
        mOriFilePath = oriFile.getAbsolutePath();
        File mediaFile = new File(Environment.getExternalStorageDirectory(), "media.mp4");
        mMediaFilePath = mediaFile.getAbsolutePath();
        File audioFile = new File(Environment.getExternalStorageDirectory(), "audio");
        mAudioFilePath = audioFile.getAbsolutePath();
        File combineFile = new File(Environment.getExternalStorageDirectory(), "mv.mp4");
        mCombineFilePath = combineFile.getAbsolutePath();

        mEdit = findViewById(R.id.edit);
        mPathTv = findViewById(R.id.path);
        mPathTv.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public void setOriFilePath(View view) {
        File oriFile = new File(Environment.getExternalStorageDirectory(), mEdit.getText().toString());
        mOriFilePath = oriFile.getAbsolutePath();
        mPathTv.setText(mOriFilePath);
    }

    /**
     *  抽取视频不含音频
     */
    public void extractMedia(View view) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mOriFilePath);
            // 获取通道的个数
            int trackCount = mediaExtractor.getTrackCount();
            int videoIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                //这个trackFormat可不得了可以获取视频的宽高，视频的通道（音频视频），获取视频的长。有的人用这个获取帧率我试过，有的手机行有的
                //不行，会报错。这是个坑，所以还是另求方式
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String typeMimb = trackFormat.getString(MediaFormat.KEY_MIME);
                if (typeMimb.startsWith("video/")) {
                    // 这就获取了音频的信号通道了
                    videoIndex = i;
                    break;
                }
            }

            if (videoIndex < 0) {
                Toast.makeText(this, "no video", Toast.LENGTH_SHORT).show();
                return;
            }

            // 设置音频通道信号
            mediaExtractor.selectTrack(videoIndex);
            // 再次拿到这个视频通道的format
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(videoIndex);
            // 初始化视频合成器
            MediaMuxer mediaMuxer = new MediaMuxer(mMediaFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // 添加合成器的通道
            mediaMuxer.addTrack(trackFormat);

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);

            mediaMuxer.start();

            // 获取视频的帧率
            mediaExtractor.readSampleData(byteBuffer, 0);
            // 跳过I帧，要P帧（视频是由个别I帧和很多P帧组成）
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }

            mediaExtractor.readSampleData(byteBuffer, 0);
            long firstTime = mediaExtractor.getSampleTime();
            // 下一帧
            mediaExtractor.advance();

            mediaExtractor.readSampleData(byteBuffer, 0);
            long secondTime = mediaExtractor.getSampleTime();

            long videoSameTime = Math.abs(firstTime - secondTime);

            // 重新设置通道，读取文件
            mediaExtractor.unselectTrack(videoIndex);
            mediaExtractor.selectTrack(videoIndex);

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                int readSamSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSamSize < 0) {
                    break;
                }
                mediaExtractor.advance();

                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.size = readSamSize;
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs += videoSameTime;
                mediaMuxer.writeSampleData(videoIndex, byteBuffer, bufferInfo);
            }

            mediaMuxer.stop();
            mediaMuxer.release();
            mediaExtractor.release();

            Toast.makeText(this, "extrac media done", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  抽取音频
     */
    public void extractAudio(View view) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mOriFilePath);
            int trackCount = mediaExtractor.getTrackCount();
            int audioIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String type = trackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioIndex = i;
                    break;
                }
            }

            if (audioIndex < 0) {
                Toast.makeText(this, "no audio", Toast.LENGTH_SHORT).show();
                return;
            }

            mediaExtractor.selectTrack(audioIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioIndex);

            MediaMuxer mediaMuxer = new MediaMuxer(mAudioFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = mediaMuxer.addTrack(trackFormat); // 1
            mediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 500);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            mediaExtractor.readSampleData(byteBuffer, 0);
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }

            mediaExtractor.readSampleData(byteBuffer, 0);
            long firstTime = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            long secondTime = mediaExtractor.getSampleTime();
            long sampleTime = Math.abs(firstTime - secondTime);

            mediaExtractor.unselectTrack(audioIndex);
            mediaExtractor.selectTrack(audioIndex);

            while (true) {
                int readSamSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSamSize < 0) {
                    break;
                }
                mediaExtractor.advance();
                bufferInfo.size = readSamSize;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs += sampleTime;
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo); // 2
            }

            mediaMuxer.stop();
            mediaMuxer.release();
            mediaExtractor.release();

            Toast.makeText(this, "extract audio done", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void combineVideo(View view) {
        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(mMediaFilePath);
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            MediaFormat mediaTrackFormat = null;
            for (int i = 0; i < videoTrackCount; i++) {
                mediaTrackFormat = videoExtractor.getTrackFormat(i);
                String type = mediaTrackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(mAudioFilePath);
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            MediaFormat audioTrackFormat = null;
            for (int i = 0; i < audioTrackCount; i++) {
                audioTrackFormat = audioExtractor.getTrackFormat(i);
                String type = audioTrackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            MediaMuxer mediaMuxer = new MediaMuxer(mCombineFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            assert mediaTrackFormat != null;
            int writeVideoTrackIndex = mediaMuxer.addTrack(mediaTrackFormat);
            assert audioTrackFormat != null;
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioTrackFormat);

            mediaMuxer.start();

            ByteBuffer byteBufferMedia = ByteBuffer.allocate(1024 * 500);
            videoExtractor.readSampleData(byteBufferMedia, 0);
            if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoExtractor.advance();
            }
            videoExtractor.readSampleData(byteBufferMedia, 0);
            long secondTime = videoExtractor.getSampleTime();
            videoExtractor.advance();
            long thirdTime = videoExtractor.getSampleTime();
            long videoSampleTime = Math.abs(thirdTime - secondTime);
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);

            ByteBuffer byteBufferAudio = ByteBuffer.allocate(500 * 1024);
            //获取帧之间的间隔时间
            audioExtractor.readSampleData(byteBufferAudio, 0);
            if (audioExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                audioExtractor.advance();
            }
            audioExtractor.readSampleData(byteBufferAudio, 0);
            long secondTime2 = audioExtractor.getSampleTime();
            audioExtractor.advance();
            long thirdTime2 = audioExtractor.getSampleTime();
            long audioSampleTime = Math.abs(thirdTime2 - secondTime2);
            audioExtractor.unselectTrack(audioTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBufferMedia, 0);
                if (readVideoSampleSize < 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += videoSampleTime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBufferMedia, videoBufferInfo);
                videoExtractor.advance();
            }

            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBufferAudio, 0);
                if (readAudioSampleSize < 0) {
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs += audioSampleTime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = audioExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBufferAudio, audioBufferInfo);
                audioExtractor.advance();
            }

            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();

            Toast.makeText(this, "combine video done", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
