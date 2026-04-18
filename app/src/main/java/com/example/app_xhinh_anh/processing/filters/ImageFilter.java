package com.example.app_xhinh_anh.processing.filters;

import android.graphics.Bitmap;

/**
 * Interface cơ sở cho các bộ lọc hình ảnh.
 */
public interface ImageFilter {
    Bitmap apply(Bitmap input);
    String getName();
}