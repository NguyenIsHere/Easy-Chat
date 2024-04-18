package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModeViewHolder> {

    Context context;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModeViewHolder holder, int position, @NonNull UserModel model) {
        holder.usernameText.setText(model.getUsername());
        holder.phoneText.setText(model.getPhone());
        if (model.getUserId().equals(FirebaseUtil.currentUserId())) {
            holder.usernameText.setText(model.getUsername() + " (You)");
        }

        FirebaseUtil.getOtherProfilePicReference(model.getUserId()).getDownloadUrl().addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Uri uri = t.getResult();
                AndroidUtil.setProfilePic(context, uri, holder.profilePic);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            // navigate to chat activity
            Intent intent = new Intent(context, ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent, model);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false);
        return new UserModeViewHolder(view);
    }

    public static class UserModeViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneText;
        ImageView profilePic;

        public UserModeViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
