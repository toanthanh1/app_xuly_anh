package com.example.app_xhinh_anh.ui.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Cho phép người dùng tô lên các vùng cần "Tẩy AI".
 * - Lưu mask thật (alpha trắng) trong maskBitmap để xuất cho thuật toán inpaint.
 * - Khi onDraw, nhuộm các nét trắng đó thành đỏ trong suốt bằng PorterDuffColorFilter
 *   (SRC_IN: giữ alpha của bitmap nguồn nhưng thay màu RGB bằng màu của paint).
 */
public class MaskOverlayView extends View {

    private static final int OVERLAY_COLOR = 0x99FF3D3D; // đỏ ~60% alpha

    private Bitmap maskBitmap;
    private Canvas maskCanvas;
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private float lastX, lastY;
    private float brushRadius = 30f;
    private boolean hasStrokes = false;

    public MaskOverlayView(Context context) {
        super(context);
        init();
    }

    public MaskOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Stroke paint vẽ vào maskBitmap: full opaque white để mask sạch.
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStrokeWidth(brushRadius * 2f);

        // Overlay paint: nhuộm vùng có alpha của maskBitmap thành đỏ trong suốt.
        overlayPaint.setColorFilter(new PorterDuffColorFilter(OVERLAY_COLOR, PorterDuff.Mode.SRC_IN));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            maskCanvas = new Canvas(maskBitmap);
            hasStrokes = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (maskBitmap == null) return;
        canvas.drawBitmap(maskBitmap, 0, 0, overlayPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (maskCanvas == null) return false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                maskCanvas.drawCircle(x, y, brushRadius, strokePaint);
                lastX = x;
                lastY = y;
                hasStrokes = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                maskCanvas.drawLine(lastX, lastY, x, y, strokePaint);
                maskCanvas.drawCircle(x, y, brushRadius, strokePaint);
                lastX = x;
                lastY = y;
                hasStrokes = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
        }
        return false;
    }

    public void setBrushRadius(float r) {
        brushRadius = Math.max(4f, r);
        strokePaint.setStrokeWidth(brushRadius * 2f);
    }

    public void clearMask() {
        if (maskBitmap != null) {
            maskBitmap.eraseColor(Color.TRANSPARENT);
            hasStrokes = false;
            invalidate();
        }
    }

    public boolean hasStrokes() {
        return hasStrokes;
    }

    /**
     * Xuất mask theo kích thước ảnh đích.
     * @param imageRectInView vùng trong view nơi ảnh đang hiển thị (đã trừ letterbox).
     * @return bitmap mask cùng kích thước ảnh: trắng = vùng cần xóa, đen = giữ nguyên.
     */
    @Nullable
    public Bitmap exportMaskForImage(RectF imageRectInView, int imageWidth, int imageHeight) {
        if (maskBitmap == null || imageWidth <= 0 || imageHeight <= 0) return null;
        if (imageRectInView.width() <= 0f || imageRectInView.height() <= 0f) return null;

        Bitmap out = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        out.eraseColor(Color.BLACK);
        Canvas c = new Canvas(out);

        Matrix m = new Matrix();
        float sx = imageWidth / imageRectInView.width();
        float sy = imageHeight / imageRectInView.height();
        m.postTranslate(-imageRectInView.left, -imageRectInView.top);
        m.postScale(sx, sy);

        // Vẽ maskBitmap (alpha trắng) lên nền đen với colorFilter SRC_IN trắng
        // để chắc chắn mọi pixel có alpha đều thành trắng đặc.
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        c.drawBitmap(maskBitmap, m, p);
        return out;
    }
}