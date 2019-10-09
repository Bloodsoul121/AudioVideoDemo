package com.example.avd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.avd.ar.ARActivity;
import com.example.avd.mp.MPActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, MPActivity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, ARActivity.class));
    }

}
