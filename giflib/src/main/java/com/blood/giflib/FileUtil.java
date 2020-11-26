package com.blood.giflib;

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    public static void copyfile(Context context, File dir, String fileName) {
        OutputStream out = null;
        InputStream in = null;
        try {
            File file = new File(dir, fileName);
            out = new FileOutputStream(file);
            in = context.getAssets().open(fileName);
            byte[] arr = new byte[1024];
            int len;
            while ((len = in.read(arr)) != -1) {
                out.write(arr, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Toast.makeText(context, "copy success", Toast.LENGTH_SHORT).show();
    }

}
