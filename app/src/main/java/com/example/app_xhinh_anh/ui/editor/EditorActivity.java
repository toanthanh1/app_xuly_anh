package com.example.app_xhinh_anh.ui.editor;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.app_xhinh_anh.R;

public class EditorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editor);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView ivSelectedImage = findViewById(R.id.ivSelectedImage);
        
        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            
            // Sử dụng Glide để load ảnh (tự động xử lý cả Gallery và Camera URI)
            Glide.with(this)
                    .load(imageUri)
                    .into(ivSelectedImage);
        }
    }
}