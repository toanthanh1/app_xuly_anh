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
    private boolean firstTouchDown = false;

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
                    zoomTo(2f, e.getX(), e.getY());
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
            zoomTo(currentScale * detector.getScaleFactor(),
                    detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
            clampPan();
            applyTransform();
        }
    }

    /**
     * Đặt zoom tới {@code targetScale} đồng thời giữ điểm (focusX, focusY) trong toạ độ
     * view ổn định trên màn hình — đây là cách pinch-to-zoom "tự nhiên".
     *
     * Vị trí trên màn hình của 1 điểm (vx, vy) sau biến đổi của View (pivot ở giữa view) là:
     *     screen = (v - pivot) * scale + pivot + pan
     * Muốn screen không đổi khi scale S → S':
     *     pan' = pan + (v - pivot) * (S - S')
     */
    private void zoomTo(float targetScale, float focusX, float focusY) {
        float oldScale = currentScale;
        float newScale = Math.max(MIN_SCALE, Math.min(targetScale, MAX_SCALE));
        if (newScale == oldScale) return;
        float pivotX = getWidth() / 2f;
        float pivotY = getHeight() / 2f;
        panX += (focusX - pivotX) * (oldScale - newScale);
        panY += (focusY - pivotY) * (oldScale - newScale);
        currentScale = newScale;
        clampPan();
        applyTransform();
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // ⭐ Theo dõi ngón tay đầu tiên
                firstTouchDown = true;
                twoFingerActive = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // ⭐ Ngón tay thứ 2 xuống → activate pinch mode
                if (ev.getPointerCount() >= 2) {
                    twoFingerActive = true;
                    lastFocusX = (ev.getX(0) + ev.getX(1)) / 2f;
                    lastFocusY = (ev.getY(0) + ev.getY(1)) / 2f;
                    // Hủy touch handler của child
                    MotionEvent cancelEvent = MotionEvent.obtain(ev);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    return true;  // Chặn child nhận ngón tay thứ 2
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                firstTouchDown = false;
                twoFingerActive = false;
                break;
        }

        // Nếu đang pinch, chặn child từ nhận events
        if (twoFingerActive) {
            return true;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        // Double-tap chỉ có nghĩa khi 1 ngón
        if (!twoFingerActive && firstTouchDown && ev.getPointerCount() == 1) {
            tapDetector.onTouchEvent(ev);
        }

        if (twoFingerActive) {
            // ⭐ Cho ScaleGestureDetector xử lý 2 ngón
            scaleDetector.onTouchEvent(ev);

            // Pan khi có 2 ngón nhưng không phóng to/thu nhỏ
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
                // Cập nhật focus khi một ngón rời
                int remaining = ev.getActionIndex() == 0 ? 1 : 0;
                lastFocusX = ev.getX(remaining);
                lastFocusY = ev.getY(remaining);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                twoFingerActive = false;
                firstTouchDown = false;
            }

            return true;  // Consume event khi đang pinch
        }

        return super.dispatchTouchEvent(ev);
    }
}