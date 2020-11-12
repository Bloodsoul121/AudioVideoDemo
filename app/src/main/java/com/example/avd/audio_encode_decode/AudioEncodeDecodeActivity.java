package com.example.avd.audio_encode_decode;

import android.annotation.SuppressLint;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import com.example.avd.BaseActivity;
import com.example.avd.MainApplication;
import com.example.avd.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AudioEncodeDecodeActivity extends BaseActivity {

    @BindView(R.id.ori_path)
    TextView mOriPath;
    @BindView(R.id.path)
    TextView mPath;
    @BindView(R.id.status)
    TextView mStatus;

    private String mOriFilePath; // 原始文件
    private File mSaveDir;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_encode_decode);
        ButterKnife.bind(this);

        mSaveDir = new File(Environment.getExternalStorageDirectory(), "Audio_Extractor");
        if (!mSaveDir.exists()) {
            mSaveDir.mkdirs();
        }

        mOriPath.setText("源文件（封装格式）：\n");
        mPath.setText("编解码存放路径：\n" + mSaveDir.getAbsolutePath());
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResultSystemSelectedFilePath(String filePath) {
        super.onResultSystemSelectedFilePath(filePath);
        mOriFilePath = filePath;
        long fileSize = getFileSize(new File(filePath));
        mOriPath.setText("源文件（封装格式）：\n" + filePath + "\n" + "文件大小：" + Formatter.formatFileSize(MainApplication.getContext(), fileSize));
    }

    public long getFileSize(File file) {
        if (file == null) {
            return 0;
        }
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return size;
    }

    public void copyMp3(View view) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            File copyFile = new File(mSaveDir, "copy.mp3");

            if (copyFile.exists()) {
                copyFile.delete();
            }
            try {
                copyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            fos = new FileOutputStream(copyFile);
            is = getAssets().open("music.mp3");

            byte[] bytes = new byte[1024];
            int len;

            while ((len = is.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
                fos.flush();
            }

            toast("拷贝成功");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setOriFilePath(View view) {
        requestSystemFilePath();
    }

    public void decode(View view) {
        String audioPath = mOriFilePath;
        String audioSavePath = mSaveDir.getAbsolutePath() + "/audio_decode.pcm";
        getPCMFromAudio(audioPath, audioSavePath);
    }

    public void encode(View view) {
        String pcmPath = mSaveDir.getAbsolutePath() + "/audio_decode.pcm";
        String audioPath = mSaveDir.getAbsolutePath() + "/audio_encode.m4a";
        pcmToAudio(pcmPath, audioPath);
    }

    /**
     * 将音频文件解码成原始的PCM数据
     *
     * @param audioPath     MP3文件目录
     * @param audioSavePath pcm文件保存位置
     */
    public void getPCMFromAudio(String audioPath, final String audioSavePath) {
        if (TextUtils.isEmpty(audioPath)) {
            toast("audio path is null");
            return;
        }

        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(audioPath);

            int audioTrack = -1;
            boolean hasAudio = false;

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String type = trackFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    break;
                }
            }

            if (hasAudio) {
                mediaExtractor.selectTrack(audioTrack);

                new Thread(new AudioDecodeRunnable(mediaExtractor, audioTrack, audioSavePath, new DecodeOverListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void decodeOver() {
                        mPath.setText("解码存放路径：\n" + audioSavePath);
                        mStatus.setText("解码完成");
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void decodeProgress(long progress) {
                        String fileSize = Formatter.formatFileSize(MainApplication.getContext(), progress);
                        mStatus.setText("解码中 " + fileSize);
                    }

                    @Override
                    public void decodeFail() {
                        mStatus.setText("解码失败");
                    }
                })).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pcmToAudio(String pcmPath, final String audioPath) {
        if (TextUtils.isEmpty(pcmPath)) {
            toast("pcm path is null");
            return;
        }

        if (!new File(pcmPath).exists()) {
            toast("pcm file is not exists");
            return;
        }

        mStatus.setText("编码中");

        new Thread(new AudioEncodeRunnable(pcmPath, audioPath, new AudioDecodeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void decodeOver() {
                mPath.setText("编码存放路径：\n" + audioPath);
                mStatus.setText("编码成功");
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void decodeProgress(long progress) {
                String fileSize = Formatter.formatFileSize(MainApplication.getContext(), progress);
                mStatus.setText("编码中 " + fileSize);
            }

            @Override
            public void decodeFail() {
                mStatus.setText("编码失败");
            }
        })).start();
    }

    /**
     * 音频解码监听器：监听是否解码成功
     */
    public interface AudioDecodeListener {

        void decodeOver();

        void decodeProgress(long progress);

        void decodeFail();
    }

    public interface DecodeOverListener {

        void decodeOver();

        void decodeProgress(long progress);

        void decodeFail();
    }

}
