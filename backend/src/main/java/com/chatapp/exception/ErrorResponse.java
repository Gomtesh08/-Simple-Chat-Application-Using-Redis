package com.chatapp.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String status,
        String message,
        Instant timestamp,
        Map<String, String> validationErrors
) {
}
