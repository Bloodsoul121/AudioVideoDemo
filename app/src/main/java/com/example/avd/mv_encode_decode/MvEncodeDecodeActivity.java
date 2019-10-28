package com.example.avd.mv_encode_decode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.avd.BaseActivity;
import com.example.avd.R;
import com.example.avd.mv_encode_decode.decode.MvDecode264Activity;
import com.example.avd.mv_encode_decode.encode.MvEncode264Activity;

public class MvEncodeDecodeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mv_encode_decode);
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, MvEncode264Activity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, MvDecode264Activity.class));
    }
}
