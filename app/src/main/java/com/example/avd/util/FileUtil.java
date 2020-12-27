package com.example.avd.util;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.example.avd.util
 *  @文件名:   FileUtil
 *  @创建者:   bloodsoul
 *  @创建时间:  2020/12/26 14:51
 *  @描述：    TODO
 */

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

    private static final String TAG = "FileUtil";

    public static File getParentDir() {
        return Environment.getExternalStorageDirectory();
    }

    public static void writeContent(byte[] array, File saveFile) {
        char[] HEX_CHAR_TABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            // 高位
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            // 低位
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Log.i(TAG, "writeContent: " + sb.toString());
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(saveFile, true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeBytes(byte[] array, File saveFile) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(saveFile, true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
