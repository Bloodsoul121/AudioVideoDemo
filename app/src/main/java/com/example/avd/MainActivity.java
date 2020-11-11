package com.example.avd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.avd.audio_encode_decode.AudioEncodeDecodeActivity;
import com.example.avd.audio_record.AudioRecordActivity;
import com.example.avd.camera.CameraActivity;
import com.example.avd.camera.reuse.CameraReuseActivity;
import com.example.avd.flow.AudioMvFlowActivity;
import com.example.avd.mp.MPActivity;
import com.example.avd.mv_encode_decode.MvEncodeDecodeActivity;
import com.example.avd.mv_split_compose.MvSplitComposeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}
