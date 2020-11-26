package com.blood.giflib;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class GifActivity extends AppCompatActivity {

    private static final String TAG = GifActivity.class.getSimpleName();

    private ImageView mIv;
    private File mFilesDir;
    private String mGifName;

    private Bitmap mBitmap;
    private GifHandler mGifHandler;

    private HandlerThread mHandlerThread;
    private Handler mBackHandler;
    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif);
        mIv = findViewById(R.id.iv);

        mFilesDir = getFilesDir();
        mGifName = "demo.gif";

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                // 将bitmap显示到view上
                mIv.setImageBitmap(mBitmap);
            }
        };

        mHandlerThread = new HandlerThread("gif-thread");
        mHandlerThread.start();
        mBackHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 0:
                        loadGif();
                        break;
                    case 1:
                        updateFrame();
                        break;
                }
            }
        };
    }

    public void copyGif(View view) {
        FileUtil.copyfile(this, mFilesDir, mGifName);
    }

    public void loadGif(View view) {
        mBackHandler.sendEmptyMessage(0);
    }

    public void loadGif() {
        // 将gif文件加载进来
        File file = new File(mFilesDir, mGifName);
        boolean exists = file.exists();
        Log.i(TAG, "File gifPath : " + exists);

        mGifHandler = GifHandler.loadGif(file.getAbsolutePath());

        final int gifWidth = mGifHandler.getWidth();
        final int gifHeight = mGifHandler.getHeight();
        Log.i(TAG, "gifWidth : " + gifWidth + " , gifHeight : " + gifHeight);

        // 根据gif图大小定制view
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams layoutParams = mIv.getLayoutParams();
                layoutParams.width = gifWidth;
                layoutParams.height = gifHeight;
                mIv.setLayoutParams(layoutParams);
            }
        });

        // 创建bitmap
        mBitmap = Bitmap.createBitmap(gifWidth, gifHeight, Bitmap.Config.ARGB_8888);

        // 将gif的每一帧图像绘制到bitmap上
        int delay = mGifHandler.updateFrame(mBitmap);

        // 将bitmap显示到view上
//        mIv.setImageBitmap(mBitmap);

//        mMainHandler.postDelayed(this, delay);

        mMainHandler.sendEmptyMessage(0);
        mBackHandler.sendEmptyMessageDelayed(1, delay);
    }

    private void updateFrame() {
        int delay = mGifHandler.updateFrame(mBitmap);
//        mIv.setImageBitmap(mBitmap);
//        mMainHandler.postDelayed(this, delay);

        mMainHandler.sendEmptyMessage(0);
        mBackHandler.sendEmptyMessageDelayed(1, delay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMainHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }

}
