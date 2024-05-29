package com.example.easychat.adapter;


import android.content.Context;
import android.content.Intent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import android.net.Uri;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.io.File;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModeViewHolder> {

    Context context;
    String chatroomId;
    String filename = "";
    @NonNull
    @Override
    public ChatModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModeViewHolder(view);
    }

    public static class ChatModeViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout leftChatLayout, rightChatLayout, leftGroupLayout, rightGroupLayout, subRightGroupLayout;
        TextView leftChatTextview, rightChatTextview;
        ImageView leftChatImageView;
        TextView seenMessageTextview;
        public TextView deleteMessageTextview;

        public ChatModeViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            // Contain the leftChatImageView, leftChatLayout and leftChatTextview
            leftGroupLayout = itemView.findViewById(R.id.left_group_layout);

            // Contain the rightChatLayout and rightChatTextview
            rightGroupLayout = itemView.findViewById(R.id.right_group_layout);

            // Contain the rightGroupLayout and seenMessageTextview
            subRightGroupLayout = itemView.findViewById(R.id.sub_right_group_layout);

            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftChatImageView = itemView.findViewById(R.id.left_chat_imageview);
            seenMessageTextview = itemView.findViewById(R.id.seen_message_textview);
            deleteMessageTextview = itemView.findViewById(R.id.delete_message_textview);
        }
    }

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, String chatroomId) {
        super(options);
        this.context = context;
        this.chatroomId = chatroomId;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModeViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.rightGroupLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextview.setText(model.getMessage());
            holder.leftGroupLayout.setVisibility(View.GONE);
            //check message on sender view
            holder.seenMessageTextview.setVisibility(View.VISIBLE);
            Task<DocumentSnapshot> reference = FirebaseUtil.getChatroomMessagesReference(chatroomId)
                    .document(model.getMessageId())
                    .get().addOnCompleteListener(task -> {
                        Boolean isSeen;
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot.exists()) {
                                isSeen = snapshot.getBoolean("seen");
                                if (isSeen) {
                                    holder.seenMessageTextview.setText("Seen");
                                } else {
                                    holder.seenMessageTextview.setText("Delivered");
                                }
                            } else {
                                task.getException().printStackTrace();
                            }
                        }
                    });
            holder.rightChatLayout.setOnLongClickListener(v -> {
                holder.deleteMessageTextview.setVisibility(View.VISIBLE);
                holder.deleteMessageTextview.setOnClickListener(v1 -> {
                    deleteMessage(model.getMessageId());
                    holder.deleteMessageTextview.setVisibility(View.GONE);
                });
                return true;
            });

            holder.rightChatLayout.setOnClickListener(view -> {
                downloadFile(model.getMessageId());
            });
        } else {
            holder.rightGroupLayout.setVisibility(View.GONE);
            holder.leftGroupLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextview.setText(model.getMessage());
            // Get the sender's profile picture URL
            FirebaseUtil.getOtherProfilePicReference(model.getSenderId()).getDownloadUrl().addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    Uri uri = t.getResult();

                    // Use Glide to load the profile picture into the ImageView
                    Glide.with(holder.leftChatImageView.getContext())
                            .load(uri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(holder.leftChatImageView);
                }
            });
            holder.leftChatLayout.setOnClickListener(view -> {
                downloadFile(model.getMessageId());
            });
        }
    }

    void deleteMessage(String messageId) {
        try {
            FirebaseUtil.getChatroomMessagesReference(chatroomId).document(messageId).delete();
        } catch (Exception e) {
            AndroidUtil.showToast(context, "Failed deleting message");
        }
    }

    public StorageReference getReference(String chatroomId, String messageID){
       return FirebaseStorage.getInstance().getReference().child("file").child(chatroomId).child(messageID);
    }

    public void downloadFile(String messageID){
        StorageReference pathReference = getReference(chatroomId, messageID);
        Log.d("eMetadate", pathReference.toString());
        pathReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddHHmmss");
                String formattedDateTime = currentDateTime.format(formatter);

                filename = storageMetadata.getCustomMetadata("File_name") ;
                String[] temp = filename.split("\\.");
                try{
                    filename = temp[0]+"_"+formattedDateTime + "." + temp[1];
                }catch (Exception e){
                    filename = temp[0]+"_"+formattedDateTime;
                }


                Log.d("eMetadate1", formattedDateTime);
                File rootPath = new File(Environment.getExternalStorageDirectory(), "Download");

                Log.d("eMetadate2", filename);
                final File localFile = new File(rootPath,filename);

                pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(context, "File "+filename+" downloaded", Toast.LENGTH_SHORT).show();
                        Log.d("eMetadate", "new file");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                return;
            }
        });
    }

    private DocumentSnapshot lastVisible;

    public void loadInitialMessages() {
        Query query = FirebaseUtil.getChatroomMessagesReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();

        // Use this options to initialize your adapter

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (!documents.isEmpty()) {
                    lastVisible = documents.get(documents.size() - 1);
                }
                Log.d("LoadInitialMessages", "Loaded " + documents.size() + " messages");
            }
        });
    }

    public void loadMoreMessages() {

        if (lastVisible == null) {
            // No more documents to load
            return;
        }

        Query query = FirebaseUtil.getChatroomMessagesReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(20);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (!documents.isEmpty()) {
                    lastVisible = documents.get(documents.size() - 1);
                    // Add the new messages to your adapter
                }
                Log.d("LoadMoreMessages", "Loaded " + documents.size() + " messages");
            }
        });
    }
}