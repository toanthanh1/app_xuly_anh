package com.example.app_xhinh_anh.ui.editor;

/** Preset bộ lọc theo nhóm — mỗi variant là một ColorMatrix 4x5 để intensity slider có thể nội suy. */
public final class FilterPreset {

    public final String displayName;
    /** Có thể null cho mục "không bộ lọc". */
    public final float[] matrix;

    private FilterPreset(String displayName, float[] matrix) {
        this.displayName = displayName;
        this.matrix = matrix;
    }

    public static final class Category {
        public final String name;
        public final FilterPreset[] variants;

        Category(String name, FilterPreset[] variants) {
            this.name = name;
            this.variants = variants;
        }
    }

    // === Trắng đen ===
    private static final float[] BW_CLASSIC = {
            0.30f, 0.59f, 0.11f, 0f,   0f,
            0.30f, 0.59f, 0.11f, 0f,   0f,
            0.30f, 0.59f, 0.11f, 0f,   0f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] BW_PUNCHY = {
            0.45f, 0.85f, 0.10f, 0f, -25f,
            0.45f, 0.85f, 0.10f, 0f, -25f,
            0.45f, 0.85f, 0.10f, 0f, -25f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] BW_SOFT = {
            0.25f, 0.50f, 0.10f, 0f,  25f,
            0.25f, 0.50f, 0.10f, 0f,  25f,
            0.25f, 0.50f, 0.10f, 0f,  25f,
            0f,    0f,    0f,    1f,   0f
    };

    // === Hoài cổ ===
    private static final float[] SEPIA = {
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
    };
    private static final float[] SEPIA_BROWN = {
            0.50f, 0.60f, 0.20f, 0f,  10f,
            0.30f, 0.55f, 0.15f, 0f,   0f,
            0.20f, 0.40f, 0.10f, 0f, -10f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] FADED = {
            0.85f, 0.10f, 0.10f, 0f, 25f,
            0.10f, 0.85f, 0.10f, 0f, 25f,
            0.10f, 0.10f, 0.85f, 0f, 25f,
            0f,    0f,    0f,    1f,  0f
    };

    // === Cổ điển ===
    private static final float[] CLASSIC_POLAROID = {
            1.10f, 0.05f, 0.00f, 0f, 10f,
            0.05f, 1.05f, 0.05f, 0f, 10f,
            0.05f, 0.05f, 0.90f, 0f,  5f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] CLASSIC_OLD = {
            0.95f, 0.05f, 0.05f, 0f,  5f,
            0.10f, 0.85f, 0.10f, 0f,  0f,
            0.10f, 0.20f, 0.65f, 0f, -5f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] CLASSIC_VINTAGE = {
            0.90f, 0.10f, 0.10f, 0f, 15f,
            0.10f, 0.90f, 0.10f, 0f,  5f,
            0.10f, 0.10f, 0.70f, 0f,  0f,
            0f,    0f,    0f,    1f,  0f
    };

    // === Hits (8 sub-filters) ===
    private static final float[] HIT_ROMAN = {
            1.10f, 0.05f, 0.00f, 0f,  10f,
            0.05f, 1.05f, 0.00f, 0f,   5f,
            0.00f, 0.00f, 0.85f, 0f,  10f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] HIT_NEON = {
            1.30f, 0.10f, 0.00f, 0f, -10f,
            0.05f, 1.15f, 0.00f, 0f,  -5f,
            0.00f, 0.00f, 0.80f, 0f, -15f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] HIT_OLD_ROMANTICS = {
            0.85f, 0.10f, 0.10f, 0f, 10f,
            0.10f, 0.80f, 0.10f, 0f,  5f,
            0.15f, 0.10f, 0.85f, 0f, 10f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] HIT_SUMMER_SODA = {
            0.85f, 0.10f, 0.10f, 0f,  0f,
            0.05f, 1.10f, 0.10f, 0f,  5f,
            0.00f, 0.10f, 1.20f, 0f, 10f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] HIT_DREAMY_EMERALD = {
            0.80f, 0.20f, 0.00f, 0f,  0f,
            0.10f, 1.10f, 0.00f, 0f, 10f,
            0.00f, 0.20f, 0.80f, 0f,  0f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] HIT_SOMBER_PICNIC = {
            1.00f, 0.05f, 0.00f, 0f, -5f,
            0.10f, 0.85f, 0.00f, 0f, -5f,
            0.05f, 0.05f, 0.65f, 0f, -5f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] HIT_CARBON_SUMMER = {
            0.95f, 0.00f, 0.05f, 0f, -10f,
            0.00f, 0.95f, 0.05f, 0f, -10f,
            0.05f, 0.05f, 1.05f, 0f,  -5f,
            0f,    0f,    0f,    1f,   0f
    };
    private static final float[] HIT_BRONZE = {
            1.20f, 0.10f, 0.00f, 0f, 10f,
            0.10f, 1.05f, 0.00f, 0f,  5f,
            0.00f, 0.00f, 0.75f, 0f,  5f,
            0f,    0f,    0f,    1f,  0f
    };

    // === Portrait (47 variants) ===
    private static final float[] P_VIVID = {
            1.30f, -0.10f, -0.10f, 0f, -10f,
            -0.10f, 1.30f, -0.10f, 0f, -10f,
            -0.10f, -0.10f, 1.30f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_HD_DARK = {
            1.10f, 0.00f, 0.00f, 0f, -25f,
            0.00f, 1.10f, 0.00f, 0f, -25f,
            0.00f, 0.00f, 1.20f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_PEACH_FUZZ = {
            1.10f, 0.10f, 0.05f, 0f, 10f,
            0.05f, 1.05f, 0.05f, 0f,  8f,
            0.10f, 0.05f, 0.85f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CRYSTAL = {
            0.95f, 0.05f, 0.05f, 0f,  5f,
            0.05f, 1.00f, 0.05f, 0f,  5f,
            0.05f, 0.05f, 1.10f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_METAL = {
            0.50f, 0.30f, 0.20f, 0f,  0f,
            0.30f, 0.50f, 0.20f, 0f,  0f,
            0.20f, 0.30f, 0.50f, 0f,  0f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CRYSTAL_TOUCH = {
            0.98f, 0.02f, 0.05f, 0f,  3f,
            0.02f, 1.02f, 0.05f, 0f,  3f,
            0.02f, 0.02f, 1.05f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_OLD_MONEY = {
            0.95f, 0.10f, 0.00f, 0f,  5f,
            0.05f, 0.90f, 0.00f, 0f,  3f,
            0.00f, 0.10f, 0.85f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_TAN = {
            1.15f, 0.10f, 0.00f, 0f, 10f,
            0.05f, 1.05f, 0.00f, 0f,  5f,
            0.00f, 0.00f, 0.80f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_FRESH = {
            1.05f, 0.00f, 0.00f, 0f, 15f,
            0.00f, 1.05f, 0.00f, 0f, 15f,
            0.00f, 0.00f, 1.05f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CHROME = {
            1.10f, -0.05f, -0.05f, 0f, -10f,
            -0.05f, 1.10f, -0.05f, 0f, -10f,
            -0.05f, -0.05f, 1.20f, 0f,  0f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CLASSY_CHAMPAGNE = {
            1.10f, 0.10f, 0.00f, 0f, 10f,
            0.05f, 1.00f, 0.00f, 0f,  8f,
            0.05f, 0.10f, 0.80f, 0f,  0f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_QUALITY_I = {
            1.15f, 0.00f, 0.00f, 0f,  5f,
            0.00f, 1.10f, 0.00f, 0f,  5f,
            0.00f, 0.00f, 1.05f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_LIGHT_SKIN = {
            1.10f, 0.05f, 0.00f, 0f, 20f,
            0.05f, 1.05f, 0.00f, 0f, 18f,
            0.05f, 0.05f, 0.95f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_MILKY_GREEN = {
            0.85f, 0.20f, 0.05f, 0f, 10f,
            0.10f, 1.10f, 0.10f, 0f, 15f,
            0.05f, 0.20f, 0.85f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_BRINY_WATER = {
            0.80f, 0.10f, 0.10f, 0f,  0f,
            0.05f, 1.00f, 0.10f, 0f,  5f,
            0.10f, 0.15f, 1.10f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_FOCUS = {
            1.25f, -0.05f, -0.05f, 0f, -15f,
            -0.05f, 1.25f, -0.05f, 0f, -15f,
            -0.05f, -0.05f, 1.25f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_MATTE_HIGHLIGHTS = {
            0.85f, 0.00f, 0.00f, 0f, 20f,
            0.00f, 0.85f, 0.00f, 0f, 20f,
            0.00f, 0.00f, 0.90f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_SUNBATH = {
            1.20f, 0.15f, 0.00f, 0f, 15f,
            0.10f, 1.10f, 0.00f, 0f, 10f,
            0.00f, 0.00f, 0.75f, 0f,  0f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_HDR = {
            1.30f, -0.05f, -0.05f, 0f, -10f,
            -0.05f, 1.30f, -0.05f, 0f, -10f,
            -0.05f, -0.05f, 1.30f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_HILIGHT_SHIBUYA = {
            1.10f, 0.10f, 0.05f, 0f, 20f,
            0.05f, 1.05f, 0.10f, 0f, 20f,
            0.10f, 0.10f, 0.95f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CREAM_SIN = {
            1.05f, 0.10f, 0.00f, 0f,  5f,
            0.05f, 1.00f, 0.00f, 0f,  3f,
            0.05f, 0.05f, 0.85f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_QUALITY_II = {
            1.10f, 0.00f, 0.00f, 0f,  3f,
            0.00f, 1.10f, 0.00f, 0f,  5f,
            0.00f, 0.00f, 1.15f, 0f,  8f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_SNOW_WHITE = {
            1.05f, 0.05f, 0.05f, 0f, 25f,
            0.05f, 1.05f, 0.05f, 0f, 25f,
            0.05f, 0.05f, 1.10f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CLEAR = {
            1.05f, 0.00f, 0.00f, 0f,  8f,
            0.00f, 1.05f, 0.00f, 0f,  8f,
            0.00f, 0.00f, 1.05f, 0f,  8f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_ENHANCE = {
            1.10f, 0.00f, 0.00f, 0f,  5f,
            0.00f, 1.15f, 0.00f, 0f,  5f,
            0.00f, 0.00f, 1.10f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_COMPLEXION = {
            1.05f, 0.05f, 0.05f, 0f, 10f,
            0.05f, 1.00f, 0.05f, 0f,  8f,
            0.05f, 0.05f, 0.95f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_INSTA_NOSTALGIA = {
            0.90f, 0.15f, 0.05f, 0f, 15f,
            0.05f, 0.90f, 0.10f, 0f, 10f,
            0.10f, 0.10f, 0.80f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_PEACH_GLOW = {
            1.15f, 0.10f, 0.05f, 0f, 15f,
            0.05f, 1.05f, 0.05f, 0f, 10f,
            0.10f, 0.05f, 0.90f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_RUSSET_GLAZE = {
            1.20f, 0.05f, 0.00f, 0f,  5f,
            0.10f, 0.90f, 0.00f, 0f,  0f,
            0.00f, 0.05f, 0.70f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CALM = {
            0.95f, 0.05f, 0.05f, 0f, 10f,
            0.05f, 0.95f, 0.05f, 0f, 10f,
            0.05f, 0.05f, 0.95f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_MATTE_WHEAT = {
            1.00f, 0.10f, 0.00f, 0f, 15f,
            0.05f, 0.95f, 0.00f, 0f, 12f,
            0.05f, 0.10f, 0.80f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_NATURE = {
            0.90f, 0.10f, 0.00f, 0f,  5f,
            0.05f, 1.05f, 0.05f, 0f, 10f,
            0.00f, 0.10f, 0.85f, 0f,  0f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_COLD_IVORY = {
            0.95f, 0.05f, 0.05f, 0f, 15f,
            0.05f, 1.00f, 0.05f, 0f, 15f,
            0.05f, 0.05f, 1.05f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CREAM = {
            1.10f, 0.10f, 0.00f, 0f, 10f,
            0.05f, 1.05f, 0.00f, 0f,  8f,
            0.05f, 0.10f, 0.85f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_MATTE_WHEAT_2 = {
            0.95f, 0.15f, 0.00f, 0f, 20f,
            0.10f, 0.90f, 0.00f, 0f, 15f,
            0.05f, 0.15f, 0.75f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_HEALTHY_GLOW = {
            1.15f, 0.05f, 0.05f, 0f, 10f,
            0.10f, 1.05f, 0.05f, 0f,  8f,
            0.10f, 0.05f, 0.90f, 0f,  5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_BRONZER = {
            1.25f, 0.10f, 0.00f, 0f,  5f,
            0.10f, 1.05f, 0.00f, 0f,  0f,
            0.00f, 0.00f, 0.65f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_HD_SINLIGHT = {
            1.20f, 0.05f, 0.00f, 0f, -5f,
            0.05f, 1.15f, 0.00f, 0f, -5f,
            0.00f, 0.00f, 1.05f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_CINEMATIC_GLOW = {
            1.15f, 0.05f, 0.00f, 0f,  0f,
            0.05f, 1.05f, 0.00f, 0f, -5f,
            0.00f, 0.00f, 0.85f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_COOL = {
            0.90f, 0.00f, 0.00f, 0f, -5f,
            0.00f, 0.95f, 0.00f, 0f,  0f,
            0.00f, 0.05f, 1.15f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_GLOW = {
            1.10f, 0.05f, 0.05f, 0f, 20f,
            0.05f, 1.10f, 0.05f, 0f, 20f,
            0.05f, 0.05f, 1.05f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_RADIANT = {
            1.20f, 0.00f, 0.00f, 0f, 10f,
            0.00f, 1.15f, 0.00f, 0f, 10f,
            0.00f, 0.00f, 1.10f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_NOSTALGIC_NEG = {
            0.85f, 0.10f, 0.10f, 0f, 20f,
            0.10f, 0.85f, 0.10f, 0f, 18f,
            0.10f, 0.10f, 0.85f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_SALT = {
            0.95f, 0.00f, 0.05f, 0f, 10f,
            0.00f, 0.95f, 0.05f, 0f, 12f,
            0.05f, 0.05f, 1.05f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_ROBUST = {
            1.20f, 0.05f, 0.00f, 0f, -5f,
            0.05f, 1.15f, 0.00f, 0f, -8f,
            0.00f, 0.05f, 1.00f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_SUMMER_NOSTALGIA = {
            1.10f, 0.10f, 0.05f, 0f, 20f,
            0.10f, 1.05f, 0.05f, 0f, 18f,
            0.10f, 0.10f, 0.85f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
    };
    private static final float[] P_MULLED_WINE = {
            1.15f, 0.00f, 0.00f, 0f, -10f,
            0.05f, 0.85f, 0.00f, 0f, -15f,
            0.00f, 0.00f, 0.80f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
    };

    // === Texture ===
    private static final float[] TEXTURE_PUNCHY = {
            1.20f,  0.00f,  0.00f, 0f, -15f,
            0.00f,  1.20f,  0.00f, 0f, -15f,
            0.00f,  0.00f,  1.20f, 0f, -15f,
            0f,     0f,     0f,    1f,  0f
    };
    private static final float[] TEXTURE_MATTE = {
            0.85f, 0.05f, 0.05f, 0f, 25f,
            0.05f, 0.85f, 0.05f, 0f, 25f,
            0.05f, 0.05f, 0.85f, 0f, 25f,
            0f,    0f,    0f,    1f,  0f
    };
    private static final float[] TEXTURE_CRISP = {
            1.30f, -0.10f, -0.10f, 0f, -10f,
            -0.10f, 1.30f, -0.10f, 0f, -10f,
            -0.10f, -0.10f, 1.30f, 0f, -10f,
            0f,     0f,     0f,    1f,   0f
    };

    public static final Category[] CATEGORIES = new Category[]{
            new Category("Trắng đen", new FilterPreset[]{
                    new FilterPreset("Classic", BW_CLASSIC),
                    new FilterPreset("Punchy", BW_PUNCHY),
                    new FilterPreset("Soft", BW_SOFT)
            }),
            new Category("Hoài cổ", new FilterPreset[]{
                    new FilterPreset("Sepia", SEPIA),
                    new FilterPreset("Brown", SEPIA_BROWN),
                    new FilterPreset("Faded", FADED)
            }),
            new Category("Cổ điển", new FilterPreset[]{
                    new FilterPreset("Polaroid", CLASSIC_POLAROID),
                    new FilterPreset("Old Photo", CLASSIC_OLD),
                    new FilterPreset("Vintage", CLASSIC_VINTAGE)
            }),
            new Category("Hits", new FilterPreset[]{
                    new FilterPreset("Roman Holiday", HIT_ROMAN),
                    new FilterPreset("Neon Fire", HIT_NEON),
                    new FilterPreset("Old Romantics", HIT_OLD_ROMANTICS),
                    new FilterPreset("Summer Soda", HIT_SUMMER_SODA),
                    new FilterPreset("Dreamy Emerald", HIT_DREAMY_EMERALD),
                    new FilterPreset("Somber Picnic", HIT_SOMBER_PICNIC),
                    new FilterPreset("Carbon Summer", HIT_CARBON_SUMMER),
                    new FilterPreset("Bronze Highlight", HIT_BRONZE)
            }),
            new Category("Portrait", new FilterPreset[]{
                    new FilterPreset("Vivid", P_VIVID),
                    new FilterPreset("HD Dark", P_HD_DARK),
                    new FilterPreset("Peach Fuzz", P_PEACH_FUZZ),
                    new FilterPreset("Crystal", P_CRYSTAL),
                    new FilterPreset("Metal", P_METAL),
                    new FilterPreset("Crystal Touch", P_CRYSTAL_TOUCH),
                    new FilterPreset("Old Money", P_OLD_MONEY),
                    new FilterPreset("Tan", P_TAN),
                    new FilterPreset("Fresh", P_FRESH),
                    new FilterPreset("Chrome", P_CHROME),
                    new FilterPreset("Classy Champagne", P_CLASSY_CHAMPAGNE),
                    new FilterPreset("Quality I", P_QUALITY_I),
                    new FilterPreset("Light Skin", P_LIGHT_SKIN),
                    new FilterPreset("Milky Green", P_MILKY_GREEN),
                    new FilterPreset("Briny Water", P_BRINY_WATER),
                    new FilterPreset("Focus", P_FOCUS),
                    new FilterPreset("Matte Highlights", P_MATTE_HIGHLIGHTS),
                    new FilterPreset("Sunbath", P_SUNBATH),
                    new FilterPreset("HDR", P_HDR),
                    new FilterPreset("Hi-light Shibuya", P_HILIGHT_SHIBUYA),
                    new FilterPreset("Cream Sin", P_CREAM_SIN),
                    new FilterPreset("Quality II", P_QUALITY_II),
                    new FilterPreset("Snow White", P_SNOW_WHITE),
                    new FilterPreset("Clear", P_CLEAR),
                    new FilterPreset("Enhance", P_ENHANCE),
                    new FilterPreset("Complexion", P_COMPLEXION),
                    new FilterPreset("Insta Nostalgia", P_INSTA_NOSTALGIA),
                    new FilterPreset("Peach Glow", P_PEACH_GLOW),
                    new FilterPreset("Russet Glaze", P_RUSSET_GLAZE),
                    new FilterPreset("Calm", P_CALM),
                    new FilterPreset("Matte Wheat", P_MATTE_WHEAT),
                    new FilterPreset("Nature", P_NATURE),
                    new FilterPreset("Cold Ivory", P_COLD_IVORY),
                    new FilterPreset("Cream", P_CREAM),
                    new FilterPreset("Matte Wheat 2", P_MATTE_WHEAT_2),
                    new FilterPreset("Healthy Glow", P_HEALTHY_GLOW),
                    new FilterPreset("Bronzer", P_BRONZER),
                    new FilterPreset("HD Sinlight", P_HD_SINLIGHT),
                    new FilterPreset("Cinematic Glow", P_CINEMATIC_GLOW),
                    new FilterPreset("Cool", P_COOL),
                    new FilterPreset("Glow", P_GLOW),
                    new FilterPreset("Radiant", P_RADIANT),
                    new FilterPreset("Nostalgic Negative", P_NOSTALGIC_NEG),
                    new FilterPreset("Salt", P_SALT),
                    new FilterPreset("Robust", P_ROBUST),
                    new FilterPreset("Summer Nostalgia", P_SUMMER_NOSTALGIA),
                    new FilterPreset("Mulled Wine", P_MULLED_WINE)
            }),
            new Category("Texture", new FilterPreset[]{
                    new FilterPreset("Punchy", TEXTURE_PUNCHY),
                    new FilterPreset("Matte", TEXTURE_MATTE),
                    new FilterPreset("Crisp", TEXTURE_CRISP)
            })
    };
}