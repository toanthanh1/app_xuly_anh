package com.example.app_xhinh_anh.ui.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Bọc PhotoEditorView (và overlay) để hỗ trợ pinch-to-zoom + pan 2 ngón.
 * - 1 ngón: pass-through cho child (brush, mask, drag text/sticker, ...).
 * - 2 ngón: pinch để phóng to/thu nhỏ, kéo cùng chiều để di chuyển ảnh.
 * - Double-tap: đặt lại zoom về 1x.
 */
public class ZoomableFrameLayout extends FrameLayout {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 6f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector tapDetector;

    private float currentScale = 1f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean isScaling = false;
    private boolean twoFingerActive = false;
    private float lastFocusX, lastFocusY;

    public ZoomableFrameLayout(Context context) {
        this(context, null);
    }

    public ZoomableFrameLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        scaleDetector.setQuickScaleEnabled(false);
        tapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > 1f) {
                    resetZoom();
                } else {
                    currentScale = 2f;
                    clampPan();
                    applyTransform();
                }
                return true;
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float next = currentScale * detector.getScaleFactor();
            currentScale = Math.max(MIN_SCALE, Math.min(next, MAX_SCALE));
            clampPan();
            applyTransform();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
            clampPan();
            applyTransform();
        }
    }

    private void applyTransform() {
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);
        setScaleX(currentScale);
        setScaleY(currentScale);
        setTranslationX(panX);
        setTranslationY(panY);
    }

    private void clampPan() {
        float maxPanX = (getWidth() * (currentScale - 1f)) / 2f;
        float maxPanY = (getHeight() * (currentScale - 1f)) / 2f;
        if (maxPanX < 0f) maxPanX = 0f;
        if (maxPanY < 0f) maxPanY = 0f;
        panX = Math.max(-maxPanX, Math.min(panX, maxPanX));
        panY = Math.max(-maxPanY, Math.min(panY, maxPanY));
    }

    public void resetZoom() {
        currentScale = 1f;
        panX = 0f;
        panY = 0f;
        applyTransform();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Double-tap để reset / nhanh phóng 2x
        tapDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();

        if (action == MotionEvent.ACTION_POINTER_DOWN && ev.getPointerCount() == 2) {
            twoFingerActive = true;
            lastFocusX = (ev.getX(0) + ev.getX(1)) / 2f;
            lastFocusY = (ev.getY(0) + ev.getY(1)) / 2f;
            // Hủy thao tác 1-ngón đang dở của child (vd: nét brush vừa bắt đầu).
            cancelChildTouches(ev);
        }

        if (twoFingerActive) {
            scaleDetector.onTouchEvent(ev);

            if (action == MotionEvent.ACTION_MOVE
                    && ev.getPointerCount() >= 2
                    && !isScaling
                    && currentScale > 1f) {
                float fx = (ev.getX(0) + ev.getX(1)) / 2f;
                float fy = (ev.getY(0) + ev.getY(1)) / 2f;
                panX += (fx - lastFocusX);
                panY += (fy - lastFocusY);
                clampPan();
                applyTransform();
                lastFocusX = fx;
                lastFocusY = fy;
            } else if (action == MotionEvent.ACTION_POINTER_UP && ev.getPointerCount() == 2) {
                int remaining = ev.getActionIndex() == 0 ? 1 : 0;
                lastFocusX = ev.getX(remaining);
                lastFocusY = ev.getY(remaining);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                twoFingerActive = false;
            }
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    private void cancelChildTouches(MotionEvent original) {
        MotionEvent cancel = MotionEvent.obtain(original.getDownTime(), original.getEventTime(),
                MotionEvent.ACTION_CANCEL, original.getX(), original.getY(), 0);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
    }
}