package com.example.app_xhinh_anh.processing.tools;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.databinding.ActivityEditorBinding;
import com.example.app_xhinh_anh.ui.editor.ColorPickerAdapter;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;

public class TextManager {

    private final AppCompatActivity activity;
    private final ActivityEditorBinding binding;
    private final PhotoEditor photoEditor;

    private View currentSelectedTextViewRoot;
    private int currentTextColor = Color.WHITE;
    private int currentTextBgColor = Color.TRANSPARENT;
    private boolean isTextBold = false;
    private boolean isTextItalic = false;
    private boolean isTextUnderline = false;
    private Typeface currentBaseTypeface = Typeface.DEFAULT;

    public TextManager(AppCompatActivity activity, ActivityEditorBinding binding, PhotoEditor photoEditor) {
        this.activity = activity;
        this.binding = binding;
        this.photoEditor = photoEditor;
        initViews();
    }

    private void initViews() {
        setupTextStylingPanel();
    }

    private void setupTextStylingPanel() {
        final int activeColor = ContextCompat.getColor(activity, R.color.brand_green);
        final int inactiveColor = Color.parseColor("#888888");

        // Color Adapters
        binding.rvTextColors.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        ColorPickerAdapter textCP = new ColorPickerAdapter(activity);
        textCP.setOnColorPickerClickListener(color -> {
            currentTextColor = color;
            applyStylesToCurrentText();
        });
        binding.rvTextColors.setAdapter(textCP);

        binding.rvTextBgColors.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        ColorPickerAdapter bgCP = new ColorPickerAdapter(activity);
        bgCP.setOnColorPickerClickListener(color -> {
            currentTextBgColor = color;
            applyStylesToCurrentText();
        });
        binding.rvTextBgColors.setAdapter(bgCP);

        // Fonts
        String[] fontNames = {"Mặc định", "Serif", "Monospace", "Siêu đậm", "Vừa", "Mỏng", "Hẹp", "Be Vietnam", "Patrick Hand"};
        
        Typeface[] baseTypefaces = new Typeface[fontNames.length];
        baseTypefaces[0] = Typeface.DEFAULT;
        baseTypefaces[1] = Typeface.SERIF;
        baseTypefaces[2] = Typeface.MONOSPACE;
        baseTypefaces[3] = Typeface.create("sans-serif-black", Typeface.NORMAL);
        baseTypefaces[4] = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        baseTypefaces[5] = Typeface.create("sans-serif-light", Typeface.NORMAL);
        baseTypefaces[6] = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        
        try {
            baseTypefaces[7] = ResourcesCompat.getFont(activity, R.font.be_vietnam_pro);
            baseTypefaces[8] = ResourcesCompat.getFont(activity, R.font.patrick_hand);
        } catch (Exception e) {
            baseTypefaces[7] = Typeface.DEFAULT;
            baseTypefaces[8] = Typeface.DEFAULT;
        }
        
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < fontNames.length; i++) {
            final Typeface tf = baseTypefaces[i];
            TextView fontView = (TextView) inflater.inflate(R.layout.item_filter_category_tab, binding.textFontList, false);
            fontView.setText(fontNames[i]);
            fontView.setOnClickListener(v -> {
                currentBaseTypeface = tf != null ? tf : Typeface.DEFAULT;
                applyStylesToCurrentText();
            });
            binding.textFontList.addView(fontView);
        }

        binding.btnStyleBold.setOnClickListener(v -> {
            isTextBold = !isTextBold;
            binding.btnStyleBold.setTextColor(isTextBold ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });
        binding.btnStyleItalic.setOnClickListener(v -> {
            isTextItalic = !isTextItalic;
            binding.btnStyleItalic.setTextColor(isTextItalic ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });
        binding.btnStyleUnderline.setOnClickListener(v -> {
            isTextUnderline = !isTextUnderline;
            binding.btnStyleUnderline.setTextColor(isTextUnderline ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });

        // Tabs
        binding.tabTextBtnColor.setOnClickListener(v -> {
            binding.textPanelColor.setVisibility(View.VISIBLE);
            binding.textPanelBackground.setVisibility(View.GONE);
            binding.textPanelFont.setVisibility(View.GONE);
            binding.tabTextBtnColor.setColorFilter(activeColor);
            binding.tabTextBtnBackground.setColorFilter(inactiveColor);
            binding.tabTextBtnFont.setColorFilter(inactiveColor);
            binding.btnTextEditContent.setColorFilter(inactiveColor);
        });

        binding.tabTextBtnBackground.setOnClickListener(v -> {
            binding.textPanelColor.setVisibility(View.GONE);
            binding.textPanelBackground.setVisibility(View.VISIBLE);
            binding.textPanelFont.setVisibility(View.GONE);
            binding.tabTextBtnColor.setColorFilter(inactiveColor);
            binding.tabTextBtnBackground.setColorFilter(activeColor);
            binding.tabTextBtnFont.setColorFilter(inactiveColor);
            binding.btnTextEditContent.setColorFilter(inactiveColor);
        });

        binding.tabTextBtnFont.setOnClickListener(v -> {
            binding.textPanelColor.setVisibility(View.GONE);
            binding.textPanelBackground.setVisibility(View.GONE);
            binding.textPanelFont.setVisibility(View.VISIBLE);
            binding.tabTextBtnColor.setColorFilter(inactiveColor);
            binding.tabTextBtnBackground.setColorFilter(inactiveColor);
            binding.tabTextBtnFont.setColorFilter(activeColor);
            binding.btnTextEditContent.setColorFilter(inactiveColor);
        });

        binding.btnTextEditContent.setOnClickListener(v -> {
            if (currentSelectedTextViewRoot != null) {
                TextView tv = findTextView(currentSelectedTextViewRoot);
                if (tv != null) {
                    showContentEditDialog(currentSelectedTextViewRoot, tv.getText().toString());
                }
            }
            binding.tabTextBtnColor.setColorFilter(inactiveColor);
            binding.tabTextBtnBackground.setColorFilter(inactiveColor);
            binding.tabTextBtnFont.setColorFilter(inactiveColor);
            binding.btnTextEditContent.setColorFilter(activeColor);
        });

        binding.btnTextStylingClose.setOnClickListener(v -> hideStylingPanel());
        binding.btnTextStylingDone.setOnClickListener(v -> hideStylingPanel());
    }

    public void addDefaultText() {
        TextStyleBuilder styleBuilder = new TextStyleBuilder();
        styleBuilder.withTextColor(Color.WHITE);
        styleBuilder.withTextFont(Typeface.DEFAULT);
        photoEditor.addText("Text", styleBuilder);
        binding.textStylingPanel.setVisibility(View.VISIBLE);
    }

    public void openTextStylingPanel(View view) {
        currentSelectedTextViewRoot = view;
        binding.textStylingPanel.setVisibility(View.VISIBLE);
        
        TextView tv = findTextView(view);
        if (tv != null) {
            currentTextColor = tv.getCurrentTextColor();
            Typeface tf = tv.getTypeface();
            if (tf != null) {
                isTextBold = tf.isBold();
                isTextItalic = tf.isItalic();
                currentBaseTypeface = tf;
            }
            if (tv.getBackground() instanceof ColorDrawable) {
                currentTextBgColor = ((ColorDrawable) tv.getBackground()).getColor();
            } else {
                currentTextBgColor = Color.TRANSPARENT;
            }
            isTextUnderline = (tv.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) != 0;

            // Update UI
            int activeColor = ContextCompat.getColor(activity, R.color.brand_green);
            binding.btnStyleBold.setTextColor(isTextBold ? activeColor : Color.WHITE);
            binding.btnStyleItalic.setTextColor(isTextItalic ? activeColor : Color.WHITE);
            binding.btnStyleUnderline.setTextColor(isTextUnderline ? activeColor : Color.WHITE);
        }
    }

    private TextView findTextView(View root) {
        if (root instanceof TextView) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView tv = findTextView(vg.getChildAt(i));
                if (tv != null) return tv;
            }
        }
        return null;
    }

    private void applyStylesToCurrentText() {
        if (currentSelectedTextViewRoot == null) return;
        TextView tv = findTextView(currentSelectedTextViewRoot);
        String text = tv != null ? tv.getText().toString() : "";
        photoEditor.editText(currentSelectedTextViewRoot, text, buildStyle(currentTextColor, currentTextBgColor, isTextBold, isTextItalic, isTextUnderline, currentBaseTypeface));
    }

    private void showContentEditDialog(final View view, String text) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_add_text_dialog); 
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (dialog.getWindow() != null) {
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }

        final EditText etAddText = dialog.findViewById(R.id.etAddText);
        final TextView btnBoxOk = dialog.findViewById(R.id.btnBoxOk);
        final TextView btnBoxCancel = dialog.findViewById(R.id.btnBoxCancel);
        
        etAddText.setText(text);
        etAddText.requestFocus();

        btnBoxOk.setOnClickListener(v -> {
            photoEditor.editText(view, etAddText.getText().toString(), buildStyle(currentTextColor, currentTextBgColor, isTextBold, isTextItalic, isTextUnderline, currentBaseTypeface));
            hideKeyboard(etAddText);
            dialog.dismiss();
        });

        btnBoxCancel.setOnClickListener(v -> {
            hideKeyboard(etAddText);
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setAttributes(lp);
    }

    private TextStyleBuilder buildStyle(int color, int bgColor, boolean bold, boolean italic, boolean underline, Typeface baseTf) {
        TextStyleBuilder sb = new TextStyleBuilder();
        sb.withTextColor(color);
        if (bgColor != Color.TRANSPARENT) sb.withBackgroundColor(bgColor);
        int style = Typeface.NORMAL;
        if (bold && italic) style = Typeface.BOLD_ITALIC;
        else if (bold) style = Typeface.BOLD;
        else if (italic) style = Typeface.ITALIC;
        
        Typeface tf = baseTf != null ? baseTf : Typeface.DEFAULT;
        sb.withTextFont(Typeface.create(tf, style));

        if (underline) sb.withTextFlag(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        else sb.withTextFlag(Paint.ANTI_ALIAS_FLAG);
        return sb;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void hideStylingPanel() {
        binding.textStylingPanel.setVisibility(View.GONE);
    }
    
    public boolean isPanelVisible() {
        return binding.textStylingPanel.getVisibility() == View.VISIBLE;
    }
}
