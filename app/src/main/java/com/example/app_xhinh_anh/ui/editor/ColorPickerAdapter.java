package com.example.app_xhinh_anh.ui.editor;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_xhinh_anh.R;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private final List<Integer> colorList;
    private OnColorPickerClickListener onColorPickerClickListener;

    public ColorPickerAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
        this.colorList = getDefaultColors();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_color, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int color = colorList.get(position);
        holder.colorView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return colorList.size();
    }

    public void setOnColorPickerClickListener(OnColorPickerClickListener onColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;

        ViewHolder(View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.colorView);
            itemView.setOnClickListener(v -> {
                if (onColorPickerClickListener != null) {
                    onColorPickerClickListener.onColorPickerClick(colorList.get(getAdapterPosition()));
                }
            });
        }
    }

    public interface OnColorPickerClickListener {
        void onColorPickerClick(int colorCode);
    }

    private List<Integer> getDefaultColors() {
        List<Integer> colors = new ArrayList<>();
        // Monochrome / Grayscale
        colors.add(Color.TRANSPARENT);
        colors.add(Color.parseColor("#000000"));
        colors.add(Color.parseColor("#212121"));
        colors.add(Color.parseColor("#424242"));
        colors.add(Color.parseColor("#616161"));
        colors.add(Color.parseColor("#757575"));
        colors.add(Color.parseColor("#9E9E9E"));
        colors.add(Color.parseColor("#BDBDBD"));
        colors.add(Color.parseColor("#E0E0E0"));
        colors.add(Color.parseColor("#EEEEEE"));
        colors.add(Color.parseColor("#FFFFFF"));

        // Browns/Warm
        colors.add(Color.parseColor("#3E2723"));
        colors.add(Color.parseColor("#4E342E"));
        colors.add(Color.parseColor("#5D4037"));
        colors.add(Color.parseColor("#6D4C41"));
        colors.add(Color.parseColor("#795548"));
        colors.add(Color.parseColor("#8D6E63"));
        colors.add(Color.parseColor("#A1887F"));

        // Blues
        colors.add(Color.parseColor("#E3F2FD"));
        colors.add(Color.parseColor("#BBDEFB"));
        colors.add(Color.parseColor("#90CAF9"));
        colors.add(Color.parseColor("#64B5F6"));
        colors.add(Color.parseColor("#42A5F5"));
        colors.add(Color.parseColor("#2196F3"));
        colors.add(Color.parseColor("#1E88E5"));
        colors.add(Color.parseColor("#1976D2"));
        colors.add(Color.parseColor("#1565C0"));
        colors.add(Color.parseColor("#0D47A1"));

        // Others
        colors.add(Color.parseColor("#F44336"));
        colors.add(Color.parseColor("#E91E63"));
        colors.add(Color.parseColor("#9C27B0"));
        colors.add(Color.parseColor("#673AB7"));
        colors.add(Color.parseColor("#3F51B5"));
        colors.add(Color.parseColor("#03A9F4"));
        colors.add(Color.parseColor("#00BCD4"));
        colors.add(Color.parseColor("#009688"));
        colors.add(Color.parseColor("#4CAF50"));
        colors.add(Color.parseColor("#8BC34A"));
        colors.add(Color.parseColor("#CDDC39"));
        colors.add(Color.parseColor("#FFEB3B"));
        colors.add(Color.parseColor("#FFC107"));
        colors.add(Color.parseColor("#FF9800"));
        colors.add(Color.parseColor("#FF5722"));
        return colors;
    }
}
