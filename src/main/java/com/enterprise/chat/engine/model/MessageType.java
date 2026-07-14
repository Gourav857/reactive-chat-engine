package com.enterprise.chat.engine.model;

public enum MessageType {
    JOIN,    // User comes online / connects to a specific room
    CHAT,    // Normal text message packet delivery
    LEAVE    // User disconnects / session terminates
}