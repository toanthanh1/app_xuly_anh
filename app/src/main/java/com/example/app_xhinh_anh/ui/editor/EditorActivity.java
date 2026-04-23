package com.example.app_xhinh_anh.ui.editor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_xhinh_anh.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;

public class EditorActivity extends AppCompatActivity {

    private PhotoEditorView photoEditorView;
    private PhotoEditor photoEditor;
    private Uri currentImageUri;
    private boolean isBrushMode = false;

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

        initViews();
        setupPhotoEditor();

        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            currentImageUri = Uri.parse(imageUriString);
            photoEditorView.getSource().setImageURI(currentImageUri);
        }
    }

    private void initViews() {
        photoEditorView = findViewById(R.id.photoEditorView);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button btnFlip = findViewById(R.id.btnFlip);
        Button btnFilter = findViewById(R.id.btnFilter);
        Button btnBrush = findViewById(R.id.btnBrush);
        Button btnAddText = findViewById(R.id.btnAddText);
        Button btnSticker = findViewById(R.id.btnSticker);
        Button btnSave = findViewById(R.id.btnSave);

        btnCrop.setOnClickListener(v -> startCrop(currentImageUri));
        
        btnFlip.setOnClickListener(v -> {
            // Lấy Bitmap hiện tại từ ImageView
            if (photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
                Bitmap originalBitmap = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                
                // Sử dụng Matrix để lật ngược ảnh
                Matrix matrix = new Matrix();
                matrix.postScale(-1, 1); // Lật ngang
                
                Bitmap flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, 
                        originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                
                // Gán lại Bitmap đã lật vào ImageView
                photoEditorView.getSource().setImageBitmap(flippedBitmap);
                Toast.makeText(this, "Đã lật ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        btnBrush.setOnClickListener(v -> {
            isBrushMode = !isBrushMode;
            photoEditor.setBrushDrawingMode(isBrushMode);
            btnBrush.setText(isBrushMode ? "Đang vẽ" : "Vẽ");
            if (isBrushMode) {
                photoEditor.setBrushColor(ContextCompat.getColor(this, R.color.brand_green));
                Toast.makeText(this, "Chế độ vẽ đã bật", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddText.setOnClickListener(v -> {
            photoEditor.addText("Text", ContextCompat.getColor(this, R.color.white));
        });

        btnFilter.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Không bộ lọc");
            popup.getMenu().add("Trắng đen");
            popup.getMenu().add("Hoài cổ (Sepia)");
            popup.getMenu().add("Cổ điển (Vignette)");

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Không bộ lọc":
                        photoEditor.setFilterEffect(PhotoFilter.NONE);
                        break;
                    case "Trắng đen":
                        photoEditor.setFilterEffect(PhotoFilter.GRAY_SCALE);
                        break;
                    case "Hoài cổ (Sepia)":
                        photoEditor.setFilterEffect(PhotoFilter.SEPIA);
                        break;
                    case "Cổ điển (Vignette)":
                        photoEditor.setFilterEffect(PhotoFilter.VIGNETTE);
                        break;
                }
                Toast.makeText(this, "Đã áp dụng: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        btnSticker.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng Sticker/Icon sẽ cần thêm bộ icon", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> saveProcessedImage());
    }

    private void setupPhotoEditor() {
        photoEditor = new PhotoEditor.Builder(this, photoEditorView)
                .setPinchTextScalable(true)
                .build();

        photoEditor.setOnPhotoEditorListener(new OnPhotoEditorListener() {
            @Override
            public void onEditTextChangeListener(@Nullable View view, @NonNull String s, int i) {}

            @Override
            public void onAddViewListener(@NonNull ViewType viewType, int i) {}

            @Override
            public void onRemoveViewListener(@NonNull ViewType viewType, int i) {}

            @Override
            public void onStartViewChangeListener(@NonNull ViewType viewType) {}

            @Override
            public void onStopViewChangeListener(@NonNull ViewType viewType) {}

            @Override
            public void onTouchSourceImage(@NonNull MotionEvent motionEvent) {}
        });
    }

    private void saveProcessedImage() {
        File file = new File(getCacheDir(), "EditedImage_" + System.currentTimeMillis() + ".jpg");
        try {
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();

            photoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    saveImageToGallery(Uri.fromFile(new File(imagePath)));
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(EditorActivity.this, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void saveImageToGallery(Uri uri) {
        ContentValues values = new ContentValues();
        String fileName = "App_xhinh_anh_" + System.currentTimeMillis() + ".jpg";
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AppXinhAnh");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (itemUri != null) {
            try (OutputStream out = resolver.openOutputStream(itemUri);
                 InputStream in = resolver.openInputStream(uri)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(itemUri, values, null, null);
                }
                Toast.makeText(this, "Đã lưu ảnh vào thư viện!", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(Uri uri) {
        String destinationFileName = "CroppedImage_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        
        UCrop.Options options = new UCrop.Options();
        int brandColor = ContextCompat.getColor(this, R.color.brand_green);
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        
        // Đặt màu cho Toolbar (Thanh công cụ)
        options.setToolbarColor(brandColor);
        // Đặt màu cho các icon/text trên Toolbar
        options.setToolbarWidgetColor(whiteColor);
        // Đặt màu cho các nút điều khiển đang được chọn (Active)
        options.setActiveControlsWidgetColor(brandColor);
        // Đặt tiêu đề cho màn hình cắt
        options.setToolbarTitle("Cắt ảnh");

        uCrop.withOptions(options);
        uCrop.start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                currentImageUri = resultUri;
                photoEditorView.getSource().setImageURI(currentImageUri);
            }
        }
    }
}