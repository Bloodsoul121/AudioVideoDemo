package com.example.avd.h264.demo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.avd.BaseActivity;
import com.example.avd.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class H264ParserActivity extends BaseActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = H264ParserActivity.class.getSimpleName();

    private static final String FILE_DIR = "H264";
    private static final String FILE_NAME_FULL = "full.h264";
//    private static final String FILE_NAME_PART = "part.h264";
    private static final String FILE_NAME_PART = "out2.h264";

    @BindView(R.id.texture)
    TextureView mTexture;
    @BindView(R.id.path)
    TextView mPath;
    @BindView(R.id.toggle)
    Button mToggle;

    private File mDirFile = new File(Environment.getExternalStorageDirectory(), FILE_DIR);
    private H264Parser mParser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_parser);
        ButterKnife.bind(this);
        mPath.setText(mDirFile.getAbsolutePath());

        mTexture.setSurfaceTextureListener(this);
    }

    public void clickBtn1(View view) {
        copyAssetsFile(FILE_NAME_FULL, mDirFile, FILE_NAME_FULL);
    }

    public void clickBtn2(View view) {
        copyAssetsFile(FILE_NAME_PART, mDirFile, FILE_NAME_PART);
    }

    public void clickBtn3(View view) {
        requestSystemFilePath();
    }

    public void clickBtn4(View view) {
        if (mParser == null) {
            return;
        }
        if (mParser.isRunning()) {
            mParser.stop();
            mToggle.setText("播放");
        } else {
            mParser.start(false);
            mToggle.setText("暂停");
        }
    }

    @Override
    protected void onResultSystemSelectedFilePath(String filePath) {
        super.onResultSystemSelectedFilePath(filePath);
        File selectFilePath = new File(filePath);
        mPath.setText(selectFilePath.getAbsolutePath());
        if (mParser != null) {
            mParser.setFilePath(selectFilePath);
            mParser.start();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "======================= onSurfaceTextureAvailable");
        mParser = new H264Parser(this, new Surface(surface), width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "======================= onSurfaceTextureDestroyed");
        if (mParser != null) {
            mParser.stop();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

}
