package com.example.avd.livechat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.avd.R;
import com.example.avd.livechat.client.ClientLiveChatActivity;
import com.example.avd.livechat.server.ServerLiveChatActivity;

import androidx.appcompat.app.AppCompatActivity;

public class LiveChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_chat);
    }

    public void clickBtn1(View view) {
        startActivity(new Intent(this, ServerLiveChatActivity.class));
    }

    public void clickBtn2(View view) {
        startActivity(new Intent(this, ClientLiveChatActivity.class));
    }
}