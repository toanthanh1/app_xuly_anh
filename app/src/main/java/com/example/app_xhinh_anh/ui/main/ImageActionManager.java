package com.example.app_xhinh_anh.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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
    }

    public void setUpButtons(View rootView) {
        Button btnPickImage = rootView.findViewById(R.id.btnPickImage);
        Button btnCaptureImage = rootView.findViewById(R.id.btnCaptureImage);

        if (btnPickImage != null) {
            btnPickImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        }

        if (btnCaptureImage != null) {
            btnCaptureImage.setOnClickListener(v -> {
                try {
                    photoUri = createImageUri();
                    if (photoUri != null) {
                        mTakePicture.launch(photoUri);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void navigateToEditorActivity(Uri uri) {
        Intent intent = new Intent(activity, EditorActivity.class);
        intent.putExtra("image_uri", uri.toString());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
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