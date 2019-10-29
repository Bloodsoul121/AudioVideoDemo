package com.example.avd.mv_encode_decode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.avd.BaseActivity;
import com.example.avd.R;
import com.example.avd.mv_encode_decode.decode.MvDecode264Activity;
import com.example.avd.mv_encode_decode.encode.MvEncode264Activity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;

public class MvEncodeDecodeActivity extends BaseActivity {

    @BindView(R.id.default_save_path)
    TextView mDefaultSavePath;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mv_encode_decode);
        ButterKnife.bind(this);
        mDefaultSavePath.setText("默认存储路径 : " + getExternalFilesDir(Environment.DIRECTORY_MOVIES));
        requestPermissions();
    }

    @SuppressLint("CheckResult")
    public void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA
        ).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                if (aBoolean) {
                    Toast.makeText(MvEncodeDecodeActivity.this, "accept", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MvEncodeDecodeActivity.this, "deny", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, MvEncode264Activity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, MvDecode264Activity.class));
    }
}
