package com.example.app_xhinh_anh.data.repository;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Interface cho việc lưu trữ và truy xuất ảnh.
 */
public interface ImageRepository {
    void saveImage(Bitmap bitmap, String fileName);
    Bitmap loadImage(Uri uri);
}