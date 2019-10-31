package com.example.avd.mv_encode_decode.encode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 第一个会出现镜像效果
 * 第三个可用 {@link #rotateYUV420SP3(byte[], int, int)}
 */
public class RotateYuvUtil {

    /**
     * 将视频流先旋转90度，否则录制的视频是横向的
     */
    public static byte[] rotateYUV420SP(byte[] src, int width, int height) {
        byte[] des = new byte[src.length];

        int wh = width * height;
        //旋转Y
        int k = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                des[k] = src[width * j + i];
                k++;
            }
        }

        for (int i = 0; i < width; i += 2) {
            for (int j = 0; j < height / 2; j++) {
                des[k] = src[wh + width * j + i];
                des[k + 1] = src[wh + width * j + i + 1];
                k += 2;
            }
        }

        return des;
    }

    public static byte[] rotateYUV420SP2(byte[] src, int width, int height) {
        byte[] des = new byte[src.length];

        int wh = width * height;

        int k = 0;
        for (int i = 0; i < width; i++) {
            for (int j = height - 1; j >= 0; j--) {
                des[k] = src[width * j + i];
                k++;
            }
        }

        for (int i = 0; i < width; i += 2) {
            for (int j = height / 2 - 1; j >= 0; j--) {
                des[k] = src[wh + width * j + i];
                des[k + 1] = src[wh + width * j + i + 1];
                k += 2;
            }
        }

        return des;
    }

    public static byte[] rotateYUV420SP3(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        int wh = width * height;
        //旋转Y
        int k = 0;
        for (int i = 0; i < width; i++) {
            for (int j = height - 1; j >= 0; j--) {
                dst[k] = src[width * j + i];
                k++;
            }
        }

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int colIndex = 0; colIndex < halfWidth; colIndex++) {
            for (int rowIndex = halfHeight - 1; rowIndex >= 0; rowIndex--) {
                int index = (halfWidth * rowIndex + colIndex) * 2;
                dst[k] = src[wh + index];
                k++;
                dst[k] = src[wh + index + 1];
                k++;
            }
        }
        return dst;
    }

    /**
     * 将YUV420SP数据顺时针旋转90度
     *
     * @param data        要旋转的数据
     * @param imageWidth  要旋转的图片宽度
     * @param imageHeight 要旋转的图片高度
     * @return 旋转后的数据
     */
    public static byte[] rotateNV21Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    /**
     * 保存数据到本地
     *
     * @param buffer 要保存的数据
     * @param offset 要保存数据的起始位置
     * @param length 要保存数据长度
     * @param path   保存路径
     * @param append 是否追加
     */
    public static void save(byte[] buffer, int offset, int length, String path, boolean append) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path, append);
            fos.write(buffer, offset, length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
