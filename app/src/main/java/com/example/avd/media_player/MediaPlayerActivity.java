package com.example.avd.media_player;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.avd.R;
import com.example.avd.media_player.android.AndroidMPActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MediaPlayerActivity extends AppCompatActivity {

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
