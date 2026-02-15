package com.chatapp.dto;

public record ChatMessage(
        String participant,
        String message,
        String timestamp
) {
}
