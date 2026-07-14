package com.enterprise.chat.engine.model;

public class ChatMessage {
    private MessageType type;
    private String sender;
    private String roomId;
    private String content;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(MessageType type, String sender, String roomId, String content, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.roomId = roomId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Explicit Getters and Setters (Zero build failures)
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}