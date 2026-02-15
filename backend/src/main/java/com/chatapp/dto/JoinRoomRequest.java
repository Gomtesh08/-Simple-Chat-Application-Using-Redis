package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank(message = "participant is required")
        String participant
) {
}
