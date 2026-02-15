package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRoomRequest(
        @NotBlank(message = "roomName is required")
        String roomName
) {
}
