package com.example.app_xhinh_anh.processing.tools;

import android.graphics.Bitmap;

/**
 * Interface cơ sở cho các công cụ chỉnh sửa (Crop, Rotate...).
 */
public interface ImageTool {
    Bitmap process(Bitmap input);
}