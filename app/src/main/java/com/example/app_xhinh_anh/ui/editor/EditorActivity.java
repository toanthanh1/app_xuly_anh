package com.example.app_xhinh_anh.ui.editor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_xhinh_anh.R;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;
import ja.burhanrashid52.photoeditor.shape.ArrowPointerLocation;
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder;
import ja.burhanrashid52.photoeditor.shape.ShapeType;

public class EditorActivity extends AppCompatActivity {

    private static final String TAG = "EditorActivity";

    private PhotoEditorView photoEditorView;
    private PhotoEditor photoEditor;
    private Uri currentImageUri;
    private ImageButton btnUndo, btnRedo;
    private final Stack<Bitmap> undoBitmapStack = new Stack<>();
    private final Stack<Bitmap> redoBitmapStack = new Stack<>();

    private LinearLayout adjustPanel;
    private SeekBar seekAdjust;
    private TextView adjustValueText;
    private LinearLayout tabBrightness, tabContrast, tabSaturation, tabSharpness, tabClarity, tabHsl,
            tabCurves, tabHighlights, tabShadows;
    private ImageView iconBrightness, iconContrast, iconSaturation, iconSharpness, iconClarity, iconHsl,
            iconCurves, iconHighlights, iconShadows;
    private TextView labelBrightness, labelContrast, labelSaturation, labelSharpness, labelClarity, labelHsl,
            labelCurves, labelHighlights, labelShadows;
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

    private LinearLayout filterPanel;
    private LinearLayout filterCategoryTabs;
    private LinearLayout filterVariantsList;
    private TextView filterIntensityValueText;
    private SeekBar filterIntensitySeek;
    private Bitmap filterBaseBitmap;
    private Bitmap filterThumbBitmap;
    private TextView selectedCategoryTabView;
    private FilterPreset selectedVariant;
    private View selectedVariantView;
    private int filterIntensity = 100;

    // Brush & Drawing
    private LinearLayout brushPanel;
    private SeekBar seekBrushWidth;
    private View btnChooseColor;
    private ImageButton btnBrushEraser, btnBrushFree, btnBrushArrow, btnBrushLine, btnBrushRect, btnBrushOval;
    private LinearLayout colorPickerPanel;
    private GridLayout colorGrid;
    private LinearLayout commonColorsLayout;
    private int currentBrushColor = Color.WHITE;
    private ShapeType currentShapeType = ShapeType.Brush.INSTANCE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        photoEditorView = findViewById(R.id.photoEditorView);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);

        photoEditor = new PhotoEditor.Builder(this, photoEditorView)
                .setPinchTextScalable(true)
                .setClipSourceImage(true)
                .build();
        setupPhotoEditorListener();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            currentImageUri = Uri.parse(imageUriString);
            photoEditorView.getSource().setImageURI(currentImageUri);
        }
    }

    private void setupAdjustControls(View btnAdjust, Button btnAdjustReset, Button btnAdjustApply) {
        btnAdjust.setOnClickListener(v -> {
            if (adjustPanel.getVisibility() == View.VISIBLE) {
                closeAdjustPanel(false);
                return;
            }
            hideAllPanels();
            if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
                Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Bake current layers before starting adjustment
            saveBitmapState();
            photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    adjustBaseBitmap = copyBitmap(bitmap);
                    adjustBasePreview = makePreviewBitmap(adjustBaseBitmap, PREVIEW_MAX_DIM);
                    adjustConvBitmap = adjustBasePreview;
                    resetAdjustValues();
                    
                    photoEditor.clearAllViews();
                    photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
                    photoEditorView.getSource().clearColorFilter();
                    selectAdjustMode(MODE_BRIGHTNESS);
                    adjustPanel.setVisibility(View.VISIBLE);
                }
                @Override public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        });

        tabBrightness.setOnClickListener(v -> selectAdjustMode(MODE_BRIGHTNESS));
        tabContrast.setOnClickListener(v -> selectAdjustMode(MODE_CONTRAST));
        tabSaturation.setOnClickListener(v -> selectAdjustMode(MODE_SATURATION));
        tabSharpness.setOnClickListener(v -> selectAdjustMode(MODE_SHARPNESS));
        tabClarity.setOnClickListener(v -> selectAdjustMode(MODE_CLARITY));
        tabHsl.setOnClickListener(v -> selectAdjustMode(MODE_HSL));
        tabCurves.setOnClickListener(v -> selectAdjustMode(MODE_CURVES));
        tabHighlights.setOnClickListener(v -> selectAdjustMode(MODE_HIGHLIGHTS));
        tabShadows.setOnClickListener(v -> selectAdjustMode(MODE_SHADOWS));

        seekAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int value = progress - 50;
                adjustValueText.setText(String.valueOf(value));
                if (!fromUser || adjustBaseBitmap == null) return;
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

        btnAdjustReset.setOnClickListener(v -> {
            resetAdjustValues();
            seekAdjust.setProgress(50);
            adjustValueText.setText("0");
            if (adjustBasePreview != null) {
                adjustConvBitmap = adjustBasePreview;
                photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
                photoEditorView.getSource().clearColorFilter();
            }
        });

        btnAdjustApply.setOnClickListener(v -> {
            bakeAdjustments();
            closeAdjustPanel(true);
        });
    }

    private void closeAdjustPanel(boolean isApplied) {
        adjustPanel.setVisibility(View.GONE);
        photoEditorView.getSource().clearColorFilter();
        if (!isApplied && adjustBaseBitmap != null) {
            photoEditorView.getSource().setImageBitmap(adjustBaseBitmap);
        }
        adjustBaseBitmap = null;
        adjustBasePreview = null;
        adjustConvBitmap = null;
    }

    private boolean isHeavyMode(int mode) {
        return mode == MODE_SHARPNESS || mode == MODE_CLARITY
                || mode == MODE_CURVES || mode == MODE_HIGHLIGHTS || mode == MODE_SHADOWS;
    }

    private Bitmap makePreviewBitmap(Bitmap src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        int largest = Math.max(w, h);
        if (largest <= maxDim) return src;
        float ratio = (float) maxDim / largest;
        return Bitmap.createScaledBitmap(src,
                Math.max(1, Math.round(w * ratio)),
                Math.max(1, Math.round(h * ratio)),
                true);
    }

    private void selectAdjustMode(int mode) {
        currentAdjustMode = mode;
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);

        iconBrightness.setColorFilter(mode == MODE_BRIGHTNESS ? active : inactive);
        labelBrightness.setTextColor(mode == MODE_BRIGHTNESS ? active : inactive);
        iconContrast.setColorFilter(mode == MODE_CONTRAST ? active : inactive);
        labelContrast.setTextColor(mode == MODE_CONTRAST ? active : inactive);
        iconSaturation.setColorFilter(mode == MODE_SATURATION ? active : inactive);
        labelSaturation.setTextColor(mode == MODE_SATURATION ? active : inactive);
        iconSharpness.setColorFilter(mode == MODE_SHARPNESS ? active : inactive);
        labelSharpness.setTextColor(mode == MODE_SHARPNESS ? active : inactive);
        iconClarity.setColorFilter(mode == MODE_CLARITY ? active : inactive);
        labelClarity.setTextColor(mode == MODE_CLARITY ? active : inactive);
        iconHsl.setColorFilter(mode == MODE_HSL ? active : inactive);
        labelHsl.setTextColor(mode == MODE_HSL ? active : inactive);
        iconCurves.setColorFilter(mode == MODE_CURVES ? active : inactive);
        labelCurves.setTextColor(mode == MODE_CURVES ? active : inactive);
        iconHighlights.setColorFilter(mode == MODE_HIGHLIGHTS ? active : inactive);
        labelHighlights.setTextColor(mode == MODE_HIGHLIGHTS ? active : inactive);
        iconShadows.setColorFilter(mode == MODE_SHADOWS ? active : inactive);
        labelShadows.setTextColor(mode == MODE_SHADOWS ? active : inactive);

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
        seekAdjust.setProgress(value + 50);
        adjustValueText.setText(String.valueOf(value));
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
        photoEditorView.getSource().setColorFilter(
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
            cm.postConcat(buildHueMatrix(hueDegrees));
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
        adjustConvBitmap = applyHeavyPipeline(adjustBasePreview);
        photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
    }

    private Bitmap applyHeavyPipeline(Bitmap src) {
        Bitmap out = src;
        if (curvesValue != 0 || highlightsValue != 0 || shadowsValue != 0) {
            int[] lut = buildToneLut(
                    curvesValue / 50f,
                    highlightsValue / 50f,
                    shadowsValue / 50f);
            out = applyLut(out, lut);
        }
        if (sharpnessValue != 0) {
            out = applySharpness(out, sharpnessValue / 50f);
        }
        if (clarityValue != 0) {
            out = applyClarity(out, clarityValue / 50f);
        }
        return out;
    }

    private int[] buildToneLut(float curves, float highlights, float shadows) {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            float x = i / 255f;
            float y = x;
            if (curves != 0f) {
                float ss = x * x * (3f - 2f * x);
                y = y + curves * (ss - x);
            }
            if (highlights != 0f) {
                float maskH = Math.max(0f, 2f * x - 1f);
                y = y + highlights * 0.4f * maskH;
            }
            if (shadows != 0f) {
                float maskS = Math.max(0f, 1f - 2f * x);
                y = y + shadows * 0.4f * maskS;
            }
            lut[i] = clampByte(Math.round(y * 255f));
        }
        return lut;
    }

    private Bitmap applyLut(Bitmap src, int[] lut) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >> 24) & 0xff;
            int r = lut[(p >> 16) & 0xff];
            int g = lut[(p >> 8) & 0xff];
            int b = lut[p & 0xff];
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    private void bakeAdjustments() {
        if (adjustBaseBitmap == null) return;
        Bitmap full = applyHeavyPipeline(adjustBaseBitmap);
        boolean hasColorMatrix = brightnessValue != 0 || contrastValue != 0
                || saturationValue != 0 || hslValue != 0;
        Bitmap finalBitmap;
        if (hasColorMatrix) {
            finalBitmap = Bitmap.createBitmap(full.getWidth(), full.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(buildColorMatrix()));
            canvas.drawBitmap(full, 0, 0, paint);
        } else {
            finalBitmap = full;
        }
        photoEditorView.getSource().clearColorFilter();
        photoEditorView.getSource().setImageBitmap(finalBitmap);
    }

    private void setupFilterControls(Button btnFilterReset, Button btnFilterApply) {
        filterIntensitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                filterIntensity = progress;
                filterIntensityValueText.setText(String.valueOf(progress));
                if (fromUser) applyFilterPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnFilterReset.setOnClickListener(v -> clearVariantSelection());

        btnFilterApply.setOnClickListener(v -> {
            bakeFilter();
            closeFilterPanel(true);
        });
    }

    private void openFilterPanel() {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
            return;
        }
        hideAllPanels();
        
        // Bake layers before starting filter
        saveBitmapState();
        photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
            @Override
            public void onBitmapReady(@NonNull Bitmap bitmap) {
                filterBaseBitmap = copyBitmap(bitmap);
                int thumbDim = Math.round(128f * getResources().getDisplayMetrics().density);
                filterThumbBitmap = makePreviewBitmap(filterBaseBitmap, thumbDim);

                photoEditor.clearAllViews();
                photoEditorView.getSource().setImageBitmap(filterBaseBitmap);

                populateCategoryTabs();
                selectCategory(FilterPreset.CATEGORIES[0], (TextView) filterCategoryTabs.getChildAt(0));

                filterIntensity = 100;
                filterIntensitySeek.setProgress(100);
                filterIntensityValueText.setText("100");
                photoEditorView.getSource().clearColorFilter();

                filterPanel.setVisibility(View.VISIBLE);
            }
            @Override public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeFilterPanel(boolean isApplied) {
        filterPanel.setVisibility(View.GONE);
        photoEditorView.getSource().clearColorFilter();
        if (selectedVariantView != null) selectedVariantView.setBackground(null);
        selectedVariantView = null;
        selectedVariant = null;
        selectedCategoryTabView = null;
        if (!isApplied && filterBaseBitmap != null) {
            photoEditorView.getSource().setImageBitmap(filterBaseBitmap);
        }
        filterBaseBitmap = null;
        filterThumbBitmap = null;
    }

    private void populateCategoryTabs() {
        filterCategoryTabs.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FilterPreset.Category category : FilterPreset.CATEGORIES) {
            final FilterPreset.Category cat = category;
            TextView tab = (TextView) inflater.inflate(R.layout.item_filter_category_tab,
                    filterCategoryTabs, false);
            tab.setText(cat.name);
            tab.setOnClickListener(v -> selectCategory(cat, tab));
            filterCategoryTabs.addView(tab);
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
        filterVariantsList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (FilterPreset preset : category.variants) {
            final FilterPreset variant = preset;
            View item = inflater.inflate(R.layout.item_filter_thumb, filterVariantsList, false);
            ImageView thumb = item.findViewById(R.id.filterThumb);
            TextView name = item.findViewById(R.id.filterName);
            if (variant.matrix != null) {
                thumb.setImageBitmap(applyMatrixToBitmap(filterThumbBitmap, variant.matrix));
            } else {
                thumb.setImageBitmap(filterThumbBitmap);
            }
            name.setText(variant.displayName);
            item.setOnClickListener(v -> selectVariant(variant, item));
            filterVariantsList.addView(item);
        }
    }

    private void selectVariant(FilterPreset variant, View itemView) {
        if (selectedVariantView != null && selectedVariantView != itemView) {
            selectedVariantView.setBackground(null);
        }
        itemView.setBackgroundResource(R.drawable.bg_filter_thumb_selected);
        selectedVariantView = itemView;
        selectedVariant = variant;
        applyFilterPreview();
    }

    private void clearVariantSelection() {
        if (selectedVariantView != null) selectedVariantView.setBackground(null);
        selectedVariantView = null;
        selectedVariant = null;
        filterIntensity = 100;
        filterIntensitySeek.setProgress(100);
        filterIntensityValueText.setText("100");
        photoEditorView.getSource().clearColorFilter();
    }

    private void applyFilterPreview() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) {
            photoEditorView.getSource().clearColorFilter();
            return;
        }
        float t = filterIntensity / 100f;
        photoEditorView.getSource().setColorFilter(
                new ColorMatrixColorFilter(lerpToIdentity(selectedVariant.matrix, t)));
    }

    private void bakeFilter() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) return;
        float t = filterIntensity / 100f;
        Bitmap full = applyMatrixToBitmap(filterBaseBitmap, lerpToIdentity(selectedVariant.matrix, t));
        photoEditorView.getSource().clearColorFilter();
        photoEditorView.getSource().setImageBitmap(full);
    }

    private float[] lerpToIdentity(float[] target, float t) {
        float[] identity = {
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
        };
        float[] r = new float[20];
        for (int i = 0; i < 20; i++) {
            r[i] = identity[i] + t * (target[i] - identity[i]);
        }
        return r;
    }

    private Bitmap applyMatrixToBitmap(Bitmap src, float[] matrix) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    private ColorMatrix buildHueMatrix(float degrees) {
        float r = (float) Math.toRadians(degrees);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        float lr = 0.213f, lg = 0.715f, lb = 0.072f;
        return new ColorMatrix(new float[]{
                lr + c * (1 - lr) + s * (-lr),       lg + c * (-lg) + s * (-lg),       lb + c * (-lb) + s * (1 - lb), 0, 0,
                lr + c * (-lr) + s * (0.143f),       lg + c * (1 - lg) + s * (0.140f), lb + c * (-lb) + s * (-0.283f), 0, 0,
                lr + c * (-lr) + s * (-(1 - lr)),    lg + c * (-lg) + s * (lg),        lb + c * (1 - lb) + s * (lb),  0, 0,
                0, 0, 0, 1, 0
        });
    }

    private Bitmap applySharpness(Bitmap src, float strength) {
        if (Math.abs(strength) < 0.01f) return src;
        float[] kernel;
        if (strength > 0) {
            float s = strength;
            kernel = new float[]{
                    0,    -s,        0,
                    -s,   1 + 4 * s, -s,
                    0,    -s,        0
            };
        } else {
            float w = -strength;
            float side = w / 9f;
            kernel = new float[]{
                    side, side,           side,
                    side, 1f - 8 * side,  side,
                    side, side,           side
            };
        }
        return applyConvolution(src, kernel, 3);
    }

    private Bitmap applyClarity(Bitmap src, float strength) {
        if (Math.abs(strength) < 0.01f) return src;
        float s = strength;
        float[] kernel = new float[]{
                -s * 0.5f, -s,            -s * 0.5f,
                -s,        1 + 6 * s,     -s,
                -s * 0.5f, -s,            -s * 0.5f
        };
        return applyConvolution(src, kernel, 3);
    }

    private Bitmap applyConvolution(Bitmap src, float[] kernel, int kSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        int[] result = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int half = kSize / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = 0, g = 0, b = 0;
                for (int ky = 0; ky < kSize; ky++) {
                    int py = y + ky - half;
                    if (py < 0) py = 0; else if (py >= h) py = h - 1;
                    for (int kx = 0; kx < kSize; kx++) {
                        int px = x + kx - half;
                        if (px < 0) px = 0; else if (px >= w) px = w - 1;
                        int p = pixels[py * w + px];
                        float k = kernel[ky * kSize + kx];
                        r += ((p >> 16) & 0xff) * k;
                        g += ((p >> 8) & 0xff) * k;
                        b += (p & 0xff) * k;
                    }
                }
                int a = (pixels[y * w + x] >> 24) & 0xff;
                result[y * w + x] = (a << 24)
                        | (clampByte((int) r) << 16)
                        | (clampByte((int) g) << 8)
                        | clampByte((int) b);
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(result, 0, w, 0, 0, w, h);
        return out;
    }

    private int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private Bitmap copyBitmap(Bitmap src) {
        Bitmap.Config config = src.getConfig();
        if (config == null) config = Bitmap.Config.ARGB_8888;
        return src.copy(config, true);
    }

    private void saveBitmapState() {
        if (photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
            Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
            if (current != null) {
                undoBitmapStack.push(copyBitmap(current));
                redoBitmapStack.clear();
            }
        }
    }

    private void setupPhotoEditorListener() {
        photoEditor.setOnPhotoEditorListener(new OnPhotoEditorListener() {
            @Override
            public void onEditTextChangeListener(@Nullable View view, @Nullable String s, int i) {}
            @Override public void onAddViewListener(@Nullable ViewType viewType, int i) {}
            @Override public void onRemoveViewListener(@Nullable ViewType viewType, int i) {}
            @Override public void onStartViewChangeListener(@Nullable ViewType viewType) {}
            @Override public void onStopViewChangeListener(@Nullable ViewType viewType) {}
            @Override public void onTouchSourceImage(@Nullable MotionEvent motionEvent) {}
        });
    }

    private void initViews() {
        photoEditorView = findViewById(R.id.photoEditorView);
        LinearLayout btnCrop = findViewById(R.id.btnCrop);
        LinearLayout btnFlip = findViewById(R.id.btnFlip);
        LinearLayout btnAdjust = findViewById(R.id.btnAdjust);
        LinearLayout btnFilter = findViewById(R.id.btnFilter);
        LinearLayout btnBrush = findViewById(R.id.btnBrush);
        LinearLayout btnAddText = findViewById(R.id.btnAddText);
        LinearLayout btnSticker = findViewById(R.id.btnSticker);

        adjustPanel = findViewById(R.id.adjustPanel);
        seekAdjust = findViewById(R.id.seekAdjust);
        adjustValueText = findViewById(R.id.adjustValueText);
        tabBrightness = findViewById(R.id.tabBrightness);
        tabContrast = findViewById(R.id.tabContrast);
        tabSaturation = findViewById(R.id.tabSaturation);
        tabSharpness = findViewById(R.id.tabSharpness);
        tabClarity = findViewById(R.id.tabClarity);
        tabHsl = findViewById(R.id.tabHsl);
        tabCurves = findViewById(R.id.tabCurves);
        tabHighlights = findViewById(R.id.tabHighlights);
        tabShadows = findViewById(R.id.tabShadows);
        iconBrightness = findViewById(R.id.iconBrightness);
        iconContrast = findViewById(R.id.iconContrast);
        iconSaturation = findViewById(R.id.iconSaturation);
        iconSharpness = findViewById(R.id.iconSharpness);
        iconClarity = findViewById(R.id.iconClarity);
        iconHsl = findViewById(R.id.iconHsl);
        iconCurves = findViewById(R.id.iconCurves);
        iconHighlights = findViewById(R.id.iconHighlights);
        iconShadows = findViewById(R.id.iconShadows);
        labelBrightness = findViewById(R.id.labelBrightness);
        labelContrast = findViewById(R.id.labelContrast);
        labelSaturation = findViewById(R.id.labelSaturation);
        labelSharpness = findViewById(R.id.labelSharpness);
        labelClarity = findViewById(R.id.labelClarity);
        labelHsl = findViewById(R.id.labelHsl);
        labelCurves = findViewById(R.id.labelCurves);
        labelHighlights = findViewById(R.id.labelHighlights);
        labelShadows = findViewById(R.id.labelShadows);
        Button btnAdjustReset = findViewById(R.id.btnAdjustReset);
        Button btnAdjustApply = findViewById(R.id.btnAdjustApply);

        filterPanel = findViewById(R.id.filterPanel);
        filterCategoryTabs = findViewById(R.id.filterCategoryTabs);
        filterVariantsList = findViewById(R.id.filterVariantsList);
        filterIntensityValueText = findViewById(R.id.filterIntensityValueText);
        filterIntensitySeek = findViewById(R.id.filterIntensitySeek);
        Button btnFilterReset = findViewById(R.id.btnFilterReset);
        Button btnFilterApply = findViewById(R.id.btnFilterApply);

        // Brush & Drawing views
        brushPanel = findViewById(R.id.brushPanel);
        seekBrushWidth = findViewById(R.id.seekBrushWidth);
        btnChooseColor = findViewById(R.id.btnChooseColor);
        btnBrushEraser = findViewById(R.id.btnBrushEraser);
        btnBrushFree = findViewById(R.id.btnBrushFree);
        btnBrushArrow = findViewById(R.id.btnBrushArrow);
        btnBrushLine = findViewById(R.id.btnBrushLine);
        btnBrushRect = findViewById(R.id.btnBrushRect);
        btnBrushOval = findViewById(R.id.btnBrushOval);
        colorPickerPanel = findViewById(R.id.colorPickerPanel);
        colorGrid = findViewById(R.id.colorGrid);
        commonColorsLayout = findViewById(R.id.commonColors);
        ImageButton btnBrushClose = findViewById(R.id.btnBrushClose);
        ImageButton btnBrushDone = findViewById(R.id.btnBrushDone);

        setupAdjustControls(btnAdjust, btnAdjustReset, btnAdjustApply);
        setupFilterControls(btnFilterReset, btnFilterApply);
        setupBrushControls(btnBrush, btnBrushClose, btnBrushDone);

        TextView btnSave = findViewById(R.id.btnSave);

        btnCrop.setOnClickListener(v -> {
            // Bake current layers before crop
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();
            photoEditor.saveAsBitmap(saveSettings, new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    File tempFile = new File(getCacheDir(), "crop_source_" + System.currentTimeMillis() + ".jpg");
                    try (OutputStream out = new java.io.FileOutputStream(tempFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        startCrop(Uri.fromFile(tempFile));
                    } catch (Exception e) {
                        Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditorActivity.this, "Lỗi chuẩn bị ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnFlip.setOnClickListener(v -> {
            saveBitmapState();
            photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                @Override
                public void onBitmapReady(@NonNull Bitmap bitmap) {
                    Matrix matrix = new Matrix();
                    matrix.postScale(-1, 1);
                    Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    
                    photoEditor.clearAllViews();
                    photoEditorView.getSource().setImageBitmap(flippedBitmap);
                    Toast.makeText(EditorActivity.this, "Đã lật ảnh và vẽ", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(@NonNull Exception e) {}
            });
        });

        btnFilter.setOnClickListener(v -> openFilterPanel());

        btnUndo.setOnClickListener(v -> {
            if (!undoBitmapStack.isEmpty()) {
                photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                    @Override
                    public void onBitmapReady(@NonNull Bitmap bitmap) {
                        redoBitmapStack.push(copyBitmap(bitmap));
                        photoEditor.clearAllViews();
                        photoEditorView.getSource().setImageBitmap(undoBitmapStack.pop());
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
            } else {
                photoEditor.undo();
            }
        });

        btnRedo.setOnClickListener(v -> {
            if (!redoBitmapStack.isEmpty()) {
                photoEditor.saveAsBitmap(new ja.burhanrashid52.photoeditor.OnSaveBitmap() {
                    @Override
                    public void onBitmapReady(@NonNull Bitmap bitmap) {
                        undoBitmapStack.push(copyBitmap(bitmap));
                        photoEditor.clearAllViews();
                        photoEditorView.getSource().setImageBitmap(redoBitmapStack.pop());
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
            } else {
                photoEditor.redo();
            }
        });

        btnAddText.setOnClickListener(v ->
                photoEditor.addText("Text", ContextCompat.getColor(this, R.color.white)));

        btnSticker.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Sticker/Icon sẽ cần thêm bộ icon", Toast.LENGTH_SHORT).show());

        btnSave.setOnClickListener(v -> saveProcessedImage());
    }

    private void hideAllPanels() {
        if (adjustPanel.getVisibility() == View.VISIBLE) closeAdjustPanel(false);
        if (filterPanel.getVisibility() == View.VISIBLE) closeFilterPanel(false);
        if (brushPanel.getVisibility() == View.VISIBLE) closeBrushPanel();
    }

    private void setupBrushControls(View btnBrush, View btnClose, View btnDone) {
        btnBrush.setOnClickListener(v -> {
            if (brushPanel.getVisibility() == View.VISIBLE) {
                closeBrushPanel();
                return;
            }
            hideAllPanels();
            openBrushPanel();
        });

        seekBrushWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        btnChooseColor.setOnClickListener(v -> {
            if (colorPickerPanel.getVisibility() == View.VISIBLE) {
                colorPickerPanel.setVisibility(View.GONE);
            } else {
                showColorPicker();
            }
        });

        btnBrushEraser.setOnClickListener(v -> selectBrushTool(ShapeType.Brush.INSTANCE, true));
        btnBrushFree.setOnClickListener(v -> selectBrushTool(ShapeType.Brush.INSTANCE, false));
        btnBrushArrow.setOnClickListener(v -> selectBrushTool(new ShapeType.Arrow(), false));
        btnBrushLine.setOnClickListener(v -> selectBrushTool(ShapeType.Line.INSTANCE, false));
        btnBrushRect.setOnClickListener(v -> selectBrushTool(ShapeType.Rectangle.INSTANCE, false));
        btnBrushOval.setOnClickListener(v -> selectBrushTool(ShapeType.Oval.INSTANCE, false));

        btnClose.setOnClickListener(v -> closeBrushPanel());
        btnDone.setOnClickListener(v -> closeBrushPanel());
    }

    private void openBrushPanel() {
        brushPanel.setVisibility(View.VISIBLE);
        photoEditor.setBrushDrawingMode(true);
        selectBrushTool(ShapeType.Brush.INSTANCE, false);
        updateColorPreview();
    }

    private void closeBrushPanel() {
        brushPanel.setVisibility(View.GONE);
        colorPickerPanel.setVisibility(View.GONE);
        photoEditor.setBrushDrawingMode(false);
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
                    .withShapeSize(seekBrushWidth.getProgress());
            photoEditor.setShape(sb);
        }

        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = Color.TRANSPARENT;
        btnBrushEraser.setBackgroundColor(isEraser ? active : inactive);
        btnBrushFree.setBackgroundColor(!isEraser && type instanceof ShapeType.Brush ? active : inactive);
        btnBrushArrow.setBackgroundColor(type instanceof ShapeType.Arrow ? active : inactive);
        btnBrushLine.setBackgroundColor(type instanceof ShapeType.Line ? active : inactive);
        btnBrushRect.setBackgroundColor(type instanceof ShapeType.Rectangle ? active : inactive);
        btnBrushOval.setBackgroundColor(type instanceof ShapeType.Oval ? active : inactive);
    }

    private void showColorPicker() {
        colorPickerPanel.setVisibility(View.VISIBLE);
        if (colorGrid.getChildCount() == 0) {
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
                View view = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        Math.round(36 * getResources().getDisplayMetrics().density),
                        Math.round(36 * getResources().getDisplayMetrics().density));
                params.setMargins(0, 0, 12, 0);
                view.setLayoutParams(params);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(color);
                shape.setStroke(2, Color.GRAY);
                view.setBackground(shape);
                view.setOnClickListener(v -> onColorSelected(color));
                commonColorsLayout.addView(view);
            }
        }
    }

    private void addColorToGrid(int color) {
        View view = new View(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = Math.round(32 * getResources().getDisplayMetrics().density);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 2, 2, 2);
        view.setLayoutParams(params);
        view.setBackgroundColor(color);
        view.setOnClickListener(v -> onColorSelected(color));
        colorGrid.addView(view);
    }

    private void onColorSelected(int color) {
        currentBrushColor = color;
        ShapeBuilder sb = new ShapeBuilder()
                .withShapeType(currentShapeType)
                .withShapeColor(color)
                .withShapeSize(seekBrushWidth.getProgress());
        photoEditor.setShape(sb);
        updateColorPreview();
        colorPickerPanel.setVisibility(View.GONE);
    }

    private void updateColorPreview() {
        LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(this, R.drawable.bg_color_picker_button);
        if (layerDrawable != null) {
            GradientDrawable colorCircle = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.color_circle);
            if (colorCircle != null) {
                colorCircle.setColor(currentBrushColor);
                btnChooseColor.setBackground(layerDrawable);
            }
        }
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
                finish();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                saveBitmapState();
                currentImageUri = resultUri;
                photoEditor.clearAllViews();
                photoEditorView.getSource().setImageURI(currentImageUri);
            }
        }
    }
}
