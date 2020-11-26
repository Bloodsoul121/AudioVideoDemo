package com.blood.giflib;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class GifActivity extends AppCompatActivity implements Runnable {

    private static final String TAG = GifActivity.class.getSimpleName();

    private ImageView mIv;
    private Bitmap mBitmap;
    private GifHandler mGifHandler;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif);
        mIv = findViewById(R.id.iv);
    }

    public void loadGif(View view) {
        // 将gif文件加载进来
        String gifPath = "file:///android_asset/demo.gif";
        mGifHandler = GifHandler.loadGif(gifPath);

        int gifWidth = mGifHandler.getWidth();
        int gifHeight = mGifHandler.getHeight();
        Log.i(TAG, "gifWidth : " + gifWidth + " , gifHeight : " + gifHeight);

        // 根据gif图大小定制view
        ViewGroup.LayoutParams layoutParams = mIv.getLayoutParams();
        layoutParams.width = gifWidth;
        layoutParams.height = gifHeight;
        mIv.setLayoutParams(layoutParams);

        // 创建bitmap
        mBitmap = Bitmap.createBitmap(gifWidth, gifHeight, Bitmap.Config.ARGB_8888);
        // 将gif的每一帧图像绘制到bitmap上
        int delay = mGifHandler.updateFrame(mBitmap);
        // 将bitmap显示到view上
        mIv.setImageBitmap(mBitmap);

        mHandler.postDelayed(this, delay);
    }

    @Override
    public void run() {
        int delay = mGifHandler.updateFrame(mBitmap);
        mIv.setImageBitmap(mBitmap);
        mHandler.postDelayed(this, delay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
