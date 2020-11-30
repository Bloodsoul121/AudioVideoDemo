package com.blood.jni;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class JniActivity extends AppCompatActivity {

    static {
        System.loadLibrary("jni");
    }

    private static final String TAG = "JniActivity";

    public String mField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jni);
    }

    public void changeField(View view) {
        changeField("hello");
    }

    public void toast() {
        Log.i(TAG, "mField : " + mField);
        Toast.makeText(this, mField, Toast.LENGTH_SHORT).show();
    }

    public native void changeField(String msg);
}