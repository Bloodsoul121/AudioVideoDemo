package com.example.avd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.blood.giflib.GifActivity;
import com.blood.jni.JniActivity;
import com.example.avd.audio_encode_decode.AudioEncodeDecodeActivity;
import com.example.avd.audio_record.AudioRecordActivity;
import com.example.avd.camera.CameraActivity;
import com.example.avd.camera.reuse.CameraReuseActivity;
import com.example.avd.flow.AudioMvFlowActivity;
import com.example.avd.h264.decode.H264ParserActivity;
import com.example.avd.h264.encode.H264EncodeActivity;
import com.example.avd.mp.MPActivity;
import com.example.avd.mv_encode_decode.MvEncodeDecodeActivity;
import com.example.avd.mv_split_compose.MvSplitComposeActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    @SuppressLint("CheckResult")
    public void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (aBoolean) {
                    toast("accept");
                } else {
                    toast("deny");
                    finish();
                }
            }
        });
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, CameraReuseActivity.class));
    }

    public void clickBtn3(View view) {
        startActivity(new Intent(this, MPActivity.class));
    }

    public void clickBtn4(View view) {
        startActivity(new Intent(this, AudioRecordActivity.class));
    }

    public void clickBtn5(View view) {
        startActivity(new Intent(this, AudioEncodeDecodeActivity.class));
    }

    public void clickBtn6(View view) {
        startActivity(new Intent(this, MvSplitComposeActivity.class));
    }

    public void clickBtn7(View view) {
        startActivity(new Intent(this, MvEncodeDecodeActivity.class));
    }

    public void clickBtn8(View view) {
        startActivity(new Intent(this, AudioMvFlowActivity.class));
    }

    public void clickBtn9(View view) {
        startActivity(new Intent(this, H264ParserActivity.class));
    }

    public void clickBtn10(View view) {
        startActivity(new Intent(this, GifActivity.class));
    }

    public void clickBtn11(View view) {
        startActivity(new Intent(this, JniActivity.class));
    }

    public void clickBtn12(View view) {
        startActivity(new Intent(this, H264EncodeActivity.class));
    }
}
