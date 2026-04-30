package com.example.app_xhinh_anh.features.ai_assistant.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.tvMessage.setText(message.getMessage());

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();
        LinearLayout.LayoutParams imgParams = (LinearLayout.LayoutParams) holder.ivMessageImage.getLayoutParams();

        if (message.isUser()) {
            params.gravity = Gravity.END;
            imgParams.gravity = Gravity.END;
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_user);
        } else {
            params.gravity = Gravity.START;
            imgParams.gravity = Gravity.START;
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_ai);
        }
        holder.tvMessage.setLayoutParams(params);
        holder.ivMessageImage.setLayoutParams(imgParams);

        if (message.getImage() != null) {
            holder.ivMessageImage.setVisibility(View.VISIBLE);
            holder.ivMessageImage.setImageBitmap(message.getImage());
        } else {
            holder.ivMessageImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivMessageImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        }
    }
}
