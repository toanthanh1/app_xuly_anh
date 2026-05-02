package com.example.app_xhinh_anh.ui.editor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.processing.tools.BrushManager;
import com.example.app_xhinh_anh.processing.tools.StickerManager;
import com.example.app_xhinh_anh.processing.tools.TextManager;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import com.example.app_xhinh_anh.features.ai_assistant.data.GeminiApiClient;
import com.example.app_xhinh_anh.features.ai_assistant.domain.ActionMapper;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;

import com.example.app_xhinh_anh.processing.ai.AiProcessor;
import com.example.app_xhinh_anh.processing.ai.BackgroundRemoverAi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.Dialog;

import com.example.app_xhinh_anh.databinding.ActivityEditorBinding;
import com.example.app_xhinh_anh.processing.ImageProcessor;

public class EditorActivity extends AppCompatActivity {

    private static final String TAG = "EditorActivity";

    private ActivityEditorBinding binding;
    private PhotoEditor photoEditor;
    private Uri currentImageUri;
    private final Stack<Bitmap> undoBitmapStack = new Stack<>();
    private final Stack<Bitmap> redoBitmapStack = new Stack<>();

    private Bitmap adjustBaseBitmap;
    private Bitmap adjustBasePreview;
    private Bitmap adjustConvBitmap;
    private static final int PREVIEW_MAX_DIM = 720;

    private static final int MODE_BRIGHTNESS = 0;
    private static final int MODE_CONTRAST = 1;
    private static final int MODE_SATURATION = 2;
    private static final int MODE_SHARPNESS = 3;
    private static final int MODE_CLARITY = 4;
    private static final int MODE_HSL = 5;
    private static final int MODE_CURVES = 6;
    private static final int MODE_HIGHLIGHTS = 7;
    private static final int MODE_SHADOWS = 8;
    private int currentAdjustMode = MODE_BRIGHTNESS;
    private int brightnessValue = 0;
    private int contrastValue = 0;
    private int saturationValue = 0;
    private int sharpnessValue = 0;
    private int clarityValue = 0;
    private int hslValue = 0;
    private int curvesValue = 0;
    private int highlightsValue = 0;
    private int shadowsValue = 0;

    private BackgroundRemoverAi backgroundRemoverAi;
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor();

    private TextView selectedCategoryTabView;
    private FilterPreset selectedVariant;
    private View selectedVariantView;
    private int filterIntensity = 100;
    private Bitmap filterBaseBitmap;
    private Bitmap filterThumbBitmap;

    // AI Assistant
    private ChatAdapter chatAdapter;
    private GeminiApiClient geminiApiClient;
    private com.example.app_xhinh_anh.features.ai_assistant.domain.AiActionExecutor aiActionExecutor;

    // Managers
    private BrushManager brushManager;
    private TextManager textManager;
    private StickerManager stickerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        photoEditor = new PhotoEditor.Builder(this, binding.photoEditorView)
                .setPinchTextScalable(true)
                .setClipSourceImage(true)
                .build();
        setupPhotoEditorListener();

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        initViews();

        binding.etChatInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && chatAdapter != null && chatAdapter.getItemCount() > 0) {
                binding.rvChatHistory.postDelayed(() -> {
                    binding.rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }, 300);
            }
        });

        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            currentImageUri = Uri.parse(imageUriString);
            loadImage(currentImageUri);
        }
    }

    private void loadImage(Uri uri) {
        Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        binding.photoEditorView.getSource().setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Toast.makeText(EditorActivity.this, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void setupAdjustControls() {
        binding.btnAdjust.setOnClickListener(v -> {
            if (binding.adjustPanel.getVisibility() == View.VISIBLE) {
                closeAdjustPanel(false);
                return;
            }
            hideAllPanels();
            if (!(binding.photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
                Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
                return;
            }
            
            saveBitmapState();
            photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    adjustBaseBitmap = ImageProcessor.copyBitmap(bitmap);
                    adjustBasePreview = ImageProcessor.makePreviewBitmap(adjustBaseBitmap, PREVIEW_MAX_DIM);
                    adjustConvBitmap = adjustBasePreview;
                    resetAdjustValues();
                    
                    photoEditor.clearAllViews();
                    binding.photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
                    binding.photoEditorView.getSource().clearColorFilter();
                    selectAdjustMode(MODE_BRIGHTNESS);
                    binding.adjustPanel.setVisibility(View.VISIBLE);
                }
                @Override public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.tabBrightness.setOnClickListener(v -> selectAdjustMode(MODE_BRIGHTNESS));
        binding.tabContrast.setOnClickListener(v -> selectAdjustMode(MODE_CONTRAST));
        binding.tabSaturation.setOnClickListener(v -> selectAdjustMode(MODE_SATURATION));
        binding.tabSharpness.setOnClickListener(v -> selectAdjustMode(MODE_SHARPNESS));
        binding.tabClarity.setOnClickListener(v -> selectAdjustMode(MODE_CLARITY));
        binding.tabHsl.setOnClickListener(v -> selectAdjustMode(MODE_HSL));
        binding.tabCurves.setOnClickListener(v -> selectAdjustMode(MODE_CURVES));
        binding.tabHighlights.setOnClickListener(v -> selectAdjustMode(MODE_HIGHLIGHTS));
        binding.tabShadows.setOnClickListener(v -> selectAdjustMode(MODE_SHADOWS));

        binding.seekAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int value = progress - 50;
                binding.adjustValueText.setText(String.valueOf(value));
                if (adjustBaseBitmap == null) return;
                switch (currentAdjustMode) {
                    case MODE_CONTRAST: contrastValue = value; break;
                    case MODE_SATURATION: saturationValue = value; break;
                    case MODE_SHARPNESS: sharpnessValue = value; break;
                    case MODE_CLARITY: clarityValue = value; break;
                    case MODE_HSL: hslValue = value; break;
                    case MODE_CURVES: curvesValue = value; break;
                    case MODE_HIGHLIGHTS: highlightsValue = value; break;
                    case MODE_SHADOWS: shadowsValue = value; break;
                    default: brightnessValue = value;
                }
                applyColorAdjustments();
                if (isHeavyMode(currentAdjustMode)) {
                    rebuildConvolutionBitmap();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        binding.btnAdjustReset.setOnClickListener(v -> {
            resetAdjustValues();
            binding.seekAdjust.setProgress(50);
            binding.adjustValueText.setText("0");
            if (adjustBasePreview != null) {
                adjustConvBitmap = adjustBasePreview;
                binding.photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
                binding.photoEditorView.getSource().clearColorFilter();
            }
        });

        binding.btnAdjustApply.setOnClickListener(v -> {
            bakeAdjustments();
            closeAdjustPanel(true);
        });
    }

    private void closeAdjustPanel(boolean isApplied) {
        binding.adjustPanel.setVisibility(View.GONE);
        binding.photoEditorView.getSource().clearColorFilter();
        if (!isApplied && adjustBaseBitmap != null) {
            binding.photoEditorView.getSource().setImageBitmap(adjustBaseBitmap);
        }
        adjustBaseBitmap = null;
        adjustBasePreview = null;
        adjustConvBitmap = null;
    }

    private boolean isHeavyMode(int mode) {
        return mode == MODE_SHARPNESS || mode == MODE_CLARITY
                || mode == MODE_CURVES || mode == MODE_HIGHLIGHTS || mode == MODE_SHADOWS;
    }

    private void selectAdjustMode(int mode) {
        currentAdjustMode = mode;
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);

        binding.iconBrightness.setColorFilter(mode == MODE_BRIGHTNESS ? active : inactive);
        binding.labelBrightness.setTextColor(mode == MODE_BRIGHTNESS ? active : inactive);
        binding.iconContrast.setColorFilter(mode == MODE_CONTRAST ? active : inactive);
        binding.labelContrast.setTextColor(mode == MODE_CONTRAST ? active : inactive);
        binding.iconSaturation.setColorFilter(mode == MODE_SATURATION ? active : inactive);
        binding.labelSaturation.setTextColor(mode == MODE_SATURATION ? active : inactive);
        binding.iconSharpness.setColorFilter(mode == MODE_SHARPNESS ? active : inactive);
        binding.labelSharpness.setTextColor(mode == MODE_SHARPNESS ? active : inactive);
        binding.iconClarity.setColorFilter(mode == MODE_CLARITY ? active : inactive);
        binding.labelClarity.setTextColor(mode == MODE_CLARITY ? active : inactive);
        binding.iconHsl.setColorFilter(mode == MODE_HSL ? active : inactive);
        binding.labelHsl.setTextColor(mode == MODE_HSL ? active : inactive);
        binding.iconCurves.setColorFilter(mode == MODE_CURVES ? active : inactive);
        binding.labelCurves.setTextColor(mode == MODE_CURVES ? active : inactive);
        binding.iconHighlights.setColorFilter(mode == MODE_HIGHLIGHTS ? active : inactive);
        binding.labelHighlights.setTextColor(mode == MODE_HIGHLIGHTS ? active : inactive);
        binding.iconShadows.setColorFilter(mode == MODE_SHADOWS ? active : inactive);
        binding.labelShadows.setTextColor(mode == MODE_SHADOWS ? active : inactive);

        int value;
        switch (mode) {
            case MODE_CONTRAST: value = contrastValue; break;
            case MODE_SATURATION: value = saturationValue; break;
            case MODE_SHARPNESS: value = sharpnessValue; break;
            case MODE_CLARITY: value = clarityValue; break;
            case MODE_HSL: value = hslValue; break;
            case MODE_CURVES: value = curvesValue; break;
            case MODE_HIGHLIGHTS: value = highlightsValue; break;
            case MODE_SHADOWS: value = shadowsValue; break;
            default: value = brightnessValue;
        }
        binding.seekAdjust.setProgress(value + 50);
        binding.adjustValueText.setText(String.valueOf(value));
    }

    private void resetAdjustValues() {
        brightnessValue = 0;
        contrastValue = 0;
        saturationValue = 0;
        sharpnessValue = 0;
        clarityValue = 0;
        hslValue = 0;
        curvesValue = 0;
        highlightsValue = 0;
        shadowsValue = 0;
    }

    private void applyColorAdjustments() {
        if (adjustBaseBitmap == null) return;
        binding.photoEditorView.getSource().setColorFilter(
                new ColorMatrixColorFilter(buildColorMatrix()));
    }

    private ColorMatrix buildColorMatrix() {
        float brightness = brightnessValue * 2f;
        float contrast = 1f + contrastValue / 50f;
        float saturation = 1f + saturationValue / 50f;
        float hueDegrees = hslValue * 3.6f;

        ColorMatrix cm = new ColorMatrix();
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturation);
        cm.postConcat(saturationMatrix);

        if (hueDegrees != 0f) {
            cm.postConcat(ImageProcessor.buildHueMatrix(hueDegrees));
        }

        float translate = (-.5f * contrast + .5f) * 255f;
        cm.postConcat(new ColorMatrix(new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1f, 0
        }));

        cm.postConcat(new ColorMatrix(new float[]{
                1f, 0, 0, 0, brightness,
                0, 1f, 0, 0, brightness,
                0, 0, 1f, 0, brightness,
                0, 0, 0, 1f, 0
        }));

        return cm;
    }

    private void rebuildConvolutionBitmap() {
        if (adjustBasePreview == null) return;
        final Bitmap src = adjustBasePreview;
        
        processingExecutor.execute(() -> {
            final Bitmap result = applyHeavyPipeline(src);
            runOnUiThread(() -> {
                adjustConvBitmap = result;
                binding.photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
            });
        });
    }

    private Bitmap applyHeavyPipeline(Bitmap src) {
        Bitmap out = src;
        if (curvesValue != 0 || highlightsValue != 0 || shadowsValue != 0) {
            int[] lut = ImageProcessor.buildToneLut(
                    curvesValue / 50f,
                    highlightsValue / 50f,
                    shadowsValue / 50f);
            out = ImageProcessor.applyLut(out, lut);
        }
        if (sharpnessValue != 0) {
            out = ImageProcessor.applySharpness(out, sharpnessValue / 50f);
        }
        if (clarityValue != 0) {
            out = ImageProcessor.applyClarity(out, clarityValue / 50f);
        }
        return out;
    }

    private void bakeAdjustments() {
        if (adjustBaseBitmap == null) return;
        
        Dialog loadingDialog = showLoadingDialog("Đang áp dụng thay đổi...");
        
        processingExecutor.execute(() -> {
            Bitmap full = applyHeavyPipeline(adjustBaseBitmap);
            boolean hasColorMatrix = brightnessValue != 0 || contrastValue != 0
                    || saturationValue != 0 || hslValue != 0;
            Bitmap finalBitmap;
            if (hasColorMatrix) {
                finalBitmap = ImageProcessor.applyColorMatrix(full, buildColorMatrix());
            } else {
                finalBitmap = full;
            }
            
            runOnUiThread(() -> {
                binding.photoEditorView.getSource().clearColorFilter();
                binding.photoEditorView.getSource().setImageBitmap(finalBitmap);
                loadingDialog.dismiss();
            });
        });
    }

    private void setupFilterControls() {
        binding.filterIntensitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                filterIntensity = progress;
                binding.filterIntensityValueText.setText(String.valueOf(progress));
                applyFilterPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        binding.btnFilterReset.setOnClickListener(v -> clearVariantSelection());

        binding.btnFilterApply.setOnClickListener(v -> {
            bakeFilter();
            closeFilterPanel(true);
        });
    }

    private void openFilterPanel() {
        if (!(binding.photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
            return;
        }
        hideAllPanels();
        
        saveBitmapState();
        photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
            @Override
            public void onBitmapReady(@NonNull Bitmap bitmap) {
                filterBaseBitmap = ImageProcessor.copyBitmap(bitmap);
                int thumbDim = Math.round(128f * getResources().getDisplayMetrics().density);
                filterThumbBitmap = ImageProcessor.makePreviewBitmap(filterBaseBitmap, thumbDim);

                photoEditor.clearAllViews();
                binding.photoEditorView.getSource().setImageBitmap(filterBaseBitmap);

                populateCategoryTabs();
                selectCategory(FilterPreset.CATEGORIES[0], (TextView) binding.filterCategoryTabs.getChildAt(0));

                filterIntensity = 100;
                binding.filterIntensitySeek.setProgress(100);
                binding.filterIntensityValueText.setText("100");
                binding.photoEditorView.getSource().clearColorFilter();

                binding.filterPanel.setVisibility(View.VISIBLE);
            }
            @Override public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeFilterPanel(boolean isApplied) {
        binding.filterPanel.setVisibility(View.GONE);
        binding.photoEditorView.getSource().clearColorFilter();
        if (selectedVariantView != null) selectedVariantView.setBackground(null);
        selectedVariantView = null;
        selectedVariant = null;
        selectedCategoryTabView = null;
        if (!isApplied && filterBaseBitmap != null) {
            binding.photoEditorView.getSource().setImageBitmap(filterBaseBitmap);
        }
        filterBaseBitmap = null;
        filterThumbBitmap = null;
    }

    private void populateCategoryTabs() {
        binding.filterCategoryTabs.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FilterPreset.Category category : FilterPreset.CATEGORIES) {
            final FilterPreset.Category cat = category;
            TextView tab = (TextView) inflater.inflate(R.layout.item_filter_category_tab,
                    binding.filterCategoryTabs, false);
            tab.setText(cat.name);
            tab.setOnClickListener(v -> selectCategory(cat, tab));
            binding.filterCategoryTabs.addView(tab);
        }
    }

    private void selectCategory(FilterPreset.Category category, TextView tabView) {
        if (selectedCategoryTabView != null && selectedCategoryTabView != tabView) {
            selectedCategoryTabView.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        tabView.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        selectedCategoryTabView = tabView;
        clearVariantSelection();
        populateVariants(category);
    }

    private void populateVariants(FilterPreset.Category category) {
        binding.filterVariantsList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FilterPreset preset : category.variants) {
            final FilterPreset variant = preset;
            View item = inflater.inflate(R.layout.item_filter_thumb, binding.filterVariantsList, false);
            ImageView thumb = item.findViewById(R.id.filterThumb);
            TextView name = item.findViewById(R.id.filterName);
            if (variant.matrix != null) {
                thumb.setImageBitmap(ImageProcessor.applyColorMatrix(filterThumbBitmap, variant.matrix));
            } else {
                thumb.setImageBitmap(filterThumbBitmap);
            }
            name.setText(variant.displayName);
            item.setOnClickListener(v -> selectVariant(variant, item));
            binding.filterVariantsList.addView(item);
        }
    }

    private void selectVariant(FilterPreset variant, View itemView) {
        if (selectedVariantView != null && selectedVariantView != itemView) {
            selectedVariantView.setBackground(null);
        }
        if (itemView != null) {
            itemView.setBackgroundResource(R.drawable.bg_filter_thumb_selected);
        }
        selectedVariantView = itemView;
        selectedVariant = variant;
        applyFilterPreview();
    }

    private void clearVariantSelection() {
        if (selectedVariantView != null) selectedVariantView.setBackground(null);
        selectedVariantView = null;
        selectedVariant = null;
        filterIntensity = 100;
        binding.filterIntensitySeek.setProgress(100);
        binding.filterIntensityValueText.setText("100");
        binding.photoEditorView.getSource().clearColorFilter();
    }

    private void applyFilterPreview() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) {
            binding.photoEditorView.getSource().clearColorFilter();
            return;
        }
        float t = filterIntensity / 100f;
        binding.photoEditorView.getSource().setColorFilter(
                new ColorMatrixColorFilter(ImageProcessor.lerpToIdentity(selectedVariant.matrix, t)));
    }

    private void bakeFilter() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) return;
        float t = filterIntensity / 100f;
        Bitmap full = ImageProcessor.applyColorMatrix(filterBaseBitmap, ImageProcessor.lerpToIdentity(selectedVariant.matrix, t));
        binding.photoEditorView.getSource().clearColorFilter();
        binding.photoEditorView.getSource().setImageBitmap(full);
    }

    private void saveBitmapState() {
        if (binding.photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
            Bitmap current = ((BitmapDrawable) binding.photoEditorView.getSource().getDrawable()).getBitmap();
            if (current != null) {
                undoBitmapStack.push(ImageProcessor.copyBitmap(current));
                redoBitmapStack.clear();
            }
        }
    }

    private void setupPhotoEditorListener() {
        photoEditor.setOnPhotoEditorListener(new OnPhotoEditorListener() {
            @Override
            public void onEditTextChangeListener(@Nullable View view, @Nullable String text, int colorCode) {
                if (view != null && textManager != null) {
                    textManager.openTextStylingPanel(view);
                }
            }
            @Override public void onAddViewListener(@Nullable ViewType viewType, int i) {}
            @Override public void onRemoveViewListener(@Nullable ViewType viewType, int i) {}
            @Override public void onStartViewChangeListener(@Nullable ViewType viewType) {}
            @Override public void onStopViewChangeListener(@Nullable ViewType viewType) {}
            @Override public void onTouchSourceImage(@Nullable MotionEvent motionEvent) {
                hideAllPanels();
            }
        });
    }

    private void initViews() {
        // Managers
        brushManager = new BrushManager(this, binding, photoEditor);
        textManager = new TextManager(this, binding, photoEditor);
        stickerManager = new StickerManager(this, binding, photoEditor);

        setupAdjustControls();
        setupFilterControls();

        // AI Assistant
        setupAiAssistant();

        binding.btnCrop.setOnClickListener(v -> {
            File tempFile = new File(getCacheDir(), "crop_source_" + System.currentTimeMillis() + ".jpg");

            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();

            photoEditor.saveAsFile(tempFile.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    startCrop(Uri.fromFile(new File(imagePath)));
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(EditorActivity.this, "Không thể chuẩn bị ảnh để cắt", Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        binding.btnFlipHorizontal.setOnClickListener(v -> flipImage(-1, 1));
        binding.btnFlipVertical.setOnClickListener(v -> flipImage(1, -1));

        binding.btnUndo.setOnClickListener(v -> {
            if (!undoBitmapStack.isEmpty()) {
                Bitmap current = ((BitmapDrawable) binding.photoEditorView.getSource().getDrawable()).getBitmap();
                if (current != null) {
                    redoBitmapStack.push(ImageProcessor.copyBitmap(current));
                }
                binding.photoEditorView.getSource().setImageBitmap(undoBitmapStack.pop());

            } else {
                photoEditor.undo();
            }
        });

        binding.btnRedo.setOnClickListener(v -> {
            if (!redoBitmapStack.isEmpty()) {
                Bitmap current = ((BitmapDrawable) binding.photoEditorView.getSource().getDrawable()).getBitmap();
                if (current != null) undoBitmapStack.push(ImageProcessor.copyBitmap(current));
                binding.photoEditorView.getSource().setImageBitmap(redoBitmapStack.pop());
            } else {
                photoEditor.redo();
            }
        });

        binding.btnFilter.setOnClickListener(v -> openFilterPanel());
        
        binding.btnBrush.setOnClickListener(v -> {
            if (brushManager.isPanelVisible()) {
                brushManager.closeBrushPanel();
            } else {
                hideAllPanels();
                brushManager.openBrushPanel();
            }
        });

        binding.btnAddText.setOnClickListener(v -> {
            hideAllPanels();
            if (textManager != null) {
                textManager.addDefaultText();
            }
        });

        backgroundRemoverAi = new BackgroundRemoverAi();
        binding.btnAiRmBg.setOnClickListener(v -> performAiBackgroundRemoval());

        binding.btnSticker.setOnClickListener(v -> {
            if (stickerManager.isPanelVisible()) {
                stickerManager.closeStickerPanel();
            } else {
                hideAllPanels();
                stickerManager.openStickerPanel();
            }
        });

        binding.btnSave.setOnClickListener(v -> saveProcessedImage());
    }

    private void flipImage(float sx, float sy) {
        if (!(binding.photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) return;
        saveBitmapState();
        Bitmap src = ((BitmapDrawable) binding.photoEditorView.getSource().getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy, src.getWidth() / 2f, src.getHeight() / 2f);
        Bitmap flipped = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        photoEditor.clearAllViews();
        binding.photoEditorView.getSource().setImageBitmap(flipped);
    }

    private void hideAllPanels() {
        if (binding.adjustPanel.getVisibility() == View.VISIBLE) closeAdjustPanel(false);
        if (binding.filterPanel.getVisibility() == View.VISIBLE) closeFilterPanel(false);
        // Không đóng chatPanel khi hideAllPanels để AI có thể làm việc liên tục
        if (brushManager != null && brushManager.isPanelVisible()) brushManager.closeBrushPanel();
        if (textManager != null && textManager.isPanelVisible()) textManager.hideStylingPanel();
        if (stickerManager != null && stickerManager.isPanelVisible()) stickerManager.closeStickerPanel();
    }

    private void performAiBackgroundRemoval() {
        if (!(binding.photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Không có ảnh để xử lý", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap currentBitmap = ((BitmapDrawable) binding.photoEditorView.getSource().getDrawable()).getBitmap();
        if (currentBitmap == null) return;

        Dialog loadingDialog = showLoadingDialog("Đang xóa nền AI...");

        backgroundRemoverAi.process(currentBitmap, new AiProcessor.AiCallback() {
            @Override
            public void onSuccess(Bitmap result) {
                loadingDialog.dismiss();
                photoEditor.clearAllViews();
                saveBitmapState();
                binding.photoEditorView.getSource().setImageBitmap(result);
                Toast.makeText(EditorActivity.this, "Đã xóa nền!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                loadingDialog.dismiss();
                Toast.makeText(EditorActivity.this, "Lỗi AI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "AI Error: ", e);
            }
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
            Log.e(TAG, "Lỗi quyền khi lưu ảnh", e);
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
                if (in == null || out == null) {
                    Toast.makeText(this, "Không mở được luồng ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
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
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCrop(Uri uri) {
        if (uri == null) return;
        String destinationFileName = "CroppedImage_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        
        UCrop.Options options = new UCrop.Options();
        int brandColor = ContextCompat.getColor(this, R.color.brand_green);
        int whiteColor = ContextCompat.getColor(this, R.color.white);

        options.setToolbarColor(brandColor);
        options.setToolbarWidgetColor(whiteColor);
        options.setActiveControlsWidgetColor(brandColor);
        options.setToolbarTitle("Cắt ảnh");
        options.setHideBottomControls(false);
        options.setAspectRatioOptions(0,
                new com.yalantis.ucrop.model.AspectRatio("Gốc", 0f, 0f),
                new com.yalantis.ucrop.model.AspectRatio("1:1", 1f, 1f),
                new com.yalantis.ucrop.model.AspectRatio("4:3", 4f, 3f),
                new com.yalantis.ucrop.model.AspectRatio("3:4", 3f, 4f),
                new com.yalantis.ucrop.model.AspectRatio("16:9", 16f, 9f),
                new com.yalantis.ucrop.model.AspectRatio("9:16", 9f, 16f)
        );
        options.setFreeStyleCropEnabled(true);
        options.setAllowedGestures(UCropActivity.ALL, UCropActivity.ALL, UCropActivity.ALL);
        options.setMaxScaleMultiplier(10f);

        uCrop.withOptions(options);
        uCrop.start(this);
    }

    private void setupAiAssistant() {
        geminiApiClient = new GeminiApiClient(com.example.app_xhinh_anh.BuildConfig.GEMINI_API_KEY);
        
        // Khởi tạo AiActionExecutor
        aiActionExecutor = new com.example.app_xhinh_anh.features.ai_assistant.domain.AiActionExecutor(new com.example.app_xhinh_anh.features.ai_assistant.domain.AiActionExecutor.EditorActions() {
            @Override
            public void applyFilter(String filterName) {
                applyAiFilter(filterName);
            }

            @Override
            public void adjustProperty(String property, int value) {
                applyAiAdjustment(property, value);
            }

            @Override
            public void openTool(String toolName) {
                openAiTool(toolName);
            }

            @Override
            public void removeBackground() {
                runOnUiThread(() -> binding.btnAiRmBg.performClick());
            }

            @Override
            public void addChatMessage(String message, boolean isUser) {
                chatAdapter.addMessage(new ChatMessage(message, isUser));
                binding.rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void showThinking(boolean show) {
                binding.pbAiThinking.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        chatAdapter = new ChatAdapter();
        binding.rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatHistory.setAdapter(chatAdapter);

        // Tin nhắn hướng dẫn chi tiết ban đầu
        String guideMessage = "Chào bạn! Tôi là Trợ lý AI. Tôi có thể giúp bạn:\n\n" +
                "🎨 **Bộ lọc**: \"Áp dụng bộ lọc Polaroid\", \"Làm ảnh cũ đi\", \"Chỉnh màu Neon\"...\n\n" +
                "🛠️ **Công cụ**: \"Xóa nền ảnh này\", \"Mở đồ thị Curves\", \"Chỉnh HSL\"...\n\n" +
                "✨ **Căn chỉnh**: \"Tăng độ sáng lên 20%\", \"Làm ảnh sắc nét hơn\", \"Giảm tương phản\"...\n\n" +
                "Bạn muốn tôi giúp gì cho bức ảnh này?";
        chatAdapter.addMessage(new ChatMessage(guideMessage, false));

        binding.btnAiAssistant.setOnClickListener(v -> {
            if (binding.chatPanel.getVisibility() == View.VISIBLE) {
                binding.chatPanel.setVisibility(View.GONE);
            } else {
                hideAllPanels();
                binding.chatPanel.setVisibility(View.VISIBLE);
                if (chatAdapter.getItemCount() <= 1) {
                    binding.rvChatHistory.smoothScrollToPosition(0);
                }
            }
        });

        binding.btnSendChat.setOnClickListener(v -> {
            String msg = binding.etChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendChatMessage(msg);
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etChatInput.getWindowToken(), 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 1001) {
                String action = data.getStringExtra("action");
                if ("APPLY_FILTER".equals(action)) {
                    String filterName = data.getStringExtra("filter_name");
                    applyAiFilter(filterName);
                } else if ("ADJUST".equals(action)) {
                    String property = data.getStringExtra("property");
                    int value = data.getIntExtra("value", 0);
                    applyAiAdjustment(property, value);
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    saveBitmapState();
                    currentImageUri = resultUri;
                    photoEditor.clearAllViews();
                    binding.photoEditorView.getSource().setImageURI(currentImageUri);
                }
            }
        }
    }

    private void sendChatMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, true));
        binding.etChatInput.setText("");
        binding.rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        binding.pbAiThinking.setVisibility(View.VISIBLE);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                aiActionExecutor.executeResponse(response);
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    binding.pbAiThinking.setVisibility(View.GONE);
                    chatAdapter.addMessage(new ChatMessage("Lỗi: " + t.getMessage(), false));
                    binding.rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }
        });
    }

    private void applyAiFilter(String filterName) {
        FilterPreset targetVariant = null;
        for (FilterPreset.Category category : FilterPreset.CATEGORIES) {
            for (FilterPreset variant : category.variants) {
                if (variant.displayName.equalsIgnoreCase(filterName)) {
                    targetVariant = variant;
                    break;
                }
            }
            if (targetVariant != null) break;
        }

        if (targetVariant == null) {
            Toast.makeText(this, "Không tìm thấy bộ lọc: " + filterName, Toast.LENGTH_SHORT).show();
            return;
        }

        final FilterPreset variant = targetVariant;
        runOnUiThread(() -> {
            if (binding.filterPanel.getVisibility() == View.VISIBLE) {
                selectVariant(variant, null);
                Toast.makeText(this, "AI: Đã chọn bộ lọc " + filterName, Toast.LENGTH_SHORT).show();
                return;
            }

            photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    saveBitmapState();
                    Bitmap result;
                    if (variant.matrix != null) {
                        result = ImageProcessor.applyColorMatrix(bitmap, variant.matrix);
                    } else {
                        result = ImageProcessor.copyBitmap(bitmap);
                    }
                    
                    photoEditor.clearAllViews();
                    binding.photoEditorView.getSource().setImageBitmap(result);
                    binding.photoEditorView.getSource().clearColorFilter();
                    Toast.makeText(EditorActivity.this, "AI: Đã áp dụng bộ lọc " + filterName, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditorActivity.this, "Lỗi khi áp dụng bộ lọc AI", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void applyAiAdjustment(String property, int value) {
        if (property == null) return;
        
        runOnUiThread(() -> {
            String prop = property.toLowerCase();
            int internalValue = Math.max(-50, Math.min(50, value));

            if (binding.adjustPanel.getVisibility() == View.VISIBLE) {
                switch (prop) {
                    case "brightness": brightnessValue = internalValue; selectAdjustMode(MODE_BRIGHTNESS); break;
                    case "contrast": contrastValue = internalValue; selectAdjustMode(MODE_CONTRAST); break;
                    case "saturation": saturationValue = internalValue; selectAdjustMode(MODE_SATURATION); break;
                    case "sharpness": sharpnessValue = internalValue; selectAdjustMode(MODE_SHARPNESS); break;
                    case "clarity": clarityValue = internalValue; selectAdjustMode(MODE_CLARITY); break;
                    case "hsl": hslValue = internalValue; selectAdjustMode(MODE_HSL); break;
                    case "highlights": highlightsValue = internalValue; selectAdjustMode(MODE_HIGHLIGHTS); break;
                    case "shadows": shadowsValue = internalValue; selectAdjustMode(MODE_SHADOWS); break;
                }
                applyColorAdjustments();
                if (isHeavyMode(currentAdjustMode)) {
                    rebuildConvolutionBitmap();
                }
                Toast.makeText(this, "AI: Đã chỉnh " + property + " thành " + internalValue, Toast.LENGTH_SHORT).show();
                return;
            }

            saveBitmapState();
            photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    float val = (float) internalValue;
                    ColorMatrix cm = new ColorMatrix();
                    Bitmap result = bitmap;

                    // Xử lý các thông số màu sắc (ColorMatrix)
                    if (prop.equals("brightness")) {
                        float b = val * 2f;
                        cm.postConcat(new ColorMatrix(new float[]{
                                1, 0, 0, 0, b,
                                0, 1, 0, 0, b,
                                0, 0, 1, 0, b,
                                0, 0, 0, 1, 0
                        }));
                    } else if (prop.equals("contrast")) {
                        float c = 1f + val / 50f;
                        float t = (-.5f * c + .5f) * 255f;
                        cm.postConcat(new ColorMatrix(new float[]{
                                c, 0, 0, 0, t,
                                0, c, 0, 0, t,
                                0, 0, c, 0, t,
                                0, 0, 0, 1, 0
                        }));
                    } else if (prop.equals("saturation")) {
                        cm.setSaturation(1f + val / 50f);
                    } else if (prop.equals("hsl")) {
                        cm.postConcat(ImageProcessor.buildHueMatrix(val * 3.6f));
                    }
                    
                    result = ImageProcessor.applyColorMatrix(result, cm);

                    // Xử lý các hiệu ứng nâng cao (Convolution/LUT)
                    if (prop.equals("sharpness")) {
                        result = ImageProcessor.applySharpness(result, val / 50f);
                    } else if (prop.equals("clarity")) {
                        result = ImageProcessor.applyClarity(result, val / 50f);
                    } else if (prop.equals("highlights") || prop.equals("shadows")) {
                        float h = prop.equals("highlights") ? val / 50f : 0;
                        float s = prop.equals("shadows") ? val / 50f : 0;
                        int[] lut = ImageProcessor.buildToneLut(0, h, s);
                        result = ImageProcessor.applyLut(result, lut);
                    }

                    photoEditor.clearAllViews();
                    binding.photoEditorView.getSource().setImageBitmap(result);
                    binding.photoEditorView.getSource().clearColorFilter();
                    Toast.makeText(EditorActivity.this, "AI: Đã áp dụng " + property, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditorActivity.this, "Lỗi khi thực hiện chỉnh sửa AI", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void openAiTool(String toolName) {
        runOnUiThread(() -> {
            // Khi mở một công cụ cụ thể, ta sẽ đóng chat để người dùng thao tác
            if (toolName.equalsIgnoreCase("curves") || toolName.equalsIgnoreCase("hsl") || 
                toolName.equalsIgnoreCase("adjust") || toolName.equalsIgnoreCase("filter") ||
                toolName.equalsIgnoreCase("bg_removal")) {
                
                binding.chatPanel.setVisibility(View.GONE);
                
                if (toolName.equalsIgnoreCase("filter")) {
                    openFilterPanel();
                } else if (toolName.equalsIgnoreCase("bg_removal")) {
                    performAiBackgroundRemoval();
                } else {
                    if (binding.adjustPanel.getVisibility() != View.VISIBLE) {
                        binding.btnAdjust.performClick();
                    }
                    if (toolName.equalsIgnoreCase("curves")) selectAdjustMode(MODE_CURVES);
                    else if (toolName.equalsIgnoreCase("hsl")) selectAdjustMode(MODE_HSL);
                }
            }
        });
    }

    private Dialog showLoadingDialog(String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_loading_dialog, null);
        TextView tvMessage = view.findViewById(R.id.tvLoadingMessage);
        tvMessage.setText(message);
        
        Dialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();
        dialog.show();
        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        processingExecutor.shutdown();
    }
}
