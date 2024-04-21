package com.example.easychat.model;

import com.google.firebase.Timestamp;

public class ChatMessageModel {
    private String message;
    private String senderId;
    private Timestamp timestamp;
    private String messageId;

    //variable for checking if message has seen
    private String receiverId;
    private Boolean seen;

    public ChatMessageModel() {
    }
    public ChatMessageModel(String message, String senderId, Timestamp timestamp, String messageId, String receiverId, Boolean seen) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.messageId = messageId;
        this.receiverId = receiverId;
        this.seen = seen;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

}
