package com.example.app_xhinh_anh.processing.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.databinding.ActivityEditorBinding;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class StickerManager {

    private final AppCompatActivity activity;
    private final ActivityEditorBinding binding;
    private final PhotoEditor photoEditor;
    private TextView selectedCategoryTabView;

    public StickerManager(AppCompatActivity activity, ActivityEditorBinding binding, PhotoEditor photoEditor) {
        this.activity = activity;
        this.binding = binding;
        this.photoEditor = photoEditor;

        binding.btnStickerDone.setOnClickListener(v -> closeStickerPanel());
    }

    public void openStickerPanel() {
        populateCategoryTabs();
        selectCategory(CATEGORIES[0], (TextView) binding.stickerCategoryTabs.getChildAt(0));
        binding.stickerPanel.setVisibility(View.VISIBLE);
    }

    public void closeStickerPanel() {
        binding.stickerPanel.setVisibility(View.GONE);
    }

    public boolean isPanelVisible() {
        return binding.stickerPanel.getVisibility() == View.VISIBLE;
    }

    private void populateCategoryTabs() {
        binding.stickerCategoryTabs.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (StickerCategory category : CATEGORIES) {
            TextView tab = (TextView) inflater.inflate(R.layout.item_filter_category_tab, binding.stickerCategoryTabs, false);
            tab.setText(category.name);
            tab.setOnClickListener(v -> selectCategory(category, tab));
            binding.stickerCategoryTabs.addView(tab);
        }
    }

    private void selectCategory(StickerCategory category, TextView tabView) {
        if (selectedCategoryTabView != null && selectedCategoryTabView != tabView) {
            selectedCategoryTabView.setTextColor(ContextCompat.getColor(activity, R.color.white));
        }
        tabView.setTextColor(ContextCompat.getColor(activity, R.color.brand_green));
        selectedCategoryTabView = tabView;
        populateStickers(category);
    }

    private void populateStickers(StickerCategory category) {
        binding.stickerList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        
        int targetWidth = calculateTargetWidth();

        for (int stickerResId : category.stickerResIds) {
            View item = inflater.inflate(R.layout.item_filter_thumb, binding.stickerList, false);
            ImageView thumb = item.findViewById(R.id.filterThumb);
            TextView name = item.findViewById(R.id.filterName);
            name.setVisibility(View.GONE);

            // Hiển thị thumbnail
            if (category.name.equals("Animal")) {
                loadAndTintSticker(stickerResId, thumb, true);
            } else {
                // Sử dụng Glide để load thumbnail mượt hơn và đúng scale
                Glide.with(activity).load(stickerResId).into(thumb);
            }
            
            item.setOnClickListener(v -> {
                Glide.with(activity)
                        .asBitmap()
                        .load(stickerResId)
                        .override(targetWidth) 
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                Bitmap finalBitmap = resource;
                                if (category.name.equals("Animal")) {
                                    finalBitmap = tintBitmapToWhite(resource);
                                }
                                photoEditor.addImage(finalBitmap);
                            }
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            });
            binding.stickerList.addView(item);
        }
    }

    private int calculateTargetWidth() {
        if (binding.photoEditorView.getSource().getDrawable() != null) {
            int imageWidth = binding.photoEditorView.getSource().getDrawable().getIntrinsicWidth();
            // Thu nhỏ sticker xuống 1/6 chiều rộng ảnh để không quá to
            return Math.max(150, imageWidth / 6);
        }
        return 300; // Giá trị mặc định an toàn
    }

    private void loadAndTintSticker(int resId, ImageView imageView, boolean toWhite) {
        Glide.with(activity)
                .asBitmap()
                .load(resId)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (toWhite) {
                            imageView.setImageBitmap(tintBitmapToWhite(resource));
                        } else {
                            imageView.setImageBitmap(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private Bitmap tintBitmapToWhite(Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    private static class StickerCategory {
        String name;
        int[] stickerResIds;
        StickerCategory(String name, int[] stickerResIds) {
            this.name = name;
            this.stickerResIds = stickerResIds;
        }
    }

    private static final StickerCategory[] CATEGORIES = new StickerCategory[]{
            new StickerCategory("Logo", new int[]{
                    R.drawable.sticker_logo_1, R.drawable.sticker_logo_2, R.drawable.sticker_logo_3,
                    R.drawable.sticker_logo_4, R.drawable.sticker_logo_5, R.drawable.sticker_logo_6,
                    R.drawable.sticker_logo_7, R.drawable.sticker_logo_8, R.drawable.sticker_logo_9,
                    R.drawable.sticker_logo_10
            }),
            new StickerCategory("Emoji", new int[]{
                    R.drawable.sticker_emoji_smile, R.drawable.sticker_emoji_happy, R.drawable.sticker_emoji_laugh,
                    R.drawable.sticker_emoji_wink, R.drawable.sticker_emoji_love, R.drawable.sticker_emoji_cool,
                    R.drawable.sticker_emoji_star, R.drawable.sticker_emoji_smiling, R.drawable.sticker_emoji_smiley,
                    R.drawable.sticker_emoji_grinning, R.drawable.sticker_emoji_surprised, R.drawable.sticker_emoji_confused,
                    R.drawable.sticker_emoji_confusing, R.drawable.sticker_emoji_scare, R.drawable.sticker_emoji_sad,
                    R.drawable.sticker_emoji_angry, R.drawable.sticker_emoji_sick, R.drawable.sticker_emoji_sleep,
                    R.drawable.sticker_emoji_tired, R.drawable.sticker_emoji_vain
            }),
            new StickerCategory("Animal", new int[]{
                    R.drawable.sticker_animal_bird, R.drawable.sticker_animal_lion, R.drawable.sticker_animal_shark,
                    R.drawable.sticker_animal_turtle, R.drawable.sticker_animal_chicken, R.drawable.sticker_animal_chameleon,
                    R.drawable.sticker_animal_dragonfly, R.drawable.sticker_animal_caterpillar, R.drawable.sticker_animal_fox,
                    R.drawable.sticker_animal_owl
            })
    };
}
