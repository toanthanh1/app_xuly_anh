package com.example.app_xhinh_anh.ui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Vẽ thước đo có vạch chia (major + minor) đặt phía trên SeekBar điều chỉnh. */
public class RulerView extends View {

    private static final int SEGMENTS = 50;     // 51 vạch (gồm cả 2 đầu)
    private static final int MAJOR_EVERY = 5;   // cứ 5 ô là một vạch lớn

    private static final int COLOR_CENTER = 0xFFFFFFFF;
    private static final int COLOR_MAJOR  = 0xFFCCCCCC;
    private static final int COLOR_MINOR  = 0xFF777777;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        // Bù trừ độ rộng thumb của SeekBar để các vạch khớp với khoảng chạy thực tế
        float padding = 12f * density;
        float usable = w - 2f * padding;
        if (usable <= 0) return;

        float majorLen = h * 0.85f;
        float minorLen = h * 0.45f;
        float centerLen = h;

        for (int i = 0; i <= SEGMENTS; i++) {
            float x = padding + usable * i / SEGMENTS;
            boolean isCenter = i == SEGMENTS / 2;
            boolean isMajor = i % MAJOR_EVERY == 0;
            float len;
            if (isCenter) {
                paint.setColor(COLOR_CENTER);
                paint.setStrokeWidth(density * 1.5f);
                len = centerLen;
            } else if (isMajor) {
                paint.setColor(COLOR_MAJOR);
                paint.setStrokeWidth(density);
                len = majorLen;
            } else {
                paint.setColor(COLOR_MINOR);
                paint.setStrokeWidth(density * 0.8f);
                len = minorLen;
            }
            canvas.drawLine(x, h - len, x, h, paint);
        }
    }
}