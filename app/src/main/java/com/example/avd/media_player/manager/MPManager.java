package com.example.avd.media_player.manager;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;

import com.example.avd.MainApplication;
import com.example.avd.media_player.android.Music;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MPManager implements MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    refreshProgress();
                    break;
            }
        }
    };

    private boolean mIsRunning;
    private int mPosition;
    private Music mCurMusic;
    private Timer mTimer;
    private MediaPlayer mMediaPlayer;
    private Set<Callback> mCallbacks = new HashSet<>();
    private List<Music> mMediaList = new ArrayList<>();

    public static MPManager getInstance() {
        return Holder.sInstance;
    }

    private static class Holder {
        private static final MPManager sInstance = new MPManager();
    }

    private MPManager() {
    }

    public void init() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
        }
        startProgressTask();
    }

    private void startProgressTask() {
        mIsRunning = true;
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!mIsRunning) {
                    return;
                }
                mHandler.sendEmptyMessage(0);
            }
        }, 100, 800);
    }

    public void recycle() {
        mTimer.cancel();
        mTimer = null;
        stop();
        mMediaPlayer.release();
        mMediaPlayer = null; // 这里必须要置null，不然状态异常
        mCurMusic = null;
        mPosition = 0;
    }

    public void setMediaList(List<Music> list) {
        if (list == null) {
            return;
        }
        mMediaList.clear();
        mMediaList.addAll(list);
    }

    public void setMediaPlayPosition(int position) {
        mPosition = position;
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void playOrPause() {
        if (mMediaList.size() == 0 || mPosition < 0 || mPosition >= mMediaList.size()) {
            throw new IllegalArgumentException();
        }

        Music music = mMediaList.get(mPosition);
        boolean isSame = mCurMusic != null && mCurMusic.id == music.id;
        mCurMusic = music;

        if (isSame) {
            doPlayOrPause();
        } else {
            play();
        }
    }

    private void play() {
        try {
            mMediaPlayer.stop();
            mMediaPlayer.reset();//进行重置
//            mMediaPlayer.setDataSource(mMusicPath);
            AssetFileDescriptor fd = MainApplication.getContext().getAssets().openFd(mCurMusic.path);
            mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mMediaPlayer.prepare();
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doPlayOrPause() {
        if (isPlaying()) {
            pause();
        } else {
            start();
        }
    }

    private void start() {
        mIsRunning = true;
        mMediaPlayer.start();
        callbackOnPlayOrPause(true);
    }

    public void pause() {
        mIsRunning = false;
        mMediaPlayer.pause();
        callbackOnPlayOrPause(false);
    }

    public void stop() {
        mIsRunning = false;
        if (isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }
    }

    private void refreshProgress() {
        int progress = mMediaPlayer.getCurrentPosition();
        callbackOnMediaProgressChanged(progress);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        int totalTime = mMediaPlayer.getDuration();
        callbackOnMediaProgressDuration(totalTime);
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public interface Callback {

        void onPlayOrPause(boolean isPlay);

        void onMediaProgressDuration(int total);

        void onMediaProgressChanged(int progress);
    }

    private void callbackOnPlayOrPause(boolean isPlay) {
        for (Callback callback : mCallbacks) {
            callback.onPlayOrPause(isPlay);
        }
    }

    private void callbackOnMediaProgressDuration(int total) {
        for (Callback callback : mCallbacks) {
            callback.onMediaProgressDuration(total);
        }
    }

    private void callbackOnMediaProgressChanged(int progress) {
        for (Callback callback : mCallbacks) {
            callback.onMediaProgressChanged(progress);
        }
    }
}
