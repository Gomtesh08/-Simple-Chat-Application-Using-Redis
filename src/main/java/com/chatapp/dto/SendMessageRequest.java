package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "participant is required")
        String participant,
        @NotBlank(message = "message is required")
        String message
) {
}
