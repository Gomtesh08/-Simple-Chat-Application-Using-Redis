package com.chatapp.dto;

import java.util.List;

public record ChatHistoryResponse(List<ChatMessage> messages) {
}
