package com.example.app_xhinh_anh.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.ui.editor.EditorActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageActionManager {

    private final AppCompatActivity activity;
    private ActivityResultLauncher<String> mGetContent;
    private ActivityResultLauncher<Uri> mTakePicture;
    private ActivityResultLauncher<String> mRequestPermission;
    private ActivityResultLauncher<String> mRequestStoragePermission;
    private Uri photoUri;

    public ImageActionManager(AppCompatActivity activity) {
        this.activity = activity;
        initLaunchers();
    }

    private void initLaunchers() {
        mGetContent = activity.registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        navigateToEditorActivity(uri);
                    }
                });

        mTakePicture = activity.registerForActivityResult(new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        navigateToEditorActivity(photoUri);
                    }
                });

        mRequestPermission = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        captureImage();
                    } else {
                        Toast.makeText(activity, "Ứng dụng cần quyền Camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                    }
                });

        mRequestStoragePermission = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        mGetContent.launch("image/*");
                    } else {
                        Toast.makeText(activity, "Ứng dụng cần quyền truy cập ảnh để chọn ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getStoragePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void pickImageWithPermission() {
        String perm = getStoragePermission();
        if (ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED) {
            mGetContent.launch("image/*");
        } else {
            mRequestStoragePermission.launch(perm);
        }
    }

    public void setUpButtons(View rootView) {
        View btnPickImage = rootView.findViewById(R.id.btnPickImage);
        View btnCaptureImage = rootView.findViewById(R.id.btnCaptureImage);

        if (btnPickImage != null) {
            btnPickImage.setOnClickListener(v -> pickImageWithPermission());
        }

        if (btnCaptureImage != null) {
            btnCaptureImage.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    captureImage();
                } else {
                    mRequestPermission.launch(Manifest.permission.CAMERA);
                }
            });
        }
    }

    private void captureImage() {
        try {
            photoUri = createImageUri();
            if (photoUri != null) {
                mTakePicture.launch(photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToEditorActivity(Uri uri) {
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.showLoadingDialog("Đang chuẩn bị trình chỉnh sửa...");
            mainActivity.setWaitingForEditor(true);
        }

        // Tạo một khoảng trễ nhỏ để người dùng thấy loading trước khi chuyển cảnh
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(activity, EditorActivity.class);
            intent.putExtra("image_uri", uri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        }, 400);
    }

    private Uri createImageUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        return FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider",
                image);
    }
}