package com.example.app_xhinh_anh.ui.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageActionManager imageActionManager;
    private Dialog loadingDialog;
    private boolean isWaitingForEditor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageActionManager = new ImageActionManager(this);
        imageActionManager.setUpButtons(binding.getRoot());

        setupTitleAnimation();
    }

    private void setupTitleAnimation() {
        TextView tvTitle = binding.tvTitle;
        if (tvTitle == null) return;

        tvTitle.post(() -> {
            float width = tvTitle.getPaint().measureText(tvTitle.getText().toString());
            int[] colors = {
                    Color.parseColor("#FF6B6B"), // đỏ hồng
                    Color.parseColor("#FFD93D"), // vàng
                    Color.parseColor("#6BCB77"), // xanh lá
                    Color.parseColor("#4D96FF"), // xanh dương
                    Color.parseColor("#A66CFF")  // tím
            };
            LinearGradient gradient = new LinearGradient(
                    0, 0, width, tvTitle.getTextSize(),
                    colors, null, Shader.TileMode.MIRROR);
            tvTitle.getPaint().setShader(gradient);

            ValueAnimator shimmer = ValueAnimator.ofFloat(0f, width * 2);
            shimmer.setDuration(2500);
            shimmer.setRepeatCount(ValueAnimator.INFINITE);
            shimmer.setInterpolator(new AccelerateDecelerateInterpolator());
            shimmer.addUpdateListener(a -> {
                Matrix m = new Matrix();
                m.setTranslate((float) a.getAnimatedValue(), 0);
                gradient.setLocalMatrix(m);
                tvTitle.invalidate();
            });
            shimmer.start();
        });

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvTitle, "scaleX", 1f, 1.04f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvTitle, "scaleY", 1f, 1.04f, 1f);
        scaleX.setDuration(1800);
        scaleY.setDuration(1800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Chỉ đóng loading khi thực sự quay lại từ EditorActivity
        if (isWaitingForEditor) {
            dismissLoadingDialog();
            isWaitingForEditor = false; // Reset cờ
        }
    }

    public void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public void setWaitingForEditor(boolean waiting) {
        this.isWaitingForEditor = waiting;
    }

    public Dialog showLoadingDialog(String message) {
        // Trước khi hiện cái mới, xóa cái cũ nếu có (đề phòng)
        dismissLoadingDialog();

        // Sử dụng Dialog cơ bản thay vì MaterialAlertDialogBuilder để tránh bị "nhảy"
        loadingDialog = new Dialog(this, R.style.TransparentDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_loading_dialog, null);
        
        TextView tvMessage = view.findViewById(R.id.tvLoadingMessage);
        if (tvMessage != null) tvMessage.setText(message);

        loadingDialog.setContentView(view);
        loadingDialog.setCancelable(false);
        
        // Đảm bảo window không có background và căn giữa chính xác
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Xóa bỏ các insets mặc định có thể gây lệch vị trí
            loadingDialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }
        
        loadingDialog.show();
        return loadingDialog;
    }
}