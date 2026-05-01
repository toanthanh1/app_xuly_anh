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
import com.example.app_xhinh_anh.ui.editor.ColorPickerAdapter;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;

public class TextManager {

    private final AppCompatActivity activity;
    private final PhotoEditor photoEditor;

    private LinearLayout textStylingPanel;
    private View currentSelectedTextViewRoot;
    private int currentTextColor = Color.WHITE;
    private int currentTextBgColor = Color.TRANSPARENT;
    private boolean isTextBold = false;
    private boolean isTextItalic = false;
    private boolean isTextUnderline = false;
    private Typeface currentBaseTypeface = Typeface.DEFAULT;

    public TextManager(AppCompatActivity activity, PhotoEditor photoEditor) {
        this.activity = activity;
        this.photoEditor = photoEditor;
        initViews();
    }

    private void initViews() {
        textStylingPanel = activity.findViewById(R.id.textStylingPanel);
        setupTextStylingPanel();
    }

    private void setupTextStylingPanel() {
        RecyclerView rvTextColors = activity.findViewById(R.id.rvTextColors);
        RecyclerView rvTextBgColors = activity.findViewById(R.id.rvTextBgColors);
        LinearLayout textPanelColor = activity.findViewById(R.id.textPanelColor);
        LinearLayout textPanelBackground = activity.findViewById(R.id.textPanelBackground);
        LinearLayout textPanelFont = activity.findViewById(R.id.textPanelFont);
        ImageButton tabTextBtnColor = activity.findViewById(R.id.tabTextBtnColor);
        ImageButton tabTextBtnBackground = activity.findViewById(R.id.tabTextBtnBackground);
        ImageButton tabTextBtnFont = activity.findViewById(R.id.tabTextBtnFont);
        ImageButton btnTextEditContent = activity.findViewById(R.id.btnTextEditContent);
        ImageButton btnTextStylingClose = activity.findViewById(R.id.btnTextStylingClose);
        ImageButton btnTextStylingDone = activity.findViewById(R.id.btnTextStylingDone);
        
        TextView btnStyleBold = activity.findViewById(R.id.btnStyleBold);
        TextView btnStyleItalic = activity.findViewById(R.id.btnStyleItalic);
        TextView btnStyleUnderline = activity.findViewById(R.id.btnStyleUnderline);
        LinearLayout textFontList = activity.findViewById(R.id.textFontList);

        final int activeColor = ContextCompat.getColor(activity, R.color.brand_green);
        final int inactiveColor = Color.parseColor("#888888");

        // Color Adapters
        rvTextColors.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        ColorPickerAdapter textCP = new ColorPickerAdapter(activity);
        textCP.setOnColorPickerClickListener(color -> {
            currentTextColor = color;
            applyStylesToCurrentText();
        });
        rvTextColors.setAdapter(textCP);

        rvTextBgColors.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        ColorPickerAdapter bgCP = new ColorPickerAdapter(activity);
        bgCP.setOnColorPickerClickListener(color -> {
            currentTextBgColor = color;
            applyStylesToCurrentText();
        });
        rvTextBgColors.setAdapter(bgCP);

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
            TextView fontView = (TextView) inflater.inflate(R.layout.item_filter_category_tab, textFontList, false);
            fontView.setText(fontNames[i]);
            fontView.setOnClickListener(v -> {
                currentBaseTypeface = tf != null ? tf : Typeface.DEFAULT;
                applyStylesToCurrentText();
            });
            textFontList.addView(fontView);
        }

        btnStyleBold.setOnClickListener(v -> {
            isTextBold = !isTextBold;
            btnStyleBold.setTextColor(isTextBold ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });
        btnStyleItalic.setOnClickListener(v -> {
            isTextItalic = !isTextItalic;
            btnStyleItalic.setTextColor(isTextItalic ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });
        btnStyleUnderline.setOnClickListener(v -> {
            isTextUnderline = !isTextUnderline;
            btnStyleUnderline.setTextColor(isTextUnderline ? activeColor : Color.WHITE);
            applyStylesToCurrentText();
        });

        // Tabs
        tabTextBtnColor.setOnClickListener(v -> {
            textPanelColor.setVisibility(View.VISIBLE);
            textPanelBackground.setVisibility(View.GONE);
            textPanelFont.setVisibility(View.GONE);
            tabTextBtnColor.setColorFilter(activeColor);
            tabTextBtnBackground.setColorFilter(inactiveColor);
            tabTextBtnFont.setColorFilter(inactiveColor);
            btnTextEditContent.setColorFilter(inactiveColor);
        });

        tabTextBtnBackground.setOnClickListener(v -> {
            textPanelColor.setVisibility(View.GONE);
            textPanelBackground.setVisibility(View.VISIBLE);
            textPanelFont.setVisibility(View.GONE);
            tabTextBtnColor.setColorFilter(inactiveColor);
            tabTextBtnBackground.setColorFilter(activeColor);
            tabTextBtnFont.setColorFilter(inactiveColor);
            btnTextEditContent.setColorFilter(inactiveColor);
        });

        tabTextBtnFont.setOnClickListener(v -> {
            textPanelColor.setVisibility(View.GONE);
            textPanelBackground.setVisibility(View.GONE);
            textPanelFont.setVisibility(View.VISIBLE);
            tabTextBtnColor.setColorFilter(inactiveColor);
            tabTextBtnBackground.setColorFilter(inactiveColor);
            tabTextBtnFont.setColorFilter(activeColor);
            btnTextEditContent.setColorFilter(inactiveColor);
        });

        btnTextEditContent.setOnClickListener(v -> {
            if (currentSelectedTextViewRoot != null) {
                TextView tv = findTextView(currentSelectedTextViewRoot);
                if (tv != null) {
                    showContentEditDialog(currentSelectedTextViewRoot, tv.getText().toString());
                }
            }
            tabTextBtnColor.setColorFilter(inactiveColor);
            tabTextBtnBackground.setColorFilter(inactiveColor);
            tabTextBtnFont.setColorFilter(inactiveColor);
            btnTextEditContent.setColorFilter(activeColor);
        });

        btnTextStylingClose.setOnClickListener(v -> hideStylingPanel());
        btnTextStylingDone.setOnClickListener(v -> hideStylingPanel());
    }

    public void addDefaultText() {
        TextStyleBuilder styleBuilder = new TextStyleBuilder();
        styleBuilder.withTextColor(Color.WHITE);
        styleBuilder.withTextFont(Typeface.DEFAULT);
        photoEditor.addText("Text", styleBuilder);
        textStylingPanel.setVisibility(View.VISIBLE);
    }

    public void openTextStylingPanel(View view) {
        currentSelectedTextViewRoot = view;
        textStylingPanel.setVisibility(View.VISIBLE);
        
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
            ((TextView)activity.findViewById(R.id.btnStyleBold)).setTextColor(isTextBold ? activeColor : Color.WHITE);
            ((TextView)activity.findViewById(R.id.btnStyleItalic)).setTextColor(isTextItalic ? activeColor : Color.WHITE);
            ((TextView)activity.findViewById(R.id.btnStyleUnderline)).setTextColor(isTextUnderline ? activeColor : Color.WHITE);
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
        textStylingPanel.setVisibility(View.GONE);
    }
    
    public boolean isPanelVisible() {
        return textStylingPanel.getVisibility() == View.VISIBLE;
    }
}
