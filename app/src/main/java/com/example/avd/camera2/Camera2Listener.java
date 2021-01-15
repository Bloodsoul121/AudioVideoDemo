package com.example.avd.camera2;

import android.util.Size;

public interface Camera2Listener {
    /**
     * 预览数据回调
     * @param y 预览数据，Y分量
     * @param u 预览数据，U分量
     * @param v 预览数据，V分量
     * @param previewSize  预览尺寸
     * @param stride    步长
     */
    void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride);
}
