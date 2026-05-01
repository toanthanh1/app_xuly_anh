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
import android.view.ViewGroup;
import android.widget.Button;
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

import com.example.app_xhinh_anh.features.ai_assistant.data.GeminiApiClient;
import com.example.app_xhinh_anh.features.ai_assistant.domain.ActionMapper;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.AiAssistantActivity;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;

import com.example.app_xhinh_anh.processing.ai.AiProcessor;
import com.example.app_xhinh_anh.processing.ai.BackgroundRemoverAi;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.app.ProgressDialog;

public class EditorActivity extends AppCompatActivity {

    private static final String TAG = "EditorActivity";

    private PhotoEditorView photoEditorView;
    private PhotoEditor photoEditor;
    private Uri currentImageUri;
    private boolean isBrushMode = false;
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
    private Bitmap adjustBaseBitmap;     // bản gốc full-res khi mở panel (chỉ dùng để bake cuối)
    private Bitmap adjustBasePreview;    // bản thu nhỏ (~720px) dùng cho live preview
    private Bitmap adjustConvBitmap;     // adjustBasePreview đã áp LUT + sharpness + clarity
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

    // ==== Filter panel (Trắng đen / Hoài cổ / Cổ điển / Hits / Portrait / Texture) ====
    private LinearLayout filterPanel;
    private LinearLayout filterCategoryTabs;
    private LinearLayout filterVariantsList;
    private TextView filterIntensityValueText;
    private SeekBar filterIntensitySeek;
    private Bitmap filterBaseBitmap;
    private Bitmap filterThumbBitmap;
    private FilterPreset.Category selectedCategory;
    private TextView selectedCategoryTabView;
    private FilterPreset selectedVariant;
    private View selectedVariantView;
    private int filterIntensity = 100;

    // AI Assistant
    private LinearLayout chatPanel;
    private RecyclerView rvChatHistory;
    private ChatAdapter chatAdapter;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private ProgressBar pbAiThinking;
    private GeminiApiClient geminiApiClient;

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
        toolbar

                .setNavigationOnClickListener(v -> finish());
        // 1. Ánh xạ View
        photoEditorView = findViewById(R.id.photoEditorView);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);

        // 2. Khởi tạo PhotoEditor
        photoEditor = new PhotoEditor.Builder(this, photoEditorView)
                .setPinchTextScalable(true)
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
                adjustPanel.setVisibility(View.GONE);
                photoEditorView.getSource().clearColorFilter();
                if (adjustBaseBitmap != null) {
                    photoEditorView.getSource().setImageBitmap(adjustBaseBitmap);
                }
                adjustBaseBitmap = null;
                adjustBasePreview = null;
                adjustConvBitmap = null;
                return;
            }
            if (filterPanel != null && filterPanel.getVisibility() == View.VISIBLE) {
                closeFilterPanel();
            }
            if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
                Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
                return;
            }
            saveBitmapState();
            Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
            if (current == null) return;
            adjustBaseBitmap = copyBitmap(current);
            adjustBasePreview = makePreviewBitmap(adjustBaseBitmap, PREVIEW_MAX_DIM);
            adjustConvBitmap = adjustBasePreview;
            resetAdjustValues();
            photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
            photoEditorView.getSource().clearColorFilter();
            selectAdjustMode(MODE_BRIGHTNESS);
            adjustPanel.setVisibility(View.VISIBLE);
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
                // Pipeline live: ColorMatrix qua setColorFilter (luôn nhanh) +
                // LUT/convolution chạy trên bản preview ~720px (đủ nhanh để live).
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
            adjustPanel.setVisibility(View.GONE);
            adjustBaseBitmap = null;
            adjustBasePreview = null;
            adjustConvBitmap = null;
        });
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

    /** Live preview: chỉ gắn ColorMatrixColorFilter lên ImageView (GPU, không cấp phát bitmap). */
    private void applyColorAdjustments() {
        if (adjustBaseBitmap == null) return;
        photoEditorView.getSource().setColorFilter(
                new ColorMatrixColorFilter(buildColorMatrix()));
    }

    /** Dựng ColorMatrix tổ hợp từ các giá trị brightness/contrast/saturation/hue hiện tại. */
    private ColorMatrix buildColorMatrix() {
        // Mỗi giá trị nằm trong [-50..+50], 0 = không đổi:
        //  brightness offset [-100..+100] (cộng vào kênh 0..255) — *2 để cường độ rõ
        //  contrast scale [0..2] (1 = giữ nguyên)
        //  saturation [0..2] (0 = grayscale, 1 = giữ nguyên, 2 = bão hòa cao)
        //  hue: -50..+50 → -180..+180 độ
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

    /** Live: chạy LUT + sharpness + clarity trên adjustBasePreview (~720px) → adjustConvBitmap. */
    private void rebuildConvolutionBitmap() {
        if (adjustBasePreview == null) return;
        adjustConvBitmap = applyHeavyPipeline(adjustBasePreview);
        photoEditorView.getSource().setImageBitmap(adjustConvBitmap);
        // setImageBitmap giữ nguyên colorFilter, không cần set lại.
    }

    /** Áp LUT (curves+highlights+shadows) → sharpness → clarity lên một bitmap. Dùng cho cả preview và full-res. */
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

    /** Tạo LUT 256-entry kết hợp Curves (S-curve), Highlights, Shadows. amount mỗi cái: -1..+1. */
    private int[] buildToneLut(float curves, float highlights, float shadows) {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            float x = i / 255f;
            float y = x;
            // Curves: blend giữa identity và smoothstep (S-curve). +: tăng tương phản trung tần. -: giảm.
            if (curves != 0f) {
                float ss = x * x * (3f - 2f * x);
                y = y + curves * (ss - x);
            }
            // Highlights: dịch các pixel sáng. mask cao ở x>0.5, 0 ở x<=0.5.
            if (highlights != 0f) {
                float maskH = Math.max(0f, 2f * x - 1f);
                y = y + highlights * 0.4f * maskH;
            }
            // Shadows: dịch các pixel tối. mask cao ở x<0.5, 0 ở x>=0.5.
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

    /** Khi bấm "Xong": chạy lại pipeline ở full-resolution (chỉ 1 lần) để bake kết quả cuối. */
    private void bakeAdjustments() {
        if (adjustBaseBitmap == null) return;
        // 1. Pipeline nặng ở full-res
        Bitmap full = applyHeavyPipeline(adjustBaseBitmap);
        // 2. Bake ColorMatrix (nếu có thay đổi) vào bitmap
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

    // ============================================================
    // Filter panel — category tabs (ngang) + variant thumbnails (dưới) + intensity
    // ============================================================

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
            closeFilterPanel();
        });
    }

    private void openFilterPanel() {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (adjustPanel != null && adjustPanel.getVisibility() == View.VISIBLE) {
            adjustPanel.setVisibility(View.GONE);
            photoEditorView.getSource().clearColorFilter();
            if (adjustBaseBitmap != null) {
                photoEditorView.getSource().setImageBitmap(adjustBaseBitmap);
            }
            adjustBaseBitmap = null;
            adjustBasePreview = null;
            adjustConvBitmap = null;
        }
        saveBitmapState();
        Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        if (current == null) return;

        filterBaseBitmap = copyBitmap(current);
        int thumbDim = Math.round(128f * getResources().getDisplayMetrics().density);
        filterThumbBitmap = makePreviewBitmap(filterBaseBitmap, thumbDim);

        populateCategoryTabs();
        // Mặc định chọn category đầu tiên
        selectCategory(FilterPreset.CATEGORIES[0], (TextView) filterCategoryTabs.getChildAt(0));

        filterIntensity = 100;
        filterIntensitySeek.setProgress(100);
        filterIntensityValueText.setText("100");
        photoEditorView.getSource().clearColorFilter();

        filterPanel.setVisibility(View.VISIBLE);
    }

    private void closeFilterPanel() {
        filterPanel.setVisibility(View.GONE);
        photoEditorView.getSource().clearColorFilter();
        if (selectedVariantView != null) selectedVariantView.setBackground(null);
        selectedVariantView = null;
        selectedVariant = null;
        selectedCategory = null;
        selectedCategoryTabView = null;
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
        selectedCategory = category;
        // Khi đổi category → bỏ chọn variant cũ và clear preview
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
        filterIntensitySeek.setProgress(100);
        filterIntensityValueText.setText("100");
        photoEditorView.getSource().clearColorFilter();
    }

    /** Live preview: gắn ColorMatrixColorFilter ở cường độ hiện tại. */
    private void applyFilterPreview() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) {
            photoEditorView.getSource().clearColorFilter();
            return;
        }
        float t = filterIntensity / 100f;
        photoEditorView.getSource().setColorFilter(
                new ColorMatrixColorFilter(lerpToIdentity(selectedVariant.matrix, t)));
    }

    /** Bake filter ở full-resolution rồi gán lại cho ImageView. */
    private void bakeFilter() {
        if (selectedVariant == null || selectedVariant.matrix == null || filterBaseBitmap == null) return;
        float t = filterIntensity / 100f;
        Bitmap full = applyMatrixToBitmap(filterBaseBitmap, lerpToIdentity(selectedVariant.matrix, t));
        photoEditorView.getSource().clearColorFilter();
        photoEditorView.getSource().setImageBitmap(full);
    }

    /** Nội suy tuyến tính giữa identity matrix và target — dùng để "pha loãng" filter theo cường độ. */
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

    /** Hue rotation matrix dựa trên trọng số luminance chuẩn. */
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

    /** strength: -1..+1. Dương: làm nét; âm: làm mờ (box-blur). */
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
            float w = -strength; // 0..1, mức làm mờ
            float side = w / 9f;
            kernel = new float[]{
                    side, side,           side,
                    side, 1f - 8 * side,  side,
                    side, side,           side
            };
        }
        return applyConvolution(src, kernel, 3);
    }

    /** Clarity: tăng tương phản trung tần qua một kernel khác. strength: -1..+1. */
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
    @SuppressWarnings("deprecation")
    private void initViews() {
        photoEditorView = findViewById(R.id.photoEditorView);
        LinearLayout btnCrop = findViewById(R.id.btnCrop);
        LinearLayout btnFlip = findViewById(R.id.btnFlip);
        LinearLayout btnAdjust = findViewById(R.id.btnAdjust);
        LinearLayout btnFilter = findViewById(R.id.btnFilter);
        LinearLayout btnBrush = findViewById(R.id.btnBrush);
        LinearLayout btnAddText = findViewById(R.id.btnAddText);
        LinearLayout btnSticker = findViewById(R.id.btnSticker);
        TextView labelBrush = findViewById(R.id.labelBrush);
        ImageView iconBrush = findViewById(R.id.iconBrush);

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

        TextView btnSave = findViewById(R.id.btnSave);

        setupAdjustControls(btnAdjust, btnAdjustReset, btnAdjustApply);
        setupFilterControls(btnFilterReset, btnFilterApply);

        // AI Assistant
        setupAiAssistant();


        btnCrop.setOnClickListener(v -> {
            // Lưu trạng thái hiện tại (bao gồm filter, adjust, sticker, text...) vào file tạm để Crop
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
        
        btnFlip.setOnClickListener(v -> {
            saveBitmapState();
            if (photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
                Bitmap originalBitmap = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                Matrix matrix = new Matrix();
                matrix.postScale(-1, 1);
                Bitmap flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, 
                        originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                photoEditorView.getSource().setImageBitmap(flippedBitmap);
                Toast.makeText(this, "Đã lật ảnh", Toast.LENGTH_SHORT).show();
            }
        });
        // Tác vụ Undo
        btnUndo.setOnClickListener(v -> {
            if (!undoBitmapStack.isEmpty()) {
                Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                if (current != null) {
                    redoBitmapStack.push(copyBitmap(current));
                }
                photoEditorView.getSource().setImageBitmap(undoBitmapStack.pop());

            } else {
                photoEditor.undo();
            }
        });

        // Tác vụ Redo
        btnRedo.setOnClickListener(v -> {
            if (!redoBitmapStack.isEmpty()) {
                Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                if (current != null) undoBitmapStack.push(copyBitmap(current));
                photoEditorView.getSource().setImageBitmap(redoBitmapStack.pop());
            } else {
                photoEditor.redo();
            }
        });
        btnBrush.setOnClickListener(v -> {
            isBrushMode = !isBrushMode;
            photoEditor.setBrushDrawingMode(isBrushMode);
            labelBrush.setText(isBrushMode ? "Đang vẽ" : "Vẽ");
            int tintColor = ContextCompat.getColor(this,
                    isBrushMode ? R.color.brand_green : R.color.white);
            iconBrush.setColorFilter(tintColor);
            labelBrush.setTextColor(tintColor);
            if (isBrushMode) {
                photoEditor.setBrushColor(ContextCompat.getColor(this, R.color.brand_green));
                Toast.makeText(this, "Chế độ vẽ đã bật", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddText.setOnClickListener(v ->
                photoEditor.addText("Text", ContextCompat.getColor(this, R.color.white)));
        btnFilter.setOnClickListener(v -> openFilterPanel());

        btnSticker.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Sticker/Icon sẽ cần thêm bộ icon", Toast.LENGTH_SHORT).show());

        LinearLayout btnAiRmBg = findViewById(R.id.btnAiRmBg);
        backgroundRemoverAi = new BackgroundRemoverAi();
        btnAiRmBg.setOnClickListener(v -> performAiBackgroundRemoval());

        btnSave.setOnClickListener(v -> saveProcessedImage());
    }

    private void performAiBackgroundRemoval() {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Không có ảnh để xử lý", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap currentBitmap = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        if (currentBitmap == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xóa nền AI...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        backgroundRemoverAi.process(currentBitmap, new AiProcessor.AiCallback() {
            @Override
            public void onSuccess(Bitmap result) {
                progressDialog.dismiss();
                saveBitmapState();
                photoEditorView.getSource().setImageBitmap(result);
                Toast.makeText(EditorActivity.this, "Đã xóa nền!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
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

        // Hiển thị bottom bar (tab Aspect Ratio + Rotate)
        options.setHideBottomControls(false);
        // Danh sách tỉ lệ cố định — index 0 = "Gốc" (0,0) sẽ dùng tỉ lệ ảnh gốc
        options.setAspectRatioOptions(0,
                new com.yalantis.ucrop.model.AspectRatio("Gốc", 0f, 0f),
                new com.yalantis.ucrop.model.AspectRatio("1:1", 1f, 1f),
                new com.yalantis.ucrop.model.AspectRatio("4:3", 4f, 3f),
                new com.yalantis.ucrop.model.AspectRatio("3:4", 3f, 4f),
                new com.yalantis.ucrop.model.AspectRatio("16:9", 16f, 9f),
                new com.yalantis.ucrop.model.AspectRatio("9:16", 9f, 16f)
        );
        // Cho phép kéo cạnh khung crop tự do (xen kẽ với việc chọn tỉ lệ cố định)
        options.setFreeStyleCropEnabled(true);
        // Mở mọi gesture ở mọi tab: phóng to/thu nhỏ (pinch) + xoay + chỉnh khung
        options.setAllowedGestures(UCropActivity.ALL, UCropActivity.ALL, UCropActivity.ALL);
        // Giới hạn tỉ lệ phóng to tối đa (10x)
        options.setMaxScaleMultiplier(10f);

        uCrop.withOptions(options);
        uCrop.start(this);
    }

    private void setupAiAssistant() {
        findViewById(R.id.btnAiAssistant).setOnClickListener(v -> {
            Intent intent = new Intent(this, AiAssistantActivity.class);
            startActivityForResult(intent, 1001);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 1001) {
                String aiResponse = data.getStringExtra("ai_response");
                if (aiResponse != null) {
                    processAiAction(aiResponse);
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    // Lưu trạng thái cũ vào Undo trước khi đổi sang ảnh đã crop
                    saveBitmapState();

                    // Cập nhật Uri hiện tại và làm mới PhotoEditor
                    currentImageUri = resultUri;
                    photoEditor.clearAllViews(); // Xóa các layer cũ vì chúng đã được bake vào ảnh crop
                    photoEditorView.getSource().setImageURI(currentImageUri);
                }
            }
        }
    }

    private void processAiAction(String aiResponse) {
        ActionMapper.map(aiResponse, new ActionMapper.ActionListener() {
            @Override
            public void onApplyFilter(String filterName) {
                applyAiFilter(filterName);
            }

            @Override
            public void onAdjustProperty(String property, int value) {
                applyAiAdjustment(property, value);
            }

            @Override
            public void onMessage(String message) {
                // Có thể hiển thị Toast hoặc một thông báo nhỏ
                Toast.makeText(EditorActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendChatMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, true));
        etChatInput.setText("");
        rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        pbAiThinking.setVisibility(View.VISIBLE);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    ActionMapper.map(response, new ActionMapper.ActionListener() {
                        @Override
                        public void onApplyFilter(String filterName) {
                            // Thêm tin nhắn xác nhận thay vì hiện mã JSON
                            chatAdapter.addMessage(new ChatMessage("Đang áp dụng bộ lọc: " + filterName, false));
                            applyAiFilter(filterName);
                        }

                        @Override
                        public void onAdjustProperty(String property, int value) {
                            // Thêm tin nhắn xác nhận
                            chatAdapter.addMessage(new ChatMessage("Đang chỉnh " + property + " thành " + value + "%", false));
                            applyAiAdjustment(property, value);
                        }

                        @Override
                        public void onMessage(String msg) {
                            chatAdapter.addMessage(new ChatMessage(msg, false));
                        }
                    });
                    rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    chatAdapter.addMessage(new ChatMessage("Lỗi: " + t.getMessage(), false));
                    rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }
        });
    }

    private void applyAiFilter(String filterName) {
        // Tìm kiếm bộ lọc trong danh sách Category
        for (FilterPreset.Category category : FilterPreset.CATEGORIES) {
            for (FilterPreset variant : category.variants) {
                if (variant.displayName.equalsIgnoreCase(filterName)) {
                    runOnUiThread(() -> {
                        if (filterPanel.getVisibility() != View.VISIBLE) {
                            findViewById(R.id.btnFilter).performClick();
                        }

                        // Cập nhật UI category nếu cần
                        int catIndex = -1;
                        for (int i = 0; i < FilterPreset.CATEGORIES.length; i++) {
                            if (FilterPreset.CATEGORIES[i] == category) {
                                catIndex = i;
                                break;
                            }
                        }
                        if (catIndex != -1 && filterCategoryTabs.getChildCount() > catIndex) {
                            selectCategory(category, (TextView) filterCategoryTabs.getChildAt(catIndex));
                        }

                        // Tìm và chọn variant trong list để có highlight UI
                        View targetItem = null;
                        for (int i = 0; i < filterVariantsList.getChildCount(); i++) {
                            View item = filterVariantsList.getChildAt(i);
                            TextView nameTv = item.findViewById(R.id.filterName);
                            if (nameTv != null && nameTv.getText().toString().equalsIgnoreCase(filterName)) {
                                targetItem = item;
                                break;
                            }
                        }

                        selectVariant(variant, targetItem);
                        Toast.makeText(this, "Đã áp dụng: " + filterName, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
            }
        }
        Toast.makeText(this, "Không tìm thấy bộ lọc: " + filterName, Toast.LENGTH_SHORT).show();
    }

    private void applyAiAdjustment(String property, int value) {
        Toast.makeText(this, "Đang chỉnh " + property + " thành " + value, Toast.LENGTH_SHORT).show();

        int mode = -1;
        switch (property.toUpperCase()) {
            case "BRIGHTNESS": mode = MODE_BRIGHTNESS; break;
            case "CONTRAST": mode = MODE_CONTRAST; break;
            case "SATURATION": mode = MODE_SATURATION; break;
            // Add more mappings
        }

        if (mode != -1) {
            final int targetMode = mode;
            runOnUiThread(() -> {
                if (adjustPanel.getVisibility() != View.VISIBLE) {
                    findViewById(R.id.btnAdjust).performClick();
                }
                selectAdjustMode(targetMode);
                seekAdjust.setProgress(value);
            });
        }
    }

    private void hideAllPanels() {
        if (adjustPanel != null) adjustPanel.setVisibility(View.GONE);
        if (filterPanel != null) filterPanel.setVisibility(View.GONE);
        if (chatPanel != null) chatPanel.setVisibility(View.GONE);
    }

}
