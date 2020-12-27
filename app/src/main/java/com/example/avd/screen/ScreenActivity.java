package com.example.avd.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.avd.R;
import com.example.avd.screen.client.ClientScreenActivity;
import com.example.avd.screen.server.ServerScreenActivity;

import androidx.appcompat.app.AppCompatActivity;

public class ScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, ServerScreenActivity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, ClientScreenActivity.class));
    }
}