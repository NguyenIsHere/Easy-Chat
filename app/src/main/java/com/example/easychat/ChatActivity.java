package com.example.easychat;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.easychat.adapter.ChatRecyclerAdapter;
import com.example.easychat.adapter.SearchUserRecyclerAdapter;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageMetadata;
import com.google.protobuf.NullValue;

import com.google.protobuf.NullValue;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;


import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ChatRecyclerAdapter adapter;
    ImageView imageView;

    String messageId;
    static boolean inChat;

    ImageButton uploadBtn;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //get UserModel
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(Objects.requireNonNull(FirebaseUtil.currentUserId()), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
        uploadBtn = findViewById(R.id.upload_btn);

        FirebaseApp.initializeApp(ChatActivity.this);


        FirebaseUtil.getOtherProfilePicReference(otherUser.getUserId()).getDownloadUrl().addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Uri uri = t.getResult();
                AndroidUtil.setProfilePic(this, uri, imageView);
            }
        });

        //Change here to upload
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        backBtn.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            ;
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty() && uri == null) {
                return;
            } else if (message.isEmpty()) {
                String path = uri.getPath();

                String[] str = path.split("/");
                message = str[str.length - 1];
                sendMessageToUser(message);
                uri = null;
            } else {
                sendMessageToUser(message);
            }
        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
        // what is this
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    void setupChatRecyclerView() {
        // Tạo câu truy vấn để lấy tin nhắn từ Firestore
        Query query = FirebaseUtil.getChatroomMessagesReference(chatroomId).orderBy("timestamp", Query.Direction.DESCENDING);
        // Tạo tùy chọn cho FirestoreRecyclerAdapter
        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();
        // Tạo adapter cho RecyclerView
        adapter = new ChatRecyclerAdapter(options, getApplicationContext(), chatroomId);
        // Tạo và thiết lập LinearLayoutManager cho RecyclerView
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    for (int i = 0; i < recyclerView.getChildCount(); i++) {
                        View child = recyclerView.getChildAt(i);
                        ChatRecyclerAdapter.ChatModeViewHolder holder = getViewHolder(i);
                        if (holder != null && holder.deleteMessageTextview.getVisibility() == View.VISIBLE) {
                            Rect outRect = new Rect();
                            holder.rightChatLayout.getGlobalVisibleRect(outRect);
                            if (!outRect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                                holder.deleteMessageTextview.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                return false;
            }
        });

        // Bắt đầu lắng nghe thay đổi từ Firestore
        adapter.startListening();
        // Đăng ký observer cho adapter để lắng nghe sự kiện thêm item
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkLast();
            }
        });
    }

    void getOrCreateChatroomModel() {
        // get or create chatroom model
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    // first time chat
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }

    void sendMessageToUser(String message) {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        DocumentReference newMessage = FirebaseUtil.getChatroomMessagesReference(chatroomId).document();
        messageId = newMessage.getId();
        chatroomModel.setLastMessageId(messageId);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now(), messageId, otherUser.getUserId(), false);
        if (uri != null) {
            uploadFile(uri, messageId);
        }
        newMessage.set(chatMessageModel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");
                sendNotification(message);
            }
        });
    }

    void sendNotification(String message) {
        // current username, message, current userid, other user token
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try {
                    JSONObject jsonObject = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());

                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());

                    callApi(jsonObject);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer AAAAFwQ4qog:APA91bHeiZW-ENxXtIu6SaS1q4eZE6lxo8gHXWtwpNW1SpCTmnlAAW9XoA_8JwCn3AUirmL9PNoYBTv-4r5eKHwa-z4cPD9r7BNuagWGIaDYp5C_mhk8_CtX9b2IInzANdopZF4Vuz7C")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }

    void checkLast() {
        AggregateQuery countQuery = FirebaseUtil.getChatroomMessagesReference(chatroomId).count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Count fetched successfully
                AggregateQuerySnapshot snapshot = task.getResult();
                // Check there no more documents in collection
                if (snapshot.getCount() == 0) {
                    chatroomModel.setLastMessageTimestamp(Timestamp.now());
                    chatroomModel.setLastMessageSenderId("");
                    chatroomModel.setLastMessage("");
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                } else {
                    // Get last message by timestamp
                    Query query = FirebaseUtil.getChatroomMessagesReference(chatroomId)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1);
                    query.get().addOnCompleteListener(task_2 -> {
                        if (task_2.isSuccessful()) {
                            QuerySnapshot snapshot_2 = task_2.getResult();
                            ChatMessageModel lastMessage = snapshot_2.getDocuments().get(0).toObject(ChatMessageModel.class);
                            // Compare last message in database with client model message
                            if (!Objects.equals(chatroomModel.getLastMessageId(), lastMessage.getMessageId())) {
                                chatroomModel.setLastMessageTimestamp(lastMessage.getTimestamp());
                                chatroomModel.setLastMessageSenderId(lastMessage.getSenderId());
                                chatroomModel.setLastMessage(lastMessage.getMessage());
                                chatroomModel.setLastMessageId(lastMessage.getMessageId());
                                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                            }
                        }
                    });
                }
            }
        });
    }

    void seenMessage() {
        CollectionReference reference = FirebaseUtil.getChatroomMessagesReference(chatroomId);
        reference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    error.printStackTrace();
                }
                for (DocumentSnapshot snapshot : value.getDocuments()) {
                    // nguoi 1 send message nguoi 2 thi phai kiem tra nguoc nhau
                    if (snapshot.getString("senderId").equals(otherUser.getUserId())
                            && snapshot.getString("receiverId").equals(FirebaseUtil.currentUserId())
                            && inChat) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("seen", true);
                        snapshot.getReference().update(map);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        inChat = true;
        seenMessage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        inChat = false;
    }


    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100);
        } catch (Exception exception) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
            String path = uri.getPath();
            String extension = "";

            int i = path.lastIndexOf('.');
            if (i > 0) {
                extension = path.substring(i+1);
            }
            //Log.d("Myapp", extension);

            super.onActivityResult(requestCode, resultCode, data);

        }

    }

    void uploadFile(Uri uri, String messageId){

        StorageReference reference = adapter.getReference(chatroomId, messageId);
        String file_name = uri.getPath();
        String[] temp = file_name.split("/");
        file_name = temp[temp.length-1];

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("File_name", file_name).build();

        reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ChatActivity.this, "file uploaded", Toast.LENGTH_SHORT).show();
                 reference.updateMetadata(metadata);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChatActivity.this, "Failure to upload", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public ChatRecyclerAdapter.ChatModeViewHolder getViewHolder(int position) {
        return (ChatRecyclerAdapter.ChatModeViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
    }

}