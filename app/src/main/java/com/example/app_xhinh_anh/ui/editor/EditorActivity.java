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
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.app_xhinh_anh.features.ai_assistant.ui.AiAssistantActivity;
import com.example.app_xhinh_anh.processing.tools.StickerManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;

public class EditorActivity extends AppCompatActivity {

    private static final String TAG = "EditorActivity";
    private static final int AI_REQUEST_CODE = 1001;

    private PhotoEditorView photoEditorView;
    private PhotoEditor photoEditor;
    private StickerManager stickerManager;
    private Uri currentImageUri;
    private boolean isBrushMode = false;
    private ImageButton btnUndo, btnRedo;
    private final Stack<Bitmap> undoBitmapStack = new Stack<>();
    private final Stack<Bitmap> redoBitmapStack = new Stack<>();
    // Ghi nhớ thứ tự xen kẽ giữa thao tác bitmap (flip/crop/adjust/filter) và view (text/brush)
    private enum OpType { BITMAP, VIEW }
    private final Stack<OpType> undoOpStack = new Stack<>();
    private final Stack<OpType> redoOpStack = new Stack<>();
    private boolean isPerformingUndoRedo = false;

    private LinearLayout adjustPanel;
    private SeekBar seekAdjust;
    private TextView adjustValueText;
    private LinearLayout tabBrightness, tabContrast, tabSaturation, tabSharpness, tabClarity, tabHsl,
            tabCurves, tabHighlights, tabShadows,
            tabTemp, tabHue, tabFade, tabVignette, tabGrain;
    private ImageView iconBrightness, iconContrast, iconSaturation, iconSharpness, iconClarity, iconHsl,
            iconCurves, iconHighlights, iconShadows,
            iconTemp, iconHue, iconFade, iconVignette, iconGrain;
    private TextView labelBrightness, labelContrast, labelSaturation, labelSharpness, labelClarity, labelHsl,
            labelCurves, labelHighlights, labelShadows,
            labelTemp, labelHue, labelFade, labelVignette, labelGrain;
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
    private static final int MODE_TEMP = 9;
    private static final int MODE_HUE = 10;
    private static final int MODE_FADE = 11;
    private static final int MODE_VIGNETTE = 12;
    private static final int MODE_GRAIN = 13;
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
    private int tempValue = 0;
    private int hueValue = 0;
    private int fadeValue = 0;
    private int vignetteValue = 0;
    private int grainValue = 0;

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

    // ==== Brush panel ====
    private ImageView iconBrush;
    private TextView labelBrush;
    private LinearLayout brushPanel;
    private LinearLayout brushStylePen, brushStyleHighlighter, brushStyleEraser;
    private ImageView iconBrushStylePen, iconBrushStyleHighlighter, iconBrushStyleEraser;
    private TextView labelBrushStylePen, labelBrushStyleHighlighter, labelBrushStyleEraser;
    private LinearLayout brushColorRow;
    private SeekBar brushSizeSeek, brushOpacitySeek;
    private TextView brushSizeValueText, brushOpacityValueText;
    private static final int BRUSH_STYLE_PEN = 0;
    private static final int BRUSH_STYLE_HIGHLIGHTER = 1;
    private static final int BRUSH_STYLE_ERASER = 2;
    private int currentBrushStyle = BRUSH_STYLE_PEN;
    private int currentBrushColor = 0xFF000000;
    private int currentBrushSize = 20;
    private int currentBrushOpacity = 100;
    private View selectedBrushColorView;
    private static final int[] BRUSH_COLORS = new int[]{
            // Đen và trắng
            0xFF000000, 0xFFFFFFFF,
            // Đỏ - các tone
            0xFFFF0000, 0xFFE53935, 0xFFC62828, 0xFFB71C1C, 0xFFFF5252, 0xFFFF1744, 0xFFE91E63, 0xFFFF6090,
            // Cam - các tone
            0xFFFF6F00, 0xFFFB8C00, 0xFFF57C00, 0xFFE65100, 0xFFFF9800, 0xFFFFB74D, 0xFFFFA726,
            // Vàng - các tone
            0xFFFFEB3B, 0xFFFDD835, 0xFFFBC02D, 0xFFF57F17, 0xFFFFF176, 0xFFFFF59D,
            // Xanh lá - các tone
            0xFF43A047, 0xFF2E7D32, 0xFF1B5E20, 0xFF4CAF50, 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7,
            // Xanh dương - các tone
            0xFF1E88E5, 0xFF1565C0, 0xFF0D47A1, 0xFF2196F3, 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9,
            // Tím - các tone
            0xFF8E24AA, 0xFF6A1B9A, 0xFF4A148C, 0xFF9C27B0, 0xFFBA68C8, 0xFFCE93D8
    };
    
    // 30+ màu sắc cho text
    private static final int[] TEXT_COLORS = new int[]{
            // Đen và trắng
            0xFF000000, 0xFFFFFFFF,
            // Đỏ - các tone
            0xFFFF0000, 0xFFE53935, 0xFFC62828, 0xFFB71C1C, 0xFFFF5252, 0xFFFF1744, 0xFFE91E63, 0xFFFF6090,
            // Cam - các tone
            0xFFFF6F00, 0xFFFB8C00, 0xFFF57C00, 0xFFE65100, 0xFFFF9800, 0xFFFFB74D, 0xFFFFA726,
            // Vàng - các tone
            0xFFFFEB3B, 0xFFFDD835, 0xFFFBC02D, 0xFFF57F17, 0xFFFFF176, 0xFFFFF59D,
            // Xanh lá - các tone
            0xFF43A047, 0xFF2E7D32, 0xFF1B5E20, 0xFF4CAF50, 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7,
            // Xanh dương - các tone
            0xFF1E88E5, 0xFF1565C0, 0xFF0D47A1, 0xFF2196F3, 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9,
            // Tím - các tone
            0xFF8E24AA, 0xFF6A1B9A, 0xFF4A148C, 0xFF9C27B0, 0xFFBA68C8, 0xFFCE93D8
    };

    // ==== Text panel ====
    private LinearLayout textPanel;
    private EditText textInput;
    private LinearLayout tabTextFont, tabTextColor, tabTextSize, tabTextFormat, tabTextAlign;
    private ImageView iconTextFont, iconTextColor, iconTextSize, iconTextFormat, iconTextAlign;
    private TextView labelTextFont, labelTextColor, labelTextSize, labelTextFormat, labelTextAlign;
    private View textFontPanel, textColorPanel, textSizePanel, textFormatPanel, textAlignPanel;
    private LinearLayout textFontRow, textColorRow;
    private SeekBar textSizeSeek;
    private TextView textSizeValueText;
    private ImageButton btnTextBold, btnTextItalic, btnTextUnderline;
    private ImageButton btnTextAlignLeft, btnTextAlignCenter, btnTextAlignRight;
    private static final int TEXT_TAB_FONT = 0;
    private static final int TEXT_TAB_COLOR = 1;
    private static final int TEXT_TAB_SIZE = 2;
    private static final int TEXT_TAB_FORMAT = 3;
    private static final int TEXT_TAB_ALIGN = 4;
    private int currentTextTab = TEXT_TAB_FONT;
    private int currentTextFontIndex = 0;
    private int currentTextColor = 0xFFFFFFFF;
    private boolean isTextBold = false;
    private boolean isTextItalic = false;
    private boolean isTextUnderline = false;
    private int currentTextAlign = Gravity.CENTER;
    private int currentTextSize = 32;
    private View selectedTextFontView;
    private View selectedTextColorView;
    private List<FontDef> textFontList;

    // ==== Smart eraser panel ====
    private LinearLayout smartEraserPanel;
    private ImageView iconSmartEraser;
    private TextView labelSmartEraser;

    // ==== Mask painting panel (for "Tẩy AI") ====
    private LinearLayout maskPanel;
    private MaskOverlayView maskOverlayView;
    private SeekBar maskBrushSizeSeek;
    private TextView maskBrushSizeValue;
    private static final int MASK_BRUSH_DEFAULT = 30;
    private boolean isAiErasing = false;

    private static final class FontDef {
        final String name;
        final Typeface typeface;
        FontDef(String name, Typeface typeface) {
            this.name = name;
            this.typeface = typeface;
        }
    }

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
        stickerManager = new StickerManager(this, photoEditor, photoEditorView);
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
            if (brushPanel != null && brushPanel.getVisibility() == View.VISIBLE) {
                closeBrushPanel();
            }
            if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) {
                closeTextPanel();
            }
            if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) {
                closeSmartEraserPanel();
            }
            if (stickerManager != null) stickerManager.closeStickerPanel();
            if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
                Toast.makeText(this, "Chưa có ảnh để chỉnh", Toast.LENGTH_SHORT).show();
                return;
            }
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
        tabTemp.setOnClickListener(v -> selectAdjustMode(MODE_TEMP));
        tabHue.setOnClickListener(v -> selectAdjustMode(MODE_HUE));
        tabFade.setOnClickListener(v -> selectAdjustMode(MODE_FADE));
        tabVignette.setOnClickListener(v -> selectAdjustMode(MODE_VIGNETTE));
        tabGrain.setOnClickListener(v -> selectAdjustMode(MODE_GRAIN));

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
                    case MODE_TEMP: tempValue = value; break;
                    case MODE_HUE: hueValue = value; break;
                    case MODE_FADE: fadeValue = value; break;
                    case MODE_VIGNETTE: vignetteValue = value; break;
                    case MODE_GRAIN: grainValue = value; break;
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
                || mode == MODE_CURVES || mode == MODE_HIGHLIGHTS || mode == MODE_SHADOWS
                || mode == MODE_FADE || mode == MODE_VIGNETTE || mode == MODE_GRAIN;
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
        iconTemp.setColorFilter(mode == MODE_TEMP ? active : inactive);
        labelTemp.setTextColor(mode == MODE_TEMP ? active : inactive);
        iconHue.setColorFilter(mode == MODE_HUE ? active : inactive);
        labelHue.setTextColor(mode == MODE_HUE ? active : inactive);
        iconFade.setColorFilter(mode == MODE_FADE ? active : inactive);
        labelFade.setTextColor(mode == MODE_FADE ? active : inactive);
        iconVignette.setColorFilter(mode == MODE_VIGNETTE ? active : inactive);
        labelVignette.setTextColor(mode == MODE_VIGNETTE ? active : inactive);
        iconGrain.setColorFilter(mode == MODE_GRAIN ? active : inactive);
        labelGrain.setTextColor(mode == MODE_GRAIN ? active : inactive);

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
            case MODE_TEMP: value = tempValue; break;
            case MODE_HUE: value = hueValue; break;
            case MODE_FADE: value = fadeValue; break;
            case MODE_VIGNETTE: value = vignetteValue; break;
            case MODE_GRAIN: value = grainValue; break;
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
        tempValue = 0;
        hueValue = 0;
        fadeValue = 0;
        vignetteValue = 0;
        grainValue = 0;
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
        float hueDegrees2 = hueValue * 3.6f;
        // Temp: ấm (đẩy R lên, B xuống) ở dương, lạnh (R xuống, B lên) ở âm. ±60 ở biên.
        float tempOffset = (tempValue / 50f) * 60f;

        ColorMatrix cm = new ColorMatrix();

        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturation);
        cm.postConcat(saturationMatrix);

        if (hueDegrees != 0f) {
            cm.postConcat(buildHueMatrix(hueDegrees));
        }
        if (hueDegrees2 != 0f) {
            cm.postConcat(buildHueMatrix(hueDegrees2));
        }
        if (tempOffset != 0f) {
            cm.postConcat(new ColorMatrix(new float[]{
                    1, 0, 0, 0,  tempOffset,
                    0, 1, 0, 0,  0,
                    0, 0, 1, 0, -tempOffset,
                    0, 0, 0, 1,  0
            }));
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

    /** Áp LUT (curves+highlights+shadows+fade) → sharpness → clarity → vignette → grain lên một bitmap. */
    private Bitmap applyHeavyPipeline(Bitmap src) {
        Bitmap out = src;
        if (curvesValue != 0 || highlightsValue != 0 || shadowsValue != 0 || fadeValue != 0) {
            int[] lut = buildToneLut(
                    curvesValue / 50f,
                    highlightsValue / 50f,
                    shadowsValue / 50f,
                    fadeValue / 50f);
            out = applyLut(out, lut);
        }
        if (sharpnessValue != 0) {
            out = applySharpness(out, sharpnessValue / 50f);
        }
        if (clarityValue != 0) {
            out = applyClarity(out, clarityValue / 50f);
        }
        if (vignetteValue != 0) {
            out = applyVignette(out, vignetteValue / 50f);
        }
        if (grainValue != 0) {
            out = applyGrain(out, grainValue / 50f);
        }
        return out;
    }

    /** Tạo LUT 256-entry kết hợp Curves (S-curve), Highlights, Shadows, Fade. amount mỗi cái: -1..+1. */
    private int[] buildToneLut(float curves, float highlights, float shadows, float fade) {
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
            // Fade: nâng đáy (lift blacks) → look "phai màu" / ngược lại nhấn đáy.
            if (fade != 0f) {
                float lift = fade * 0.25f; // -0.25..+0.25
                y = lift + (1f - lift) * y;
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
        // 0. Lưu trạng thái trước khi bake để undo có thể quay về
        pushUndoBitmap(adjustBaseBitmap);
        // 1. Pipeline nặng ở full-res
        Bitmap full = applyHeavyPipeline(adjustBaseBitmap);
        // 2. Bake ColorMatrix (nếu có thay đổi) vào bitmap
        boolean hasColorMatrix = brightnessValue != 0 || contrastValue != 0
                || saturationValue != 0 || hslValue != 0
                || tempValue != 0 || hueValue != 0;
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
        if (brushPanel != null && brushPanel.getVisibility() == View.VISIBLE) {
            closeBrushPanel();
        }
        if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) {
            closeTextPanel();
        }
        if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) {
            closeSmartEraserPanel();
        }
        if (maskPanel != null && maskPanel.getVisibility() == View.VISIBLE) {
            closeMaskPanel();
        }
        if (stickerManager != null) stickerManager.closeStickerPanel();
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

    // ============================================================
    // Brush panel — chọn kiểu vẽ (Bút / Dạ quang / Tẩy), bảng màu, độ dày, độ trong
    // ============================================================

    private void setupBrushControls(Button btnBrushDone) {
        brushStylePen.setOnClickListener(v -> selectBrushStyle(BRUSH_STYLE_PEN));
        brushStyleHighlighter.setOnClickListener(v -> selectBrushStyle(BRUSH_STYLE_HIGHLIGHTER));
        brushStyleEraser.setOnClickListener(v -> selectBrushStyle(BRUSH_STYLE_ERASER));

        brushSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int size = Math.max(1, progress);
                currentBrushSize = size;
                brushSizeValueText.setText(String.valueOf(size));
                photoEditor.setBrushSize(size);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        brushOpacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                currentBrushOpacity = progress;
                brushOpacityValueText.setText(String.valueOf(progress));
                photoEditor.setOpacity(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnBrushDone.setOnClickListener(v -> closeBrushPanel());
    }

    private void openBrushPanel() {
        if (brushPanel.getVisibility() == View.VISIBLE) {
            closeBrushPanel();
            return;
        }
        // Đóng adjust panel
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
        // Đóng filter panel
        if (filterPanel != null && filterPanel.getVisibility() == View.VISIBLE) {
            closeFilterPanel();
        }
        if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) {
            closeTextPanel();
        }
        if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) {
            closeSmartEraserPanel();
        }
        if (maskPanel != null && maskPanel.getVisibility() == View.VISIBLE) {
            closeMaskPanel();
        }
        if (stickerManager != null) stickerManager.closeStickerPanel();

        if (brushColorRow.getChildCount() == 0) {
            populateBrushColors();
        }

        isBrushMode = true;
        photoEditor.setBrushDrawingMode(true);
        photoEditor.setBrushSize(currentBrushSize);
        photoEditor.setBrushColor(currentBrushColor);
        photoEditor.setOpacity(currentBrushOpacity);
        // Khôi phục lại trạng thái style đã chọn (gồm cả tẩy)
        selectBrushStyle(currentBrushStyle);

        brushSizeSeek.setProgress(currentBrushSize);
        brushSizeValueText.setText(String.valueOf(currentBrushSize));
        brushOpacitySeek.setProgress(currentBrushOpacity);
        brushOpacityValueText.setText(String.valueOf(currentBrushOpacity));

        int active = ContextCompat.getColor(this, R.color.brand_green);
        iconBrush.setColorFilter(active);
        labelBrush.setText("Đang vẽ");
        labelBrush.setTextColor(active);

        brushPanel.setVisibility(View.VISIBLE);
    }

    private void closeBrushPanel() {
        brushPanel.setVisibility(View.GONE);
        isBrushMode = false;
        photoEditor.setBrushDrawingMode(false);
        int inactive = ContextCompat.getColor(this, R.color.white);
        iconBrush.setColorFilter(inactive);
        labelBrush.setText("Vẽ");
        labelBrush.setTextColor(inactive);
    }

    private void selectBrushStyle(int style) {
        currentBrushStyle = style;
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);
        iconBrushStylePen.setColorFilter(style == BRUSH_STYLE_PEN ? active : inactive);
        labelBrushStylePen.setTextColor(style == BRUSH_STYLE_PEN ? active : inactive);
        iconBrushStyleHighlighter.setColorFilter(style == BRUSH_STYLE_HIGHLIGHTER ? active : inactive);
        labelBrushStyleHighlighter.setTextColor(style == BRUSH_STYLE_HIGHLIGHTER ? active : inactive);
        iconBrushStyleEraser.setColorFilter(style == BRUSH_STYLE_ERASER ? active : inactive);
        labelBrushStyleEraser.setTextColor(style == BRUSH_STYLE_ERASER ? active : inactive);

        switch (style) {
            case BRUSH_STYLE_ERASER:
                photoEditor.brushEraser();
                break;
            case BRUSH_STYLE_HIGHLIGHTER:
                photoEditor.setBrushDrawingMode(true);
                photoEditor.setBrushColor(currentBrushColor);
                currentBrushOpacity = 40;
                photoEditor.setOpacity(40);
                brushOpacitySeek.setProgress(40);
                brushOpacityValueText.setText("40");
                break;
            default: // PEN
                photoEditor.setBrushDrawingMode(true);
                photoEditor.setBrushColor(currentBrushColor);
                photoEditor.setOpacity(currentBrushOpacity);
                break;
        }
    }

    private void populateBrushColors() {
        brushColorRow.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int outerSize = (int) (40 * density);
        int innerSize = (int) (28 * density);
        int margin = (int) (4 * density);
        int pad = (int) (2 * density);
        for (int color : BRUSH_COLORS) {
            final int c = color;
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(outerSize, outerSize);
            clp.setMarginEnd(margin);
            container.setLayoutParams(clp);
            container.setPadding(pad, pad, pad, pad);

            View inner = new View(this);
            android.widget.FrameLayout.LayoutParams ilp =
                    new android.widget.FrameLayout.LayoutParams(innerSize, innerSize);
            ilp.gravity = android.view.Gravity.CENTER;
            inner.setLayoutParams(ilp);
            android.graphics.drawable.GradientDrawable innerBg = new android.graphics.drawable.GradientDrawable();
            innerBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            innerBg.setColor(c);
            innerBg.setStroke((int) (1 * density), 0xFF555555);
            inner.setBackground(innerBg);
            container.addView(inner);

            container.setClickable(true);
            container.setFocusable(true);
            container.setOnClickListener(v -> selectBrushColor(c, container));
            brushColorRow.addView(container);
        }
        // Mặc định chọn màu đầu tiên
        if (brushColorRow.getChildCount() > 0) {
            selectBrushColor(BRUSH_COLORS[0], brushColorRow.getChildAt(0));
        }
    }

    private void selectBrushColor(int color, View container) {
        currentBrushColor = color;
        if (selectedBrushColorView != null) {
            selectedBrushColorView.setBackground(null);
        }
        container.setBackgroundResource(R.drawable.bg_brush_color_selected);
        selectedBrushColorView = container;
        if (currentBrushStyle != BRUSH_STYLE_ERASER) {
            photoEditor.setBrushColor(color);
        }
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
        pushUndoBitmap(filterBaseBitmap);
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

    /** Vignette: tối/sáng dần ở vùng rìa theo bán kính. strength: -1..+1. +: rìa tối; -: rìa sáng. */
    private Bitmap applyVignette(Bitmap src, float strength) {
        if (Math.abs(strength) < 0.01f) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float maxR = (float) Math.sqrt(cx * cx + cy * cy);
        float darken = strength;             // dương → factor < 1 ở rìa
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float r = (float) Math.sqrt(dx * dx + dy * dy) / maxR; // 0..1
                // Mặt nạ: 0 ở phần lõi (r<0.3), tăng smoothstep ra biên
                float t = Math.max(0f, (r - 0.3f) / 0.7f);
                t = t * t * (3f - 2f * t);
                float factor = (darken >= 0f) ? (1f - darken * t) : (1f + (-darken) * t);
                int idx = y * w + x;
                int p = pixels[idx];
                int a = (p >> 24) & 0xff;
                int rC = clampByte(Math.round(((p >> 16) & 0xff) * factor));
                int gC = clampByte(Math.round(((p >> 8) & 0xff) * factor));
                int bC = clampByte(Math.round((p & 0xff) * factor));
                pixels[idx] = (a << 24) | (rC << 16) | (gC << 8) | bC;
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    /** Grain: cộng nhiễu cùng giá trị vào RGB từng pixel. Seed cố định để preview ổn định. */
    private Bitmap applyGrain(Bitmap src, float strength) {
        if (Math.abs(strength) < 0.01f) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        java.util.Random random = new java.util.Random(42);
        float amount = Math.abs(strength) * 50f; // ±50 ở biên
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int n = Math.round((random.nextFloat() - 0.5f) * 2f * amount);
            int a = (p >> 24) & 0xff;
            int r = clampByte(((p >> 16) & 0xff) + n);
            int g = clampByte(((p >> 8) & 0xff) + n);
            int b = clampByte((p & 0xff) + n);
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
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
            pushUndoBitmap(current);
        }
    }

    private void flipImage(int sx, int sy) {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) return;
        saveBitmapState();
        Bitmap original = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy);
        Bitmap flipped = Bitmap.createBitmap(original, 0, 0,
                original.getWidth(), original.getHeight(), matrix, true);
        photoEditorView.getSource().setImageBitmap(flipped);
        Toast.makeText(this, "Đã lật ảnh", Toast.LENGTH_SHORT).show();
    }

    private void closeAllOverlayPanels() {
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
        if (filterPanel != null && filterPanel.getVisibility() == View.VISIBLE) closeFilterPanel();
        if (brushPanel != null && brushPanel.getVisibility() == View.VISIBLE) closeBrushPanel();
        if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) closeTextPanel();
        if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) closeSmartEraserPanel();
        if (maskPanel != null && maskPanel.getVisibility() == View.VISIBLE) closeMaskPanel();
    }

    /** Đẩy một bitmap cụ thể (không nhất thiết là bitmap đang hiển thị) vào undo stack. */
    private void pushUndoBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        undoBitmapStack.push(copyBitmap(bitmap));
        undoOpStack.push(OpType.BITMAP);
        redoBitmapStack.clear();
        redoOpStack.clear();
    }
    private void setupPhotoEditorListener() {
        photoEditor.setOnPhotoEditorListener(new OnPhotoEditorListener() {
            @Override
            public void onEditTextChangeListener(@Nullable View view, @Nullable String s, int i) {}
            @Override public void onAddViewListener(@Nullable ViewType viewType, int i) {
                // Bỏ qua sự kiện do chính undo/redo của ta gây ra (photoEditor.redo() sẽ add lại view)
                if (isPerformingUndoRedo) return;
                undoOpStack.push(OpType.VIEW);
                redoBitmapStack.clear();
                redoOpStack.clear();
            }
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
        LinearLayout btnFlipHorizontal = findViewById(R.id.btnFlipHorizontal);
        LinearLayout btnFlipVertical = findViewById(R.id.btnFlipVertical);
        LinearLayout btnAdjust = findViewById(R.id.btnAdjust);
        LinearLayout btnFilter = findViewById(R.id.btnFilter);
        LinearLayout btnBrush = findViewById(R.id.btnBrush);
        LinearLayout btnAddText = findViewById(R.id.btnAddText);
        LinearLayout btnSticker = findViewById(R.id.btnSticker);
        LinearLayout btnSmartEraser = findViewById(R.id.btnSmartEraser);
        LinearLayout btnAiAssistant = findViewById(R.id.btnAiAssistant);
        labelBrush = findViewById(R.id.labelBrush);
        iconBrush = findViewById(R.id.iconBrush);

        smartEraserPanel = findViewById(R.id.smartEraserPanel);
        iconSmartEraser = findViewById(R.id.iconSmartEraser);
        labelSmartEraser = findViewById(R.id.labelSmartEraser);
        LinearLayout btnSmartEraserAi = findViewById(R.id.btnSmartEraserAi);
        LinearLayout btnRemoveBackground = findViewById(R.id.btnRemoveBackground);
        Button btnSmartEraserCancel = findViewById(R.id.btnSmartEraserCancel);

        maskPanel = findViewById(R.id.maskPanel);
        maskOverlayView = findViewById(R.id.maskOverlayView);
        maskBrushSizeSeek = findViewById(R.id.maskBrushSizeSeek);
        maskBrushSizeValue = findViewById(R.id.maskBrushSizeValue);
        Button btnMaskClear = findViewById(R.id.btnMaskClear);
        Button btnMaskCancel = findViewById(R.id.btnMaskCancel);
        Button btnMaskApply = findViewById(R.id.btnMaskApply);

        maskBrushSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int r = Math.max(4, progress);
                maskBrushSizeValue.setText(String.valueOf(r));
                if (maskOverlayView != null) maskOverlayView.setBrushRadius(r);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        btnMaskClear.setOnClickListener(v -> {
            if (maskOverlayView != null) maskOverlayView.clearMask();
        });
        btnMaskCancel.setOnClickListener(v -> closeMaskPanel());
        btnMaskApply.setOnClickListener(v -> applyAiErase());

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
        tabTemp = findViewById(R.id.tabTemp);
        tabHue = findViewById(R.id.tabHue);
        tabFade = findViewById(R.id.tabFade);
        tabVignette = findViewById(R.id.tabVignette);
        tabGrain = findViewById(R.id.tabGrain);
        iconBrightness = findViewById(R.id.iconBrightness);
        iconContrast = findViewById(R.id.iconContrast);
        iconSaturation = findViewById(R.id.iconSaturation);
        iconSharpness = findViewById(R.id.iconSharpness);
        iconClarity = findViewById(R.id.iconClarity);
        iconHsl = findViewById(R.id.iconHsl);
        iconCurves = findViewById(R.id.iconCurves);
        iconHighlights = findViewById(R.id.iconHighlights);
        iconShadows = findViewById(R.id.iconShadows);
        iconTemp = findViewById(R.id.iconTemp);
        iconHue = findViewById(R.id.iconHue);
        iconFade = findViewById(R.id.iconFade);
        iconVignette = findViewById(R.id.iconVignette);
        iconGrain = findViewById(R.id.iconGrain);
        labelBrightness = findViewById(R.id.labelBrightness);
        labelContrast = findViewById(R.id.labelContrast);
        labelSaturation = findViewById(R.id.labelSaturation);
        labelSharpness = findViewById(R.id.labelSharpness);
        labelClarity = findViewById(R.id.labelClarity);
        labelHsl = findViewById(R.id.labelHsl);
        labelCurves = findViewById(R.id.labelCurves);
        labelHighlights = findViewById(R.id.labelHighlights);
        labelShadows = findViewById(R.id.labelShadows);
        labelTemp = findViewById(R.id.labelTemp);
        labelHue = findViewById(R.id.labelHue);
        labelFade = findViewById(R.id.labelFade);
        labelVignette = findViewById(R.id.labelVignette);
        labelGrain = findViewById(R.id.labelGrain);
        Button btnAdjustReset = findViewById(R.id.btnAdjustReset);
        Button btnAdjustApply = findViewById(R.id.btnAdjustApply);

        filterPanel = findViewById(R.id.filterPanel);
        filterCategoryTabs = findViewById(R.id.filterCategoryTabs);
        filterVariantsList = findViewById(R.id.filterVariantsList);
        filterIntensityValueText = findViewById(R.id.filterIntensityValueText);
        filterIntensitySeek = findViewById(R.id.filterIntensitySeek);
        Button btnFilterReset = findViewById(R.id.btnFilterReset);
        Button btnFilterApply = findViewById(R.id.btnFilterApply);

        brushPanel = findViewById(R.id.brushPanel);
        brushStylePen = findViewById(R.id.brushStylePen);
        brushStyleHighlighter = findViewById(R.id.brushStyleHighlighter);
        brushStyleEraser = findViewById(R.id.brushStyleEraser);
        iconBrushStylePen = findViewById(R.id.iconBrushStylePen);
        iconBrushStyleHighlighter = findViewById(R.id.iconBrushStyleHighlighter);
        iconBrushStyleEraser = findViewById(R.id.iconBrushStyleEraser);
        labelBrushStylePen = findViewById(R.id.labelBrushStylePen);
        labelBrushStyleHighlighter = findViewById(R.id.labelBrushStyleHighlighter);
        labelBrushStyleEraser = findViewById(R.id.labelBrushStyleEraser);
        brushColorRow = findViewById(R.id.brushColorRow);
        brushSizeSeek = findViewById(R.id.brushSizeSeek);
        brushOpacitySeek = findViewById(R.id.brushOpacitySeek);
        brushSizeValueText = findViewById(R.id.brushSizeValueText);
        brushOpacityValueText = findViewById(R.id.brushOpacityValueText);
        Button btnBrushDone = findViewById(R.id.btnBrushDone);

        textPanel = findViewById(R.id.textPanel);
        textInput = findViewById(R.id.textInput);
        tabTextFont = findViewById(R.id.tabTextFont);
        tabTextColor = findViewById(R.id.tabTextColor);
        tabTextSize = findViewById(R.id.tabTextSize);
        tabTextFormat = findViewById(R.id.tabTextFormat);
        tabTextAlign = findViewById(R.id.tabTextAlign);
        iconTextFont = findViewById(R.id.iconTextFont);
        iconTextColor = findViewById(R.id.iconTextColor);
        iconTextSize = findViewById(R.id.iconTextSize);
        iconTextFormat = findViewById(R.id.iconTextFormat);
        iconTextAlign = findViewById(R.id.iconTextAlign);
        labelTextFont = findViewById(R.id.labelTextFont);
        labelTextColor = findViewById(R.id.labelTextColor);
        labelTextSize = findViewById(R.id.labelTextSize);
        labelTextFormat = findViewById(R.id.labelTextFormat);
        labelTextAlign = findViewById(R.id.labelTextAlign);
        textFontPanel = findViewById(R.id.textFontPanel);
        textColorPanel = findViewById(R.id.textColorPanel);
        textSizePanel = findViewById(R.id.textSizePanel);
        textFormatPanel = findViewById(R.id.textFormatPanel);
        textAlignPanel = findViewById(R.id.textAlignPanel);
        textFontRow = findViewById(R.id.textFontRow);
        textColorRow = findViewById(R.id.textColorRow);
        textSizeSeek = findViewById(R.id.textSizeSeek);
        textSizeValueText = findViewById(R.id.textSizeValueText);
        btnTextBold = findViewById(R.id.btnTextBold);
        btnTextItalic = findViewById(R.id.btnTextItalic);
        btnTextUnderline = findViewById(R.id.btnTextUnderline);
        btnTextAlignLeft = findViewById(R.id.btnTextAlignLeft);
        btnTextAlignCenter = findViewById(R.id.btnTextAlignCenter);
        btnTextAlignRight = findViewById(R.id.btnTextAlignRight);
        Button btnTextCancel = findViewById(R.id.btnTextCancel);
        Button btnTextDone = findViewById(R.id.btnTextDone);

        TextView btnSave = findViewById(R.id.btnSave);

        setupAdjustControls(btnAdjust, btnAdjustReset, btnAdjustApply);
        setupFilterControls(btnFilterReset, btnFilterApply);
        setupBrushControls(btnBrushDone);
        setupTextControls(btnTextCancel, btnTextDone);


        btnCrop.setOnClickListener(v -> startCrop(currentImageUri));
        
        btnFlipHorizontal.setOnClickListener(v -> flipImage(-1, 1));
        btnFlipVertical.setOnClickListener(v -> flipImage(1, -1));
        // Tác vụ Undo — route theo loại thao tác cuối cùng (BITMAP hay VIEW)
        btnUndo.setOnClickListener(v -> {
            if (undoOpStack.isEmpty()) return;
            OpType op = undoOpStack.pop();
            if (op == OpType.BITMAP) {
                if (undoBitmapStack.isEmpty()) return;
                Bitmap current = null;
                if (photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
                    current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                }
                if (current != null) {
                    redoBitmapStack.push(copyBitmap(current));
                }
                photoEditorView.getSource().clearColorFilter();
                photoEditorView.getSource().setImageBitmap(undoBitmapStack.pop());
                redoOpStack.push(OpType.BITMAP);
            } else {
                isPerformingUndoRedo = true;
                try {
                    photoEditor.undo();
                } finally {
                    isPerformingUndoRedo = false;
                }
                redoOpStack.push(OpType.VIEW);
            }
        });

        // Tác vụ Redo — đối xứng với Undo
        btnRedo.setOnClickListener(v -> {
            if (redoOpStack.isEmpty()) return;
            OpType op = redoOpStack.pop();
            if (op == OpType.BITMAP) {
                if (redoBitmapStack.isEmpty()) return;
                Bitmap current = null;
                if (photoEditorView.getSource().getDrawable() instanceof BitmapDrawable) {
                    current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
                }
                if (current != null) {
                    undoBitmapStack.push(copyBitmap(current));
                }
                photoEditorView.getSource().clearColorFilter();
                photoEditorView.getSource().setImageBitmap(redoBitmapStack.pop());
                undoOpStack.push(OpType.BITMAP);
            } else {
                isPerformingUndoRedo = true;
                try {
                    photoEditor.redo();
                } finally {
                    isPerformingUndoRedo = false;
                }
                undoOpStack.push(OpType.VIEW);
            }
        });
        btnBrush.setOnClickListener(v -> openBrushPanel());

        btnAddText.setOnClickListener(v -> openTextPanel());
        btnFilter.setOnClickListener(v -> openFilterPanel());

        btnSticker.setOnClickListener(v -> {
            if (stickerManager.isPanelVisible()) {
                stickerManager.closeStickerPanel();
                return;
            }
            closeAllOverlayPanels();
            stickerManager.openStickerPanel();
        });

        btnSmartEraser.setOnClickListener(v -> openSmartEraserPanel());
        btnAiAssistant.setOnClickListener(v ->
                startActivityForResult(new Intent(this, AiAssistantActivity.class), AI_REQUEST_CODE));
        btnSmartEraserAi.setOnClickListener(v -> openMaskPanel());
        btnRemoveBackground.setOnClickListener(v -> applyRemoveBackground());
        btnSmartEraserCancel.setOnClickListener(v -> closeSmartEraserPanel());

        btnSave.setOnClickListener(v -> saveProcessedImage());
    }

    // ============================================================
    // Text panel — inline panel có tab ngang giống Adjust/Brush:
    // Phông / Màu / Cỡ chữ / Định dạng / Căn lề
    // ============================================================

    private void setupTextControls(Button btnTextCancel, Button btnTextDone) {
        tabTextFont.setOnClickListener(v -> selectTextTab(TEXT_TAB_FONT));
        tabTextColor.setOnClickListener(v -> selectTextTab(TEXT_TAB_COLOR));
        tabTextSize.setOnClickListener(v -> selectTextTab(TEXT_TAB_SIZE));
        tabTextFormat.setOnClickListener(v -> selectTextTab(TEXT_TAB_FORMAT));
        tabTextAlign.setOnClickListener(v -> selectTextTab(TEXT_TAB_ALIGN));

        btnTextBold.setOnClickListener(v -> {
            isTextBold = !isTextBold;
            btnTextBold.setColorFilter(isTextBold
                    ? ContextCompat.getColor(this, R.color.brand_green)
                    : ContextCompat.getColor(this, R.color.white));
        });
        btnTextItalic.setOnClickListener(v -> {
            isTextItalic = !isTextItalic;
            btnTextItalic.setColorFilter(isTextItalic
                    ? ContextCompat.getColor(this, R.color.brand_green)
                    : ContextCompat.getColor(this, R.color.white));
        });

        btnTextUnderline.setOnClickListener(v -> {
            isTextUnderline = !isTextUnderline;
            btnTextUnderline.setColorFilter(isTextUnderline
                    ? ContextCompat.getColor(this, R.color.brand_green)
                    : ContextCompat.getColor(this, R.color.white));
        });

        btnTextAlignLeft.setOnClickListener(v -> {
            currentTextAlign = Gravity.START;
            updateTextAlignTints();
        });
        btnTextAlignCenter.setOnClickListener(v -> {
            currentTextAlign = Gravity.CENTER;
            updateTextAlignTints();
        });
        btnTextAlignRight.setOnClickListener(v -> {
            currentTextAlign = Gravity.END;
            updateTextAlignTints();
        });

        textSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int s = Math.max(8, progress);
                currentTextSize = s;
                textSizeValueText.setText(String.valueOf(s));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnTextCancel.setOnClickListener(v -> closeTextPanel());

        btnTextDone.setOnClickListener(v -> {
            String text = textInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập văn bản", Toast.LENGTH_SHORT).show();
                return;
            }
            TextStyleBuilder builder = new TextStyleBuilder();
            builder.withTextSize((float) currentTextSize);
            builder.withTextColor(currentTextColor);
            if (textFontList != null && currentTextFontIndex < textFontList.size()) {
                builder.withTextFont(textFontList.get(currentTextFontIndex).typeface);
            }
            int style = Typeface.NORMAL;
            if (isTextBold && isTextItalic) style = Typeface.BOLD_ITALIC;
            else if (isTextBold) style = Typeface.BOLD;
            else if (isTextItalic) style = Typeface.ITALIC;
            if (style != Typeface.NORMAL) builder.withTextStyle(style);
            builder.withGravity(currentTextAlign | Gravity.CENTER_VERTICAL);
            
            photoEditor.addText(text, builder);
            
            // Áp dụng underline sau khi addText nếu được chọn
            if (isTextUnderline) {
                applyUnderlineToLastTextView();
            }
            
            textInput.setText("");
            closeTextPanel();
        });
    }

    private void openTextPanel() {
        if (textPanel.getVisibility() == View.VISIBLE) {
            closeTextPanel();
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
        if (filterPanel != null && filterPanel.getVisibility() == View.VISIBLE) {
            closeFilterPanel();
        }
        if (brushPanel != null && brushPanel.getVisibility() == View.VISIBLE) {
            closeBrushPanel();
        }
        if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) {
            closeSmartEraserPanel();
        }
        if (maskPanel != null && maskPanel.getVisibility() == View.VISIBLE) {
            closeMaskPanel();
        }
        if (stickerManager != null) stickerManager.closeStickerPanel();
        
        // Reset text formatting
        isTextBold = false;
        isTextItalic = false;
        isTextUnderline = false;
        
        if (textFontRow.getChildCount() == 0) populateTextFontRow();
        if (textColorRow.getChildCount() == 0) populateTextColorRow();
        selectTextTab(currentTextTab);
        updateTextAlignTints();
        // Sync trạng thái nút Bold/Italic/Underline theo state hiện tại
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);
        btnTextBold.setColorFilter(isTextBold ? active : inactive);
        btnTextItalic.setColorFilter(isTextItalic ? active : inactive);
        btnTextUnderline.setColorFilter(isTextUnderline ? active : inactive);
        textSizeSeek.setProgress(currentTextSize);
        textSizeValueText.setText(String.valueOf(currentTextSize));
        textPanel.setVisibility(View.VISIBLE);
    }

    private void closeTextPanel() {
        textPanel.setVisibility(View.GONE);
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && textInput != null) {
            imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
        }
    }

    private void applyUnderlineToLastTextView() {
        // Lấy view cuối cùng được thêm vào photoEditorView
        int childCount = photoEditorView.getChildCount();
        if (childCount == 0) return;
        
        // Tìm TextView cuối cùng (view mới thêm)
        for (int i = childCount - 1; i >= 0; i--) {
            View child = photoEditorView.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                int flags = textView.getPaintFlags();
                // Thêm UNDERLINE_TEXT_FLAG nếu chưa có
                if ((flags & Paint.UNDERLINE_TEXT_FLAG) == 0) {
                    textView.setPaintFlags(flags | Paint.UNDERLINE_TEXT_FLAG);
                }
                break;
            }
        }
    }

    // ============================================================
    // Smart eraser panel — chứa Tẩy AI và Xóa phông nền
    // ============================================================
    private void openSmartEraserPanel() {
        if (smartEraserPanel.getVisibility() == View.VISIBLE) {
            closeSmartEraserPanel();
            return;
        }
        if (adjustPanel != null && adjustPanel.getVisibility() == View.VISIBLE) {
            adjustPanel.setVisibility(View.GONE);
        }
        if (filterPanel != null && filterPanel.getVisibility() == View.VISIBLE) {
            closeFilterPanel();
        }
        if (brushPanel != null && brushPanel.getVisibility() == View.VISIBLE) {
            closeBrushPanel();
        }
        if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) {
            closeTextPanel();
        }
        if (stickerManager != null) stickerManager.closeStickerPanel();
        int active = ContextCompat.getColor(this, R.color.brand_green);
        iconSmartEraser.setColorFilter(active);
        labelSmartEraser.setTextColor(active);
        smartEraserPanel.setVisibility(View.VISIBLE);
    }

    private void closeSmartEraserPanel() {
        smartEraserPanel.setVisibility(View.GONE);
        int inactive = ContextCompat.getColor(this, R.color.white);
        iconSmartEraser.setColorFilter(inactive);
        labelSmartEraser.setTextColor(inactive);
    }

    // ============================================================
    // Mask painting + Tẩy AI (inpainting offline)
    // ============================================================
    private void openMaskPanel() {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để tẩy", Toast.LENGTH_SHORT).show();
            return;
        }
        if (smartEraserPanel != null && smartEraserPanel.getVisibility() == View.VISIBLE) {
            closeSmartEraserPanel();
        }
        if (maskPanel != null && maskPanel.getVisibility() == View.VISIBLE) {
            closeMaskPanel();
        }
        if (textPanel != null && textPanel.getVisibility() == View.VISIBLE) {
            closeTextPanel();
        }
        if (stickerManager != null) stickerManager.closeStickerPanel();
        maskOverlayView.clearMask();
        maskOverlayView.setBrushRadius(MASK_BRUSH_DEFAULT);
        maskBrushSizeSeek.setProgress(MASK_BRUSH_DEFAULT);
        maskBrushSizeValue.setText(String.valueOf(MASK_BRUSH_DEFAULT));
        maskOverlayView.setVisibility(View.VISIBLE);
        maskPanel.setVisibility(View.VISIBLE);
    }

    private void closeMaskPanel() {
        if (maskOverlayView != null) {
            maskOverlayView.clearMask();
            maskOverlayView.setVisibility(View.GONE);
        }
        if (maskPanel != null) maskPanel.setVisibility(View.GONE);
    }

    /** Tính vùng (RectF) trong toạ độ của maskOverlayView nơi ảnh đang hiển thị (đã trừ letterbox). */
    @Nullable
    private RectF computeImageRectInOverlay(Bitmap bmp) {
        if (maskOverlayView == null || bmp == null) return null;
        int vw = maskOverlayView.getWidth();
        int vh = maskOverlayView.getHeight();
        if (vw <= 0 || vh <= 0) return null;
        int iw = bmp.getWidth(), ih = bmp.getHeight();
        if (iw <= 0 || ih <= 0) return null;
        float scale = Math.min((float) vw / iw, (float) vh / ih);
        float dw = iw * scale, dh = ih * scale;
        float ox = (vw - dw) / 2f, oy = (vh - dh) / 2f;
        return new RectF(ox, oy, ox + dw, oy + dh);
    }

    private void applyAiErase() {
        if (isAiErasing) return;
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để tẩy", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!maskOverlayView.hasStrokes()) {
            Toast.makeText(this, "Hãy tô lên vùng cần xóa trước", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        if (current == null) return;

        RectF rect = computeImageRectInOverlay(current);
        if (rect == null) return;

        Bitmap maskBmp = maskOverlayView.exportMaskForImage(rect, current.getWidth(), current.getHeight());
        if (maskBmp == null) {
            Toast.makeText(this, "Không xuất được mask", Toast.LENGTH_SHORT).show();
            return;
        }

        isAiErasing = true;
        Toast.makeText(this, "Đang xử lý Tẩy AI...", Toast.LENGTH_SHORT).show();
        final Bitmap srcCopy = copyBitmap(current);
        new Thread(() -> {
            Bitmap result;
            try {
                result = Inpainter.inpaint(srcCopy, maskBmp);
            } catch (Throwable t) {
                Log.e(TAG, "Inpaint failed", t);
                result = null;
            }
            final Bitmap out = result;
            runOnUiThread(() -> {
                isAiErasing = false;
                maskBmp.recycle();
                if (out == null) {
                    Toast.makeText(this, "Tẩy AI thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveBitmapState();
                photoEditorView.getSource().setImageBitmap(out);
                closeMaskPanel();
                Toast.makeText(this, "Đã tẩy xong", Toast.LENGTH_SHORT).show();
            });
        }, "ai-erase").start();
    }

    // ============================================================
    // Xóa phông nền — ML Kit Subject Segmentation (offline)
    // ============================================================
    private void applyRemoveBackground() {
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để xóa phông", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        if (current == null) return;
        Toast.makeText(this, "Đang tách chủ thể...", Toast.LENGTH_SHORT).show();

        SubjectSegmenterOptions options = new SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .enableForegroundConfidenceMask()
                .build();
        SubjectSegmenter segmenter = SubjectSegmentation.getClient(options);
        InputImage input = InputImage.fromBitmap(current, 0);
        final Bitmap srcRef = current;
        segmenter.process(input)
                .addOnSuccessListener(result -> {
                    Bitmap fg = result.getForegroundBitmap();
                    if (fg == null) {
                        Toast.makeText(this, "Không tách được chủ thể", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // foregroundBitmap có thể nhỏ hơn ảnh gốc → scale lên cho khớp.
                    Bitmap scaled = (fg.getWidth() == srcRef.getWidth()
                            && fg.getHeight() == srcRef.getHeight())
                            ? fg
                            : Bitmap.createScaledBitmap(fg, srcRef.getWidth(), srcRef.getHeight(), true);
                    saveBitmapState();
                    photoEditorView.getSource().setImageBitmap(scaled);
                    closeSmartEraserPanel();
                    Toast.makeText(this, "Đã xóa phông", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Subject segmentation failed", e);
                    Toast.makeText(this,
                            "Xóa phông thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void selectTextTab(int tab) {
        currentTextTab = tab;
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);

        iconTextFont.setColorFilter(tab == TEXT_TAB_FONT ? active : inactive);
        labelTextFont.setTextColor(tab == TEXT_TAB_FONT ? active : inactive);
        iconTextColor.setColorFilter(tab == TEXT_TAB_COLOR ? active : inactive);
        labelTextColor.setTextColor(tab == TEXT_TAB_COLOR ? active : inactive);
        iconTextSize.setColorFilter(tab == TEXT_TAB_SIZE ? active : inactive);
        labelTextSize.setTextColor(tab == TEXT_TAB_SIZE ? active : inactive);
        iconTextFormat.setColorFilter(tab == TEXT_TAB_FORMAT ? active : inactive);
        labelTextFormat.setTextColor(tab == TEXT_TAB_FORMAT ? active : inactive);
        iconTextAlign.setColorFilter(tab == TEXT_TAB_ALIGN ? active : inactive);
        labelTextAlign.setTextColor(tab == TEXT_TAB_ALIGN ? active : inactive);

        textFontPanel.setVisibility(tab == TEXT_TAB_FONT ? View.VISIBLE : View.GONE);
        textColorPanel.setVisibility(tab == TEXT_TAB_COLOR ? View.VISIBLE : View.GONE);
        textSizePanel.setVisibility(tab == TEXT_TAB_SIZE ? View.VISIBLE : View.GONE);
        textFormatPanel.setVisibility(tab == TEXT_TAB_FORMAT ? View.VISIBLE : View.GONE);
        textAlignPanel.setVisibility(tab == TEXT_TAB_ALIGN ? View.VISIBLE : View.GONE);
    }

    private void updateTextAlignTints() {
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);
        btnTextAlignLeft.setColorFilter(currentTextAlign == Gravity.START ? active : inactive);
        btnTextAlignCenter.setColorFilter(currentTextAlign == Gravity.CENTER ? active : inactive);
        btnTextAlignRight.setColorFilter(currentTextAlign == Gravity.END ? active : inactive);
    }

    /**
     * Danh sách 50+ font đại diện cho các nhóm Sans / Serif / Mono / Casual / Cursive.
     */
    private void buildTextFontList() {
        if (textFontList != null) return;
        textFontList = new ArrayList<>();
        String[][] fonts = {
                // Sans-serif family (16 fonts)
                {"sans-serif",                 "Sans",            "0"},
                {"sans-serif",                 "Sans Đậm",        "1"},
                {"sans-serif",                 "Sans Ng.",        "2"},
                {"sans-serif",                 "Sans Đậm/Ng.",    "3"},
                {"sans-serif-light",           "Sans Light",      "0"},
                {"sans-serif-light",           "Sans Light Ng.",  "2"},
                {"sans-serif-thin",            "Sans Thin",       "0"},
                {"sans-serif-thin",            "Sans Thin Ng.",   "2"},
                {"sans-serif-medium",          "Sans Medium",     "0"},
                {"sans-serif-medium",          "Sans Medium Ng.", "2"},
                {"sans-serif-black",           "Sans Black",      "0"},
                {"sans-serif-black",           "Sans Black Ng.",  "2"},
                {"sans-serif-condensed",       "Sans Hẹp",        "0"},
                {"sans-serif-condensed",       "Sans Hẹp Đậm",    "1"},
                {"sans-serif-condensed-light", "Sans Hẹp Light",  "0"},
                {"sans-serif-smallcaps",       "Sans Smallcaps",  "0"},
                // Serif family (12 fonts)
                {"serif",                      "Serif",           "0"},
                {"serif",                      "Serif Đậm",       "1"},
                {"serif",                      "Serif Ng.",       "2"},
                {"serif",                      "Serif Đậm/Ng.",   "3"},
                {"serif",                      "Serif Light",     "0"},
                {"serif",                      "Serif Thin",      "0"},
                {"serif",                      "Serif Medium",    "0"},
                {"serif",                      "Serif Black",     "0"},
                {"serif-monospace",            "Serif Mono",      "0"},
                {"serif-monospace",            "Serif Mono Đậm",  "1"},
                {"serif-monospace",            "Serif Mono Ng.",  "2"},
                {"serif-monospace",            "Serif Mono Đậm/Ng.", "3"},
                // Monospace family (8 fonts)
                {"monospace",                  "Mono",            "0"},
                {"monospace",                  "Mono Đậm",        "1"},
                {"monospace",                  "Mono Ng.",        "2"},
                {"monospace",                  "Mono Đậm/Ng.",    "3"},
                {"monospace",                  "Mono Light",      "0"},
                {"monospace",                  "Mono Thin",       "0"},
                {"monospace",                  "Mono Medium",     "0"},
                {"monospace",                  "Mono Black",      "0"},
                // Casual family (4 fonts)
                {"casual",                     "Casual",          "0"},
                {"casual",                     "Casual Đậm",      "1"},
                {"casual",                     "Casual Ng.",      "2"},
                {"casual",                     "Casual Đậm/Ng.",  "3"},
                // Cursive family (4 fonts)
                {"cursive",                    "Viết tay",        "0"},
                {"cursive",                    "Viết tay Đậm",    "1"},
                {"cursive",                    "Viết tay Ng.",    "2"},
                {"cursive",                    "Viết tay Đậm/Ng.","3"},
                // System fonts (8 fonts)
                {"system-ui",                  "System",          "0"},
                {"ui-monospace",               "UI Mono",         "0"},
                {"ui-rounded",                 "UI Rounded",      "0"},
                {"ui-sans-serif",              "UI Sans",         "0"},
                {"emoji",                      "Emoji",           "0"},
                {"emoji",                      "Emoji Đậm",       "1"},
                {"math",                       "Math",            "0"},
                {"fangsong",                   "Fangsong",        "0"}
        };
        for (String[] f : fonts) {
            int style = Integer.parseInt(f[2]);
            textFontList.add(new FontDef(f[1], Typeface.create(f[0], style)));
        }
    }

    private void populateTextFontRow() {
        textFontRow.removeAllViews();
        buildTextFontList();
        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (10 * density);
        int padV = (int) (4 * density);
        int margin = (int) (4 * density);
        int active = ContextCompat.getColor(this, R.color.brand_green);
        int inactive = ContextCompat.getColor(this, R.color.white);
        for (int i = 0; i < textFontList.size(); i++) {
            FontDef fd = textFontList.get(i);
            final int index = i;
            final TextView item = new TextView(this);
            item.setText(fd.name);
            item.setTypeface(fd.typeface);
            item.setTextSize(15);
            item.setSingleLine(true);
            item.setPadding(padH, padV, padH, padV);
            boolean isSelected = (index == currentTextFontIndex);
            item.setTextColor(isSelected ? active : inactive);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(margin);
            item.setLayoutParams(lp);
            item.setClickable(true);
            item.setFocusable(true);
            if (isSelected) selectedTextFontView = item;
            item.setOnClickListener(v -> {
                currentTextFontIndex = index;
                if (selectedTextFontView != null) {
                    ((TextView) selectedTextFontView).setTextColor(inactive);
                }
                item.setTextColor(active);
                selectedTextFontView = item;
            });
            textFontRow.addView(item);
        }
    }

    private void populateTextColorRow() {
        textColorRow.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int outerSize = (int) (36 * density);
        int innerSize = (int) (24 * density);
        int pad = (int) (2 * density);
        int margin = (int) (4 * density);
        // Mặc định chọn trắng (index 1) — dễ đọc trên ảnh tối
        final int defaultIdx = 1;
        for (int i = 0; i < TEXT_COLORS.length; i++) {
            final int c = TEXT_COLORS[i];
            final android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(outerSize, outerSize);
            clp.setMarginEnd(margin);
            container.setLayoutParams(clp);
            container.setPadding(pad, pad, pad, pad);

            View inner = new View(this);
            android.widget.FrameLayout.LayoutParams ilp =
                    new android.widget.FrameLayout.LayoutParams(innerSize, innerSize);
            ilp.gravity = Gravity.CENTER;
            inner.setLayoutParams(ilp);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(c);
            bg.setStroke((int) (1 * density), 0xFF555555);
            inner.setBackground(bg);
            container.addView(inner);

            container.setClickable(true);
            container.setFocusable(true);
            if (i == defaultIdx) {
                container.setBackgroundResource(R.drawable.bg_brush_color_selected);
                selectedTextColorView = container;
                currentTextColor = c;
            }
            container.setOnClickListener(v -> {
                if (selectedTextColorView != null) {
                    selectedTextColorView.setBackground(null);
                }
                container.setBackgroundResource(R.drawable.bg_brush_color_selected);
                selectedTextColorView = container;
                currentTextColor = c;
            });
            textColorRow.addView(container);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                // Lưu bitmap trước khi crop để undo có thể quay về
                saveBitmapState();
                currentImageUri = resultUri;
                photoEditorView.getSource().setImageURI(currentImageUri);
            }
        } else if (resultCode == RESULT_OK && requestCode == AI_REQUEST_CODE && data != null) {
            String action = data.getStringExtra("action");
            if ("APPLY_FILTER".equals(action)) {
                applyAiFilter(data.getStringExtra("filter_name"));
            } else if ("ADJUST".equals(action)) {
                // Schema mới (đa thuộc tính) — gửi qua 2 mảng song song.
                String[] props = data.getStringArrayExtra("adjust_props");
                int[] values = data.getIntArrayExtra("adjust_values");
                if (props != null && values != null && props.length == values.length && props.length > 0) {
                    for (int i = 0; i < props.length; i++) {
                        applyAiAdjustment(props[i], values[i]);
                    }
                } else {
                    // Fallback schema cũ (đơn thuộc tính) — giữ để tương thích.
                    applyAiAdjustment(data.getStringExtra("property"), data.getIntExtra("value", 0));
                }
            } else if ("OPEN_TOOL".equals(action)) {
                applyAiOpenTool(data.getStringExtra("tool_name"));
            } else if ("REMOVE_BACKGROUND".equals(action)) {
                applyRemoveBackground();
            }
        }
    }

    /**
     * Mở công cụ theo yêu cầu của AI. Map tool_name → mode chỉnh / panel tương ứng.
     */
    private void applyAiOpenTool(String toolName) {
        if (toolName == null) return;
        String t = toolName.toLowerCase().trim();
        // Nhóm 1: các mode trong panel "Chỉnh"
        Integer mode = null;
        switch (t) {
            case "curves":      mode = MODE_CURVES; break;
            case "hsl":         mode = MODE_HSL; break;
            case "highlights":  mode = MODE_HIGHLIGHTS; break;
            case "shadows":     mode = MODE_SHADOWS; break;
            case "brightness":  mode = MODE_BRIGHTNESS; break;
            case "contrast":    mode = MODE_CONTRAST; break;
            case "saturation":  mode = MODE_SATURATION; break;
            case "sharpness":   mode = MODE_SHARPNESS; break;
            case "clarity":     mode = MODE_CLARITY; break;
            case "temperature": mode = MODE_TEMP; break;
            case "hue":         mode = MODE_HUE; break;
            case "fade":        mode = MODE_FADE; break;
            case "vignette":    mode = MODE_VIGNETTE; break;
            case "grain":       mode = MODE_GRAIN; break;
            default: break;
        }
        if (mode != null) {
            if (adjustPanel.getVisibility() != View.VISIBLE) {
                findViewById(R.id.btnAdjust).performClick();
            }
            if (adjustPanel.getVisibility() != View.VISIBLE) return;
            selectAdjustMode(mode);
            Toast.makeText(this, "AI: Đã mở " + toolName, Toast.LENGTH_SHORT).show();
            return;
        }
        // Nhóm 2: các panel khác
        switch (t) {
            case "brush":         findViewById(R.id.btnBrush).performClick(); break;
            case "text":          findViewById(R.id.btnAddText).performClick(); break;
            case "sticker":       findViewById(R.id.btnSticker).performClick(); break;
            case "crop":          findViewById(R.id.btnCrop).performClick(); break;
            case "flip":          findViewById(R.id.btnFlipHorizontal).performClick(); break;
            case "smart_eraser":  findViewById(R.id.btnSmartEraser).performClick(); break;
            case "mask":          openMaskPanel(); break;
            default:
                Toast.makeText(this, "Không hỗ trợ công cụ: " + toolName, Toast.LENGTH_SHORT).show();
                return;
        }
        Toast.makeText(this, "AI: Đã mở " + toolName, Toast.LENGTH_SHORT).show();
    }

    private void applyAiFilter(String filterName) {
        if (filterName == null) return;
        FilterPreset target = null;
        for (FilterPreset.Category category : FilterPreset.CATEGORIES) {
            for (FilterPreset variant : category.variants) {
                if (variant.displayName.equalsIgnoreCase(filterName)) {
                    target = variant;
                    break;
                }
            }
            if (target != null) break;
        }
        if (target == null) {
            Toast.makeText(this, "Không tìm thấy bộ lọc: " + filterName, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(photoEditorView.getSource().getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "Chưa có ảnh để áp dụng bộ lọc", Toast.LENGTH_SHORT).show();
            return;
        }
        saveBitmapState();
        Bitmap current = ((BitmapDrawable) photoEditorView.getSource().getDrawable()).getBitmap();
        Bitmap result = (target.matrix != null)
                ? applyColorMatrixToBitmap(current, target.matrix)
                : copyBitmap(current);
        photoEditorView.getSource().setImageBitmap(result);
        photoEditorView.getSource().clearColorFilter();
        Toast.makeText(this, "AI: Đã áp dụng bộ lọc " + filterName, Toast.LENGTH_SHORT).show();
    }

    private Bitmap applyColorMatrixToBitmap(Bitmap src, float[] matrix) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(matrix)));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    private void applyAiAdjustment(String property, int value) {
        if (property == null) return;
        int internalValue = Math.max(-50, Math.min(50, value));
        int targetMode;
        switch (property.toLowerCase()) {
            case "brightness": brightnessValue = internalValue; targetMode = MODE_BRIGHTNESS; break;
            case "contrast": contrastValue = internalValue; targetMode = MODE_CONTRAST; break;
            case "saturation": saturationValue = internalValue; targetMode = MODE_SATURATION; break;
            case "sharpness": sharpnessValue = internalValue; targetMode = MODE_SHARPNESS; break;
            case "clarity": clarityValue = internalValue; targetMode = MODE_CLARITY; break;
            case "hsl": hslValue = internalValue; targetMode = MODE_HSL; break;
            case "highlights": highlightsValue = internalValue; targetMode = MODE_HIGHLIGHTS; break;
            case "shadows": shadowsValue = internalValue; targetMode = MODE_SHADOWS; break;
            case "temperature": tempValue = internalValue; targetMode = MODE_TEMP; break;
            case "hue": hueValue = internalValue; targetMode = MODE_HUE; break;
            case "fade": fadeValue = internalValue; targetMode = MODE_FADE; break;
            case "vignette": vignetteValue = internalValue; targetMode = MODE_VIGNETTE; break;
            case "grain": grainValue = internalValue; targetMode = MODE_GRAIN; break;
            default:
                Toast.makeText(this, "Không hỗ trợ chỉnh: " + property, Toast.LENGTH_SHORT).show();
                return;
        }
        if (adjustPanel.getVisibility() != View.VISIBLE) {
            findViewById(R.id.btnAdjust).performClick();
        }
        if (adjustPanel.getVisibility() != View.VISIBLE) return;
        selectAdjustMode(targetMode);
        applyColorAdjustments();
        if (isHeavyMode(targetMode)) rebuildConvolutionBitmap();
        Toast.makeText(this, "AI: Đã chỉnh " + property + " = " + internalValue, Toast.LENGTH_SHORT).show();
    }
}
