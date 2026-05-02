package com.example.app_xhinh_anh.processing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class ImageProcessor {

    public static Bitmap applyColorMatrix(Bitmap src, ColorMatrix cm) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    public static Bitmap applyColorMatrix(Bitmap src, float[] matrix) {
        return applyColorMatrix(src, new ColorMatrix(matrix));
    }

    public static Bitmap applyLut(Bitmap src, int[] lut) {
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

    public static Bitmap applyConvolution(Bitmap src, float[] kernel, int kSize) {
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

    public static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static ColorMatrix buildHueMatrix(float degrees) {
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

    /**
     * Tạo ColorMatrix cho nhiệt độ màu (Temperature).
     * @param value -50 (Lạnh/Xanh) đến 50 (Ấm/Vàng)
     */
    public static ColorMatrix buildTemperatureMatrix(float value) {
        float shift = value / 100f; // -0.5 to 0.5
        return new ColorMatrix(new float[]{
                1 + shift, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1 - shift, 0, 0,
                0, 0, 0, 1, 0
        });
    }

    /**
     * Tạo ColorMatrix cho độ phơi sáng (Exposure).
     * @param value -50 đến 50
     */
    public static ColorMatrix buildExposureMatrix(float value) {
        float scale = (float) Math.pow(2, value / 50f);
        return new ColorMatrix(new float[]{
                scale, 0, 0, 0, 0,
                0, scale, 0, 0, 0,
                0, 0, scale, 0, 0,
                0, 0, 0, 1, 0
        });
    }

    public static Bitmap copyBitmap(Bitmap src) {
        if (src == null) return null;
        Bitmap.Config config = src.getConfig();
        if (config == null) config = Bitmap.Config.ARGB_8888;
        return src.copy(config, true);
    }

    public static Bitmap makePreviewBitmap(Bitmap src, int maxDim) {
        if (src == null) return null;
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

    public static int[] buildToneLut(float curves, float highlights, float shadows) {
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
                float maskS = Math.max(0f, 1 - 2f * x);
                y = y + shadows * 0.4f * maskS;
            }
            lut[i] = clampByte(Math.round(y * 255f));
        }
        return lut;
    }

    public static Bitmap applySharpness(Bitmap src, float strength) {
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

    public static Bitmap applyClarity(Bitmap src, float strength) {
        if (Math.abs(strength) < 0.01f) return src;
        float s = strength;
        float[] kernel = new float[]{
                -s * 0.5f, -s,            -s * 0.5f,
                -s,        1 + 6 * s,     -s,
                -s * 0.5f, -s,            -s * 0.5f
        };
        return applyConvolution(src, kernel, 3);
    }

    public static float[] lerpToIdentity(float[] target, float t) {
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
}
