package com.example.app_xhinh_anh;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> mGetContent;
    private ActivityResultLauncher<Uri> mTakePicture;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Chọn ảnh từ thư viện
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        navigateToSecondActivity(uri);
                    }
                });

        // Chụp ảnh từ Camera
        mTakePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        navigateToSecondActivity(photoUri);
                    }
                });

        Button btnPickImage = findViewById(R.id.btnPickImage);
        btnPickImage.setOnClickListener(v -> mGetContent.launch("image/*"));

        Button btnCaptureImage = findViewById(R.id.btnCaptureImage);
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

    private void navigateToSecondActivity(Uri uri) {
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra("image_uri", uri.toString());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private Uri createImageUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        
        return FileProvider.getUriForFile(this,
                "com.example.app_xhinh_anh.fileprovider",
                image);
    }
}