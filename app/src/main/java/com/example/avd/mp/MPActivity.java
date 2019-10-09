package com.example.avd.mp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.avd.R;
import com.example.avd.mp.android.AndroidMPActivity;

public class MPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp);
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, AndroidMPActivity.class));
    }

    public void clickBtn2(View view) {

    }
}
