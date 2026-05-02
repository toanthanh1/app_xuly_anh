package com.example.app_xhinh_anh.processing.tools;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.databinding.ActivityEditorBinding;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder;
import ja.burhanrashid52.photoeditor.shape.ShapeType;

public class BrushManager {

    private final AppCompatActivity activity;
    private final ActivityEditorBinding binding;
    private final PhotoEditor photoEditor;

    private int currentBrushColor = Color.WHITE;
    private ShapeType currentShapeType = ShapeType.Brush.INSTANCE;

    public BrushManager(AppCompatActivity activity, ActivityEditorBinding binding, PhotoEditor photoEditor) {
        this.activity = activity;
        this.binding = binding;
        this.photoEditor = photoEditor;
        initViews();
    }

    private void initViews() {
        binding.btnBrushClose.setOnClickListener(v -> closeBrushPanel());
        binding.btnBrushDone.setOnClickListener(v -> closeBrushPanel());

        setupBrushControls();
    }

    private void setupBrushControls() {
        binding.seekBrushWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float size = Math.max(1f, progress);
                photoEditor.setBrushSize(size);
                ShapeBuilder sb = new ShapeBuilder()
                        .withShapeType(currentShapeType)
                        .withShapeColor(currentBrushColor)
                        .withShapeSize(size);
                photoEditor.setShape(sb);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.btnChooseColor.setOnClickListener(v -> {
            if (binding.colorPickerPanel.getVisibility() == View.VISIBLE) {
                binding.colorPickerPanel.setVisibility(View.GONE);
            } else {
                showColorPicker();
            }
        });

        binding.btnBrushEraser.setOnClickListener(v -> selectBrushTool(ShapeType.Brush.INSTANCE, true));
        binding.btnBrushFree.setOnClickListener(v -> selectBrushTool(ShapeType.Brush.INSTANCE, false));
        binding.btnBrushArrow.setOnClickListener(v -> selectBrushTool(new ShapeType.Arrow(), false));
        binding.btnBrushLine.setOnClickListener(v -> selectBrushTool(ShapeType.Line.INSTANCE, false));
        binding.btnBrushRect.setOnClickListener(v -> selectBrushTool(ShapeType.Rectangle.INSTANCE, false));
        binding.btnBrushOval.setOnClickListener(v -> selectBrushTool(ShapeType.Oval.INSTANCE, false));
    }

    public void openBrushPanel() {
        binding.brushPanel.setVisibility(View.VISIBLE);
        photoEditor.setBrushDrawingMode(true);
        selectBrushTool(ShapeType.Brush.INSTANCE, false);
        updateColorPreview();
    }

    public void closeBrushPanel() {
        binding.brushPanel.setVisibility(View.GONE);
        binding.colorPickerPanel.setVisibility(View.GONE);
        photoEditor.setBrushDrawingMode(false);
    }

    public boolean isPanelVisible() {
        return binding.brushPanel.getVisibility() == View.VISIBLE;
    }

    private void selectBrushTool(ShapeType type, boolean isEraser) {
        currentShapeType = type;
        if (isEraser) {
            photoEditor.brushEraser();
        } else {
            photoEditor.setBrushDrawingMode(true);
            ShapeBuilder sb = new ShapeBuilder()
                    .withShapeType(type)
                    .withShapeColor(currentBrushColor)
                    .withShapeSize(binding.seekBrushWidth.getProgress());
            photoEditor.setShape(sb);
        }

        int active = ContextCompat.getColor(activity, R.color.brand_green);
        int inactive = Color.TRANSPARENT;
        binding.btnBrushEraser.setBackgroundColor(isEraser ? active : inactive);
        binding.btnBrushFree.setBackgroundColor(!isEraser && type instanceof ShapeType.Brush ? active : inactive);
        binding.btnBrushArrow.setBackgroundColor(type instanceof ShapeType.Arrow ? active : inactive);
        binding.btnBrushLine.setBackgroundColor(type instanceof ShapeType.Line ? active : inactive);
        binding.btnBrushRect.setBackgroundColor(type instanceof ShapeType.Rectangle ? active : inactive);
        binding.btnBrushOval.setBackgroundColor(type instanceof ShapeType.Oval ? active : inactive);
    }

    private void showColorPicker() {
        binding.colorPickerPanel.setVisibility(View.VISIBLE);
        if (binding.colorGrid.getChildCount() == 0) {
            int[] hues = {0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330};
            float[] lights = {0.9f, 0.7f, 0.5f, 0.3f};

            for (int i = 0; i < 12; i++) {
                float gray = 1.0f - (i / 11.0f);
                addColorToGrid(Color.rgb((int)(gray*255), (int)(gray*255), (int)(gray*255)));
            }
            for (int h : hues) {
                for (float l : lights) {
                    addColorToGrid(Color.HSVToColor(new float[]{h, 1.0f, l}));
                }
            }

            int[] common = {0xFF2196F3, 0xFFF44336, 0xFFFFC107, 0xFF4CAF50, 0xFF3F51B5, 0xFF9C27B0, 0xFF000000};
            for (int color : common) {
                View view = new View(activity);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        Math.round(36 * activity.getResources().getDisplayMetrics().density),
                        Math.round(36 * activity.getResources().getDisplayMetrics().density));
                params.setMargins(0, 0, 12, 0);
                view.setLayoutParams(params);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(color);
                shape.setStroke(2, Color.GRAY);
                view.setBackground(shape);
                view.setOnClickListener(v -> onColorSelected(color));
                binding.commonColors.addView(view);
            }
        }
    }

    private void addColorToGrid(int color) {
        View view = new View(activity);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = Math.round(32 * activity.getResources().getDisplayMetrics().density);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 2, 2, 2);
        view.setLayoutParams(params);
        view.setBackgroundColor(color);
        view.setOnClickListener(v -> onColorSelected(color));
        binding.colorGrid.addView(view);
    }

    private void onColorSelected(int color) {
        currentBrushColor = color;
        ShapeBuilder sb = new ShapeBuilder()
                .withShapeType(currentShapeType)
                .withShapeColor(color)
                .withShapeSize(binding.seekBrushWidth.getProgress());
        photoEditor.setShape(sb);
        updateColorPreview();
        binding.colorPickerPanel.setVisibility(View.GONE);
    }

    private void updateColorPreview() {
        LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(activity, R.drawable.bg_color_picker_button);
        if (layerDrawable != null) {
            GradientDrawable colorCircle = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.color_circle);
            if (colorCircle != null) {
                colorCircle.setColor(currentBrushColor);
                binding.btnChooseColor.setBackground(layerDrawable);
            }
        }
    }
}
