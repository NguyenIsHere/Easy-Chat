package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModeViewHolder> {

    Context context;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModeViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if(model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextview.setText(model.getMessage());
        } else {
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatTextview.setText(model.getMessage());
        }
    }

    @NonNull
    @Override
    public ChatModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModeViewHolder(view);
    }

    public static class ChatModeViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;

        public ChatModeViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
        }
    }
}
