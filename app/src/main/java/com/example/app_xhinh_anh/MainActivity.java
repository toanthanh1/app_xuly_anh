package com.example.app_xhinh_anh;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_xhinh_anh.utils.ImageActionManager;

public class MainActivity extends AppCompatActivity {

    private ImageActionManager imageActionManager;

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

        // Khởi tạo trình quản lý chức năng hình ảnh
        imageActionManager = new ImageActionManager(this);
        
        // Thiết lập sự kiện cho các button (được gọi từ layout include)
        imageActionManager.setUpButtons(findViewById(R.id.main));
    }
}