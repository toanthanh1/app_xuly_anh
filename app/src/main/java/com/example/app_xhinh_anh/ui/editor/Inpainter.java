package com.example.app_xhinh_anh.ui.editor;

import android.graphics.Bitmap;

/**
 * Inpainting offline cho chức năng "Tẩy AI / cục tẩy thông minh".
 *
 * Thuật toán: onion-peel content-aware fill.
 *  1) Hạ kích thước ảnh + mask về tối đa {@link #WORK_MAX_DIM} để chạy nhanh.
 *  2) Lặp: với mỗi pixel mask có ít nhất một hàng xóm non-mask, lấy trung bình
 *     có trọng số (Gaussian 3x3) các hàng xóm non-mask để fill, sau đó coi
 *     pixel đó là non-mask cho lượt tiếp theo. Lặp đến khi mask rỗng hoặc
 *     hết iteration.
 *  3) Làm mượt nhẹ vùng đã fill (vài lượt blur giới hạn trong mask gốc).
 *  4) Upscale kết quả về kích thước gốc và blend chỉ trên vùng mask gốc
 *     (ngoài mask giữ nguyên ảnh gốc — không để mất chi tiết).
 *
 * Cho kết quả đủ tốt với các vết bẩn, đối tượng nhỏ-vừa và chạy hoàn toàn offline.
 */
public final class Inpainter {

    private static final int WORK_MAX_DIM = 512;
    private static final int MAX_ITER = 600;
    private static final int SMOOTH_PASSES = 2;

    private Inpainter() {}

    /**
     * @param src   bitmap gốc (sẽ không bị thay đổi).
     * @param mask  bitmap cùng kích thước với src; pixel được coi là "cần xóa"
     *              khi kênh đỏ &gt; 128 (thường là trắng).
     * @return      bitmap mới đã được fill.
     */
    public static Bitmap inpaint(Bitmap src, Bitmap mask) {
        if (src == null || mask == null) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) return src;

        float scale = Math.min(1f, (float) WORK_MAX_DIM / Math.max(w, h));
        int sw = Math.max(1, Math.round(w * scale));
        int sh = Math.max(1, Math.round(h * scale));

        Bitmap srcSmall = (sw == w && sh == h)
                ? src.copy(Bitmap.Config.ARGB_8888, true)
                : Bitmap.createScaledBitmap(src, sw, sh, true);
        Bitmap maskSmall = (sw == w && sh == h)
                ? mask.copy(Bitmap.Config.ARGB_8888, false)
                : Bitmap.createScaledBitmap(mask, sw, sh, true);

        int[] pix = new int[sw * sh];
        int[] mraw = new int[sw * sh];
        srcSmall.getPixels(pix, 0, sw, 0, 0, sw, sh);
        maskSmall.getPixels(mraw, 0, sw, 0, 0, sw, sh);

        boolean[] isMask = new boolean[sw * sh];
        boolean[] origMask = new boolean[sw * sh];
        int maskCount = 0;
        for (int i = 0; i < sw * sh; i++) {
            boolean m = ((mraw[i] >> 16) & 0xFF) > 128;
            isMask[i] = m;
            origMask[i] = m;
            if (m) maskCount++;
        }
        if (maskCount == 0) {
            srcSmall.recycle();
            maskSmall.recycle();
            return src.copy(Bitmap.Config.ARGB_8888, true);
        }

        onionPeelFill(pix, isMask, sw, sh);
        smoothInsideMask(pix, origMask, sw, sh, SMOOTH_PASSES);

        Bitmap filledSmall = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        filledSmall.setPixels(pix, 0, sw, 0, 0, sw, sh);

        Bitmap filledFull = (sw == w && sh == h)
                ? filledSmall
                : Bitmap.createScaledBitmap(filledSmall, w, h, true);

        // Blend: chỉ thay pixel trong vùng mask gốc, ngoài mask giữ nguyên ảnh gốc.
        Bitmap result = src.copy(Bitmap.Config.ARGB_8888, true);
        int[] origMaskFull = new int[w * h];
        mask.getPixels(origMaskFull, 0, w, 0, 0, w, h);
        int[] filledFullPix = new int[w * h];
        filledFull.getPixels(filledFullPix, 0, w, 0, 0, w, h);
        int[] resPix = new int[w * h];
        result.getPixels(resPix, 0, w, 0, 0, w, h);

        for (int i = 0; i < w * h; i++) {
            int a = (origMaskFull[i] >> 16) & 0xFF; // hệ số alpha biên (0..255)
            if (a == 0) continue;
            if (a >= 250) {
                resPix[i] = filledFullPix[i];
            } else {
                resPix[i] = blend(resPix[i], filledFullPix[i], a);
            }
        }
        result.setPixels(resPix, 0, w, 0, 0, w, h);

        if (filledSmall != filledFull) filledSmall.recycle();
        if (filledFull != result) filledFull.recycle();
        if (srcSmall != src) srcSmall.recycle();
        maskSmall.recycle();
        return result;
    }

    private static void onionPeelFill(int[] pix, boolean[] isMask, int w, int h) {
        // Trọng số Gaussian 3x3 cho 8 hàng xóm (chuẩn hóa khi dùng).
        final int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        final int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
        final int[] wt = {1, 2, 1, 2, 2, 1, 2, 1};

        int[] outPix = new int[pix.length];
        boolean[] toClear = new boolean[isMask.length];

        for (int iter = 0; iter < MAX_ITER; iter++) {
            boolean changed = false;

            for (int y = 0; y < h; y++) {
                int rowBase = y * w;
                for (int x = 0; x < w; x++) {
                    int idx = rowBase + x;
                    if (!isMask[idx]) continue;

                    int sa = 0, sr = 0, sg = 0, sb = 0, sw_ = 0;
                    for (int k = 0; k < 8; k++) {
                        int nx = x + dx[k];
                        int ny = y + dy[k];
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                        int nidx = ny * w + nx;
                        if (isMask[nidx]) continue;
                        int p = pix[nidx];
                        int wk = wt[k];
                        sa += ((p >>> 24) & 0xFF) * wk;
                        sr += ((p >> 16) & 0xFF) * wk;
                        sg += ((p >> 8) & 0xFF) * wk;
                        sb += (p & 0xFF) * wk;
                        sw_ += wk;
                    }
                    if (sw_ > 0) {
                        int a = sa / sw_, r = sr / sw_, g = sg / sw_, b = sb / sw_;
                        outPix[idx] = (a << 24) | (r << 16) | (g << 8) | b;
                        toClear[idx] = true;
                        changed = true;
                    }
                }
            }
            if (!changed) break;

            for (int i = 0; i < isMask.length; i++) {
                if (toClear[i]) {
                    pix[i] = outPix[i];
                    isMask[i] = false;
                    toClear[i] = false;
                }
            }
        }

        // Pixel còn sót lại (nếu có): fill bằng pixel gần nhất bằng quét tia 4 hướng.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!isMask[idx]) continue;
                pix[idx] = nearestNonMask(pix, isMask, w, h, x, y);
            }
        }
    }

    private static int nearestNonMask(int[] pix, boolean[] isMask, int w, int h, int x, int y) {
        int maxR = Math.max(w, h);
        for (int r = 1; r < maxR; r++) {
            int x0 = Math.max(0, x - r), x1 = Math.min(w - 1, x + r);
            int y0 = Math.max(0, y - r), y1 = Math.min(h - 1, y + r);
            // Cạnh trên + dưới
            for (int xi = x0; xi <= x1; xi++) {
                int t = y0 * w + xi;
                if (!isMask[t]) return pix[t];
                int b = y1 * w + xi;
                if (!isMask[b]) return pix[b];
            }
            // Cạnh trái + phải
            for (int yi = y0; yi <= y1; yi++) {
                int l = yi * w + x0;
                if (!isMask[l]) return pix[l];
                int rr = yi * w + x1;
                if (!isMask[rr]) return pix[rr];
            }
        }
        return pix[y * w + x];
    }

    /**
     * Box-blur 3x3 lặp lại, chỉ tác động lên các pixel thuộc mask gốc, dùng nguồn
     * là toàn bộ pix hiện tại để biên hòa với khu vực ngoài.
     */
    private static void smoothInsideMask(int[] pix, boolean[] mask, int w, int h, int passes) {
        if (passes <= 0) return;
        int[] tmp = new int[pix.length];
        for (int p = 0; p < passes; p++) {
            System.arraycopy(pix, 0, tmp, 0, pix.length);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    if (!mask[idx]) continue;
                    int sa = 0, sr = 0, sg = 0, sb = 0, c = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (ny < 0 || ny >= h) continue;
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            if (nx < 0 || nx >= w) continue;
                            int q = tmp[ny * w + nx];
                            sa += (q >>> 24) & 0xFF;
                            sr += (q >> 16) & 0xFF;
                            sg += (q >> 8) & 0xFF;
                            sb += q & 0xFF;
                            c++;
                        }
                    }
                    if (c > 0) {
                        pix[idx] = ((sa / c) << 24) | ((sr / c) << 16) | ((sg / c) << 8) | (sb / c);
                    }
                }
            }
        }
    }

    /** Pha trộn 2 pixel ARGB theo trọng số alpha (0..255) của lớp trên (top). */
    private static int blend(int base, int top, int aTop) {
        int aBase = 255 - aTop;
        int r = (((base >> 16) & 0xFF) * aBase + ((top >> 16) & 0xFF) * aTop) / 255;
        int g = (((base >> 8) & 0xFF) * aBase + ((top >> 8) & 0xFF) * aTop) / 255;
        int b = ((base & 0xFF) * aBase + (top & 0xFF) * aTop) / 255;
        int a = (((base >>> 24) & 0xFF) * aBase + ((top >>> 24) & 0xFF) * aTop) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}