package com.example.avd.mp.android;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.avd.R;
import com.example.avd.mp.manager.MPManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AndroidMPActivity extends AppCompatActivity implements MPManager.Callback {

    @BindView(R.id.cur_progress)
    TextView mCurProgress;
    @BindView(R.id.total_progress)
    TextView mTotalProgress;
    @BindView(R.id.seekbar)
    SeekBar mSeekbar;
    @BindView(R.id.play)
    ImageView mPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        ButterKnife.bind(this);
        MPManager.getInstance().init();
        MPManager.getInstance().addCallback(this);
        init();
        initMediaData();
    }

    private void initMediaData() {
        List<Music> list = new ArrayList<>();
        Music music = new Music();
        music.id = 1;
        music.path = "music.mp3";
        music.songName = "unknown";
        music.authorName = "unknown";
        list.add(music);
        MPManager.getInstance().setMediaList(list);
        MPManager.getInstance().setMediaPlayPosition(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MPManager.getInstance().removeCallback(this);
        MPManager.getInstance().recycle();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        mCurProgress.setText("00:00");
        mTotalProgress.setText("00:00");
        mPlay.setActivated(false);
    }

    @OnClick({R.id.play, R.id.pre, R.id.next, R.id.cur_progress, R.id.seekbar, R.id.total_progress})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.play:
                MPManager.getInstance().playOrPause();
                break;
            case R.id.pre:
                break;
            case R.id.next:
                break;
        }
    }

    @Override
    public void onPlayOrPause(boolean isPlay) {
        mPlay.setActivated(isPlay);
    }

    @Override
    public void onMediaProgressDuration(int total) {
        mSeekbar.setMax(total);
        mTotalProgress.setText(formatTime(total));
    }

    @Override
    public void onMediaProgressChanged(int progress) {
        mSeekbar.setProgress(progress);
        mCurProgress.setText(formatTime(progress));
    }

    private String formatTime(int length) {
        Date date = new Date(length);//调用Date方法获值
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss", Locale.CHINA);//规定需要形式
        return simpleDateFormat.format(date);
    }
}
