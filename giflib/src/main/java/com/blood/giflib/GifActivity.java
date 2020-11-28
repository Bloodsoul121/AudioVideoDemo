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
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class GifActivity extends AppCompatActivity {

    private static final String TAG = GifActivity.class.getSimpleName();

    private static final int MAIN_TASK_RESIZE = 0;
    private static final int MAIN_TASK_SHOW_FRAME = 1;

    private static final int BACK_TASK_PREPARE_GIF = 0;
    private static final int BACK_TASK_LOAD_GIF = 1;
    private static final int BACK_TASK_UPDATE_FRAME = 2;

    private ImageView mIv;
    private File mFilesDir;
    private String mGifName;

    private Bitmap mBitmap;
    private GifHandler mGifHandler;

    private Handler mMainHandler;
    private Handler mBackHandler;
    private HandlerThread mBackThread;

    private boolean mIsPrepared;

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
                switch (msg.what) {
                    case MAIN_TASK_RESIZE:
                        ViewGroup.LayoutParams layoutParams = mIv.getLayoutParams();
                        layoutParams.width = msg.arg1;
                        layoutParams.height = msg.arg2;
                        mIv.setLayoutParams(layoutParams);
                        break;
                    case MAIN_TASK_SHOW_FRAME:
                        mIv.setImageBitmap(mBitmap);
                        break;
                }

            }
        };

        mBackThread = new HandlerThread("gif-thread");
        mBackThread.start();
        mBackHandler = new Handler(mBackThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case BACK_TASK_PREPARE_GIF:
                        prepareGif();
                        break;
                    case BACK_TASK_LOAD_GIF:
                        loadGif();
                        break;
                    case BACK_TASK_UPDATE_FRAME:
                        updateFrame();
                        break;
                }
            }
        };
    }

    // 复制gif文件
    public void copyGif(View view) {
        FileUtil.copyfile(this, mFilesDir, mGifName);
    }

    // 预加载
    public void prepareLoadGif(View view) {
        mBackHandler.sendEmptyMessage(BACK_TASK_PREPARE_GIF);
    }

    // 播放gif文件
    public void playGif(View view) {
        mBackHandler.sendEmptyMessage(BACK_TASK_LOAD_GIF);
    }

    private void prepareGif() {
        if (mIsPrepared) {
            return;
        }

        long startTime = System.currentTimeMillis();

        // 将gif文件加载进来
        File file = new File(mFilesDir, mGifName);
        boolean exists = file.exists();
        Log.i(TAG, "File gifPath : " + exists);

        mGifHandler = GifHandler.loadGif(file.getAbsolutePath());

        final int gifWidth = mGifHandler.getWidth();
        final int gifHeight = mGifHandler.getHeight();
        Log.i(TAG, "gifWidth : " + gifWidth + " , gifHeight : " + gifHeight);

        // 创建bitmap
        mBitmap = Bitmap.createBitmap(gifWidth, gifHeight, Bitmap.Config.ARGB_8888);

        // 根据gif图大小定制view
        Message message = Message.obtain();
        message.arg1 = gifWidth;
        message.arg2 = gifHeight;
        mMainHandler.sendMessage(message);

        mIsPrepared = true;

        long subTime = System.currentTimeMillis() - startTime;
        Toast.makeText(this, "预加载，耗时：" + subTime + "ms", Toast.LENGTH_SHORT).show();
    }

    private void loadGif() {
        // 准备工作
        prepareGif();
        // 绘制
        updateFrame();
    }

    private void updateFrame() {
        // 将gif的每一帧图像绘制到bitmap上
        int delay = mGifHandler.updateFrame(mBitmap);
        // 将bitmap显示到view上
        mMainHandler.sendEmptyMessage(MAIN_TASK_SHOW_FRAME);
        // delay之后开始下一次绘制
        mBackHandler.sendEmptyMessageDelayed(BACK_TASK_UPDATE_FRAME, delay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMainHandler.removeCallbacksAndMessages(null);
        mBackHandler.removeCallbacksAndMessages(null);
        mBackThread.quit();
    }

}
