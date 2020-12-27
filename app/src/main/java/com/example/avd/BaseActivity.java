package com.example.avd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.blankj.utilcode.util.UriUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SYSTEM_PATH = 0xffff;

    private static final String TAG = "BaseActivity";

    private final Handler mHandler = new Handler();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            im.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        return super.onTouchEvent(event);
    }

    protected void toast(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(BaseActivity.this, "", Toast.LENGTH_SHORT);
                toast.setText(message);
                toast.show();
            }
        });
    }

    protected void requestSystemFilePath() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        //        intent.setType("video/*;audio/*");
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_SYSTEM_PATH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
            if (requestCode == REQUEST_CODE_SYSTEM_PATH) {
                Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。 Intent { dat=content://com.android.providers.media.documents/document/image:25090 flg=0x1 }
                Log.i(TAG, "onActivityResult: uri " + uri);
                //            String filePath = FileUtil.parseUri(this, uri);

                // content://com.amaze.filemanager.FILE_PROVIDER/storage_root/H264/out2.h264
                // 如果找不到路径，就复制到cache目录下
                File file = UriUtils.uri2File(uri);
                if (file == null) {
                    toast("未找到文件");
                    return;
                }
                String filePath = file.getAbsolutePath();
                Log.i(TAG, "onActivityResult: filePath " + filePath);
                if (TextUtils.isEmpty(filePath)) {
                    toast("无法解析文件路径");
                    return;
                }
                onResultSystemSelectedFilePath(filePath);
            }
        }
    }

    protected void onResultSystemSelectedFilePath(String filePath) {
        toast(filePath);
    }

    public void copyAssetsFile(String assetsFileName, File dir, String fileName) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File copyFile = new File(dir, fileName);

            if (copyFile.exists()) {
                copyFile.delete();
            }
            try {
                copyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            fos = new FileOutputStream(copyFile);
            is = getAssets().open(assetsFileName);

            byte[] bytes = new byte[1024];
            int len;

            while ((len = is.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
                fos.flush();
            }

            toast("拷贝成功");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
